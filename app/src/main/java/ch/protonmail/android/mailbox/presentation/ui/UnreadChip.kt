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

package ch.protonmail.android.mailbox.presentation.ui

import android.content.Context
import android.util.AttributeSet
import ch.protonmail.android.R
import ch.protonmail.android.mailbox.presentation.model.UnreadChipUiModel
import ch.protonmail.android.utils.extensions.isInPreviewMode
import com.google.android.material.chip.Chip
import me.proton.core.presentation.utils.onClick
import com.google.android.material.R as MaterialR

/**
 * [Chip] for unread messages filter in Mailbox
 * Binds with [UnreadChipUiModel]
 */
class UnreadChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = MaterialR.attr.chipStyle
) : Chip(context, attrs, defStyleAttr) {

    init {
        if (isInPreviewMode()) {
            bind(
                UnreadChipUiModel(
                    unreadCount = 0,
                    isFilterEnabled = true
                ),
                onEnableFilter = {},
                onDisableFilter = {}
            )
        }
    }

    fun bind(model: UnreadChipUiModel, onEnableFilter: () -> Unit, onDisableFilter: () -> Unit) {
        text = resources.getQuantityString(R.plurals.mailbox_unread_count, model.unreadCount, model.unreadCount)
        isCloseIconVisible = model.isFilterEnabled
        isChecked = model.isFilterEnabled
        onClick {
            if (model.isFilterEnabled) onDisableFilter()
            else onEnableFilter()
        }
        setOnCloseIconClickListener { onDisableFilter() }
    }
}
