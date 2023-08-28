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

package ch.protonmail.android.attachments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.AddAttachmentsActivity.EXTRA_DRAFT_ID
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.MessageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

@HiltViewModel
class AttachmentsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dispatchers: DispatcherProvider,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val networkConnectivityManager: NetworkConnectivityManager
) : ViewModel() {

    val viewState: MutableLiveData<AttachmentsViewState> = MutableLiveData()

    fun init() {
        viewModelScope.launch(dispatchers.Io) {
            val messageId = savedStateHandle.get<String>(EXTRA_DRAFT_ID) ?: return@launch
            val message = messageDetailsRepository.findMessageById(messageId).first()

            message?.let { existingMessage ->
                val messageDbId = requireNotNull(existingMessage.dbId)
                val messageFlow = messageDetailsRepository.findMessageByDatabaseId(messageDbId)

                if (!networkConnectivityManager.isInternetConnectionPossible()) {
                    viewState.postValue(AttachmentsViewState.MissingConnectivity)
                }

                messageFlow.collect { updatedMessage ->
                    if (updatedMessage == null) {
                        return@collect
                    }
                    if (!this.isActive) {
                        return@collect
                    }
                    if (draftCreationHappened(existingMessage, updatedMessage)) {
                        viewState.postValue(AttachmentsViewState.UpdateAttachments(updatedMessage.attachments))
                        this.cancel()
                    }
                    if (draftWasAlreadyCreated(existingMessage, updatedMessage)) {
                        // Opening the attachments screen after draft creation happened,
                        // ensure the displayed attachments has a valid `attachmentId`
                        viewState.postValue(AttachmentsViewState.UpdateAttachments(updatedMessage.attachments))
                        this.cancel()
                    }
                }
            }
        }
    }

    private fun draftWasAlreadyCreated(existingMessage: Message, updatedMessage: Message) =
        isRemoteMessage(existingMessage) &&
            isRemoteMessage(updatedMessage) &&
            existingMessage.messageId == updatedMessage.messageId

    private fun draftCreationHappened(existingMessage: Message, updatedMessage: Message) =
        !isRemoteMessage(existingMessage) && isRemoteMessage(updatedMessage)

    private fun isRemoteMessage(message: Message) = !MessageUtils.isLocalMessageId(message.messageId)

}
