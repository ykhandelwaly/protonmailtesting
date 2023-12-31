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

import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.AttachmentHeaders
import ch.protonmail.android.utils.extensions.notNull
import ch.protonmail.android.utils.extensions.notNullOrEmpty

class AttachmentFactory : IAttachmentFactory {

    override fun createServerAttachment(attachment: Attachment): ServerAttachment {
        val (
            attachmentId,
            fileName,
            mimeType,
            fileSize,
            keyPackets,
            messageId,
            isUploaded,
            isUploading,
            signature,
            headers,
            _,
            _,
            _
        ) = attachment
        return ServerAttachment(
            attachmentId,
            fileName,
            mimeType,
            fileSize,
            keyPackets,
            signature,
            headers
        )
    }

    override fun createAttachment(serverAttachment: ServerAttachment): Attachment {
        val attachmentId: String = serverAttachment.ID.notNullOrEmpty("Attachment id")
        val fileName: String = serverAttachment.Name.notNullOrEmpty("File name")
        val mimeType: String = serverAttachment.MIMEType.notNullOrEmpty("Mime type")
        val fileSize: Long = serverAttachment.Size.notNull("Filesize")
        val keyPackets: String = serverAttachment.KeyPackets.notNullOrEmpty("Key packets")
        val headers: AttachmentHeaders = serverAttachment.headers.notNull("Headers")

        return Attachment(
            attachmentId = attachmentId,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            keyPackets = keyPackets,
            headers = headers
        )
    }
}
