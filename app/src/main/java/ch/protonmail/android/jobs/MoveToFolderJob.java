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

@Deprecated // replaced with [PostToLocationWorker]
public class MoveToFolderJob extends ProtonMailBaseJob {
    private List<String> mMessageIds;
    private String mLabelId;

    public MoveToFolderJob(List<String> messageIds, String labelId) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL));
        this.mMessageIds = messageIds;
        this.mLabelId = labelId;
    }

    @Override
    public void onAdded() {
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext(), getUserId())
                .getDao();
        int totalUnread = 0;
        for (String id : mMessageIds) {
            final Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                if (markMessageLocally(counterDao, message)) {
                    totalUnread++;
                }
            }
        }

        UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationByIdBlocking(Constants.MessageLocationType.SPAM.getMessageLocationTypeValue());
        if (unreadLocationCounter == null) {
            return;
        }
        unreadLocationCounter.increment(totalUnread);
        counterDao.insertUnreadLocationBlocking(unreadLocationCounter);
    }

    private boolean markMessageLocally(CounterDao counterDao, Message message) {
        boolean unreadIncrease = false;


        if (!TextUtils.isEmpty(mLabelId)) {
            message.addLabels(Collections.singletonList(mLabelId));
            removeOldFolderIds(message);
        }

        if (!message.isRead()) {
            UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationByIdBlocking(message.getLocation());
            if (unreadLocationCounter != null) {
                unreadLocationCounter.decrement();
                counterDao.insertUnreadLocationBlocking(unreadLocationCounter);
            }
            unreadIncrease = true;
        }
        Constants.MessageLocationType location = Constants.MessageLocationType.Companion.fromInt(message.getLocation());
        if (location == Constants.MessageLocationType.SENT || location == Constants.MessageLocationType.ALL_SENT) {
            message.addLabels(Collections.singletonList(mLabelId));
        } else {
            message.setLocation(Constants.MessageLocationType.LABEL_FOLDER.getMessageLocationTypeValue());
        }

        message.setFolderLocation(getLabelRepository());
        Timber.d("Move message id: %s, location: %s, labels: %s", message.getMessageId(), message.getLocation(), message.getAllLabelIDs());
        getMessageDetailsRepository().saveMessageBlocking(message);
        return unreadIncrease;
    }

    private void removeOldFolderIds(Message message) {
        List<String> oldLabels = message.getAllLabelIDs();
        ArrayList<String> labelsToRemove = new ArrayList<>();

        for (String labelId : oldLabels) {
            Label label = getLabelRepository().findLabelBlocking(new LabelId(labelId));
            // find folders
            if (label != null && (label.getType() == LabelType.FOLDER) && !label.getId().equals(mLabelId)) {
                labelsToRemove.add(labelId);
            }
        }

        message.removeLabels(labelsToRemove);
    }

    @Override
    public void onRun() throws Throwable {
        IDList body = new IDList(mLabelId, mMessageIds);
        getApi().labelMessages(body);
    }
}
