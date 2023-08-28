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
package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class MessageLocationResolver @Inject constructor(
    // Unfortunately with the current "architecture" we cannot properly inject anything here and verify label type
    // correctly in resolveLabelType(), that should be updated with the networking module implementation
    // currently this is used in [MessageResponse] & [MessagesResponse] which should be simple data classes
    private val labelRepository: LabelRepository?
) {

    fun resolveLocationFromLabels(labelIds: List<String>): Constants.MessageLocationType {
        if (labelIds.isEmpty()) {
            return Constants.MessageLocationType.INBOX
        }

        if (
            labelIds.size == 1 &&
            labelIds[0] == Constants.MessageLocationType.ALL_MAIL.messageLocationTypeValue.toString()
        ) {
            return Constants.MessageLocationType.ALL_MAIL
        }

        if (labelIds.contains(Constants.MessageLocationType.ALL_SCHEDULED.messageLocationTypeValue.toString())
        ) {
            return Constants.MessageLocationType.ALL_SCHEDULED
        }

        val validLocations: List<Int> = listOf(
            Constants.MessageLocationType.INBOX.messageLocationTypeValue,
            Constants.MessageLocationType.ALL_DRAFT.messageLocationTypeValue,
            Constants.MessageLocationType.ALL_SENT.messageLocationTypeValue,
            Constants.MessageLocationType.TRASH.messageLocationTypeValue,
            Constants.MessageLocationType.SPAM.messageLocationTypeValue,
            Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue,
            Constants.MessageLocationType.SENT.messageLocationTypeValue,
            Constants.MessageLocationType.DRAFT.messageLocationTypeValue
        )

        val shortLabels = labelIds.filter { it.length <= 2 }
        val longLabels = labelIds.filter { it.length > 2 }

        for (i in shortLabels.indices) {
            val item = shortLabels[i]
            val locationInt = item.toInt()
            if (locationInt in validLocations) {
                return Constants.MessageLocationType.fromInt(locationInt)
            }
        }

        if (longLabels.isNotEmpty()) {
            longLabels.forEach {
                val resolvedLocation = resolveLabelType(it)
                if (resolvedLocation == Constants.MessageLocationType.LABEL_FOLDER) {
                    return resolvedLocation
                }
            }
            return Constants.MessageLocationType.LABEL
        }

        // special case handling of starred type if all the previous checks have failed
        if (shortLabels.contains(Constants.MessageLocationType.STARRED.asLabelIdString())) {
            return Constants.MessageLocationType.STARRED
        }

        // If there are still multiple locations but none of them were valid except All Mail
        if (labelIds.contains(Constants.MessageLocationType.ALL_MAIL.asLabelIdString())) {
            return Constants.MessageLocationType.ALL_MAIL
        }

        throw IllegalArgumentException("No valid location found in IDs: $labelIds ")
    }

    private fun resolveLabelType(labelId: String): Constants.MessageLocationType {
        return runBlocking {
            val label = labelRepository?.findLabel(LabelId(labelId))
            if (label != null && label.type == LabelType.FOLDER) {
                Constants.MessageLocationType.LABEL_FOLDER
            } else {
                Constants.MessageLocationType.LABEL
            }
        }
    }

}
