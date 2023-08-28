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

package ch.protonmail.android.security.presentation

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.pinlock.presentation.PinLockManager
import ch.protonmail.android.security.domain.usecase.ObserveIsPreventTakingScreenshots
import ch.protonmail.android.usecase.GetElapsedRealTimeMillis
import ch.protonmail.android.utils.EmptyActivityLifecycleCallbacks
import ch.protonmail.android.utils.extensions.app
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import me.proton.core.presentation.app.AppLifecycleProvider
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SecurityManager @Inject constructor(
    context: Context,
    private val pinLockManager: PinLockManager,
    private val screenshotManager: ScreenshotManager,
    private val logoutHandler: LogoutHandler,
    observeIsPreventTakingScreenshots: ObserveIsPreventTakingScreenshots,
    private val getElapsedRealTimeMillis: GetElapsedRealTimeMillis,
) {

    private val processLifecycleOwner = ProcessLifecycleOwner.get()

    private var appState: AppLifecycleProvider.State = AppLifecycleProvider.State.Background
    private var currentActivity = WeakReference<Activity>(null)
    private var lastForegroundTime: Long = 0

    private val shouldUseSecureScreen = observeIsPreventTakingScreenshots()
        .flowWithLifecycle(processLifecycleOwner.lifecycle)
        .onEach { isSecure -> currentActivity.get()?.toggleSecureScreen(isSecure) }
        .stateIn(processLifecycleOwner.lifecycleScope, SharingStarted.Eagerly, false)

    private val appLifecycleObserver = object : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            Timber.v("App Foreground")

            appState = AppLifecycleProvider.State.Foreground
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            Timber.v("App Background")

            appState = AppLifecycleProvider.State.Background
            lastForegroundTime = getElapsedRealTimeMillis()
        }
    }

    private val activityLifecycleObserver = object : EmptyActivityLifecycleCallbacks {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            super.onActivityCreated(activity, savedInstanceState)
            Timber.v("Activity created: ${activity::class.simpleName}")

            if (activity is ComponentActivity) {
                logoutHandler.register(activity)
            }
        }

        override fun onActivityStarted(activity: Activity) {
            super.onActivityStarted(activity)
            Timber.v("Activity started: ${activity::class.simpleName}")

            currentActivity = WeakReference(activity)

            activity.toggleSecureScreen(shouldUseSecureScreen.value)
            activity.lockIfNeeded()
        }
    }

    init {
        context.app.registerActivityLifecycleCallbacks(activityLifecycleObserver)
        processLifecycleOwner.lifecycle.addObserver(appLifecycleObserver)
    }

    private fun Activity.lockIfNeeded() {
        val activity = this

        @Suppress("BlockingMethodInNonBlockingContext") // This needs to be run blocking, in order to
        //  prevent the last activity to be displayed
        runBlocking {
            val shouldLock = pinLockManager.shouldLock(
                appState = appState,
                currentActivity = activity,
                lastForegroundTime = lastForegroundTime
            )
            if (shouldLock) {
                pinLockManager.lock(activity)
            }
        }
    }

    private fun Activity.toggleSecureScreen(isSecure: Boolean) {
        if (isSecure || screenshotManager.shouldPreventScreenshot(this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
