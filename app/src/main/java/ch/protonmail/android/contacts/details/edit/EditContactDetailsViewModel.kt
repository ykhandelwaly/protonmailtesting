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
package ch.protonmail.android.contacts.details.edit

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.R
import ch.protonmail.android.contacts.details.ContactDetailsViewModelOld
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.domain.usecase.DownloadFile
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.create.CreateContact
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.FileHelper
import ch.protonmail.android.views.models.LocalContact
import dagger.hilt.android.lifecycle.HiltViewModel
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.property.Anniversary
import ezvcard.property.Birthday
import ezvcard.property.Gender
import ezvcard.property.Key
import ezvcard.property.Nickname
import ezvcard.property.Note
import ezvcard.property.Organization
import ezvcard.property.Photo
import ezvcard.property.ProductId
import ezvcard.property.RawProperty
import ezvcard.property.Role
import ezvcard.property.Title
import ezvcard.property.Uid
import ezvcard.property.Url
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

const val FLOW_NEW_CONTACT = 1
const val FLOW_EDIT_CONTACT = 2
const val FLOW_CONVERT_CONTACT = 3
const val EXTRA_CONTACT = "extra_contact"
const val EXTRA_FLOW = "extra_flow"
const val EXTRA_NAME = "extra_name"
const val EXTRA_EMAIL = "extra_email"
const val EXTRA_CONTACT_VCARD_TYPE0 = "extra_vcard_type0"
const val EXTRA_CONTACT_VCARD_TYPE1_PATH = "extra_vcard_type1"
const val EXTRA_CONTACT_VCARD_TYPE2 = "extra_vcard_type2"
const val EXTRA_CONTACT_VCARD_TYPE3_PATH = "extra_vcard_type3"
const val EXTRA_LOCAL_CONTACT = "extra_local_contact"

private const val VCARD_PROD_ID = "-//ProtonMail//ProtonMail for Android vCard 1.0.0//EN"

