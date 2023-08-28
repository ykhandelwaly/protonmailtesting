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
package ch.protonmail.android.api.segments.message

import androidx.annotation.WorkerThread
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DeleteResponse
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.MoveToFolderResponse
import ch.protonmail.android.api.models.messages.delete.MessageDeleteRequest
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.api.models.messages.send.MessageSendBody
import ch.protonmail.android.api.models.messages.send.MessageSendResponse
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.mailbox.data.remote.model.CountsResponse
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import me.proton.core.domain.entity.UserId
import java.io.IOException

interface MessageApiSpec {

    suspend fun fetchMessagesCounts(userId: UserId): CountsResponse

    suspend fun getMessages(params: GetAllMessagesParameters): MessagesResponse

    suspend fun fetchMessageMetadata(messageId: String, userIdTag: UserIdTag): MessagesResponse

    @Throws(IOException::class)
    fun markMessageAsRead(messageIds: IDList)

    @Throws(IOException::class)
    fun markMessageAsUnRead(messageIds: IDList)

    suspend fun deleteMessage(messageDeleteRequest: MessageDeleteRequest): DeleteResponse

    suspend fun emptyFolder(userIdTag: UserIdTag, labelId: LabelId)

    @WorkerThread
    @Throws(Exception::class)
    fun fetchMessageDetailsBlocking(messageId: String): MessageResponse

    suspend fun fetchMessageDetails(messageId: String, userIdTag: UserIdTag): MessageResponse

    @WorkerThread
    fun fetchMessageDetailsBlocking(messageId: String, userIdTag: UserIdTag): MessageResponse?

    suspend fun createDraft(draftBody: DraftBody): MessageResponse

    suspend fun updateDraft(
        messageId: String,
        draftBody: DraftBody,
        userIdTag: UserIdTag
    ): MessageResponse

    suspend fun sendMessage(
        messageId: String,
        message: MessageSendBody,
        userIdTag: UserIdTag
    ): MessageSendResponse

    @Throws(IOException::class)
    fun unlabelMessages(idList: IDList)

    @Throws(IOException::class)
    fun labelMessages(body: IDList): MoveToFolderResponse?

    @Throws(IOException::class)
    fun labelMessagesBlocking(body: IDList, userId: UserId): MoveToFolderResponse
}
