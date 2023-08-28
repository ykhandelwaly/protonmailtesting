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

package ch.protonmail.android.details.presentation.view

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import ch.protonmail.android.R
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.testAndroidInstrumented.assertion.isGone
import ch.protonmail.android.testAndroidInstrumented.assertion.isVisible
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.util.HiltViewTest
import ch.protonmail.android.utils.ServerTimeProvider
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class CollapsedMessageViewsTest : HiltViewTest<CollapsedMessageViews>(::CollapsedMessageViews) {

    private val serverTimeProviderMock = mockk<ServerTimeProvider>()

    @Before
    fun setUp() {
        testView.serverTimeProvider = serverTimeProviderMock
    }

    @Test
    fun shouldHideExpirationViewWhenMessageDoesNotHaveExpiration() {
        // given
        every { serverTimeProviderMock.currentTimeMillis() } returns 0
        val labels = emptyList<LabelChipUiModel>()
        val message = Message(expirationTime = 0)

        // when
        runOnActivityThread {
            testView.bind(message, labels)
        }

        // then
        onExpirationTextView().check(isGone())
    }

    @Test
    fun shouldShowCorrectExpirationTimeWhenMessageDoesHaveExpiration() {
        // given
        every { serverTimeProviderMock.currentTimeMillis() } returns 60000
        val labels = emptyList<LabelChipUiModel>()
        val message = Message(expirationTime = 420)
        val expectedExpirationString = "6M"

        // when
        runOnActivityThread {
            testView.bind(message, labels)
        }

        // then
        onExpirationTextView()
            .check(isVisible())
            .check(matches(withText(expectedExpirationString)))
    }

    @Test
    fun shouldHideLabelsViewWhenNoLabelsProvided() {
        // given
        val labels = emptyList<LabelChipUiModel>()
        val message = Message()

        // when
        runOnActivityThread {
            testView.bind(message, labels)
        }

        // then
        onLabelsView().check(isGone())
    }

    @Test
    fun shouldShowLabelsViewWhenLabelsProvided() {
        // given
        val labels = listOf(
            LabelChipUiModel(
                id = LabelId("1"),
                name = Name("label 1"),
                color = null
            )
        )
        val message = Message()

        // when
        runOnActivityThread {
            testView.bind(message, labels)
        }

        // then
        onLabelsView().check(isVisible())
    }

    private fun onExpirationTextView(): ViewInteraction = onView(withId(R.id.messageExpirationTextView))

    private fun onLabelsView(): ViewInteraction = onView(withId(R.id.collapsedLabelsView))
}