@HiltViewModel
class EditContactDetailsViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    downloadFile: DownloadFile,
    private val editContactDetailsRepository: EditContactDetailsRepository,
    private val verifyConnection: VerifyConnection,
    private val createContact: CreateContact,
    private val fileHelper: FileHelper,
    userManager: UserManager
) : ContactDetailsViewModelOld(dispatchers, downloadFile, editContactDetailsRepository, userManager) {

    // region events
    private val _cleanUpComplete: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _setupNewContactFlow: MutableLiveData<String> = MutableLiveData()
    private val _setupEditContactFlow: MutableLiveData<EditContactCardsHolder> = MutableLiveData()
    private val _setupConvertContactFlow: MutableLiveData<Unit> = MutableLiveData()
    private val _verifyConnectionTrigger: MutableLiveData<Unit> = MutableLiveData()
    val cleanUpComplete: LiveData<Event<Boolean>>
        get() = _cleanUpComplete
    val setupNewContactFlow: LiveData<String>
        get() = _setupNewContactFlow
    val setupEditContactFlow: LiveData<EditContactCardsHolder>
        get() = _setupEditContactFlow
    val setupConvertContactFlow: LiveData<Unit>
        get() = _setupConvertContactFlow
    val hasConnectivity: LiveData<Constants.ConnectionState> =
        _verifyConnectionTrigger.switchMap { verifyConnection().asLiveData() }
    val createContactResult: MutableLiveData<Int> = MutableLiveData()
    // endregion

    // region data
    private lateinit var _contactId: String
    private var _localContact: LocalContact? = null
    private lateinit var _email: String
    private var flowType: Int = 0
    private var _changed: Boolean = false
    private lateinit var _vCardPhoneUIOptions: List<String>
    private lateinit var _vCardPhoneOptions: List<String>
    private lateinit var _vCardEmailUIOptions: List<String>
    private lateinit var _vCardEmailOptions: List<String>
    private lateinit var _vCardAddressUIOptions: List<String>
    private lateinit var _vCardAddressOptions: List<String>
    private lateinit var _vCardOtherOptions: List<String>

    private lateinit var _vCardType0: VCard
    private lateinit var _vCardType1: VCard
    private lateinit var _vCardType2: VCard
    private lateinit var _vCardType3: VCard
    private lateinit var _uid: String
    private lateinit var _productId: ProductId
    private var _vCardCustomProperties: List<RawProperty>? = null
    private lateinit var _vCardSigned: VCard
    private lateinit var _vCardEncrypted: VCard

    // endregion
    // region default options
    val defaultEmailOption: String
        get() = _vCardEmailOptions[0]

    val defaultEmailUIOption: String
        get() = _vCardEmailUIOptions[0]

    val defaultPhoneOption: String
        get() = _vCardPhoneOptions[0]

    val defaultPhoneUIOption: String
        get() = _vCardPhoneUIOptions[0]
    val defaultAddressOption: String
        get() = _vCardAddressOptions[0]
    val defaultAddressUIOption: String
        get() = _vCardAddressUIOptions[0]
    val defaultOtherOption: String
        get() = _vCardOtherOptions[0]

    // endregion
    //region options lists
    val emailOptions: List<String>
        get() = _vCardEmailOptions
    val emailUIOptions: List<String>
        get() = _vCardEmailUIOptions
    val phoneOptions: List<String>
        get() = _vCardPhoneOptions

    val phoneUIOptions: List<String>
        get() = _vCardPhoneUIOptions
    val addressOptions: List<String>
        get() = _vCardAddressOptions
    val addressUIOptions: List<String>
        get() = _vCardAddressUIOptions
    val otherOptions: List<String>
        get() = _vCardOtherOptions

    // endregion
    // region others
    val localContact: LocalContact?
        get() = _localContact
    val contactId: String
        get() = _contactId

    private fun postSetupFlowEvent() {
        when (flowType) {
            FLOW_NEW_CONTACT -> _setupNewContactFlow.postValue(_email)
            FLOW_EDIT_CONTACT -> _setupEditContactFlow.postValue(
                EditContactCardsHolder(_vCardType0, _vCardType1, _vCardType2, _vCardType3)
            )
            FLOW_CONVERT_CONTACT -> _setupConvertContactFlow.postValue(Unit)
        }
    }

    // endregion
    // region setup
    fun setup(
        flow: Int,
        contactId: String,
        localContact: LocalContact?,
        email: String,
        vCardPhoneUIOptions: List<String>,
        vCardPhoneOptions: List<String>,
        vCardEmailUIOptions: List<String>,
        vCardEmailOptions: List<String>,
        vCardAddressUIOptions: List<String>,
        vCardAddressOptions: List<String>,
        vCardOtherOptions: List<String>,
        vCardStringType0: String?,
        vCardStringType1Path: String?,
        vCardStringType2: String?,
        vCardStringType3Path: String?
    ) {
        flowType = flow
        _contactId = contactId
        _localContact = localContact
        _email = email
        _vCardPhoneUIOptions = vCardPhoneUIOptions
        _vCardPhoneOptions = vCardPhoneOptions
        _vCardEmailUIOptions = vCardEmailUIOptions
        _vCardEmailOptions = vCardEmailOptions
        _vCardAddressUIOptions = vCardAddressUIOptions
        _vCardAddressOptions = vCardAddressOptions
        _vCardOtherOptions = vCardOtherOptions
        setupVCards(vCardStringType0, vCardStringType1Path, vCardStringType2, vCardStringType3Path)
        postSetupFlowEvent()
        if (flowType == FLOW_EDIT_CONTACT) {
            fetchContactGroupsAndContactEmails(_contactId)
        }
    }

    private fun setupVCards(
        vCardStringType0: String?,
        vCardStringType1Path: String?,
        vCardStringType2: String?,
        vCardStringType3Path: String?
    ) {
        var vCard0: VCard? = null
        if (!TextUtils.isEmpty(vCardStringType0)) {
            vCard0 = Ezvcard.parse(vCardStringType0).first()
        }
        _vCardType0 = vCard0 ?: VCard()
        var vCard2: VCard? = null
        if (!TextUtils.isEmpty(vCardStringType2)) {
            vCard2 = Ezvcard.parse(vCardStringType2).first()
        }
        var vCard1: VCard? = null
        if (!vCardStringType1Path.isNullOrEmpty()) {
            val vCardStringType1 = fileHelper.readStringFromFilePath(vCardStringType1Path)
            vCard1 = Ezvcard.parse(vCardStringType1).first()
        }
        _vCardType1 = vCard1 ?: VCard()
        if (vCard2 == null) {
            _vCardType2 = VCard()
            if (_vCardType0.uid != null) {
                _uid = _vCardType0.uid.value
            }
            if (!::_uid.isInitialized || TextUtils.isEmpty(_uid)) {
                _uid = "proton-android-" + UUID.randomUUID().toString()
            }
        } else {
            _vCardType2 = vCard2
            _uid = _vCardType2.uid.value
        }
        var vCard3: VCard? = null
        if (!vCardStringType3Path.isNullOrEmpty()) {
            val vCardStringType3 = fileHelper.readStringFromFilePath(vCardStringType3Path)
            vCard3 = Ezvcard.parse(vCardStringType3).first()
        }
        _vCardType3 = vCard3 ?: VCard()
        // getting prod id
        val prodId = _vCardType3.productId
        _productId = if (prodId == null || TextUtils.isEmpty(prodId.value)) {
            ProductId(VCARD_PROD_ID)
        } else {
            prodId
        }
        // getting custom properties
        _vCardCustomProperties = _vCardType1.extendedProperties + _vCardType3.extendedProperties
    }

    fun isConvertContactFlow(): Boolean = flowType == FLOW_CONVERT_CONTACT

    // endregion
    // region card properties
    fun getPhotos(): List<Photo> = _vCardType1.photos + _vCardType3.photos
    fun getOrganizations(): List<Organization> = _vCardType1.organizations + _vCardType3.organizations
    fun getTitles(): List<Title> = _vCardType1.titles + _vCardType3.titles
    fun getNicknames(): List<Nickname> = _vCardType1.nicknames + _vCardType3.nicknames
    fun getBirthdays(): List<Birthday> = _vCardType1.birthdays + _vCardType3.birthdays
    fun getAnniversaries(): List<Anniversary> = _vCardType1.anniversaries + _vCardType3.anniversaries
    fun getRoles(): List<Role> = _vCardType1.roles + _vCardType3.roles
    fun getUrls(): List<Url> = _vCardType1.urls + _vCardType3.urls
    fun getGender(): Gender? = _vCardType1.gender ?: _vCardType3.gender
    fun getExtendedPropertiesEncrypted(): List<RawProperty> =
        _vCardType1.extendedProperties + _vCardType3.extendedProperties
    fun getNotes(): List<Note> = _vCardType1.notes + _vCardType3.notes
    fun getExtendedPropertiesType2(): List<RawProperty> = _vCardType2.extendedProperties
    fun getKeysType2(): List<Key> = _vCardType2.keys
    // endregion

    fun buildEncryptedCard(): VCard {
        val vCardEncrypted = VCard()
        vCardEncrypted.version = VCardVersion.V4_0
        _vCardEncrypted = vCardEncrypted
        return vCardEncrypted
    }

    fun buildSignedCard(contactName: String): VCard {
        val vCardSigned = VCard()
        vCardSigned.setFormattedName(contactName)
        vCardSigned.uid = Uid(_uid)
        vCardSigned.version = VCardVersion.V4_0
        vCardSigned.setProductId(_productId.value)
        _vCardSigned = vCardSigned
        return vCardSigned
    }

    fun save(emailsToBeRemoved: List<String>, contactName: String, emailsList: List<ContactEmail>) {
        Timber.v("Save contactName: $contactName")
        viewModelScope.launch {
            val emails = validateEmails(emailsList)
            val uniqueEmails = _vCardSigned.emails.toSet()
            _vCardSigned.emails.clear()
            _vCardSigned.emails.addAll(uniqueEmails)
            emailsToBeRemoved.forEach {
                editContactDetailsRepository.clearEmail(it)
            }

            when (flowType) {
                FLOW_EDIT_CONTACT -> {
                    val mapContactIdGroupsIds = getContactIdsForEmailsList(allContactEmails, allContactGroups)
                    editContactDetailsRepository.updateContact(
                        _contactId, contactName, emails, _vCardEncrypted, _vCardSigned, mapContactIdGroupsIds
                    )
                }
                FLOW_CONVERT_CONTACT,
                FLOW_NEW_CONTACT -> {
                    val userId = userManager.currentUserId
                        ?: return@launch
                    createContact(userId, contactName, emails, _vCardEncrypted.write(), _vCardSigned.write())
                        .observeForever { result: CreateContact.Result ->
                            val resultMessage = getMessageForResult(result)
                            createContactResult.postValue(resultMessage)
                        }
                }
            }
        }
    }

    private fun getMessageForResult(result: CreateContact.Result): Int {
        return when (result) {
            CreateContact.Result.Success -> R.string.contact_saved
            CreateContact.Result.GenericError -> R.string.error
            CreateContact.Result.ContactAlreadyExistsError -> R.string.contact_exist
            CreateContact.Result.InvalidEmailError -> R.string.invalid_email_some_contacts
            CreateContact.Result.DuplicatedEmailError -> R.string.duplicate_email
            CreateContact.Result.OnlineContactCreationPending -> R.string.contact_saved_offline
        }
    }

    fun setChanged() {
        _changed = true
    }

    /**
     * Should inform the view that the user want's to leave the screen, so that the view is able to save
     * state or do whatever needed for cleanup.
     */
    fun onBackPressed() {
        _cleanUpComplete.postValue(Event(_changed))
        _changed = false
    }

    private suspend fun getContactIdsForEmailsList(
        contactEmails: List<ContactEmail>,
        contactGroups: List<ContactLabelUiModel>
    ): Map<String, List<LabelId>> = withContext(dispatchers.Comp) {
        val contactsMap = mutableMapOf<String, List<LabelId>>()
        contactEmails.map { contactEmail ->
            val groupsForThisEmail = contactGroups.filter { group ->
                contactEmail.labelIds?.contains(group.id.id) ?: false
            }
            contactEmail.contactId?.let { contactId ->
                contactsMap[contactId] = groupsForThisEmail.map { it.id }
            }

        }
        return@withContext contactsMap
    }

    private fun validateEmails(emails: List<ContactEmail>): List<ContactEmail> = emails.distinctBy { it.email }

    fun checkConnectivity() {
        _verifyConnectionTrigger.value = Unit
    }

    data class EditContactCardsHolder(
        val vCardType0: VCard,
        val vCardType1: VCard,
        val vCardType2: VCard,
        val vCardType3: VCard
    )
}
