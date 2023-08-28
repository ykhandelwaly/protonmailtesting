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

package ch.protonmail.android.pendingaction.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.R
import ch.protonmail.android.compose.send.SendMessageWorker
import ch.protonmail.android.pendingaction.domain.repository.PendingSendRepository
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.worker.repository.WorkerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.account.domain.entity.isReady
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val KEY_INPUT_MESSAGE_ID = "keyInputMessageId"
const val KEY_INPUT_MESSAGE_SUBJECT = "keyInputMessageSubject"
const val KEY_INPUT_MESSAGE_DATABASE_ID = "keyInputMessageDatabaseId"
const val KEY_INPUT_USER_ID = "keyInputUserId"
private const val UNIQUE_WORK_NAME_PREFIX = "cleanUpPendingSendsWorker"

@HiltWorker
class CleanUpPendingSendWorker @AssistedInject constructor(
    private val pendingSendRepository: PendingSendRepository,
    private val workerRepository: WorkerRepository,
    private val getSendMessageWorkerUniqueNameFor: SendMessageWorker.ProvideUniqueName,
    private val userNotifier: UserNotifier,
    @Assisted private val context: Context,
    private val accountManager: AccountManager,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val messageId by lazy { requireNotNull(inputData.getString(KEY_INPUT_MESSAGE_ID)) }
    private val subject by lazy { requireNotNull(inputData.getString(KEY_INPUT_MESSAGE_SUBJECT)) }
    private val databaseId by lazy { inputData.getLong(KEY_INPUT_MESSAGE_DATABASE_ID, -1) }
    private val userId by lazy {
        UserId(
            requireNotNull(
                inputData.getString(KEY_INPUT_USER_ID)?.takeIfNotBlank()
            ) { "User id is required" }
        )
    }

    override suspend fun doWork(): Result {
        val sendWorkerToCancel = getSendMessageWorkerUniqueNameFor(messageId)

        workerRepository.findWorkInfoForUniqueWork(sendWorkerToCancel)
            .filter { it.state.isFinished }
            .takeIf { it.isNotEmpty() }
            ?.let {
                accountManager.getAccount(userId).firstOrNull()?.let { account ->
                    if (account.isReady())
                        pendingSendRepository.deletePendingSendByDatabaseId(databaseId)
                }
                Timber.w("Found a stuck send. Cleaning up.")
                workerRepository.cancelUniqueWork(sendWorkerToCancel)
                userNotifier.showSendMessageError(
                    context.getString(R.string.message_drafted),
                    subject
                )
            }

        return Result.success()
    }

    class ProvideUniqueName @Inject constructor() {

        operator fun invoke(messageDatabaseId: Long) = "$UNIQUE_WORK_NAME_PREFIX-$messageDatabaseId"
    }
}

@HiltWorker
class SchedulePendingSendsCleanUpWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : Worker(context, params) {

    private val messageId by lazy { requireNotNull(inputData.getString(KEY_INPUT_MESSAGE_ID)) }
    private val subject by lazy { requireNotNull(inputData.getString(KEY_INPUT_MESSAGE_SUBJECT)) }
    private val databaseId by lazy { inputData.getLong(KEY_INPUT_MESSAGE_DATABASE_ID, -1) }
    private val userId by lazy { inputData.getString(KEY_INPUT_USER_ID) }

    override fun doWork(): Result = Result.success(
        workDataOf(
            KEY_INPUT_MESSAGE_ID to messageId,
            KEY_INPUT_MESSAGE_SUBJECT to subject,
            KEY_INPUT_MESSAGE_DATABASE_ID to databaseId,
            KEY_INPUT_USER_ID to userId
        )
    )
}

class SchedulePendingSendCleanUpWhenOnline @Inject constructor(
    private val provideUniqueNameFor: CleanUpPendingSendWorker.ProvideUniqueName,
    private val workManager: WorkManager
) {

    private val onlineConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    private val performCleanUpWork = OneTimeWorkRequestBuilder<CleanUpPendingSendWorker>()
        .setInitialDelay(2, TimeUnit.HOURS)
        .build()

    operator fun invoke(messageId: String, messageSubject: String, messageDatabaseId: Long, userId: UserId) {
        workManager.beginUniqueWork(
            provideUniqueNameFor(messageDatabaseId),
            ExistingWorkPolicy.KEEP,
            scheduleCleanupWhenOnlineWork(messageId, messageSubject, messageDatabaseId, userId)
        )
            .then(performCleanUpWork)
            .enqueue()
    }

    private fun scheduleCleanupWhenOnlineWork(
        messageId: String,
        messageSubject: String,
        messageDatabaseId: Long,
        userId: UserId
    ) =
        OneTimeWorkRequestBuilder<SchedulePendingSendsCleanUpWorker>()
            .setConstraints(onlineConstraint)
            .setInputData(
                workDataOf(
                    KEY_INPUT_MESSAGE_ID to messageId,
                    KEY_INPUT_MESSAGE_SUBJECT to messageSubject,
                    KEY_INPUT_MESSAGE_DATABASE_ID to messageDatabaseId,
                    KEY_INPUT_USER_ID to userId.id
                )
            )
            .build()
}

