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

import com.birbit.android.jobqueue.Params;

import ch.protonmail.android.core.Constants;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.events.FetchDraftDetailEvent;
import ch.protonmail.android.utils.AppUtil;
import timber.log.Timber;

public class FetchDraftDetailJob extends ProtonMailBaseJob {

    private final String mMessageId;

    public FetchDraftDetailJob(final String messageId) {
        super(new Params(Priority.LOW).requireNetwork().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageId = messageId;

        if (!getQueueNetworkUtil().isConnected()) {
            Timber.d("no network - cannot fetch draft detail");
            AppUtil.postEventOnUi(new FetchDraftDetailEvent(false));
        }
    }

    @Override
    public void onRun() throws Throwable {
        try {
            final Message message = getApi().fetchMessageDetailsBlocking(mMessageId).getMessage();
            Message savedMessage = getMessageDetailsRepository().findMessageByIdBlocking(message.getMessageId());
            if (savedMessage != null) {
                message.setInline(savedMessage.isInline());
            }
            message.setDownloaded(true);
            long messageDbId = getMessageDetailsRepository().saveMessageBlocking(message);
            final FetchDraftDetailEvent event = new FetchDraftDetailEvent(true);
            // we need to re-query MessageRepository, because after saving, messageBody may be replaced with uri to file
            event.setMessage(getMessageDetailsRepository().findMessageByDatabaseIdBlocking(messageDbId));
            AppUtil.postEventOnUi(event);
        } catch (Exception e) {
            AppUtil.postEventOnUi(new FetchDraftDetailEvent(false));
            throw e;
        }
    }

    @Override
    protected int getRetryLimit() {
        return 0;
    }
}
