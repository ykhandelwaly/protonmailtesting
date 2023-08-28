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
package ch.protonmail.android.utils.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;

import ch.protonmail.android.R;
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity;
import ch.protonmail.android.contacts.details.presentation.ContactDetailsActivity;
import ch.protonmail.android.contacts.details.edit.EditContactDetailsActivity;
import ch.protonmail.android.data.local.ContactDao;
import ch.protonmail.android.data.local.model.ContactEmail;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;

/**
 Created by protonlabs on 5/31/18. */

public class RecipientDropDownClickListener implements View.OnClickListener {

    private final Context mContext;
    private final ContactDao contactDao;
    private final String mRecipientEmail;
    @Nullable
    private OpenContactDetailsTask openContactDetailsTask;

    public RecipientDropDownClickListener(Context context, ContactDao contactDao,
                                          String recipientEmail) {
        mContext = context;
        this.contactDao = contactDao;
        mRecipientEmail = recipientEmail;
    }

    @Override
    public void onClick(View view) {
        PopupMenu popup = new PopupMenu(mContext, view);
        popup.getMenuInflater().inflate(R.menu.recipient_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_compose_to:
                    final Intent intent = AppUtil.decorInAppIntent(new Intent(mContext, ComposeMessageActivity.class));
                    intent.putExtra(ComposeMessageActivity.EXTRA_TO_RECIPIENTS, new String[]{mRecipientEmail});
                    mContext.startActivity(intent);
                    break;
                case R.id.action_copy_address:
                    UiUtil.copy(mContext, mRecipientEmail);
                    TextExtensions.showToast(mContext, R.string.details_copied, Toast.LENGTH_SHORT);
                    break;
                case R.id.action_see_contact_details:
                    OpenContactDetailsTask previous = openContactDetailsTask;
                    if (previous != null) {
                        previous.cancel(true);
                    }

                    OpenContactDetailsTask current = new OpenContactDetailsTask(
                            RecipientDropDownClickListener.this.contactDao,
                            mRecipientEmail, this::openContactDetails);
                    current.execute();
                    openContactDetailsTask = current;
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void openContactDetails(@Nullable ContactEmail contactEmail) {
        if (contactEmail != null && contactEmail.getContactId() != null) {
            Intent contactDetailsIntent = AppUtil.decorInAppIntent(new Intent(mContext, ContactDetailsActivity.class));
            contactDetailsIntent.putExtra(ContactDetailsActivity.EXTRA_ARG_CONTACT_ID, contactEmail.getContactId());
            mContext.startActivity(contactDetailsIntent);
        } else {
            mContext.startActivity(EditContactDetailsActivity.startNewContactActivity(mContext, mRecipientEmail, mRecipientEmail));
        }
    }

    interface ContactEmailListener {
        void onContactEmail(@Nullable ContactEmail contactEmail);
    }

    private static class OpenContactDetailsTask extends AsyncTask<Void,Void,ContactEmail> {

        private final ContactDao contactDao;
        private final String recipientEmail;
        @Nullable
        private ContactEmailListener onContactObtained;

        OpenContactDetailsTask(ContactDao contactDao, String recipientEmail,
                               @NonNull ContactEmailListener onContactObtained) {
            this.contactDao = contactDao;
            this.onContactObtained = onContactObtained;
            this.recipientEmail = recipientEmail;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            onContactObtained = null;
        }

        @Override
        protected ContactEmail doInBackground(Void... voids) {
            return contactDao.findContactEmailByEmailBlocking(recipientEmail);
        }

        @Override
        protected void onPostExecute(ContactEmail contactEmail) {
            ContactEmailListener listener = onContactObtained;
            if (listener != null) {
                listener.onContactEmail(contactEmail);
            }
            onContactObtained = null;
        }
    }
}
