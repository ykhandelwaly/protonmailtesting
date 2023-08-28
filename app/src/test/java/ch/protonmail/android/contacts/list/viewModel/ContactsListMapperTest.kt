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

package ch.protonmail.android.contacts.list.viewModel

import android.graphics.Color
import ch.protonmail.android.R
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.contacts.groups.list.ContactGroupListItem
import ch.protonmail.android.contacts.list.listView.ContactItem
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContactsListMapperTest {

    private val mapper = ContactsListMapper()
    private val contactId1 = "contactId1"
    private val name1 = "John Doe Italiano"
    private val contactId2 = "contactId2"
    private val name2 = "Perro Caliente Comer Manana"
    private val email1 = "email1@abc.com"
    private val email2 = "email2@abc.com"
    private val testPath = "test/path123"
    private val contactItem1 = ContactItem(
        isProtonMailContact = true,
        name = name1,
        contactId = contactId1,
        contactEmails = email1,
        initials = "JD",
        additionalEmailsCount = 0,
        isSelected = false,
        headerStringRes = null,
    )
    private val contactItem2 = ContactItem(
        isProtonMailContact = true,
        name = name2,
        contactId = contactId2,
        contactEmails = email2,
        initials = "PC",
        additionalEmailsCount = 0,
        isSelected = false,
        headerStringRes = null,
    )
    private val testColorInt = 321

    @BeforeTest
    fun setUp() {
        mockkStatic(Color::class)
        every { Color.parseColor(any()) } returns testColorInt
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun verifyThatEmptyContactsAreMappedProperly() {
        // given
        val dataList = listOf<ContactData>()
        val emailsList = listOf<ContactEmail>()
        val expected = emptyList<ContactItem>()

        // when
        val result = mapper.mapToContactItems(dataList, emailsList)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatContactsAreMappedProperly() {
        // given

        val contact1 = ContactData(contactId = contactId1, name = name1)

        val contact2 = ContactData(contactId = contactId2, name = name2)
        val dataList = listOf(contact1, contact2)
        val contactEmail1 = ContactEmail(
            contactEmailId = "contactEmailId1", email = email1, name = "emailName1", contactId = contactId1,
            lastUsedTime = 111
        )
        val contactEmail2 = ContactEmail(
            contactEmailId = "contactEmailId2", email = email2, name = "emailName1", contactId = contactId2,
            lastUsedTime = 112
        )
        val emailsList = listOf(contactEmail1, contactEmail2)
        val expected = listOf(
            contactItem1,
            contactItem2
        )

        // when
        val result = mapper.mapToContactItems(dataList, emailsList)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyContactsAreMergedProperlyWithoutOverlappingContacts() {
        // given
        val contactId3 = "contactId3"
        val name3 = "name1"
        val email3 = "email3@abc.com"
        val contactItem3 = ContactItem(
            isProtonMailContact = true,
            name = name3,
            contactId = contactId3,
            contactEmails = email3,
            initials = EMPTY_STRING,
            additionalEmailsCount = 0,
            isSelected = false,
            headerStringRes = null,
        )
        val protonHeaderItem =
            ContactItem(
                isProtonMailContact = true,
                headerStringRes = R.string.protonmail_contacts,
            )
        val otherHeaderItem =
            ContactItem(
                isProtonMailContact = false,
                headerStringRes = R.string.device_contacts,
            )
        val protonContacts = listOf(contactItem1, contactItem2)
        val androidContacts = listOf(contactItem3)
        val expected = listOf(
            protonHeaderItem, contactItem1, contactItem2, otherHeaderItem, contactItem3
        )

        // when
        val result = mapper.mergeContactItems(protonContacts, androidContacts)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyContactsAreMergedProperlyWithOneOverlappingContactSkipped() {
        // given
        val contactId3 = "contactId3"
        val name3 = "name3"
        val contactItem3 = ContactItem(
            isProtonMailContact = true,
            name = name3,
            contactId = contactId3,
            contactEmails = email1,
            initials = EMPTY_STRING,
            additionalEmailsCount = 0,
            isSelected = false,
            headerStringRes = null,
        )
        val headerItem =
            ContactItem(
                isProtonMailContact = true,
                headerStringRes = R.string.protonmail_contacts,
            )
        val protonContacts = listOf(contactItem1, contactItem2)
        val androidContacts = listOf(contactItem3)
        val expected = listOf(headerItem, contactItem1, contactItem2)

        // when
        val result = mapper.mergeContactItems(protonContacts, androidContacts)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatContactLabelIsConvertedToContactGroupListUiElementCorrectly() {
        // given
        val testId = "ID1"
        val testName = "name1"

        val label1 =
            ContactLabelUiModel(
                LabelId(testId), testName, "green", LabelType.MESSAGE_LABEL, testPath, "parentId", 1
            )
        val expected = ContactGroupListItem(
            contactId = testId,
            name = testName,
            contactEmailsCount = 1,
            color = testColorInt,
        )

        // when
        val result = mapper.mapLabelToContactGroup(label1)

        // then
        assertEquals(expected, result)
    }


    @Test
    fun verifyThatListOfContactLabelsIsConvertedToContactGroupListUiElementCorrectly() {
        // given
        val testId = "ID1"
        val testId2 = "ID2"
        val testName = "name1"
        val testName2 = "name2"
        val label1 =
            ContactLabelUiModel(LabelId(testId), testName, "green", LabelType.MESSAGE_LABEL, testPath, "", 0)
        val label2 = ContactLabelUiModel(
            LabelId(testId2), testName2, "yellow", LabelType.MESSAGE_LABEL, testPath, "0", 0
        )
        val listItem1 = ContactGroupListItem(
            contactId = testId,
            name = testName,
            contactEmailsCount = 0,
            color = testColorInt,
        )
        val listItem2 = ContactGroupListItem(
            contactId = testId2,
            name = testName2,
            contactEmailsCount = 0,
            color = testColorInt,
        )
        val expected = listOf(listItem1, listItem2)

        // when
        val result = mapper.mapLabelsToContactGroups(listOf(label1, label2))

        // then
        assertEquals(expected, result)
    }
}
