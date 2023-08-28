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
package ch.protonmail.android.uitests.robots.mailbox

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedDiagnosingMatcher
import androidx.test.espresso.matcher.BoundedMatcher
import ch.protonmail.android.R
import ch.protonmail.android.adapters.messages.MailboxItemViewHolder
import kotlinx.android.synthetic.main.item_manage_labels_action.view.*
import kotlinx.android.synthetic.main.labels_list_item.view.*
import kotlinx.android.synthetic.main.list_item_contacts.view.*
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Matchers that are used by Mailbox features like Inbox, Sent, Drafts, Trash, etc.
 */
object MailboxMatchers {

    /**
     * Matches the Mailbox message represented by [MessageViewHolder] by message subject.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param subject - message subject
     */
    fun withMessageSubject(subject: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            MailboxItemViewHolder.MessageViewHolder>(MailboxItemViewHolder.MessageViewHolder::class.java) {

            val messagesList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("Message item with subject: \"$subject\"\n")
                description.appendText("Here is the actual list of messages:\n")
                messagesList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: MailboxItemViewHolder.MessageViewHolder): Boolean {
                val messageSubjectView = item.itemView.findViewById<TextView>(R.id.subject_text_view)
                val actualSubject = messageSubjectView.text.toString()
                return if (messageSubjectView != null) {
                    messagesList.add(actualSubject)
                    subject == actualSubject
                } else {
                    false
                }
            }
        }
    }

    /**
     * Matches the Mailbox message represented by [MessageViewHolder] by message subject and Reply, Reply all or
     * Forward flag. Subject must be unique in a list in order to use this matcher.
     *
     * @param subject - message subject
     * @param id - the view id of Reply, Reply all or Forward [TextView].
     */
    fun withMessageSubjectAndFlag(subject: String, @IdRes id: Int): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedDiagnosingMatcher<RecyclerView.ViewHolder,
            MailboxItemViewHolder.MessageViewHolder>(MailboxItemViewHolder.MessageViewHolder::class.java) {

            val messagesList = ArrayList<String>()

            override fun matchesSafely(
                item: MailboxItemViewHolder.MessageViewHolder?, mismatchDescription: Description?
            ): Boolean {
                val messageSubjectView = item!!.itemView.findViewById<TextView>(R.id.subject_text_view)
                val actualSubject = messageSubjectView.text.toString()
                val flagView = item.itemView.findViewById<ImageView>(id)
                return if (messageSubjectView != null) {
                    messagesList.add("$actualSubject, flag visibility: ${flagView.visibility}")
                    subject == actualSubject && flagView.visibility == View.VISIBLE
                } else {
                    false
                }
            }

            override fun describeMoreTo(description: Description?) {
                description?.apply {
                    appendText("Message item with subject: \"$subject\"\n")
                    appendText("Here is the actual list of messages:\n")
                }
                messagesList.forEach { description?.appendText(" - \"$it\"\n") }
            }
        }
    }

    /**
     * Matches the Mailbox message represented by [MessageViewHolder] by message subject and Reply, Reply all or
     * Forward flag. Subject must be unique in a list in order to use this matcher.
     *
     * @param subject - message subject
     * @param id - the view id of Reply, Reply all or Forward [TextView].
     */
    // TODO: Rewrite this to work well with location icons with MAILAND-1422. The test that uses this matcher is temporarily ignored.
    fun withMessageSubjectAndLocation(subject: String, locationText: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedDiagnosingMatcher<RecyclerView.ViewHolder,
            MailboxItemViewHolder.MessageViewHolder>(MailboxItemViewHolder.MessageViewHolder::class.java) {

            val messagesList = ArrayList<String>()

            override fun matchesSafely(
                item: MailboxItemViewHolder.MessageViewHolder?, mismatchDescription: Description?
            ): Boolean {
                val messageSubjectView = item!!.itemView.findViewById<TextView>(R.id.subject_text_view)
                val actualSubject = messageSubjectView.text.toString()
                val locationView = item.itemView.findViewById<ImageView>(R.id.first_location_image_view)
                return if (messageSubjectView != null) {
//                    messagesList.add("$actualSubject, location: ${locationView.text}")
//                    subject == actualSubject && locationView.text == locationText
                    subject == actualSubject && locationView.visibility == View.VISIBLE
                } else {
                    false
                }
            }

            override fun describeMoreTo(description: Description?) {
                description?.apply {
                    appendText("Message item with subject: \"$subject\" and location text: $locationText\n")
                    appendText("Here is the actual list of messages:\n")
                }
                messagesList.forEach { description?.appendText(" - \"$it\"\n") }
            }
        }
    }

    /**
     * Matches the Mailbox message represented by [MessageViewHolder] by message subject and sender.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param subject - message subject
     * @param to - message sender email
     */
    fun withMessageSubjectAndRecipient(subject: String, to: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            MailboxItemViewHolder.MessageViewHolder>(MailboxItemViewHolder.MessageViewHolder::class.java) {

            val messagesList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("Message item with subject and recipient: \"$subject\"\n")
                description.appendText("Here is the actual list of messages:\n")
                messagesList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: MailboxItemViewHolder.MessageViewHolder): Boolean {
                val messageSubjectView = item.itemView.findViewById<TextView>(R.id.subject_text_view)
                val messageToTextView = item.itemView.findViewById<TextView>(R.id.correspondents_text_view)
                val actualSubject = messageSubjectView.text.toString()
                val actualTo = messageToTextView.text.toString()
                return if (messageSubjectView != null && messageToTextView != null) {
                    messagesList.add("Subject: $actualSubject, to: $actualTo")
                    subject == actualSubject && to == actualTo
                } else {
                    false
                }
            }
        }
    }

    /**
     * Matches the Mailbox message represented by [MessageViewHolder] by message subject part.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param subjectPart - message subject part
     */
    fun withMessageSubjectContaining(subjectPart: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            MailboxItemViewHolder.MessageViewHolder>(MailboxItemViewHolder.MessageViewHolder::class.java) {

            val messagesList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("Message item that contains pattern: \"$subjectPart\"\n")
                description.appendText("Here is the actual list of messages:\n")
                messagesList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: MailboxItemViewHolder.MessageViewHolder): Boolean {
                val messageSubjectView = item.itemView.findViewById<TextView>(R.id.subject_text_view)
                val actualSubject = messageSubjectView.text.toString()
                return if (actualSubject.contains(subjectPart)) {
                    true
                } else {
                    messagesList.add(actualSubject)
                    false
                }
            }
        }
    }

    /**
     * Matches the first instance of Mailbox message represented by [MessageViewHolder] that contains provided text.
     * Can be used in cases when multiple messages have similar subject.
     *
     * @param text - that supposed to be the part of the message subject
     */
    fun withFirstInstanceMessageSubject(text: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            MailboxItemViewHolder.MessageViewHolder>(MailboxItemViewHolder.MessageViewHolder::class.java) {

            private var alreadyMatched = false
            val messagesList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("Message item that contains subject text: $text.\nIterated subjects:\n")
                description.appendText("Here is the actual list of messages:\n")
                messagesList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: MailboxItemViewHolder.MessageViewHolder): Boolean {
                val messageSubjectView = item.itemView.findViewById<TextView>(R.id.subject_text_view)
                val actualSubject = messageSubjectView.text.toString()
                if (messageSubjectView != null) {
                    /** since we need only the first match [alreadyMatched] var acts as a guard for other matches **/
                    val matched = !alreadyMatched && messageSubjectView.text.toString().contains(text)
                    if (matched) alreadyMatched = true
                    messagesList.add(actualSubject)
                    return matched
                }
                return false
            }
        }
    }

