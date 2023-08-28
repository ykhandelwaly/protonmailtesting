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

package ch.protonmail.android.labels.data.remote.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.api.models.MoveToFolderResponse
import ch.protonmail.android.data.local.CounterRepository
import ch.protonmail.android.worker.KEY_WORKER_ERROR_DESCRIPTION
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplyMessageLabelWorkerTest {

    private val context = mockk<Context>()

    private val parameters = mockk<WorkerParameters>(relaxed = true)

    private val api = mockk<ProtonMailApi>()

    private val accountManager = mockk<AccountManager>()

    private val counterRepository = mockk<CounterRepository>()

    private lateinit var worker: ApplyMessageLabelWorker

    private val testUserId = UserId("testUser")

    @BeforeTest
    fun setUp() {
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)

        worker = ApplyMessageLabelWorker(
            context,
            parameters,
            accountManager,
            counterRepository,
            api
        )
    }

    @Test
    fun verifyWorkerFailsWithNoLabelIdProvided() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty label id or message ids")
            )
            val response = mockk<MoveToFolderResponse>()
            every { api.labelMessages(any()) } returns response

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
    }

    @Test
    fun verifyWorkerSuccessesWithRequiredParametersProvided() {
        runBlockingTest {
            // given
            val testMessageId = "id1"
            val testLabelId = "LabelId1"
            val expected = ListenableWorker.Result.success()

            every { parameters.inputData } returns workDataOf(
                KEY_INPUT_DATA_MESSAGES_IDS to arrayOf(testMessageId),
                KEY_INPUT_DATA_LABEL_ID to testLabelId
            )

            val response = mockk<MoveToFolderResponse>()
            every { api.labelMessages(any()) } returns response
            coEvery {
                counterRepository.updateMessageLabelCounter(
                    testUserId,
                    testLabelId,
                    listOf(testMessageId),
                    CounterRepository.CounterModificationMethod.INCREMENT
                )
            } returns Unit

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
            coVerify {
                counterRepository.updateMessageLabelCounter(
                    testUserId,
                    testLabelId,
                    listOf(testMessageId),
                    CounterRepository.CounterModificationMethod.INCREMENT
                )
            }
        }
    }
}
