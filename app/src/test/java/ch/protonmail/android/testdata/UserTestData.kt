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

package ch.protonmail.android.testdata

import ch.protonmail.android.domain.entity.user.Addresses
import ch.protonmail.android.domain.entity.user.User
import io.mockk.every
import io.mockk.mockk
import me.proton.core.domain.entity.UserId

object UserTestData {

    private const val RAW_ID = "user_id"
    private const val RAW_SECONDARY_USER_ID = "secondary_user_id"
    val userId = UserId(RAW_ID)
    val secondaryUserId = UserId(RAW_SECONDARY_USER_ID)

    fun withAddresses(addressesList: Addresses): User = mockk {
        every { addresses } returns addressesList
    }
}
