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
package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.utils.Fields
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.details.data.MessageFlagsToEncryptionMapper
import com.google.gson.annotations.SerializedName

class MessageResponse : ResponseBody() {

    @SerializedName(Fields.Message.MESSAGE)
    private lateinit var serverMessage: ServerMessage

    val message by lazy {
        val attachmentFactory = AttachmentFactory()
        val messageSenderFactory = MessageSenderFactory()
        val messageLocationResolver = MessageLocationResolver(null)
        val messageFlagsMapper = MessageFlagsToEncryptionMapper()
        val messageFactory = MessageFactory(
            attachmentFactory,
            messageSenderFactory,
            messageLocationResolver,
            messageFlagsMapper
        )
        messageFactory.createMessage(serverMessage)
    }

    val messageId: String?
        get() = message.messageId

    val attachments: List<Attachment>
        get() = message.attachments

}
