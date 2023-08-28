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

import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.mailbox.domain.model.Correspondent
import me.proton.core.domain.arch.Mapper
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

/**
 * Maps from [MessageSender] API model to [Correspondent] Domain model
 */
class MessageSenderToCorrespondentMapper @Inject constructor() :
    Mapper<MessageSender, Correspondent> {

    fun toDomainModel(messageSender: MessageSender) = Correspondent(
        name = messageSender.name ?: EMPTY_STRING,
        address = messageSender.emailAddress ?: EMPTY_STRING
    )

    fun toDomainModelOrEmpty(messageSender: MessageSender?): Correspondent =
        messageSender?.let(::toDomainModel) ?: Correspondent(
            name = EMPTY_STRING,
            address = EMPTY_STRING
        )
}
