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
package ch.protonmail.android.jobs

import ch.protonmail.android.api.models.MailSettings
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.events.SettingsChangedEvent
import ch.protonmail.android.feature.user.updateAddressBlocking
import ch.protonmail.android.feature.user.updateOrderBlocking
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Params
import me.proton.core.user.domain.entity.AddressId

class UpdateSettingsJob(
    private val newDisplayName: String? = null,
    private val newSignature: String? = null,
    private val addressIds: List<String>? = null,
    private val backPressed: Boolean = false,
    private val addressId: AddressId? = null
) : ProtonMailBaseJob(Params(Priority.LOW).requireNetwork()) {

    @Throws(Throwable::class)
    override fun onRun() {
        try {
            val userId = getUserManager().requireCurrentUserId()
            if (addressIds != null) {
                getUserAddressManager().updateOrderBlocking(userId, addressIds.map { UserId(it) })
                getUserManager().clearCache()
            }
            if ((newDisplayName != null || newSignature != null) && addressId != null) {
                getUserAddressManager().updateAddressBlocking(userId, addressId, newDisplayName, newSignature)
                getUserManager().clearCache()
            }
            val mailSettings = getUserManager().getCurrentUserMailSettingsBlocking()!!
            if (mailSettings != null) {
                updateMailSettings(mailSettings)
            }
            AppUtil.postEventOnUi(SettingsChangedEvent(true, backPressed, null))
        } catch (e: Exception) {
            AppUtil.postEventOnUi(SettingsChangedEvent(false, backPressed, e.message))
        }
    }

    private fun updateMailSettings(mailSettings: MailSettings) {
        getApi().updateAutoShowImages(mailSettings.showImages)
    }
}
