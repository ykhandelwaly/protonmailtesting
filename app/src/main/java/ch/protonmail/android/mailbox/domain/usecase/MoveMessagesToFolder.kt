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

package ch.protonmail.android.mailbox.domain.usecase

import ch.protonmail.android.core.Constants
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import javax.inject.Inject

class MoveMessagesToFolder @Inject constructor(
    private val messagesRepository: MessageRepository,
    private val conversationsRepository: ConversationsRepository
) {

    suspend operator fun invoke(
        messageIds: List<String>,
        newFolderLocationId: String,
        currentFolderLabelId: String = EMPTY_STRING,
        userId: UserId,
    ) {
        Timber.v("Move to folder: $newFolderLocationId")
        when (newFolderLocationId) {
            Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString() ->
                messagesRepository.moveToTrash(messageIds, userId)
            Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString() ->
                messagesRepository.moveToArchive(messageIds, userId)
            Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString() ->
                messagesRepository.moveToInbox(messageIds, userId)
            Constants.MessageLocationType.SPAM.messageLocationTypeValue.toString() ->
                messagesRepository.moveToSpam(messageIds, userId)
            else ->
                messagesRepository.moveToCustomFolderLocation(
                    messageIds,
                    newFolderLocationId,
                    userId
                )
        }
        conversationsRepository.updateConvosBasedOnMessagesLocation(
            userId,
            messageIds,
            currentFolderLabelId,
            newFolderLocationId
        )
    }
}
