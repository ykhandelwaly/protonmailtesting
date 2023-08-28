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
package ch.protonmail.android.core

import android.content.Context
import android.content.SharedPreferences
import ch.protonmail.android.R
import ch.protonmail.android.api.local.SnoozeSettings
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.models.User
import ch.protonmail.android.di.AppCoroutineScope
import ch.protonmail.android.di.BackupSharedPreferences
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.domain.util.orThrow
import ch.protonmail.android.feature.account.primaryId
import ch.protonmail.android.feature.account.primaryLegacyUser
import ch.protonmail.android.feature.account.primaryUser
import ch.protonmail.android.feature.account.primaryUserId
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.usecase.LoadLegacyUser
import ch.protonmail.android.usecase.LoadUser
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.obfuscateUsername
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.domain.entity.UserId
import me.proton.core.util.android.sharedpreferences.clearAll
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.android.sharedpreferences.minusAssign
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.minutes
import ch.protonmail.android.domain.entity.user.User as NewUser

// region constants
const val PREF_PIN = "mailbox_pin"
const val PREF_CURRENT_USER_ID = "prefs.current.user.id"
const val PREF_USERNAME = "username"

private const val PREF_PIN_INCORRECT_ATTEMPTS = "mailbox_pin_incorrect_attempts"
private const val PREF_IS_FIRST_MAILBOX_LOAD_AFTER_LOGIN = "is_first_mailbox_load_after_login"
const val PREF_SHOW_STORAGE_LIMIT_WARNING = "show_storage_limit_warning"
const val PREF_SHOW_STORAGE_LIMIT_REACHED = "show_storage_limit_reached"
// endregion

/**
 * UserManager handles behavior of the current primary account, as well as some multi-account behaviors
 */
