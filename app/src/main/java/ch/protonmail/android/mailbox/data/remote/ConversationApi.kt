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
package ch.protonmail.android.mailbox.data.remote

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.mailbox.data.remote.model.ConversationIdsRequestBody
import ch.protonmail.android.mailbox.data.remote.model.CountsResponse
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetOneConversationParameters
import me.proton.core.domain.entity.UserId

class ConversationApi(private val service: ConversationService) : BaseApi(), ConversationApiSpec {

    override suspend fun fetchConversations(params: GetAllConversationsParameters) =
        service.fetchConversations(
            userIdTag = UserIdTag(params.userId),
            page = params.page,
            pageSize = params.pageSize,
            labelId = params.labelId?.id,
            sort = params.sortBy.stringValue,
            desc = params.sortDirection.intValue,
            begin = params.begin,
            end = params.end,
            beginId = params.beginId,
            endId = params.endId
        )

    override suspend fun fetchConversation(params: GetOneConversationParameters): ConversationResponse =
        service.fetchConversation(params.conversationId, UserIdTag(params.userId))

    override suspend fun fetchConversationsCounts(userId: UserId): CountsResponse =
        service.fetchConversationsCounts(UserIdTag(userId))

    override suspend fun markConversationsRead(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ) = service.markConversationsRead(conversationIds, userIdTag = UserIdTag(userId))

    override suspend fun markConversationsUnread(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ) = service.markConversationsUnread(conversationIds, userIdTag = UserIdTag(userId))

    override suspend fun labelConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ) = service.labelConversations(conversationIds, userIdTag = UserIdTag(userId))

    override suspend fun unlabelConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ) = service.unlabelConversations(conversationIds, userIdTag = UserIdTag(userId))

    override suspend fun deleteConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ) = service.deleteConversations(conversationIds, userIdTag = UserIdTag(userId))
}
