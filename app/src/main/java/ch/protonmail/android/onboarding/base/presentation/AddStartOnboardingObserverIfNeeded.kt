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

package ch.protonmail.android.onboarding.base.presentation

import android.content.SharedPreferences
import androidx.startup.AppInitializer
import ch.protonmail.android.core.Constants
import ch.protonmail.android.di.DefaultSharedPreferences
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class AddStartOnboardingObserverIfNeeded @Inject constructor(
    @DefaultSharedPreferences private val defaultSharedPreferences: SharedPreferences
) {

    operator fun invoke(appInitializer: AppInitializer, currentUserId: UserId?) {
        if (userAlreadyLoggedIn(currentUserId) && existingUserOnboardingNotShownYet()) {
            appInitializer.addOnboardingObserver()
        }
    }

    private fun userAlreadyLoggedIn(userId: UserId?) = userId != null

    private fun existingUserOnboardingNotShownYet() =
        !defaultSharedPreferences.getBoolean(Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN, false)

    private fun AppInitializer.addOnboardingObserver() =
        initializeComponent(StartOnboardingObserverInitializer::class.java)
}
