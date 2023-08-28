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
package ch.protonmail.android.jobs.contacts;

import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_RECIPIENT_NOT_FOUND;

import com.birbit.android.jobqueue.Params;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.protonmail.android.api.exceptions.ApiException;
import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.api.models.factories.SendPreferencesFactory;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.contacts.SendPreferencesEvent;
import ch.protonmail.android.jobs.Priority;
import ch.protonmail.android.jobs.ProtonMailBaseJob;
import ch.protonmail.android.utils.AppUtil;
import timber.log.Timber;

public class GetSendPreferenceJob extends ProtonMailBaseJob {

    private final List<String> mEmails;
    private final Destination mDestination;

    public enum Destination {
        TO, CC, BCC
    }

    public GetSendPreferenceJob(List<String> emails, Destination destination) {
        super(new Params(Priority.HIGH).requireNetwork());
		mEmails = emails;
        mDestination = destination;
    }

    @Override
    public void onRun() throws Throwable {
        SendPreferencesFactory factory = new SendPreferencesFactory(
                getApplicationContext(),
                getApi(),
                getUserManager(),
                getUserManager().requireCurrentUserId()
        );
        Map<String, SendPreference> sendPreferenceMap = new HashMap<>();
        sendPreferenceMap.put(mEmails.get(0), null);
        try {
            sendPreferenceMap = factory.fetch(mEmails);
        } catch (ApiException apiException) {
            ResponseBody body = apiException.getResponse();
            if (body.getCode() == RESPONSE_CODE_RECIPIENT_NOT_FOUND) {
                AppUtil.postEventOnUi(new SendPreferencesEvent(Status.SUCCESS, sendPreferenceMap, mDestination, false));
            } else {
                AppUtil.postEventOnUi(new SendPreferencesEvent(Status.FAILED, sendPreferenceMap, mDestination, true));
            }
            return;
        } catch (Exception e) {
            Timber.e(e);
            AppUtil.postEventOnUi(new SendPreferencesEvent(Status.FAILED, sendPreferenceMap, mDestination, false));
            return;
        }
        AppUtil.postEventOnUi(new SendPreferencesEvent(Status.SUCCESS, sendPreferenceMap, mDestination, true));
    }
}
