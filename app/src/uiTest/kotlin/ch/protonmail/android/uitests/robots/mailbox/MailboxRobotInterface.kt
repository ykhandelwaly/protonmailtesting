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
@file:Suppress("UNCHECKED_CAST")

package ch.protonmail.android.uitests.robots.mailbox

import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withFirstInstanceMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubjectAndFlag
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubjectAndRecipient
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitests.robots.mailbox.search.SearchRobot
import ch.protonmail.android.uitests.robots.menu.MenuRobot
import ch.protonmail.android.uitests.testsHelper.StringUtils
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.TIMEOUT_15S
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.TIMEOUT_30S
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.TIMEOUT_60S
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.saveMessageSubject
import ch.protonmail.android.uitests.testsHelper.waitForCondition
import me.proton.fusion.Fusion

interface MailboxRobotInterface : Fusion {

    fun swipeLeftMessageAtPosition(position: Int): Any {
        saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetSwipeLeftMessage)())
        recyclerView
            .withId(messagesRecyclerViewId)
            .onItemAtPosition(position)
            .swipeLeft()
        return Any()
    }

    fun longClickMessageOnPosition(position: Int): Any {
        saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetLongClickMessage)())
        recyclerView
            .withId(messagesRecyclerViewId)
            .onItemAtPosition(position)
            .longClick()
        return Any()
    }

    fun deleteMessageWithSwipe(subject: String): Any {
        recyclerView.onHolderItem(withMessageSubject(subject)).swipeRight()
        return Any()
    }

    fun deleteMessageWithSwipe(position: Int): Any {
        recyclerView
            .withId(messagesRecyclerViewId)
            .onItemAtPosition(position)
            .withTimeout(TIMEOUT_15S)
            .swipeRight()
        return Any()
    }

    fun searchBar(): SearchRobot {
        view.withId(R.id.search).click()
        return SearchRobot()
    }

    fun compose(): ComposerRobot {
        view.withId(R.id.compose).withTimeout(TIMEOUT_60S).click()
        return ComposerRobot()
    }

    fun menuDrawer(): MenuRobot {
        waitForCondition(
            { onView(withId(drawerLayoutId)).check(matches(isDisplayed())) },
            watchTimeout = TIMEOUT_60S
        )
        view.withId(drawerLayoutId).openDrawer()
        return MenuRobot()
    }

    fun clickMessageByPosition(position: Int): MessageRobot {
        saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetSelectMessage)())
        recyclerView
            .withId(messagesRecyclerViewId)
            .onItemAtPosition(position)
            .click()
        return MessageRobot()
    }

    fun clickMessageBySubject(subject: String): MessageRobot {
        view.withId(R.id.subject_text_view).withText(subject).click()
        return MessageRobot()
    }

    fun clickFirstMatchedMessageBySubject(subject: String): MessageRobot {
        view.instanceOf(ImageView::class.java).hasParent(view.withId(R.id.mailboxRecyclerView)).checkDoesNotExist()
        recyclerView
            .withId(messagesRecyclerViewId)
            .onHolderItem(withFirstInstanceMessageSubject(subject))
            .click()
        return MessageRobot()
    }

    fun refreshMessageList(): Any {
        view.withId(messagesRecyclerViewId).swipeDown()
        // Waits for loading icon to disappear
        view.instanceOf(ImageView::class.java).hasParent(view.withId(messagesRecyclerViewId)).checkDoesNotExist()
        return Any()
    }

    fun mailboxLayoutShown() {
//        recyclerView.withId(messagesRecyclerViewId).waitUntilPopulated()
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    @Suppress("ClassName")
    open class verify : Fusion {

        fun messageExists(messageSubject: String) {
            view.withId(messageTitleTextViewId).withText(messageSubject).checkIsDisplayed()
        }

        fun draftWithAttachmentSaved(draftSubject: String) {
            view.withId(messageTitleTextViewId).withText(draftSubject).checkIsDisplayed()
        }

        fun messageMovedToTrash(subject: String) {
            view.withText(R.string.swipe_action_trash).waitForDisplayed().checkIsDisplayed()
            view.withId(R.id.subject_text_view).withText(subject).waitUntilGone().checkDoesNotExist()
        }

        fun messageDeleted(subject: String) {
            view.withId(R.id.subject_text_view).withText(subject).checkDoesNotExist()
        }

        fun multipleMessagesMovedToTrash(subjectMessageOne: String, subjectMessageTwo: String) {
            val messagesMovedToTrash = StringUtils.quantityStringFromResource(R.plurals.action_move_to_trash, 2)
            view.withText(messagesMovedToTrash).waitForDisplayed().checkIsDisplayed()
            view.withId(R.id.subject_text_view).withText(subjectMessageOne).checkDoesNotExist()
            view.withId(R.id.subject_text_view).withText(subjectMessageTwo).checkDoesNotExist()
        }

        fun multipleMessagesDeleted(subjectMessageOne: String, subjectMessageTwo: String) {
            view.withId(R.id.subject_text_view).withText(subjectMessageOne).checkDoesNotExist()
            view.withId(R.id.subject_text_view).withText(subjectMessageTwo).checkDoesNotExist()
        }

        fun messageWithSubjectExists(subject: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
                .withTimeout(TIMEOUT_30S)
                .scrollToHolder(withFirstInstanceMessageSubject(subject))
        }

        fun messageWithSubjectHasRepliedFlag(subject: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
                .scrollToHolder(withMessageSubjectAndFlag(subject, R.id.reply_image_view))
        }

        fun messageWithSubjectHasRepliedAllFlag(subject: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
                .scrollToHolder(withMessageSubjectAndFlag(subject, R.id.reply_all_image_view))
        }

        fun messageWithSubjectHasForwardedFlag(subject: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
                .withTimeout(TIMEOUT_30S)
                .scrollToHolder(withMessageSubjectAndFlag(subject, R.id.forward_image_view))
        }

        fun messageWithSubjectAndRecipientExists(subject: String, to: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
                .scrollToHolder(withMessageSubjectAndRecipient(subject, to))
        }
    }

    private class SetLongClickMessage : (String, String) -> Unit {

        override fun invoke(subject: String, date: String) {
            longClickedMessageSubject = subject
            longClickedMessageDate = date
        }
    }

    private class SetSwipeLeftMessage : (String, String) -> Unit {

        override fun invoke(subject: String, date: String) {
            swipeLeftMessageSubject = subject
            swipeLeftMessageDate = date
        }
    }

    private class SetDeleteWithSwipeMessage : (String, String) -> Unit {

        override fun invoke(subject: String, date: String) {
            deletedMessageSubject = subject
            deletedMessageDate = date
        }
    }

    class SetSelectMessage : (String, String) -> Unit {

        override fun invoke(subject: String, date: String) {
            selectedMessageSubject = subject
            selectedMessageDate = date
        }
    }

    companion object {

        //    TODO replace below line with core test lib code
        fun saveMessageSubjectAtPosition(
            @IdRes recyclerViewId: Int,
            position: Int,
            method: (String, String) -> Unit
        ): ViewInteraction = Espresso.onView(ViewMatchers.withId(recyclerViewId))
            .perform(saveMessageSubject(position, method))

        var longClickedMessageSubject = ""
        var longClickedMessageDate = ""
        var swipeLeftMessageSubject = ""
        var swipeLeftMessageDate = ""
        var selectedMessageSubject = ""
        var selectedMessageDate = ""
        var deletedMessageSubject = ""
        var deletedMessageDate = ""

        private const val messagesRecyclerViewId = R.id.mailboxRecyclerView
        private const val messageTitleTextViewId = R.id.subject_text_view
        private const val drawerLayoutId = R.id.drawer_layout
    }
}
