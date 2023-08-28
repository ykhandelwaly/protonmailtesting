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

package ch.protonmail.android.ui.model

import androidx.annotation.ColorInt
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.ui.view.LabelChipView

/**
 * Ui Model for [LabelChipView]
 * @property color is the [ColorInt] that will be applied as background.
 *  if `null` a default background will be used, for ensure readability of the text
 */
data class LabelChipUiModel(
    val id: LabelId,
    val name: Name,
    @ColorInt
    val color: Int?
)
