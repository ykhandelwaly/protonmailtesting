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
package ch.protonmail.android.api.segments.contact

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEmailsResponseV2
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.data.mapper.LabelEntityApiMapper
import ch.protonmail.android.labels.data.mapper.LabelEntityDomainMapper
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.data.remote.model.LabelsResponse
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class ContactEmailsManager @Inject constructor(
    private var api: ProtonMailApiManager,
    private val databaseProvider: DatabaseProvider,
    private val accountManager: AccountManager,
    private val labelsMapper: LabelEntityApiMapper,
    private val labelsDomainMapper: LabelEntityDomainMapper,
    private val labelRepository: LabelRepository
) {

    suspend fun refresh(pageSize: Int = Constants.CONTACTS_PAGE_SIZE) {
        val userId = accountManager.getPrimaryUserId().filterNotNull().first()
        val contactGroupsResponse: LabelsResponse? =
            api.getContactGroups(userId).valueOrNull

        var currentPage = 0
        var hasMorePages = true
        val allResults = mutableListOf<ContactEmailsResponseV2>()
        while (hasMorePages) {
            val result = api.fetchContactEmails(currentPage, pageSize)
            allResults += result
            hasMorePages = currentPage < result.total / pageSize
            currentPage++
        }

        if (allResults.isNotEmpty()) {
            val allContactEmails = allResults.flatMap { it.contactEmails }
            val contactLabels = mapToContactLabelsEntity(contactGroupsResponse?.labels, userId)
            val contactsDao = databaseProvider.provideContactDao(userId)
            labelRepository.saveLabels(contactLabels, userId)
            contactsDao.insertNewContacts(allContactEmails)
        } else {
            Timber.v("contactEmails result list is empty")
        }
    }

    private fun mapToContactLabelsEntity(labels: List<LabelApiModel>?, userId: UserId): List<Label> {
        return labels?.map { label ->
            labelsMapper.toEntity(label, userId)
        }?.map {
            labelsDomainMapper.toLabel(it)
        } ?: emptyList()
    }

}
