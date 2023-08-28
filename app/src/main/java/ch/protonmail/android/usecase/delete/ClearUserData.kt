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

package ch.protonmail.android.usecase.delete

import android.content.Context
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.storage.AttachmentClearingService
import ch.protonmail.android.storage.MessageBodyClearingService
import ch.protonmail.android.utils.AppUtil
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

/**
 * Clear all databases for a given user.
 *
 * Note: currently the clearing is sparse in different places:
 *  * Biggest part in [AppUtil.deleteDatabases], where:
 *    * half by an AsyncTask
 *    * half delegated to a Service
 *  * Some other random places in [UserManager] and others
 *
 *  This only aim to replace the AsyncTask in [AppUtil.deleteDatabases] and still delegate other part to a Service.
 *  The Service needs a refactor and ideally extracted here or in a separate Use Case
 */
class ClearUserData @Inject constructor(
    private val context: Context,
    private val databaseProvider: DatabaseProvider,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(userId: UserId, alsoClearContacts: Boolean = true) {

        val attachmentMetadataDao = runCatching { databaseProvider.provideAttachmentMetadataDao(userId) }.getOrNull()
        val contactDao = runCatching { databaseProvider.provideContactDao(userId) }.getOrNull()
        val messageDao = runCatching { databaseProvider.provideMessageDao(userId) }.getOrNull()
        // TODO remove this dependencies and use the ConversationRepository.clear()
        //  right now it creates a circular dependency,
        //  so this needs to happen when this use-case is refactored to use only repositories
        val conversationDao = runCatching { databaseProvider.provideConversationDao(userId) }.getOrNull()
        val counterDao = runCatching { databaseProvider.provideUnreadCounterDao(userId) }.getOrNull()

        val pendingActionDao = runCatching { databaseProvider.providePendingActionDao(userId) }.getOrNull()

        // Ensure that all the queries run on Io thread, as some are still blocking calls
        withContext(dispatchers.Io) {

            attachmentMetadataDao?.clearAttachmentMetadataCache()
            if (alsoClearContacts) {
                contactDao?.run {
                    clearContactEmailsCache()
                    clearContactDataCache()
                    clearFullContactDetailsCache()
                }
            }
            counterDao?.clear()
            messageDao?.run {
                clearMessagesCache()
                clearAttachmentsCache()
            }
            conversationDao?.clear()
            pendingActionDao?.run {
                clearPendingSendCache()
                clearPendingUploadCache()
            }
        }

        startCleaningServices(userId)
    }

    // TODO replace services, options:
//  1) extract in this use case
//  2) extract in separate use case/s
//  3) extract in worker/s
    private fun startCleaningServices(userId: UserId) {
        AttachmentClearingService.startClearUpImmediatelyService(context, userId)
        MessageBodyClearingService.startClearUpService()
    }
}
