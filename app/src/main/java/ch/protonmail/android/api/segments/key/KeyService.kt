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
package ch.protonmail.android.api.segments.key

import ch.protonmail.android.api.models.PublicKeyResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.address.KeyActivationBody
import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface KeyService {

    @GET("keys")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun getPublicKeysBlocking(@Query("Email") email: String): Call<PublicKeyResponse>

    @GET("keys")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun getPublicKeys(@Query("Email") email: String): PublicKeyResponse

    // migrated user
    @PUT("keys/address/{addressId}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun activateKey(
        @Body keyActivationBody: KeyActivationBody,
        @Path("addressId") keyId: String
    ): Call<ResponseBody>

    // legacy user
    @PUT("/keys/{keyId}/activate")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun activateKeyLegacy(
        @Body keyActivationBody: KeyActivationBody,
        @Path("keyId") keyId: String
    ): ResponseBody
}