@Deprecated("Now replaced by Core UserManager/AccountManager.")
@Singleton
class UserManager @Inject constructor(
    context: Context,
    val keyStoreCrypto: KeyStoreCrypto,
    private val coreAccountManager: AccountManager,
    private val loadUser: LoadUser,
    private val loadLegacyUser: LoadLegacyUser,
    @DefaultSharedPreferences private val prefs: SharedPreferences,
    @BackupSharedPreferences private val backupPrefs: SharedPreferences,
    @Deprecated(
        "UserManager is never using this, but it's just providing for other classes, so this " +
            "should be injected directly there"
    )
    val openPgp: OpenPGP,
    private val secureSharedPreferencesFactory: SecureSharedPreferences.Factory,
    private val dispatchers: DispatcherProvider,
    @AppCoroutineScope private val scope: CoroutineScope
) {
    private val app: ProtonMailApplication = context.app

    private val cachedLegacyUsers = mutableMapOf<UserId, User>()
    private val cachedUsers = mutableMapOf<UserId, NewUser>()

    private var refreshPrimary = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var primaryId: StateFlow<UserId?>
    private var primaryLegacyUser: StateFlow<User?>
    private var primaryUser: StateFlow<NewUser?>
    var primaryUserId: StateFlow<UserId?>
        private set

    init {
        // Workaround to make sure we have fresh value and get them from main thread without impacting performances.
        runBlocking {
            refreshPrimary.emit(Unit)
            primaryUserId = coreAccountManager.primaryUserId(scope)
            primaryId = coreAccountManager.primaryId(scope)
            primaryLegacyUser = coreAccountManager.primaryLegacyUser(scope, refreshPrimary) { getLegacyUser(it) }
            primaryUser = coreAccountManager.primaryUser(scope, refreshPrimary) { getUser(it) }
        }
    }

    // region Current User/NewUser.

    val currentUserId: UserId?
        get() = primaryId.value

    val currentLegacyUser: User?
        get() = primaryLegacyUser.value

    val currentUser: NewUser?
        get() = primaryUser.value

    /**
     * Clear users cache and refresh [currentUserId], [currentLegacyUser] and [currentUser].
     *
     * Note: This is a workaround to use when User/Address/Key are updated.
     */
    fun clearCache() {
        cachedLegacyUsers.clear()
        cachedUsers.clear()
        refreshPrimary.tryEmit(Unit)
    }

    fun requireCurrentUserId(): UserId = checkNotNull(currentUserId)

    fun requireCurrentLegacyUser(): User = checkNotNull(currentLegacyUser)

    fun requireCurrentUser(): NewUser = checkNotNull(currentUser)

    suspend fun getPreviousCurrentUserId(): UserId? = coreAccountManager.getPreviousPrimaryUserId()

    // endregion

    // region User/NewUser by Id

    suspend fun getLegacyUser(userId: UserId): User = cachedLegacyUsers.getOrPut(userId) {
        withContext(dispatchers.Io) {
            loadLegacyUser(userId).orThrow()
        }
    }

    suspend fun getUser(userId: UserId): NewUser = cachedUsers.getOrPut(userId) {
        withContext(dispatchers.Io) {
            loadUser(userId).orThrow()
        }
    }

    fun getLegacyUserBlocking(userId: UserId) = runBlocking {
        getLegacyUser(userId)
    }

    fun getUserBlocking(userId: UserId): NewUser = runBlocking {
        getUser(userId)
    }

    fun getUserPassphraseBlocking(userId: UserId): EncryptedByteArray? =
        getLegacyUserBlocking(userId).passphrase

    // endregion

    suspend fun getCurrentUserMailSettings(): MailSettings? =
        currentUserId?.let { getMailSettings(it) }

    @Deprecated("Use suspend function", ReplaceWith("getCurrentUserMailSettings()"))
    fun getCurrentUserMailSettingsBlocking(): MailSettings? =
        runBlocking { getCurrentUserMailSettings() }

    @Deprecated("Use suspend function", ReplaceWith("getCurrentUserMailSettings()"))
    fun requireCurrentUserMailSettingsBlocking(): MailSettings =
        requireNotNull(getCurrentUserMailSettingsBlocking())

    suspend fun getMailSettings(userId: UserId): MailSettings = withContext(dispatchers.Io) {
        MailSettings.load(preferencesFor(userId))
    }

    @Deprecated("Use suspend function", ReplaceWith("getMailSettings(userId)"))
    fun getMailSettingsBlocking(userId: UserId): MailSettings =
        runBlocking { getMailSettings(userId) }

    val snoozeSettings: SnoozeSettings?
        get() = runBlocking { currentUserId?.let { SnoozeSettings.load(preferencesFor(it)) } }

    val isFirstMailboxLoad: Boolean
        get() = prefs.getBoolean(PREF_IS_FIRST_MAILBOX_LOAD_AFTER_LOGIN, true)

    private val currentUserPreferences
        get() = currentUserId?.let(::preferencesFor)

    private inline fun <T> withCurrentUserPreferences(block: (SharedPreferences) -> T): T? {
        currentUserPreferences ?: Timber.d("No current user set")
        return currentUserPreferences?.let(block)
    }

    val incorrectPinAttempts: Int
        get() {
            val secureSharedPreferences = app.secureSharedPreferences
            return secureSharedPreferences.getInt(PREF_PIN_INCORRECT_ATTEMPTS, 0)
        }

    @Deprecated("Use 'currentUser' variant", ReplaceWith("isCurrentUserBackgroundSyncEnabled()"))
    val isBackgroundSyncEnabled: Boolean
        get() = currentLegacyUser?.isBackgroundSync ?: false

    fun isCurrentUserSnoozeScheduledEnabled(): Boolean {
        val userId = requireNotNull(currentUserId)
        return requireNotNull(snoozeSettings).getScheduledSnooze(preferencesFor(userId))
    }

    @Deprecated("Use 'currentUser' variant", ReplaceWith("isCurrentUserSnoozeScheduledEnabled()"))
    fun isSnoozeScheduledEnabled(): Boolean =
        isCurrentUserSnoozeScheduledEnabled()

    // Very important: don't setSnoozeQuick to false if it already is false otherwise it will
    // keep saving, saving can be super slow on users with a lot of accounts!
    suspend fun isSnoozeQuickEnabled(): Boolean {
        if (snoozeSettings!!.snoozeQuick && snoozeSettings!!.snoozeQuickEndTime - System.currentTimeMillis() <= 0) {
            setSnoozeQuick(false, 0)
        }
        return snoozeSettings!!.snoozeQuick
    }

    // Very important: don't setSnoozeQuick to false if it already is false otherwise it will
    // keep saving, saving can be super slow on users with a lot of accounts!
    @Deprecated("Use suspend function", ReplaceWith("isSnoozeQuickEnabled"))
    fun isSnoozeQuickEnabledBlocking(): Boolean =
        runBlocking { isSnoozeQuickEnabled() }

    fun firstMailboxLoadDone() {
        prefs.edit().putBoolean(PREF_IS_FIRST_MAILBOX_LOAD_AFTER_LOGIN, false).apply()
    }

    fun increaseIncorrectPinAttempt() {
        val secureSharedPreferences = app.secureSharedPreferences
        var attempts = secureSharedPreferences.getInt(PREF_PIN_INCORRECT_ATTEMPTS, 0)
        secureSharedPreferences.edit().putInt(PREF_PIN_INCORRECT_ATTEMPTS, ++attempts).apply()
    }

    fun resetPinAttempts() {
        val secureSharedPreferences = app.secureSharedPreferences
        secureSharedPreferences.edit().putInt(PREF_PIN_INCORRECT_ATTEMPTS, 0).apply()
    }

    fun savePin(mailboxPin: String?) {
        val secureSharedPreferences = app.secureSharedPreferences
        secureSharedPreferences.edit().putString(PREF_PIN, mailboxPin).apply()
    }

    fun getMailboxPin(): String? =
        app.secureSharedPreferences.getString(PREF_PIN, "")

    fun canShowStorageLimitWarning(): Boolean =
        withCurrentUserPreferences { it[PREF_SHOW_STORAGE_LIMIT_WARNING] } ?: true

    fun setShowStorageLimitWarning(value: Boolean) {
        withCurrentUserPreferences {
            it[PREF_SHOW_STORAGE_LIMIT_WARNING] = value
        }
    }

    fun canShowStorageLimitReached(): Boolean =
        withCurrentUserPreferences { it[PREF_SHOW_STORAGE_LIMIT_REACHED] } ?: true

    fun setShowStorageLimitReached(value: Boolean) {
        withCurrentUserPreferences {
            it[PREF_SHOW_STORAGE_LIMIT_REACHED] = value
        }
    }

    fun clearBackupPrefs() {
        backupPrefs.clearAll()
    }

    /**
     * @throws IllegalStateException if [currentUserId] or [snoozeSettings] is `null`
     */
    suspend fun setSnoozeScheduled(
        isOn: Boolean,
        startTimeHour: Int,
        startTimeMinute: Int,
        endTimeHour: Int,
        endTimeMinute: Int,
        repeatingDays: String
    ) {
        val preferences = preferencesFor(requireCurrentUserId())
        checkNotNull(snoozeSettings).apply {
            snoozeScheduled = isOn
            snoozeScheduledStartTimeHour = startTimeHour
            snoozeScheduledStartTimeMinute = startTimeMinute
            snoozeScheduledEndTimeHour = endTimeHour
            snoozeScheduledEndTimeMinute = endTimeMinute
            snoozeScheduledRepeatingDays = repeatingDays
            save(preferences)
        }
    }

    /**
     * @throws IllegalStateException if [currentUserId] or [snoozeSettings] is `null`
     */
    @Deprecated(
        "Use suspend function",
        ReplaceWith(
            "setSnoozeScheduled(isOn, startTimeHour, startTimeMinute, endTimeHour, endTimeMinute, repeatingDays)"
        )
    )
    fun setSnoozeScheduledBlocking(
        isOn: Boolean,
        startTimeHour: Int,
        startTimeMinute: Int,
        endTimeHour: Int,
        endTimeMinute: Int,
        repeatingDays: String
    ) {
        runBlocking {
            setSnoozeScheduled(isOn, startTimeHour, startTimeMinute, endTimeHour, endTimeMinute, repeatingDays)
        }
    }

    /**
     * @throws IllegalStateException if [currentUserId] or [snoozeSettings] is `null`
     */
    suspend fun setSnoozeQuick(isOn: Boolean, minutesFromNow: Int) {
        val preferences = preferencesFor(requireCurrentUserId())
        checkNotNull(snoozeSettings).apply {
            snoozeQuick = isOn
            snoozeQuickEndTime = System.currentTimeMillis() + minutesFromNow.minutes.toLongMilliseconds()
            saveQuickSnoozeBackup(preferences)
            saveQuickSnoozeEndTimeBackup(preferences)
            save(preferences)
        }
    }

    /**
     * @throws IllegalStateException if [currentUserId] or [snoozeSettings] is `null`
     */
    @Deprecated("Use suspend function", ReplaceWith("setSnoozeQuick(isOn, minutesFromNow)"))
    fun setSnoozeQuickBlocking(isOn: Boolean, minutesFromNow: Int) {
        runBlocking { setSnoozeQuick(isOn, minutesFromNow) }
    }

    fun didReachLabelsThreshold(numberOfLabels: Int): Boolean = getMaxLabelsAllowed() < numberOfLabels

    fun getMaxLabelsAllowed(): Int {
        val accountTypes = app.resources.getStringArray(R.array.account_type_names).asList()
        val maxLabelsPerPlanArray = app.resources.getIntArray(R.array.max_labels_per_plan).asList()
        val organization = app.organization

        var paidUser = false
        var planName = accountTypes[0] // free

        var maxLabelsAllowed = maxLabelsPerPlanArray[0] // free


        if (organization != null) {
            planName = organization.planName
            paidUser = currentLegacyUser?.isPaidUser == true && organization.planName.isNullOrEmpty().not()
        }
        if (!paidUser) {
            return maxLabelsAllowed
        }

        for (i in 1 until accountTypes.size) {
            val accountName = accountTypes[i]
            if (accountName.equals(planName, ignoreCase = true)) {
                maxLabelsAllowed = maxLabelsPerPlanArray[i]
                break
            }
        }

        return maxLabelsAllowed
    }

    fun preferencesFor(userId: UserId) =
        secureSharedPreferencesFactory.userPreferences(userId)

    class UsernameToIdMigration @Inject constructor(
        @DefaultSharedPreferences private val prefs: SharedPreferences,
        private val dispatchers: DispatcherProvider
    ) {

        suspend operator fun invoke(allUsernamesToIds: Map<String, UserId>) {
            withContext(dispatchers.Io) {
                val currentUsername = prefs.get<String?>(PREF_USERNAME)?.takeIfNotBlank()
                if (currentUsername == null) {
                    Timber.v("Cannot load current username for UserManager migration")
                    return@withContext
                }
                Timber.v("Migrating UserManager for user: ${currentUsername.obfuscateUsername()}")

                prefs -= PREF_USERNAME

                allUsernamesToIds[currentUsername]?.let { currentUserId ->
                    prefs[PREF_CURRENT_USER_ID] = currentUserId.id
                }
            }
        }
    }
}
