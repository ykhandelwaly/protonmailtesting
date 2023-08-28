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

package ch.protonmail.android.usecase.message

import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

/**
 * Tests the behaviour of [ChangeMessagesStarredStatus]
 */
class ChangeMessagesStarredStatusTest {

    private val messageIds = listOf("messageId1", "messageId2")
    private val userId = UserId("userId")

    private val messageRepository: MessageRepository = mockk {
        coEvery { starMessages(messageIds) } just runs
        coEvery { unStarMessages(messageIds) } just runs
    }

    private val conversationsRepository: ConversationsRepository = mockk {
        coEvery {
            updateConvosBasedOnMessagesStarredStatus(
                userId,
                messageIds,
                any()
            )
        } just runs
    }

    private val changeMessagesStarredStatus = ChangeMessagesStarredStatus(messageRepository, conversationsRepository)

    @Test
    fun verifyCorrectMethodsAreCalledWhenActionIsStar() = runBlockingTest {
        // given
        val action = ChangeMessagesStarredStatus.Action.ACTION_STAR

        // when
        changeMessagesStarredStatus(userId, messageIds, action)

        // then
        coVerify {
            messageRepository.starMessages(messageIds)
            conversationsRepository.updateConvosBasedOnMessagesStarredStatus(
                userId,
                messageIds,
                action
            )
        }
    }

    @Test
    fun verifyCorrectMethodsAreCalledWhenActionIsUnstar() = runBlockingTest {
        // given
        val action = ChangeMessagesStarredStatus.Action.ACTION_UNSTAR

        // when
        changeMessagesStarredStatus(userId, messageIds, action)

        // then
        coVerify {
            messageRepository.unStarMessages(messageIds)
            conversationsRepository.updateConvosBasedOnMessagesStarredStatus(
                userId,
                messageIds,
                action
            )
        }
    }
}
