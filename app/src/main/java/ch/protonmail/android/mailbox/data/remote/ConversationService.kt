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
import ch.protonmail.android.api.segments.RetrofitConstants
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.mailbox.data.remote.model.ConversationIdsRequestBody
import ch.protonmail.android.mailbox.data.remote.model.ConversationsActionResponses
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import ch.protonmail.android.mailbox.data.remote.model.CountsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

@Suppress("LongParameterList")
interface ConversationService {

    @GET("mail/v4/conversations")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun fetchConversations(
        @Tag userIdTag: UserIdTag,
        @Query("Page") page: Int?,
        @Query("PageSize") pageSize: Int?,
        @Query("LabelID") labelId: String?,
        @Query("Sort") sort: String?,
        @Query("Desc") desc: Int?,
        @Query("Begin") begin: Long?,
        @Query("End") end: Long?,
        @Query("BeginID") beginId: String?,
        @Query("EndID") endId: String?,
    ): ConversationsResponse

    @GET("mail/v4/conversations/{conversationId}")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun fetchConversation(
        @Path("conversationId") conversationId: String,
        @Tag userIdTag: UserIdTag? = null
    ): ConversationResponse

    @GET("mail/v4/conversations/count")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun fetchConversationsCounts(
        @Tag userIdTag: UserIdTag
    ): CountsResponse

    @PUT("mail/v4/conversations/read")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun markConversationsRead(
        @Body conversationIds: ConversationIdsRequestBody,
        @Tag userIdTag: UserIdTag? = null
    ): ConversationsActionResponses

    @PUT("mail/v4/conversations/unread")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun markConversationsUnread(
        @Body conversationIds: ConversationIdsRequestBody,
        @Tag userIdTag: UserIdTag? = null
    ): ConversationsActionResponses

    @PUT("mail/v4/conversations/label")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun labelConversations(
        @Body conversationIds: ConversationIdsRequestBody,
        @Tag userIdTag: UserIdTag? = null
    ): ConversationsActionResponses

    @PUT("mail/v4/conversations/unlabel")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun unlabelConversations(
        @Body conversationIds: ConversationIdsRequestBody,
        @Tag userIdTag: UserIdTag? = null
    ): ConversationsActionResponses

    @PUT("mail/v4/conversations/delete")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun deleteConversations(
        @Body conversationIds: ConversationIdsRequestBody,
        @Tag userIdTag: UserIdTag? = null
    ): ConversationsActionResponses
}
