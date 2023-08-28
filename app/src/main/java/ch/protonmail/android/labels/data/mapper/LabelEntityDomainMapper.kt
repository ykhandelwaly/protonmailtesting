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

package ch.protonmail.android.labels.data.mapper

import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.model.Label
import me.proton.core.domain.arch.Mapper
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class LabelEntityDomainMapper @Inject constructor() : Mapper<LabelEntity, Label> {

    fun toLabel(model: LabelEntity) = Label(
        id = model.id,
        name = model.name,
        color = model.color,
        order = model.order,
        type = model.type,
        path = model.path,
        parentId = model.parentId
    )

    fun toEntity(model: Label, userId: UserId) = LabelEntity(
        id = model.id,
        userId = userId,
        name = model.name,
        color = model.color,
        order = model.order,
        type = model.type,
        path = model.path,
        parentId = model.parentId,
        expanded = 0,
        sticky = 0,
        notify = 0
    )
}
