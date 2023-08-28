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

package ch.protonmail.android.uitests.tests.drafts

import androidx.compose.ui.res.stringResource
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.device.DeviceRobot
import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.fwSubject
import ch.protonmail.android.uitests.testsHelper.TestData.updatedSubject
import ch.protonmail.android.uitests.testsHelper.TestUser.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import me.proton.fusion.utils.StringUtils.stringFromResource
import kotlin.test.BeforeTest
import kotlin.test.Test

class DraftsTests : BaseTest() {

    private val loginRobot = LoginMailRobot()
    private val deviceRobot = DeviceRobot()
    private val composerRobot = ComposerRobot()
    private lateinit var subject: String
    private lateinit var body: String
    private lateinit var to: String

    @BeforeTest
    override fun setUp() {
        super.setUp()
        subject = TestData.messageSubject
        body = TestData.messageBody
        to = internalEmailTrustedKeys.email
    }

    @TestId("29753")
    @Test
    fun saveDraft() {
        loginRobot
            .loginOnePassUser()
            .compose()
            .draftToSubjectBody(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("29754")
    @Test
    fun saveDraftWithAttachment() {
        loginRobot
            .loginOnePassUser()
            .compose()
            .draftSubjectBodyAttachment(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1383")
    @Test
    fun sendDraftWithAttachment() {
        loginRobot
            .loginOnePassUser()
            .compose()
            .draftSubjectBodyAttachment(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(subject)
            .send()
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }

    @TestId("4278")
    @Test
    fun changeDraftSender() {
        val onePassUserSecondEmail = "2${onePassUser.email}"
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .draftToSubjectBody(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(subject)
            .changeSenderTo(onePassUserSecondEmail)
            .clickUpButton()
            .confirmDraftSavingFromDrafts()
            .clickDraftBySubject(subject)
            .verify { fromEmailIs(onePassUserSecondEmail) }
    }

    @TestId("1384")
    @Test
    fun addRecipientsToDraft() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .draftSubjectBody(subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .clickDraftBySubject(subject)
            .recipients(to)
            .clickUpButton()
            .confirmDraftSavingFromDrafts()
            .verify { messageWithSubjectAndRecipientExists(subject, to) }
    }

    @TestId("1382")
    @Test
    fun openDraftFromSearch() {
        loginRobot
            .loginOnePassUser()
            .compose()
            .draftToSubjectBody(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .refreshMessageList()
            .searchBar()
            .searchMessageText(subject)
            .clickSearchedDraftBySubject(subject)
            .verify { messageWithSubjectOpened(subject) }
    }

    @TestId("29754")
    @Test
    fun addAttachmentToDraft() {
        loginRobot
            .loginOnePassUser()
            .compose()
            .draftToSubjectBody(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(subject)
            .attachments()
            .addFileAttachment(welcomeDrawable)
            .navigateUpToComposer()
            .confirmDraftSavingFromDrafts()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .verify { attachmentsAdded() }
    }

    @TestId("1379")
    @Test
    fun minimiseTheAppWhileReplyingToMessage() {
        loginRobot
            .loginOnePassUser()
            .refreshMessageList()
            .clickMessageByPosition(0)
            .openActionSheet()
            .reply()
            .draftSubjectBody(subject, body)

        deviceRobot
            .clickHomeButton()
            .clickRecentAppsButton()
            .clickRecentAppView()

        composerRobot
            .verify {
                messageWithSubjectOpened(subject)
                bodyWithText(body)
            }
    }

    @TestId("1381")
    @Test
    fun saveDraftWithoutSubject() {
        val noSubject = stringFromResource(R.string.empty_subject)
        loginRobot
            .loginOnePassUser()
            .compose()
            .draftToBody(to, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftByPosition(0)
            .verify {
                messageWithSubjectOpened(noSubject)
                bodyWithText(body)
            }
    }

    @TestId("1385")
    @Test
    fun savingDraftWithHyphens() {
        val bodyWithHyphens = "This-is-body-with-hyphens!"
        val subjectWithHyphens = "This-is-subject-with-hyphens-$subject"
        loginRobot
            .loginOnePassUser()
            .compose()
            .draftSubjectBody(subjectWithHyphens, bodyWithHyphens)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(subjectWithHyphens)
            .verify {
                messageWithSubjectOpened(subjectWithHyphens)
                bodyWithText(bodyWithHyphens)
            }
    }

    @TestId("35842")
    @Test
    fun editDraftMultipleTimesAndSend() {
        val subjectEditOne = "Edit one $subject"
        val subjectEditTwo = "Edit two $subject"
        val bodyEditOne = "Edit one $body"
        val bodyEditTwo = "Edit two $body"
        loginRobot
            .loginOnePassUser()
            .compose()
            .draftToSubjectBody(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()

            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(subject)
            .attachments()
            .addFileAttachment(welcomeDrawable)
            .navigateUpToComposer()
            .draftSubjectBody(subjectEditOne, bodyEditOne)
            .clickUpButton()
            .confirmDraftSavingFromDrafts()
            .refreshMessageList()
            .clickDraftBySubject(subjectEditOne)
            .draftSubjectBody(subjectEditTwo, bodyEditTwo)
            .clickUpButton()
            .confirmDraftSavingFromDrafts()
            .refreshMessageList()
            .clickDraftBySubject(subjectEditTwo)
            .verify {
                messageWithSubjectOpened(subjectEditTwo)
                bodyWithText(bodyEditTwo)
            }
    }

    @TestId("43003")
    @Test
    fun replaceAttachmentsSavingDraftsAndSend() {
        loginRobot
            .loginOnePassUser()
            .refreshMessageList()
            .compose()
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .openActionSheet()
            .forward()
            .draftToBody(to, body)
            .clickUpButton()
            .confirmDraftSavingFromSent()
            .navigateUpToSent()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(fwSubject(subject))
            .attachments()
            .removeAttachmentAtPosition(0)
            .navigateUpToComposer()
            .clickUpButton()
            .confirmDraftSavingFromDrafts()
            .refreshMessageList()
            .clickDraftBySubject(fwSubject(subject))
            .attachments()
            .addFileAttachment(welcomeDrawable)
            .navigateUpToComposer()
            .updateSubject(subject)
            .send()
            .menuDrawer()
            .sent()
            .clickMessageBySubject(updatedSubject(subject))
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }

    private companion object {

        const val welcomeDrawable = R.drawable.welcome
    }
}
