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

package ch.protonmail.android.ui.view

import android.graphics.Color
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.R
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.testAndroidInstrumented.assertion.isGone
import ch.protonmail.android.testAndroidInstrumented.assertion.isVisible
import ch.protonmail.android.testAndroidInstrumented.matcher.withBackgroundColor
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.util.ViewTest
import org.junit.runner.RunWith
import kotlin.test.Test

/**
 * Test suite for [MultiLineLabelChipGroupView]
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class MultiLineLabelChipGroupViewTest : ViewTest<MultiLineLabelChipGroupView>(::MultiLineLabelChipGroupView) {

    private val testLabelsList = listOf(
        LabelChipUiModel(LabelId("a"), Name("long name for first label"), Color.RED),
        LabelChipUiModel(LabelId("b"), Name("second label"), Color.GREEN),
        LabelChipUiModel(LabelId("c"), Name("third"), Color.BLUE),
        LabelChipUiModel(LabelId("d"), Name("long name for forth label"), Color.CYAN),
        LabelChipUiModel(LabelId("e"), Name("fifth label"), Color.MAGENTA),
        LabelChipUiModel(LabelId("f"), Name("sixth"), Color.GRAY),
        LabelChipUiModel(LabelId("g"), Name("long name for seventh label"), Color.BLACK),
    )

    @Test
    fun viewIsGoneWhenNoLabels() {

        // given - when
        testView.setLabels(emptyList())

        // then
        onTestView().check(isGone())
    }

    @Test
    fun viewIsVisibleWhenHasLabels() {

        // given - when
        testView.setLabels(testLabelsList)

        // then
        onTestView().check(isVisible())
    }

    @Test
    fun listIsEmptyWhenLabelsListIsEmpty() {
        val chipGroupView = testView

        // given
        val labels = emptyList<LabelChipUiModel>()

        // when
        chipGroupView.setLabels(labels)

        // then
        onRecyclerView().check(matches(hasChildCount(labels.size)))
    }

    @Test
    fun listContainsAllTheLabels() {
        val chipGroupView = testView

        // given
        val labels = testLabelsList

        // when
        chipGroupView.setLabels(labels)

        // then
        onRecyclerView().check(matches(hasChildCount(labels.size)))
    }

    @Test
    fun correctLabelsNamesAndColorsAreDisplayed() {
        val chipGroupView = testView

        // given
        val labels = testLabelsList

        // when
        chipGroupView.setLabels(labels)

        // then
        for (label in labels) {
            val labelColor = checkNotNull(label.color)
            onView(withText(label.name.s)).check(matches(withBackgroundColor(labelColor)))
        }
    }

    private fun onRecyclerView(): ViewInteraction =
        onView(withId(R.id.multi_line_label_recycler_view))
}
