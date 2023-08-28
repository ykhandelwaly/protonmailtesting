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
package ch.protonmail.android.drawer.presentation.mapper

import android.content.Context
import android.graphics.Color
import androidx.annotation.VisibleForTesting
import ch.protonmail.android.R
import ch.protonmail.android.drawer.presentation.model.DrawerLabelUiModel
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.utils.UiUtil
import me.proton.core.domain.arch.Mapper
import timber.log.Timber
import javax.inject.Inject

/**
 * A Mapper of [DrawerLabelUiModel]
 * Inherit from [Mapper]
 *
 * @property useFolderColor whether the user enabled the settings for use Colors for Folders.
 *  TODO to be implemented in MAILAND-1818, ideally inject its use case. Currently defaults to `true`
 *
 * TODO: the mapper currently includes a piece of logic to flatten the hierarchy of the Folders to facilitate the
 *  implementation with a conventional RecyclerView/Adapter, this must be reevaluated with MAILAND-2304, where we
 *  probably need to keep the hierarchy, in order to have collapsible groups
 */
internal class DrawerLabelUiModelMapper @Inject constructor(
    private val context: Context
) : Mapper<Collection<LabelOrFolderWithChildren>, List<DrawerLabelUiModel>> {

    private val useFolderColor: Boolean = true

    fun toUiModels(models: Collection<LabelOrFolderWithChildren>): List<DrawerLabelUiModel> =
        models.flatMap { domainModelToUiModels(model = it, folderLevel = 0, parentColor = null) }

    private fun domainModelToUiModels(
        model: LabelOrFolderWithChildren,
        folderLevel: Int,
        parentColor: Int?
    ): List<DrawerLabelUiModel> {
        val parent = domainModelToUiModel(
            model = model,
            folderLevel = folderLevel,
            parentColor = parentColor
        )

        val children = if (model is LabelOrFolderWithChildren.Folder) {
            model.children.flatMap {
                domainModelToUiModels(
                    model = it,
                    folderLevel = folderLevel + 1,
                    parentColor = parent.icon.colorInt
                )
            }
        } else {
            emptyList()
        }
        return listOf(parent) + children
    }

    private fun domainModelToUiModel(
        model: LabelOrFolderWithChildren,
        folderLevel: Int,
        parentColor: Int?
    ): DrawerLabelUiModel {

        val labelType = when (model) {
            is LabelOrFolderWithChildren.Label -> LabelType.MESSAGE_LABEL
            is LabelOrFolderWithChildren.Folder -> LabelType.FOLDER
        }

        val hasChildren = model is LabelOrFolderWithChildren.Folder && model.children.isNotEmpty()
        return DrawerLabelUiModel(
            labelId = model.id.id,
            name = model.name,
            icon = buildIcon(labelType, model.color, parentColor, hasChildren),
            type = labelType,
            folderLevel = folderLevel
        )
    }

    private fun buildIcon(
        type: LabelType,
        color: String,
        parentColor: Int?,
        hasChildren: Boolean
    ): DrawerLabelUiModel.Icon {

        val drawableRes = when (type) {
            LabelType.MESSAGE_LABEL -> R.drawable.shape_ellipse
            LabelType.FOLDER -> {
                if (useFolderColor) {
                    if (hasChildren.not()) R.drawable.ic_proton_folder_filled else R.drawable.ic_proton_folders_filled
                } else {
                    if (hasChildren.not()) R.drawable.ic_proton_folder else R.drawable.ic_proton_folders
                }
            }
            LabelType.CONTACT_GROUP ->
                throw IllegalArgumentException("Contact groups are not supported by the nav drawer")
            LabelType.SYSTEM_FOLDER -> R.drawable.ic_proton_question_circle_filled
        }

        val colorInt =
            if (useFolderColor) toColorIntOrNull(color) ?: parentColor ?: context.getColor(R.color.icon_inverted)
            else context.getColor(R.color.icon_inverted)

        return DrawerLabelUiModel.Icon(drawableRes, colorInt)
    }

    private fun toColorIntOrNull(color: String): Int? {
        return when (color) {
            AQUA_BASE_V3_COLOR -> context.getColor(R.color.aqua_base)
            SAGE_BASE_V3_COLOR -> context.getColor(R.color.sage_base)
            else -> parseColor(color)
        }
    }

    private fun parseColor(color: String): Int? =
        try {
            Color.parseColor(UiUtil.normalizeColor(color))
        } catch (exception: IllegalArgumentException) {
            Timber.w(exception, "Cannot parse color: $color")
            null
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    companion object {

        const val AQUA_BASE_V3_COLOR = "#5ec7b7"
        const val SAGE_BASE_V3_COLOR = "#97c9c1"
    }
}
