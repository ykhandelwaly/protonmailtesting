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
package ch.protonmail.android.api

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.AttachmentUploadResponse
import ch.protonmail.android.api.models.ContactEmailsResponseV2
import ch.protonmail.android.api.models.ContactResponse
import ch.protonmail.android.api.models.ContactsDataResponse
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.models.CreateContactV2BodyItem
import ch.protonmail.android.api.models.DeleteResponse
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.MailSettingsResponse
import ch.protonmail.android.api.models.MoveToFolderResponse
import ch.protonmail.android.api.models.OrganizationKeysResponse
import ch.protonmail.android.api.models.OrganizationResponse
import ch.protonmail.android.api.models.PublicKeyResponse
import ch.protonmail.android.api.models.RegisterDeviceRequestBody
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.UnregisterDeviceRequestBody
import ch.protonmail.android.api.models.address.KeyActivationBody
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.models.messages.delete.MessageDeleteRequest
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.api.models.messages.send.MessageSendBody
import ch.protonmail.android.api.models.messages.send.MessageSendResponse
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.segments.attachment.AttachmentApiSpec
import ch.protonmail.android.api.segments.connectivity.ConnectivityApiSpec
import ch.protonmail.android.api.segments.contact.ContactApiSpec
import ch.protonmail.android.api.segments.device.DeviceApiSpec
import ch.protonmail.android.api.segments.key.KeyApiSpec
import ch.protonmail.android.api.segments.message.MessageApiSpec
import ch.protonmail.android.api.segments.organization.OrganizationApiSpec
import ch.protonmail.android.api.segments.report.ReportApiSpec
import ch.protonmail.android.api.segments.settings.mail.MailSettingsApiSpec
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.di.MainBackendApi
import ch.protonmail.android.labels.data.remote.LabelApiSpec
import ch.protonmail.android.labels.data.remote.model.LabelRequestBody
import ch.protonmail.android.labels.data.remote.model.LabelResponse
import ch.protonmail.android.labels.data.remote.model.LabelsResponse
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.mailbox.data.remote.ConversationApiSpec
import ch.protonmail.android.mailbox.data.remote.model.ConversationIdsRequestBody
import ch.protonmail.android.mailbox.data.remote.model.ConversationsActionResponses
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import ch.protonmail.android.mailbox.data.remote.model.CountsResponse
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import ch.protonmail.android.mailbox.domain.model.GetOneConversationParameters
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class takes an API implementation and acts as a proxy. The real implementation is in the {@param api}
 * which can work directly with the Proton API or use any alternative proxy.
 */
