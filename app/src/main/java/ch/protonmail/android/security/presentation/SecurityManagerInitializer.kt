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

package ch.protonmail.android.security.presentation

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

internal class SecurityManagerInitializer : Initializer<SecurityManager> {

    override fun create(context: Context): SecurityManager {
        val entryPoint = EntryPointAccessors.fromApplication(context, SecurityManagerEntryPoint::class.java)
        return entryPoint.securityManager()
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SecurityManagerEntryPoint {

        fun securityManager(): SecurityManager
    }
}
