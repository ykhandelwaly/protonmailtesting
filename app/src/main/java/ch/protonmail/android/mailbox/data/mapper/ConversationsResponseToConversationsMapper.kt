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

package ch.protonmail.android.mailbox.data.mapper

import ch.protonmail.android.data.ProtonStoreMapper
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import me.proton.core.domain.arch.map
import javax.inject.Inject

/**
 * [ProtonStoreMapper] that maps from [ConversationsResponse] to [List] of [Conversation] Domain models
 */
class ConversationsResponseToConversationsMapper @Inject constructor(
    private val mapper: ConversationApiModelToConversationMapper
) : ProtonStoreMapper<GetAllConversationsParameters, ConversationsResponse, List<Conversation>> {

    override fun ConversationsResponse.toOut(key: GetAllConversationsParameters): List<Conversation> =
        conversations.map(mapper) { toDomainModel(it) }
}