@Singleton
class ProtonMailApiManager @Inject constructor(
    private var api: ProtonMailApi,
    @MainBackendApi private val mainBackendApi: ProtonMailApi
) :
    BaseApi(),
    AttachmentApiSpec,
    ConnectivityApiSpec,
    ContactApiSpec,
    DeviceApiSpec,
    KeyApiSpec,
    LabelApiSpec,
    MessageApiSpec,
    ConversationApiSpec,
    OrganizationApiSpec,
    ReportApiSpec,
    MailSettingsApiSpec {

    fun reset(newApi: ProtonMailApi) {
        api = newApi
    }

    fun getSecuredServices(): SecuredServices = api.securedServices

    suspend fun pingMainBackend(): ResponseBody = mainBackendApi.ping()

    override fun deleteAttachment(attachmentId: String): ResponseBody =
        api.deleteAttachment(attachmentId)

    override fun downloadAttachmentBlocking(attachmentId: String): ByteArray =
        api.downloadAttachmentBlocking(attachmentId)

    override suspend fun downloadAttachment(attachmentId: String): okhttp3.ResponseBody? =
        api.downloadAttachment(attachmentId)

    override suspend fun uploadAttachmentInline(
        attachment: Attachment,
        messageID: String,
        contentID: String,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse =
        api.uploadAttachmentInline(attachment, messageID, contentID, keyPackage, dataPackage, signature)

    override suspend fun uploadAttachment(
        attachment: Attachment,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse = api.uploadAttachment(attachment, keyPackage, dataPackage, signature)

    override suspend fun ping(): ResponseBody = api.ping()

    override suspend fun fetchContacts(
        page: Int,
        pageSize: Int
    ): ContactsDataResponse = api.fetchContacts(page, pageSize)

    override suspend fun fetchContactEmails(page: Int, pageSize: Int): ContactEmailsResponseV2 =
        api.fetchContactEmails(page, pageSize)

    override fun fetchContactsEmailsByLabelId(
        page: Int,
        labelId: String
    ): Observable<ContactEmailsResponseV2> = api.fetchContactsEmailsByLabelId(page, labelId)

    override fun fetchContactDetailsBlocking(
        contactId: String
    ): FullContactDetailsResponse? = api.fetchContactDetailsBlocking(contactId)

    override suspend fun fetchContactDetails(
        contactId: String
    ): FullContactDetailsResponse = api.fetchContactDetails(contactId)

    override fun fetchContactDetailsBlocking(
        contactIDs: Collection<String>
    ): Map<String, FullContactDetailsResponse?> = api.fetchContactDetailsBlocking(contactIDs)

    override fun createContactBlocking(body: CreateContact): ContactResponse? = api.createContactBlocking(body)

    override suspend fun createContact(body: CreateContact): ContactResponse? = api.createContact(body)

    override fun updateContact(contactId: String, body: CreateContactV2BodyItem): FullContactDetailsResponse? =
        api.updateContact(contactId, body)

    override fun deleteContactSingle(contactIds: IDList): Single<DeleteResponse> =
        api.deleteContactSingle(contactIds)

    override suspend fun deleteContact(contactIds: IDList): DeleteResponse = api.deleteContact(contactIds)

    override suspend fun labelContacts(labelContactsBody: LabelContactsBody) =
        api.labelContacts(labelContactsBody)

    override fun unlabelContactEmailsCompletable(labelContactsBody: LabelContactsBody): Completable =
        api.unlabelContactEmailsCompletable(labelContactsBody)

    override suspend fun unlabelContactEmails(labelContactsBody: LabelContactsBody) =
        api.unlabelContactEmails(labelContactsBody)

    override suspend fun registerDevice(
        userId: UserId,
        registerDeviceRequestBody: RegisterDeviceRequestBody
    ) = api.registerDevice(userId, registerDeviceRequestBody)

    override suspend fun unregisterDevice(
        unregisterDeviceRequestBody: UnregisterDeviceRequestBody,
    ) = api.unregisterDevice(unregisterDeviceRequestBody)

    override fun getPublicKeysBlocking(email: String): PublicKeyResponse = api.getPublicKeysBlocking(email)

    override suspend fun getPublicKeys(email: String): PublicKeyResponse = api.getPublicKeys(email)

    override fun getPublicKeys(emails: Collection<String>): Map<String, PublicKeyResponse?> = api.getPublicKeys(emails)

    override fun activateKey(
        keyActivationBody: KeyActivationBody,
        keyId: String
    ): ResponseBody = api.activateKey(keyActivationBody, keyId)

    override suspend fun activateKeyLegacy(
        keyActivationBody: KeyActivationBody,
        keyId: String
    ): ResponseBody = api.activateKeyLegacy(keyActivationBody, keyId)

    override suspend fun getLabels(userId: UserId): ApiResult<LabelsResponse> = api.getLabels(userId)

    override suspend fun getContactGroups(userId: UserId): ApiResult<LabelsResponse> =
        api.getContactGroups(userId)

    override suspend fun getFolders(userId: UserId): ApiResult<LabelsResponse> = api.getFolders(userId)

    override suspend fun createLabel(userId: UserId, label: LabelRequestBody): ApiResult<LabelResponse> =
        api.createLabel(userId, label)

    override suspend fun updateLabel(
        userId: UserId,
        labelId: String,
        labelRequestBody: LabelRequestBody
    ): ApiResult<LabelResponse> =
        api.updateLabel(userId, labelId, labelRequestBody)

    override suspend fun deleteLabel(userId: UserId, labelId: String): ApiResult<Unit> =
        api.deleteLabel(userId, labelId)

    override suspend fun fetchMessagesCounts(userId: UserId): CountsResponse =
        api.fetchMessagesCounts(userId)

    override suspend fun getMessages(params: GetAllMessagesParameters): MessagesResponse =
        api.getMessages(params)

    override suspend fun fetchMessageMetadata(messageId: String, userIdTag: UserIdTag): MessagesResponse =
        api.fetchMessageMetadata(messageId, userIdTag)

    override fun markMessageAsRead(messageIds: IDList) = api.markMessageAsRead(messageIds)

    override fun markMessageAsUnRead(messageIds: IDList) = api.markMessageAsUnRead(messageIds)

    override suspend fun deleteMessage(messageDeleteRequest: MessageDeleteRequest) =
        api.deleteMessage(messageDeleteRequest)

    override suspend fun emptyFolder(userIdTag: UserIdTag, labelId: LabelId) = api.emptyFolder(userIdTag, labelId)

    override fun fetchMessageDetailsBlocking(messageId: String): MessageResponse =
        api.fetchMessageDetailsBlocking(messageId)

    override suspend fun fetchMessageDetails(messageId: String, userIdTag: UserIdTag): MessageResponse =
        api.fetchMessageDetails(messageId, userIdTag)

    override fun fetchMessageDetailsBlocking(messageId: String, userIdTag: UserIdTag): MessageResponse? =
        api.fetchMessageDetailsBlocking(messageId, userIdTag)

    override suspend fun createDraft(draftBody: DraftBody): MessageResponse = api.createDraft(draftBody)

    override suspend fun updateDraft(
        messageId: String,
        draftBody: DraftBody,
        userIdTag: UserIdTag
    ): MessageResponse = api.updateDraft(messageId, draftBody, userIdTag)

    override suspend fun sendMessage(
        messageId: String,
        message: MessageSendBody,
        userIdTag: UserIdTag
    ): MessageSendResponse = api.sendMessage(messageId, message, userIdTag)

    override fun unlabelMessages(idList: IDList) = api.unlabelMessages(idList)

    override fun labelMessages(body: IDList): MoveToFolderResponse? = api.labelMessages(body)

    override fun labelMessagesBlocking(
        body: IDList,
        userId: UserId
    ): MoveToFolderResponse = api.labelMessagesBlocking(body, userId)

    override suspend fun fetchOrganization(userId: UserId): ApiResult<OrganizationResponse> =
        api.fetchOrganization(userId)

    override suspend fun fetchOrganizationKeys(userId: UserId): ApiResult<OrganizationKeysResponse> =
        api.fetchOrganizationKeys(userId)

    override suspend fun postPhishingReport(
        messageId: String,
        messageBody: String,
        mimeType: String,
        userId: UserId
    ): ApiResult<Unit> =
        api.postPhishingReport(messageId, messageBody, mimeType, userId)

    override suspend fun fetchMailSettings(userId: UserId): MailSettingsResponse = api.fetchMailSettings(userId)

    override fun fetchMailSettingsBlocking(userId: UserId): MailSettingsResponse =
        api.fetchMailSettingsBlocking(userId)

    override fun updateSignature(signature: String): ResponseBody? = api.updateSignature(signature)

    override fun updateDisplayName(displayName: String): ResponseBody? = api.updateDisplayName(displayName)

    override fun updateAutoShowImages(autoShowImages: Int): ResponseBody? = api.updateAutoShowImages(autoShowImages)

    override suspend fun fetchConversations(params: GetAllConversationsParameters): ConversationsResponse =
        api.fetchConversations(params)

    override suspend fun fetchConversation(params: GetOneConversationParameters): ConversationResponse =
        api.fetchConversation(params)

    override suspend fun fetchConversationsCounts(userId: UserId): CountsResponse =
        api.fetchConversationsCounts(userId)

    override suspend fun markConversationsRead(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ): ConversationsActionResponses = api.markConversationsRead(conversationIds, userId)

    override suspend fun markConversationsUnread(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ): ConversationsActionResponses = api.markConversationsUnread(conversationIds, userId)

    override suspend fun labelConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ): ConversationsActionResponses = api.labelConversations(conversationIds, userId)

    override suspend fun unlabelConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ): ConversationsActionResponses = api.unlabelConversations(conversationIds, userId)

    override suspend fun deleteConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ): ConversationsActionResponses = api.deleteConversations(conversationIds, userId)
}
