/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.jobs;

import android.text.TextUtils;

import com.birbit.android.jobqueue.Params;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.data.local.CounterDao;
import ch.protonmail.android.data.local.CounterDatabase;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.UnreadLocationCounter;
import ch.protonmail.android.labels.domain.model.Label;
import ch.protonmail.android.labels.domain.model.LabelId;
import ch.protonmail.android.labels.domain.model.LabelType;
import timber.log.Timber;

@Deprecated // replaced with MoveMessageToLocationWorker
public class PostTrashJobV2 extends ProtonMailCounterJob {

    private final List<String> mMessageIds;
    private final List<String> mFolderIds;
    private final String mLabelId;

    public PostTrashJobV2(final List<String> messageIds, String labelId) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
        mFolderIds = null;
        mLabelId = labelId;
    }

    @Override
    public void onAdded() {
        Timber.v("Post to Trash ids: %s onAdded", mMessageIds);
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext(), getUserId())
                .getDao();
        int totalUnread = 0;
        for (String id : mMessageIds) {
            Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                if (!message.isRead()) {
                    UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationByIdBlocking(message.getLocation());
                    if (unreadLocationCounter != null) {
                        unreadLocationCounter.decrement();
                        counterDao.insertUnreadLocationBlocking(unreadLocationCounter);
                    }
                    totalUnread++;
                }
                Constants.MessageLocationType location = Constants.MessageLocationType.Companion.fromInt(message.getLocation());
                if (location == Constants.MessageLocationType.SENT || location == Constants.MessageLocationType.ALL_SENT) {
                    message.setLocation(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue());
                    message.addLabels(Collections.singletonList(String.valueOf(Constants.MessageLocationType.ALL_SENT.getMessageLocationTypeValue())));
                    message.removeLabels(Collections.singletonList(String.valueOf(Constants.MessageLocationType.SENT.getMessageLocationTypeValue())));
                } else {
                    message.setLocation(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue());
                    message.addLabels(Collections.singletonList(String.valueOf(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue())));
                    if (!TextUtils.isEmpty(mLabelId)) {
                        message.removeLabels(Collections.singletonList(mLabelId));
                    }
                    removeOldFolderIds(message);
                }
                if (mFolderIds != null) {
                    for (String folderId : mFolderIds) {
                        if (!TextUtils.isEmpty(folderId)) {
                            message.removeLabels(Collections.singletonList(folderId));
                        }
                    }
                }
                getMessageDetailsRepository().saveMessageBlocking(message);
            }
        }

        UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationByIdBlocking(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue());
        if (unreadLocationCounter == null) {
            return;
        }
        unreadLocationCounter.increment(totalUnread);
        counterDao.insertUnreadLocationBlocking(unreadLocationCounter);

    }

    private void removeOldFolderIds(Message message) {
        List<String> oldLabels = message.getAllLabelIDs();
        ArrayList<String> labelsToRemove = new ArrayList<>();

        for (String labelId : oldLabels) {
            Label label = getLabelRepository().findLabelBlocking(new LabelId(labelId));
            // find folders
            if (label != null && (label.getType() == LabelType.FOLDER) && !label.getId().equals(String.valueOf(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue()))) {
                labelsToRemove.add(labelId);
            }
        }

        message.removeLabels(labelsToRemove);
    }

    @Override
    public void onRun() {
        List<String> messageIds = new ArrayList<>(mMessageIds);
        getApi().labelMessages(new IDList(String.valueOf(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue()), messageIds));
    }

    @Override
    protected List<String> getMessageIds() {
        return new ArrayList<>(mMessageIds);
    }

    @Override
    protected Constants.MessageLocationType getMessageLocation() {
        return Constants.MessageLocationType.TRASH;
    }
}
