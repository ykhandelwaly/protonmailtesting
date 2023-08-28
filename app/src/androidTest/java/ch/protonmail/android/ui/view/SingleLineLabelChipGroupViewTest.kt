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
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.testAndroidInstrumented.assertion.isGone
import ch.protonmail.android.testAndroidInstrumented.assertion.isVisible
import ch.protonmail.android.testAndroidInstrumented.matcher.withBackgroundColor
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.util.ViewTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4ClassRunner::class)
class SingleLineLabelChipGroupViewTest : ViewTest<SingleLineLabelChipGroupView>(::SingleLineLabelChipGroupView) {

    private val testLabelsList = listOf(
        LabelChipUiModel(LabelId("a"), Name("first"), Color.RED),
        LabelChipUiModel(LabelId("b"), Name("second"), Color.GREEN),
        LabelChipUiModel(LabelId("c"), Name("third"), Color.BLUE),
    )

    @Test
    fun viewIsGoneWhenNoLabels() {

        // given - when
        runOnActivityThread {
            testView.setLabels(emptyList())
        }

        // then
        onTestView().check(isGone())
    }

    @Test
    fun viewIsVisibleWhenHasLabels() {

        // given - when
        runOnActivityThread {
            testView.setLabels(testLabelsList)
        }

        // then
        onTestView().check(isVisible())
    }

    @Test
    fun firstLabelShowsTheCorrectNameAndColor() {
        val chipGroupView = testView

        // given
        val labels = testLabelsList
        val firstLabel = labels.first()
        val expectedLabelId = firstLabel.id
        val expectedLabelName = firstLabel.name.s
        val expectedLabelColor = checkNotNull(firstLabel.color)

        // when
        runOnActivityThread {
            chipGroupView.setLabels(labels)
        }

        // then
        onView(withLabelId(expectedLabelId))
            .check(matches(withText(expectedLabelName)))
            .check(matches(withBackgroundColor(expectedLabelColor)))
    }

    private fun withLabelId(labelId: LabelId): Matcher<View> {
        return object : TypeSafeDiagnosingMatcher<View>() {

            override fun matchesSafely(item: View, mismatchDescription: Description) =
                (item as? LabelChipView)?.labelId == labelId

            override fun describeTo(description: Description) {
                description.appendText("Label id: ${labelId.id}")
            }
        }
    }
}
