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
package ch.protonmail.android.uitests.robots.settings

import androidx.appcompat.widget.AppCompatImageButton
import androidx.test.espresso.matcher.ViewMatchers
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withSettingsHeader
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withSettingsValue
import ch.protonmail.android.uitests.robots.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitests.robots.settings.autolock.AutoLockRobot
import ch.protonmail.android.uitests.testsHelper.User
import me.proton.fusion.Fusion
import me.proton.fusion.utils.StringUtils.stringFromResource

/**
 * [SettingsRobot] class contains actions and verifications for Settings view.
 */
class SettingsRobot : Fusion {

    fun navigateUpToInbox(): InboxRobot {
        view.instanceOf(AppCompatImageButton::class.java).hasParent(view.withId(R.id.toolbar)).click()
        return InboxRobot()
    }

    fun emptyCache(): SettingsRobot {
        view.withId(R.id.clearCacheButton).withVisibility(ViewMatchers.Visibility.VISIBLE).click()
        return this
    }

    fun openUserAccountSettings(user: User): AccountSettingsRobot {
        selectSettingsItemByValue(user.email)
        return AccountSettingsRobot()
    }

    fun selectAutoLock(): AutoLockRobot {
        selectItemByHeader(autoLockText)
        return AutoLockRobot()
    }

    fun selectSettingsItemByValue(value: String): AccountSettingsRobot {
        recyclerView
            .withId(R.id.settingsRecyclerView)
//            .waitUntilPopulated()
            .onHolderItem(withSettingsValue(value))
            .click()
        return AccountSettingsRobot()
    }

    private fun selectItemByHeader(header: String) {
        recyclerView
            .withId(R.id.settingsRecyclerView)
//            .waitUntilPopulated()
            .onHolderItem(withSettingsHeader(header))
            .click()
    }

    /**
     * Contains all the validations that can be performed by [SettingsRobot].
     */
    class Verify : Fusion {

        fun settingsOpened() {
            view.withId(R.id.settingsRecyclerView).checkIsDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {

        val autoLockText = stringFromResource(R.string.auto_lock)
    }
}
