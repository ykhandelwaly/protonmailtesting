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

package ch.protonmail.android.contacts.details.domain

import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.contacts.details.domain.model.FetchContactGroupsResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class FetchContactGroups @Inject constructor(
    private val repository: ContactDetailsRepository,
    private val dispatchers: DispatcherProvider
) {

    operator fun invoke(userId: UserId, contactId: String): Flow<FetchContactGroupsResult> =

        repository.observeContactEmails(contactId)
            .transformLatest { list ->
                if (list.isEmpty()) {
                    emit(emptyList())
                } else {
                    val result = list.map { contactEmail ->
                        repository.getContactGroupsLabelForId(userId, contactEmail.contactEmailId)
                    }
                    emit(result.flatten())
                }
            }
            .map { FetchContactGroupsResult(it) }
            .flowOn(dispatchers.Io)
}
