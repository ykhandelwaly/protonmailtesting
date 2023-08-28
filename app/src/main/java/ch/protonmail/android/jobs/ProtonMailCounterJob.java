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

import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Params;

import java.util.List;

import ch.protonmail.android.core.Constants;
import ch.protonmail.android.data.local.CounterDao;
import ch.protonmail.android.data.local.CounterDatabase;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.UnreadLocationCounter;

public abstract class ProtonMailCounterJob extends ProtonMailEndlessJob {

    protected ProtonMailCounterJob(Params params) {
        super(params);
    }

    protected abstract List<String> getMessageIds();

    protected abstract Constants.MessageLocationType getMessageLocation();

    @Override
    protected void onProtonCancel(int cancelReason, @Nullable Throwable throwable) {
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext(), getUserId())
                .getDao();

        int totalUnread = 0;
        List<String> messageIds = getMessageIds();
        for (String id : messageIds) {
            final Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                if (!message.isRead()) {
                    UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationByIdBlocking(message.getLocation());
                    if (unreadLocationCounter != null) {
                        unreadLocationCounter.increment();
                        counterDao.insertUnreadLocationBlocking(unreadLocationCounter);
                    }
                    totalUnread++;
                }
            }
        }
        UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationByIdBlocking(getMessageLocation().getMessageLocationTypeValue());

        if (unreadLocationCounter == null) {
            return;
        }
        unreadLocationCounter.decrement(totalUnread);
        counterDao.insertUnreadLocationBlocking(unreadLocationCounter);
    }
}
