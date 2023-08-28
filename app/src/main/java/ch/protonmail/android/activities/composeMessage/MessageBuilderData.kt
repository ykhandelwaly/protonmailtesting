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
package ch.protonmail.android.activities.composeMessage

import android.text.Spanned
import android.text.SpannedString
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.core.BigContentHolder
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.LocalAttachment
import ch.protonmail.android.data.local.model.Message

class MessageBuilderData(
    val message: Message,
    val senderEmailAddress: String,
    val senderName: String,
    val messageTitle: String?,
    val content: String,
    val body: String,
    val largeMessageBody: Boolean,
    val isPGPMime: Boolean,
    val messageTimestamp: Long,
    val messageId: String?,
    val addressId: String,
    val addressEmailAlias: String?,
    val mBigContentHolder: BigContentHolder,
    val attachmentList: ArrayList<LocalAttachment>,
    val embeddedAttachmentsList: ArrayList<LocalAttachment>,
    val signature: String,
    val mobileFooter: String,
    val messagePassword: String?,
    val passwordHint: String?,
    val isPasswordValid: Boolean,
    val expiresAfterInSeconds: Long = 0,
    val sendPreferences: Map<String, SendPreference>,
    val isRespondInlineButtonVisible: Boolean,
    val isRespondInlineChecked: Boolean,
    val showImages: Boolean,
    val showRemoteContent: Boolean,
    val initialMessageContent: String,
    val decryptedMessage: String,
    val isMessageBodyVisible: Boolean,
    val quotedHeader: Spanned,
    val uploadAttachments: Boolean
) {
    class Builder {
        private lateinit var message: Message
        private lateinit var senderEmailAddress: String
        private lateinit var senderName: String
        private var messageTitle: String? = ""
        private var content: String = ""
        private var body: String = ""
        private var largeMessageBody: Boolean = false
        private var isPGPMime: Boolean = false
        private var messageTimestamp: Long = 0L
        private var messageId: String? = ""
        private var addressId: String = ""
        private var addressEmailAlias: String? = null
        private var mBigContentHolder: BigContentHolder = BigContentHolder()
        private var attachmentList: ArrayList<LocalAttachment> = ArrayList()
        private var embeddedAttachmentsList: ArrayList<LocalAttachment> = ArrayList()
        private var isDirty: Boolean = false
        private var signature: String = ""
        private var mobileFooter: String = ""
        private var messagePassword: String? = null
        private var passwordHint: String? = ""
        private var isPasswordValid: Boolean = true
        private var expiresAfterInSeconds: Long = 0
        private var sendPreferences: Map<String, SendPreference> = emptyMap()
        private var isRespondInlineButtonVisible: Boolean = true
        private var isRespondInlineChecked: Boolean = false
        private var showImages: Boolean = false
        private var showRemoteContent: Boolean = false
        private var initialMessageContent: String = ""
        private var decryptedMessage: String = ""
        private var isMessageBodyVisible: Boolean = false
        private var quotedHeader: Spanned = SpannedString("")
        private var uploadAttachments: Boolean = false

        @Synchronized
        fun fromOld(oldObject: MessageBuilderData) = apply {
            this.message = oldObject.message
            this.senderEmailAddress = oldObject.senderEmailAddress
            this.senderName = oldObject.senderName
            this.messageTitle = oldObject.messageTitle
            this.content = oldObject.content
            this.body = oldObject.body
            this.largeMessageBody = oldObject.largeMessageBody
            this.isPGPMime = oldObject.isPGPMime
            this.messageTimestamp = oldObject.messageTimestamp
            this.messageId = oldObject.messageId
            this.addressId = oldObject.addressId
            this.addressEmailAlias = oldObject.addressEmailAlias
            this.mBigContentHolder = oldObject.mBigContentHolder
            this.attachmentList = oldObject.attachmentList
            this.embeddedAttachmentsList = oldObject.embeddedAttachmentsList

            this.signature = oldObject.signature
            this.mobileFooter = oldObject.mobileFooter

            this.messagePassword = oldObject.messagePassword
            this.passwordHint = oldObject.passwordHint
            this.isPasswordValid = oldObject.isPasswordValid
            this.expiresAfterInSeconds = oldObject.expiresAfterInSeconds
            this.sendPreferences = oldObject.sendPreferences
            this.isRespondInlineButtonVisible = oldObject.isRespondInlineButtonVisible
            this.isRespondInlineChecked = oldObject.isRespondInlineChecked
            this.showImages = oldObject.showImages
            this.showRemoteContent = oldObject.showRemoteContent
            this.initialMessageContent = oldObject.initialMessageContent
            this.isMessageBodyVisible = oldObject.isMessageBodyVisible
            this.quotedHeader = oldObject.quotedHeader

            this.uploadAttachments = oldObject.uploadAttachments
        }

        fun message(message: Message) = apply { this.message = message }

        fun decryptedMessage(decryptedMessage: String) =
            apply { this.decryptedMessage = decryptedMessage }

        fun senderEmailAddress(senderEmailAddress: String) =
            apply {
                this.senderEmailAddress = senderEmailAddress
                message.sender!!.emailAddress = senderEmailAddress
            }

        fun messageSenderName(senderName: String) = apply {
            this.senderName = senderName
            message.senderName = senderName
        }

        fun messageTitle(messageTitle: String?) = apply { this.messageTitle = messageTitle }

        fun content(content: String) = apply { this.content = content }

        fun body() = apply {
            val bodyTemp = if (message.isPGPMime) message.decryptedBody else content
            if (bodyTemp != null && bodyTemp.isNotEmpty() && bodyTemp.toByteArray().size > Constants.MAX_INTENT_STRING_SIZE) {
                this.mBigContentHolder.content = bodyTemp
                this.largeMessageBody = true
            } else {
                this.body = bodyTemp ?: ""
            }
        }

        fun isPGPMime(isPGPMime: Boolean) = apply { this.isPGPMime = isPGPMime }

        fun messageTimestamp(messageTimestamp: Long) =
            apply { this.messageTimestamp = messageTimestamp }

        fun messageId() = apply { this.messageId = message.messageId }

        fun addressId(addressId: String) = apply { this.addressId = addressId }

        fun addressEmailAlias(addressEmailAlias: String?) = apply { this.addressEmailAlias = addressEmailAlias }

        fun attachmentList(attachments: ArrayList<LocalAttachment>) = apply {
            this.attachmentList = attachments
        }

        fun embeddedAttachmentsList(embeddedAttachments: ArrayList<LocalAttachment>) =
            apply { this.embeddedAttachmentsList = embeddedAttachments }

        fun signature(signature: String) = apply { this.signature = signature }

        fun mobileFooter(mobileFooter: String) =
            apply { this.mobileFooter = mobileFooter }

        fun messagePassword(messagePassword: String?) =
            apply { this.messagePassword = messagePassword }

        fun passwordHint(passwordHint: String?) = apply { this.passwordHint = passwordHint }

        fun isPasswordValid(isPasswordValid: Boolean) =
            apply { this.isPasswordValid = isPasswordValid }

        fun expiresAfterInSeconds(expiresAfterInSeconds: Long) =
            apply { this.expiresAfterInSeconds = expiresAfterInSeconds }

        fun sendPreferences(sendPreferences: Map<String, SendPreference>) =
            apply { this.sendPreferences = sendPreferences }

        fun isRespondInlineButtonVisible(isRespondInlineButtonVisible: Boolean) =
            apply { this.isRespondInlineButtonVisible = isRespondInlineButtonVisible }

        fun isRespondInlineChecked(isRespondInlineChecked: Boolean) =
            apply { this.isRespondInlineChecked = isRespondInlineChecked }

        fun showImages(showImages: Boolean) =
            apply { this.showImages = showImages }

        fun showRemoteContent(showRemoteContent: Boolean) =
            apply { this.showRemoteContent = showRemoteContent }

        fun initialMessageContent(initialMessageContent: String) =
            apply { this.initialMessageContent = initialMessageContent }

        fun isMessageBodyVisible(isMessageBodyVisible: Boolean) =
            apply { this.isMessageBodyVisible = isMessageBodyVisible }

        fun quotedHeader(quotedHeader: Spanned) =
            apply { this.quotedHeader = quotedHeader }

        fun uploadAttachments(uploadAttachments: Boolean) =
            apply { this.uploadAttachments = uploadAttachments }

        fun build(): MessageBuilderData {
            return MessageBuilderData(
                message,
                senderEmailAddress,
                senderName,
                messageTitle,
                content,
                body,
                largeMessageBody,
                isPGPMime,
                messageTimestamp,
                messageId,
                addressId,
                addressEmailAlias,
                mBigContentHolder,
                attachmentList,
                embeddedAttachmentsList,
                signature,
                mobileFooter,
                messagePassword,
                passwordHint,
                isPasswordValid,
                expiresAfterInSeconds,
                sendPreferences,
                isRespondInlineButtonVisible,
                isRespondInlineChecked,
                showImages,
                showRemoteContent,
                initialMessageContent,
                decryptedMessage,
                isMessageBodyVisible,
                quotedHeader,
                uploadAttachments
            )
        }
    }
}
