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

package ch.protonmail.android.testdata

import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.presentation.model.ConversationUiModel

object ConversationTestData {
    private const val MESSAGE_COUNT = 10
    private const val SUBJECT = "Proton Mail v4 Android app is out now!"

    val withNonDraftsOnly = ConversationUiModel(
        isStarred = false,
        subject = SUBJECT,
        messages = (1..MESSAGE_COUNT).map {
            Message(
                messageId = "$it",

            )
        },
        messagesCount = MESSAGE_COUNT
    )

    val withDraftsOnly = withNonDraftsOnly.copy(
        messages = withNonDraftsOnly.messages.map { message ->
            message.copy(allLabelIDs = listOf(Constants.MessageLocationType.DRAFT.asLabelIdString()))
        }
    )

    val withDraftAsLastMessage = withNonDraftsOnly.copy().apply {
        messages.last().apply {
            allLabelIDs = listOf(Constants.MessageLocationType.DRAFT.asLabelIdString())
        }
    }
}
