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

package ch.protonmail.android.jobs

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.di.JobEntryPoint
import ch.protonmail.android.utils.AppUtil
import dagger.hilt.EntryPoints
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test

class UpdateSettingsJobTest {

    @RelaxedMockK
    private lateinit var mockUserManager: UserManager

    @RelaxedMockK
    private lateinit var mockApiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var jobEntryPoint: JobEntryPoint

    private lateinit var updateSettings: UpdateSettingsJob

    private val mailSettings = MailSettings()

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)

        mockkStatic(ProtonMailApplication::class)
        every { ProtonMailApplication.getApplication() } returns mockk()

        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), JobEntryPoint::class.java) } returns jobEntryPoint

        mockkStatic(AppUtil::class)
        justRun { AppUtil.postEventOnUi(any()) }

        every { jobEntryPoint.apiManager() } returns mockApiManager
        every { jobEntryPoint.userManager() } returns mockUserManager

        every { mockUserManager.requireCurrentUserId() } returns UserId("id")
        every { mockUserManager.getCurrentUserMailSettingsBlocking() } returns mailSettings
    }

    @Test
    fun jobCallsApiToUpdateAutoShowImagesSettingWhenMailSettingsAreValidAndNotificationEmailDidNotChange() {
        updateSettings = UpdateSettingsJob()

        updateSettings.onRun()

        verify { mockApiManager.updateAutoShowImages(0) }
    }
}
