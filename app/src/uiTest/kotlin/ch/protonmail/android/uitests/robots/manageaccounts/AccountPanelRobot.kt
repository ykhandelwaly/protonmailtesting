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

package ch.protonmail.android.uitests.robots.manageaccounts

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.manageaccounts.ManageAccountsMatchers.withViewAtPosition
import me.proton.fusion.Fusion

/**
 * [AccountPanelRobot] class contains actions and verifications for Account Manager functionality.
 */
open class AccountPanelRobot : Fusion {

    fun addAccount(): LoginMailRobot {
        view.withId(R.id.account_action_textview).click()
        return LoginMailRobot()
    }

    fun logoutAccount(email: String): InboxRobot {
        return accountMoreMenu(email)
            .logout()
//            .confirm()
    }

    fun logoutSecondaryAccount(email: String): AccountPanelRobot {
        return accountMoreMenu(email)
            .logoutSecondaryAccount()
//            .confirm()
    }

    fun logoutLastAccount(email: String): LoginMailRobot {
        return accountMoreMenu(email)
            .logoutLastAccount()
//            .confirmLastAccountLogout()
    }

    fun removeAccount(email: String): AccountPanelRobot {
        return accountMoreMenu(email)
            .remove()
//            .confirm()
    }

    fun removeSecondaryAccount(email: String): InboxRobot {
        return accountMoreMenu(email)
            .removeSecondaryAccount()
//            .confirm()
    }

    fun removeLastAccount(email: String): LoginMailRobot {
        return accountMoreMenu(email)
            .removeLastAccount()
//        .confirmLastAccountLogout()
    }

    fun switchToAccount(accountPosition: Int): InboxRobot {
        recyclerView
            .withId(accountsRecyclerViewId)
            .onItemAtPosition(accountPosition)
            .click()
        return InboxRobot()
    }

    private fun logout(): InboxRobot {
        onView(withText("Sign out")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return InboxRobot()
    }

    private fun logoutSecondaryAccount(): AccountPanelRobot {
        onView(withText("Sign out")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return AccountPanelRobot()
    }

    private fun logoutLastAccount(): LoginMailRobot {
        onView(withText("Sign out")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return LoginMailRobot()
    }

    private fun remove(): AccountPanelRobot {
        onView(withText("Remove")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return AccountPanelRobot()
    }

    private fun removeSecondaryAccount(): InboxRobot {
        onView(withText("Remove")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return InboxRobot()
    }

    private fun removeLastAccount(): LoginMailRobot {
        onView(withText("Remove")).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        return LoginMailRobot()
    }

    private fun accountMoreMenu(email: String): AccountPanelRobot {
        view.withId(R.id.account_more_button).hasSibling(view.withText(email)).click()
        return AccountPanelRobot()
    }

    /**
     * Contains all the validations that can be performed by [AccountPanelRobot].
     */
    inner class Verify : AccountPanelRobot() {

        fun accountsListOpened(): AccountPanelRobot {
            view.withId(accountsRecyclerViewId).checkIsDisplayed()
            return AccountPanelRobot()
        }

        fun accountAdded(email: String) {
            view.withId(R.id.account_email_textview).withText(email).isEnabled().checkIsDisplayed()
        }

        fun accountLoggedOut(email: String) {
            view.withId(R.id.account_name_textview).withText(email).isDisabled().checkIsDisplayed()
        }

        fun accountRemoved(username: String) {
            view.withId(R.id.account_name_textview).withText(username).checkDoesNotExist()
        }

        fun switchedToAccount(username: String) {
            val accountFirstPosition = 0
            onView(withId(accountsRecyclerViewId))
                .check(matches(withViewAtPosition(accountFirstPosition, hasDescendant(withText(username)))))
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block) as AccountPanelRobot

    companion object {

        const val accountsRecyclerViewId = R.id.account_list_recyclerview
    }
}
