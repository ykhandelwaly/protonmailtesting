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

package ch.protonmail.android.usecase

import arrow.core.Either
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.mapper.bridge.UserBridgeMapper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.invoke
import javax.inject.Inject

class LoadUser @Inject constructor(
    @Suppress("DEPRECATION")
    private val loadLegacyUser: LoadLegacyUser,
    private val mapper: UserBridgeMapper,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(userId: UserId): Either<Error, User> =
        withContext(dispatchers.Io) {
            loadLegacyUser(userId)
                .map { mapper { it.toNewUser() } }
        }


    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("invoke(userId)")
    )
    fun blocking(userId: UserId) = runBlocking {
        invoke(userId)
    }

    sealed class Error : ch.protonmail.android.domain.Error()
}
