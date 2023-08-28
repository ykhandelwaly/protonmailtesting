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

package ch.protonmail.android.mailbox.domain

import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.domain.usecase.UpdateConversationsLabels
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the behaviour of [UpdateConversationsLabels]
 */
class UpdateConversationsLabelsTest {

    private val conversationsRepository = mockk<ConversationsRepository>()
    private val labelRepository = mockk<LabelRepository>()
    private val testPath = "a/bpath"
    private val testParentId = "parentIdForTests"

    private val allLabels = (1..3).map { count ->
        Label(
            id = LabelId("label$count"),
            name = "name$count",
            color = "color",
            order = 0,
            type = if (count > 2) LabelType.FOLDER else LabelType.MESSAGE_LABEL,
            testPath,
            testParentId,
        )
    }

    private val updateConversationsLabels = UpdateConversationsLabels(
        conversationsRepository,
        labelRepository
    )

    @Test
    fun verifyLabelUnlabelMethodsFromRepositoryAreCalled() {
        runBlockingTest {
            // given
            val conversationIds = listOf("conversationId1", "conversationId2")
            val userId = UserId("userId")
            val label1 = "label1"
            val label2 = "label2"
            val label3 = "label3"
            val selectedLabels = listOf(label1, label2)
            coEvery {
                labelRepository.findAllLabels(userId)
            } returns allLabels
            coEvery {
                conversationsRepository.label(
                    conversationIds, userId, any()
                )
            } returns ConversationsActionResult.Success
            coEvery {
                conversationsRepository.unlabel(
                    conversationIds, userId, any()
                )
            } returns ConversationsActionResult.Success

            // when
            updateConversationsLabels.invoke(conversationIds, userId, selectedLabels)

            // then
            coVerify {
                conversationsRepository.label(conversationIds, userId, label1)
            }
            coVerify {
                conversationsRepository.label(conversationIds, userId, label2)
            }
            coVerify {
                conversationsRepository.unlabel(conversationIds, userId, label3)
            }
        }
    }

    @Test
    fun verifyUseCaseReturnsErrorResultWhenAtLeastOneRepositoryCallReturnsErrorResult() {
        runBlockingTest {
            // given
            val conversationIds = listOf("conversationId1", "conversationId2")
            val userId = UserId("userId")
            val label1 = "label1"
            val label2 = "label2"
            val selectedLabels = listOf(label1, label2)
            val expectedResult = ConversationsActionResult.Error
            coEvery {
                labelRepository.findAllLabels(userId)
            } returns allLabels
            coEvery {
                conversationsRepository.label(conversationIds, userId, any())
            } returns ConversationsActionResult.Success
            coEvery {
                conversationsRepository.unlabel(conversationIds, userId, any())
            } returns ConversationsActionResult.Error

            // when
            val result = updateConversationsLabels.invoke(conversationIds, userId, selectedLabels)

            // then
            assertEquals(expectedResult, result)
        }
    }
}
