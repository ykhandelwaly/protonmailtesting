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

package ch.protonmail.android.labels.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val LABEL_TYPE_ID_MESSAGE_LABEL = 1
const val LABEL_TYPE_ID_CONTACT_GROUP = 2
const val LABEL_TYPE_ID_FOLDER = 3
const val LABEL_TYPE_ID_SYSTEM_FOLDER = 4

@Serializable
enum class LabelType(val typeInt: Int) {

    @SerialName(LABEL_TYPE_ID_MESSAGE_LABEL.toString())
    MESSAGE_LABEL(LABEL_TYPE_ID_MESSAGE_LABEL),

    @SerialName(LABEL_TYPE_ID_CONTACT_GROUP.toString())
    CONTACT_GROUP(LABEL_TYPE_ID_CONTACT_GROUP),

    @SerialName(LABEL_TYPE_ID_FOLDER.toString())
    FOLDER(LABEL_TYPE_ID_FOLDER),

    @SerialName(LABEL_TYPE_ID_SYSTEM_FOLDER.toString())
    SYSTEM_FOLDER(LABEL_TYPE_ID_SYSTEM_FOLDER);

    companion object {

        fun fromIntOrNull(typeInt: Int): LabelType? {
            return values().find {
                it.typeInt == typeInt
            }
        }
    }
}
