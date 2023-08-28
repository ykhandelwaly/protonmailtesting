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

package ch.protonmail.android.uitests.robots.contacts

import ch.protonmail.android.R
import me.proton.fusion.Fusion

/**
 * [ManageAddressesRobot] class contains actions and verifications for Adding a Contact to Group.
 */
class ManageAddressesRobot : Fusion {

    fun addContactToGroup(withEmail: String): AddContactGroupRobot = selectAddress(withEmail).done()

    private fun selectAddress(email: String): ManageAddressesRobot {
        recyclerView
            .withId(contactsRecyclerView)
            .onHolderItem(ContactsMatchers.withContactEmailInManageAddressesView(email))
            .click()
        return this
    }

    private fun done(): AddContactGroupRobot {
        view.withId(R.id.action_save).click()
        return AddContactGroupRobot()
    }

    companion object {

        const val contactsRecyclerView = R.id.contactEmailsRecyclerView
    }
}
