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
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.mailbox.data.remote.model.ConversationIdsRequestBody
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

const val KEY_MARK_READ_WORKER_CONVERSATION_IDS = "ConversationIds"
const val KEY_MARK_READ_WORKER_USER_ID = "UserId"
const val KEY_MARK_READ_WORKER_UNDO_TOKEN = "UndoToken"
const val KEY_MARK_READ_WORKER_VALID_UNTIL = "ValidUntil"
const val KEY_MARK_READ_WORKER_ERROR_DESCRIPTION = "ErrorDescription"
private const val MAX_RUN_ATTEMPTS = 5

/**
 * A worker that handles marking a conversation as read
 */
@HiltWorker
class MarkConversationsReadRemoteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val protonMailApiManager: ProtonMailApiManager
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val conversationIds = inputData.getStringArray(KEY_MARK_READ_WORKER_CONVERSATION_IDS)
            ?: return Result.failure(
                workDataOf(KEY_MARK_READ_WORKER_ERROR_DESCRIPTION to "Conversation ids list is null")
            )
        val userId = inputData.getString(KEY_MARK_READ_WORKER_USER_ID)
            ?: return Result.failure(
                workDataOf(KEY_MARK_READ_WORKER_ERROR_DESCRIPTION to "User id is null")
            )

        val requestBody = ConversationIdsRequestBody(ids = conversationIds.asList())

        Timber.v("MarkConversationsReadRemoteWorker conversationIds: ${conversationIds.asList()}")

        return runCatching {
            protonMailApiManager.markConversationsRead(requestBody, UserId(userId))
        }.fold(
            onSuccess = { response ->
                Result.success(
                    workDataOf(
                        KEY_MARK_READ_WORKER_UNDO_TOKEN to response.undoToken?.token,
                        KEY_MARK_READ_WORKER_VALID_UNTIL to response.undoToken?.validUntil
                    )
                )
            },
            onFailure = { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }
                if (runAttemptCount > MAX_RUN_ATTEMPTS) {
                    Result.failure(
                        workDataOf(KEY_MARK_READ_WORKER_ERROR_DESCRIPTION to "Run attempts exceeded the limit")
                    )
                } else {
                    Result.retry()
                }
            }
        )
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(conversationIds: List<String>, userId: UserId): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                KEY_MARK_READ_WORKER_CONVERSATION_IDS to conversationIds.toTypedArray(),
                KEY_MARK_READ_WORKER_USER_ID to userId.id
            )

            val request = OneTimeWorkRequestBuilder<MarkConversationsReadRemoteWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            return workManager.enqueue(request)
        }
    }
}
