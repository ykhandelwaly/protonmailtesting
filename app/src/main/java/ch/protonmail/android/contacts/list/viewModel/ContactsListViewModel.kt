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
package ch.protonmail.android.contacts.list.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.loader.app.LoaderManager
import androidx.work.Operation
import androidx.work.WorkManager
import ch.protonmail.android.contacts.list.listView.ContactItem
import ch.protonmail.android.contacts.list.progress.ProgressLiveData
import ch.protonmail.android.contacts.list.search.ISearchListenerViewModel
import ch.protonmail.android.contacts.repositories.andorid.baseInfo.AndroidContactsRepository
import ch.protonmail.android.contacts.repositories.andorid.details.AndroidContactDetailsRepository
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.utils.Event
import ch.protonmail.android.worker.DeleteContactWorker
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.containsNoCase
import timber.log.Timber
import javax.inject.Inject

class ContactsListViewModel(
    private val contactDao: ContactDao,
    private val workManager: WorkManager,
    private val androidContactsRepository: AndroidContactsRepository,
    private val androidContactsDetailsRepository: AndroidContactDetailsRepository,
    private val contactsListMapper: ContactsListMapper,
    private val moveMessagesToFolder: MoveMessagesToFolder
) : ViewModel(), IContactsListViewModel, ISearchListenerViewModel {

    private val progressMax = MutableLiveData<Int?>()
    private val progress = MutableLiveData<Int?>()
    private val showPermissionMissingDialogMutableLiveData = MutableLiveData<Event<Unit>>()
    val showPermissionMissingDialog: LiveData<Event<Unit>>
        get() = showPermissionMissingDialogMutableLiveData

    private val searchPhraseLiveData = MutableLiveData<String?>()
    override val androidContacts = androidContactsRepository.androidContacts
    override val contactItems = MutableLiveData<List<ContactItem>>()
    override val uploadProgress = ProgressLiveData(progress, progressMax)
    override val contactToConvert = androidContactsDetailsRepository.contactDetails

    var hasPermission: Boolean = false
        private set

    fun fetchContactItems() {
        contactDao.findAllContactData()
            .combine(contactDao.findAllContactsEmails()) { data, email ->
                contactsListMapper.mapToContactItems(data, email)
            }
            .combine(searchPhraseLiveData.asFlow()) { contacts, searchPhrase ->
                contacts.filter { contactItem ->
                    searchPhrase?.isEmpty() ?: true ||
                        contactItem.name containsNoCase (searchPhrase ?: EMPTY_STRING) ||
                        contactItem.contactEmails?.contains(searchPhrase ?: EMPTY_STRING, ignoreCase = true) ?: false
                }
            }
            .onEach {
                // emit what we have until now, in case user didn't agree to access android contacts in the next step
                Timber.d("Display proton contacts size: ${it.size}")
                contactItems.value = it
            }
            .combine(androidContacts.asFlow()) { protonContacts, androidContacts ->
                Timber.d("protonContacts size: ${protonContacts.size} androidContacts size: ${androidContacts.size}")
                contactsListMapper.mergeContactItems(protonContacts, androidContacts)
            }
            .onEach {
                Timber.d("Display all contacts size: ${it.size}")
                contactItems.value = it
            }
            .catch { Timber.w(it, "Error Fetching contacts") }
            .launchIn(viewModelScope)

        if (searchPhraseLiveData.value.isNullOrBlank()) {
            searchPhraseLiveData.value = EMPTY_STRING
        }
    }

    override fun startConvertDetails(contactId: String) =
        androidContactsDetailsRepository.makeQuery(contactId)

    override fun setHasContactsPermission(hasPermission: Boolean) {
        this.hasPermission = hasPermission
        androidContactsRepository.setContactsPermission(hasPermission)
        if (!hasPermission) {
            showPermissionMissingDialogMutableLiveData.value = Event(Unit)
        }
    }

    override fun setSearchPhrase(searchPhrase: String?) {
        this.searchPhraseLiveData.value = searchPhrase
        androidContactsRepository.setSearchPhrase(searchPhrase ?: "")
    }

    override fun setProgressMax(max: Int?) {
        progressMax.value = max
    }

    override fun setProgress(progress: Int?) {
        this.progress.value = progress
    }

    fun deleteSelected(contacts: List<String>): LiveData<Operation.State> =
        DeleteContactWorker.Enqueuer(workManager).enqueue(contacts).state

    fun moveDraftToTrash(userId: UserId, messageId: String) {
        viewModelScope.launch {
            moveMessagesToFolder(
                listOf(messageId),
                Constants.MessageLocationType.TRASH.asLabelIdString(),
                Constants.MessageLocationType.DRAFT.asLabelIdString(),
                userId
            )
        }
    }

    class Factory @Inject constructor(
        private val contactDao: ContactDao,
        private val workManager: WorkManager,
        private val androidContactsRepositoryFactory: AndroidContactsRepository.AssistedFactory,
        private val androidContactsDetailsRepositoryFactory: AndroidContactDetailsRepository.AssistedFactory,
        private val contactsListMapper: ContactsListMapper,
        private val moveMessagesToFolder: MoveMessagesToFolder
    ) : ViewModelProvider.NewInstanceFactory() {

        lateinit var loaderManager: LoaderManager

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ContactsListViewModel::class.java))
            val androidContactsRepository = androidContactsRepositoryFactory.create(loaderManager)
            val androidContactsDetailsRepository = androidContactsDetailsRepositoryFactory.create(loaderManager)
            return ContactsListViewModel(
                contactDao,
                workManager,
                androidContactsRepository,
                androidContactsDetailsRepository,
                contactsListMapper,
                moveMessagesToFolder
            ) as T
        }
    }
}