//    fun withFolderName(name: String): TypeSafeMatcher<FoldersAdapter.FolderItem> =
//        withFolderName(equalTo(name))
//
//    fun withFolderName(nameMatcher: Matcher<out Any?>): TypeSafeMatcher<FoldersAdapter.FolderItem> {
//        return object : TypeSafeMatcher<FoldersAdapter.FolderItem>(FoldersAdapter.FolderItem::class.java) {
//            override fun matchesSafely(item: FoldersAdapter.FolderItem): Boolean {
//                return nameMatcher.matches(item.name)
//            }
//
//            override fun describeTo(description: Description) {
//                description.appendText("with item content: ")
//            }
//        }
//    }
//
//    fun withLabelName(name: String): TypeSafeMatcher<LabelsAdapter.LabelItem> =
//        withLabelName(equalTo(name))
//
//    fun withLabelName(nameMatcher: Matcher<out Any?>): TypeSafeMatcher<LabelsAdapter.LabelItem> {
//        return object : TypeSafeMatcher<LabelsAdapter.LabelItem>(LabelsAdapter.LabelItem::class.java) {
//            override fun matchesSafely(item: LabelsAdapter.LabelItem): Boolean {
//                return nameMatcher.matches(item.name)
//            }
//
//            override fun describeTo(description: Description) {
//                description.appendText("with item content: ")
//            }
//        }
//    }
}
