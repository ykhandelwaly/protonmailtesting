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

package ch.protonmail.android.mailbox.data.remote.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.data.remote.worker.KEY_LABEL_WORKER_ERROR_DESCRIPTION
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class MoveMessageToLocationWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val workManager = mockk<WorkManager>()

    private val protonMailApiManager = mockk<ProtonMailApiManager>()

    private val moveMessageToLocationWorker = MoveMessageToLocationWorker(
        context,
        workerParameters,
        protonMailApiManager
    )
    private val postToLocationWorkerEnqueuer = MoveMessageToLocationWorker.Enqueuer(
        workManager
    )

    private val testUserName = "userName1"

    @Test
    fun shouldEnqueueWorkerSuccessfullyWhenEnqueuerIsCalled() {
        // given
        val testUserId = UserId(testUserName)
        val messageIds = listOf("id1", "id2")
        val newLocation = Constants.MessageLocationType.INBOX
        val operationMock = mockk<Operation>()
        every { workManager.enqueue(any<WorkRequest>()) } returns operationMock

        // when
        val operationResult = postToLocationWorkerEnqueuer.enqueue(testUserId, messageIds, newLocation)

        // then
        assertEquals(operationMock, operationResult)
    }

    @Test
    fun shouldReturnSuccessIfApiCallIsSuccessful() {
        runBlockingTest {
            // given
            val messageIds = arrayOf("id1", "id2")
            val newLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue
            every {
                workerParameters.inputData.getString(KEY_POST_WORKER_USER_ID)
            } returns testUserName
            every {
                workerParameters.inputData.getStringArray(KEY_POST_WORKER_MESSAGE_ID)
            } returns messageIds
            every {
                workerParameters.inputData.getInt(KEY_POST_WORKER_LOCATION_ID, -1)
            } returns newLocation
            coEvery {
                protonMailApiManager.labelMessagesBlocking(
                    any(), UserId(testUserName)
                )
            } returns mockk()

            val expectedResult = ListenableWorker.Result.success()

            // when
            val result = moveMessageToLocationWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnFailureIfMessagesIdsListIsNull() {
        runBlockingTest {
            // given
            val newLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue
            every {
                workerParameters.inputData.getString(KEY_POST_WORKER_USER_ID)
            } returns testUserName
            every {
                workerParameters.inputData.getStringArray(KEY_POST_WORKER_MESSAGE_ID)
            } returns null
            every {
                workerParameters.inputData.getInt(KEY_POST_WORKER_LOCATION_ID, -1)
            } returns newLocation
            coEvery {
                protonMailApiManager.labelMessagesBlocking(
                    any(), UserId(testUserName)
                )
            } returns mockk()

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_LABEL_WORKER_ERROR_DESCRIPTION to "Input data is not complete")
            )

            // when
            val result = moveMessageToLocationWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnRetryIfApiCallFailsAndRunAttemptsDoNotExceedTheLimit() {
        runBlockingTest {
            // given
            val messageIds = arrayOf("id1", "id2")
            val newLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue
            every {
                workerParameters.inputData.getString(KEY_POST_WORKER_USER_ID)
            } returns testUserName
            every {
                workerParameters.inputData.getStringArray(KEY_POST_WORKER_MESSAGE_ID)
            } returns messageIds
            every {
                workerParameters.inputData.getInt(KEY_POST_WORKER_LOCATION_ID, -1)
            } returns newLocation
            coEvery {
                protonMailApiManager.labelMessagesBlocking(
                    any(), UserId(testUserName)
                )
            } throws IOException()

            val expectedResult = ListenableWorker.Result.retry()

            // when
            val result = moveMessageToLocationWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnFailureIfApiCallFailsAndRunAttemptsExceedTheLimit() {
        runBlockingTest {
            // given
            val messageIds = arrayOf("id1", "id2")
            val newLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue
            every {
                workerParameters.inputData.getString(KEY_POST_WORKER_USER_ID)
            } returns testUserName
            every {
                workerParameters.inputData.getStringArray(KEY_POST_WORKER_MESSAGE_ID)
            } returns messageIds
            every {
                workerParameters.inputData.getInt(KEY_POST_WORKER_LOCATION_ID, -1)
            } returns newLocation
            every {
                workerParameters.runAttemptCount
            } returns 4
            coEvery {
                protonMailApiManager.labelMessagesBlocking(
                    any(), UserId(testUserName)
                )
            } throws IOException()

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_LABEL_WORKER_ERROR_DESCRIPTION to "Run attempts exceeded the limit")
            )

            // when
            val result = moveMessageToLocationWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }
}
