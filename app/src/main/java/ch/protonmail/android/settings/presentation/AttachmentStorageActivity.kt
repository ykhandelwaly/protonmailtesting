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
package ch.protonmail.android.settings.presentation

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import butterknife.OnClick
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.UNLIMITED_ATTACHMENT_STORAGE
import ch.protonmail.android.storage.AttachmentClearingService
import ch.protonmail.android.utils.extensions.showToast
import com.google.android.material.slider.Slider

const val EXTRA_SETTINGS_ATTACHMENT_STORAGE_VALUE = "EXTRA_SETTINGS_ATTACHMENT_STORAGE_VALUE"

class AttachmentStorageActivity : BaseActivity() {

    private val slider by lazy { findViewById<Slider>(R.id.attachment_storage_value) }
    private val storageTextValue by lazy { findViewById<TextView>(R.id.storage_text_value) }

    private var mAttachmentStorageCurrentValue = 0
    override fun getLayoutId() = R.layout.activity_attachment_storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        slider.setLabelFormatter { value ->
            if (value == slider.valueTo) getString(R.string.unlimited)
            else value.toInt().toString()
        }
        mAttachmentStorageCurrentValue =
            intent.getIntExtra(EXTRA_SETTINGS_ATTACHMENT_STORAGE_VALUE, Constants.DEFAULT_ATTACHMENT_STORAGE_IN_MB)

        if (mAttachmentStorageCurrentValue == UNLIMITED_ATTACHMENT_STORAGE) {
            slider.value = slider.valueTo
            storageTextValue.text = getString(R.string.attachment_storage_value_current_unlimited)
        } else {
            storageTextValue.text =
                String.format(getString(R.string.attachment_storage_value_current), mAttachmentStorageCurrentValue)
            slider.value = mAttachmentStorageCurrentValue.toFloat()
                .coerceAtLeast(slider.valueFrom)
                .coerceAtMost(slider.valueTo)
        }

        slider.addOnChangeListener(
            Slider.OnChangeListener { slider, value, _ ->
                slider.value = value
                if (value == slider.valueTo) {
                    mAttachmentStorageCurrentValue = UNLIMITED_ATTACHMENT_STORAGE
                    storageTextValue.text = getString(R.string.attachment_storage_value_current_unlimited)
                } else {
                    mAttachmentStorageCurrentValue = value.toInt()
                    storageTextValue.text = String.format(
                        getString(R.string.attachment_storage_value_current),
                        mAttachmentStorageCurrentValue
                    )
                }
                val user = mUserManager.currentLegacyUser
                val attachmentStorageChanged = mAttachmentStorageCurrentValue != user?.maxAttachmentStorage
                if (attachmentStorageChanged) {
                    user?.maxAttachmentStorage = mAttachmentStorageCurrentValue
                }
            }
        )
    }

    @OnClick(R.id.clear_local_cache)
    fun onLocalCacheClearClicked() {
        AttachmentClearingService.startClearUpImmediatelyService(
            applicationContext,
            mUserManager.requireCurrentUserId()
        )
        this.showToast(R.string.local_storage_cleared, Toast.LENGTH_SHORT)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        finish()
    }
}
