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
package ch.protonmail.android.uiModel

import android.view.View
import androidx.lifecycle.MutableLiveData
import com.google.gson.annotations.SerializedName
import me.proton.core.util.kotlin.EMPTY_STRING

data class SettingsItemUiModel constructor(

    @SerializedName("is_section")
    val isSection: Boolean = false,

    @SerializedName("setting_id")
    val settingId: String = EMPTY_STRING,

    @SerializedName("setting_hasValue")
    val settingHasValue: Boolean = false,

    @SerializedName("setting_type")
    val settingType: SettingsItemTypeEnum? = SettingsItemTypeEnum.INFO,
    var settingsDescription: String? = EMPTY_STRING,
    var settingHeader: String? = EMPTY_STRING,
    var settingValue: String? = EMPTY_STRING,
    var settingsHint: String? = EMPTY_STRING,
    var enabled: Boolean = false,
    var iconVisibility: Int = View.GONE
) {

    /**
     * [settingDisabled] is used if we don't wanna show the feature with [settingId] to user
     */
    var settingDisabled: Boolean = false
    var toggleListener: ((View, Boolean) -> Unit)? = { _: View, _: Boolean -> }
    var editTextListener: (View) -> Unit = {}
    var editTextChangeListener: (String) -> Unit = {}

    enum class SettingsItemTypeEnum {
        @SerializedName("info")
        INFO,

        @SerializedName("drill")
        DRILL_DOWN,

        @SerializedName("button")
        BUTTON,

        @SerializedName("toggle")
        TOGGLE,

        @SerializedName("spinner")
        SPINNER
    }
}
