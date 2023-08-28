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

package ch.protonmail.android.mailbox.data.mapper

import ch.protonmail.android.mailbox.data.local.model.UnreadCounterEntity
import ch.protonmail.android.mailbox.data.remote.model.CountsApiModel
import me.proton.core.domain.arch.Mapper
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Maps [CountsApiModel] to [UnreadCounterEntity]
 */
class ApiToDatabaseUnreadCounterMapper @Inject constructor() :
    Mapper<CountsApiModel, UnreadCounterEntity> {

    fun toDatabaseModel(
        apiModel: CountsApiModel,
        userId: UserId,
        type: UnreadCounterEntity.Type
    ) = UnreadCounterEntity(
        userId = userId,
        type = type,
        labelId = apiModel.labelId,
        unreadCount = apiModel.unread
    )

    fun toDatabaseModels(
        apiModels: Collection<CountsApiModel>,
        userId: UserId,
        type: UnreadCounterEntity.Type
    ): List<UnreadCounterEntity> = apiModels.map { toDatabaseModel(it, userId, type) }
}
