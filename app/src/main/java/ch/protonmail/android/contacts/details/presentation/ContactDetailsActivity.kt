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

package ch.protonmail.android.contacts.details.presentation

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.StartCompose
import ch.protonmail.android.contacts.details.edit.EditContactDetailsActivity
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsUiItem
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsViewState
import ch.protonmail.android.databinding.ActivityContactDetailsBinding
import ch.protonmail.android.usecase.create.VCARD_TEMP_FILE_NAME
import ch.protonmail.android.utils.FileHelper
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showTwoButtonInfoDialog
import ch.protonmail.android.views.ListItemThumbnail
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ContactDetailsActivity : AppCompatActivity() {

    private lateinit var thumbnailImage: ImageView
    private lateinit var detailsAdapter: ContactDetailsAdapter
    private lateinit var thumbnail: ListItemThumbnail
    private lateinit var contactNameView: TextView
    private lateinit var detailsContainer: NestedScrollView
    private lateinit var progressBar: ProgressBar
    private var contactId: String = EMPTY_STRING
    private var vCardToShare: String = EMPTY_STRING
    private var contactEmail: String = EMPTY_STRING
    private var contactPhone: String = EMPTY_STRING
    private var decryptedCardType0: String? = null
    private var decryptedCardType1: String? = null
    private var decryptedCardType2: String? = null
    private var decryptedCardType3: String? = null
    private val viewModel: ContactDetailsViewModel by viewModels()

    private val startComposeLauncher = registerForActivityResult(StartCompose()) { messageId ->
        messageId?.let {
            val snack = Snackbar.make(
                findViewById(R.id.contact_details_layout),
                R.string.snackbar_message_draft_saved,
                Snackbar.LENGTH_LONG
            )
            snack.setAction(R.string.move_to_trash) {
                viewModel.moveDraftToTrash(messageId)
                Snackbar.make(
                    findViewById(R.id.contact_details_layout),
                    R.string.snackbar_message_draft_moved_to_trash,
                    Snackbar.LENGTH_LONG
                ).show()
            }
            snack.show()
        }
    }

    @Inject
    lateinit var fileHelper: FileHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityContactDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar as Toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.contact_details)
        }

        progressBar = binding.progressBarContactDetails
        detailsContainer = binding.scrollViewContactDetails
        contactNameView = binding.textViewContactDetailsContactName
        thumbnail = binding.thumbnailContactDetails
        thumbnailImage = binding.imageViewContactDetailsThumbnail

        val itemDecoration = DividerItemDecoration(
            this,
            LinearLayout.VERTICAL
        ).apply {
            getDrawable(R.drawable.list_divider)?.let {
                setDrawable(it)
            }
        }

        detailsAdapter = ContactDetailsAdapter(
            ::onWriteToContact,
            ::onCallContact,
            ::onAddressClicked,
            ::onUrlClicked
        )
        binding.recyclerViewContactsDetails.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = detailsAdapter
            addItemDecoration(itemDecoration)
        }

        binding.includeContactDetailsButtons.textViewContactDetailsCompose.setOnClickListener {
            onWriteToContact(contactEmail)
        }

        binding.includeContactDetailsButtons.textViewContactDetailsCall.setOnClickListener {
            onCallContact(contactPhone)
        }

        binding.includeContactDetailsButtons.textViewContactDetailsShare.setOnClickListener {
            onShare(contactNameView.text.toString(), vCardToShare, this)
        }

        viewModel.contactsViewState
            .onEach { renderState(it) }
            .launchIn(lifecycleScope)

        viewModel.vCardSharedFlow
            .onEach { shareVcard(it, contactNameView.text.toString()) }
            .launchIn(lifecycleScope)

        val contactId = requireNotNull(intent.extras?.getString(EXTRA_ARG_CONTACT_ID))
        viewModel.getContactDetails(contactId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_contact_details, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_contact_details_edit -> onEditContacts()
            R.id.action_contact_details_delete ->
                onDeleteContact(
                    requireNotNull(intent.extras?.getString(EXTRA_ARG_CONTACT_ID)),
                    contactNameView.text.toString()
                )
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun onEditContacts() {

        val vCardFilePath1 = if (!decryptedCardType1.isNullOrEmpty()) {
            val filePath = "$cacheDir${File.separator}$VCARD_TEMP_FILE_NAME"
            fileHelper.saveStringToFile(filePath, checkNotNull(decryptedCardType1))
            filePath
        } else {
            EMPTY_STRING
        }
        val vCardFilePath3 = if (!decryptedCardType3.isNullOrEmpty()) {
            val filePath = "$cacheDir${File.separator}$VCARD_TEMP_FILE_NAME"
            fileHelper.saveStringToFile(filePath, checkNotNull(decryptedCardType3))
            filePath
        } else {
            EMPTY_STRING
        }
        EditContactDetailsActivity.startEditContactActivity(
            this, contactId,
            EditContactDetailsActivity.REQUEST_CODE_EDIT_CONTACT,
            decryptedCardType0,
            vCardFilePath1,
            decryptedCardType2,
            vCardFilePath3
        )
    }

    private fun onDeleteContact(contactId: String, contactName: String) {
        val clickListener = DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
            if (which == DialogInterface.BUTTON_POSITIVE) {
                viewModel.deleteContact(contactId)
                finish()
            }
        }
        if (!isFinishing) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.confirm)
                .setMessage(String.format(getString(R.string.delete_contact), contactName))
                .setNegativeButton(R.string.no, clickListener)
                .setPositiveButton(R.string.yes, clickListener)
                .create()
                .show()
        }
    }

    private fun renderState(state: ContactDetailsViewState) {
        Timber.v("State $state received")
        when (state) {
            is ContactDetailsViewState.Loading -> showLoading()
            is ContactDetailsViewState.Error -> showError(state.exception)
            is ContactDetailsViewState.Data -> showData(state)
        }
    }

    private fun showError(exception: Throwable) {
        progressBar.isVisible = false
        detailsContainer.isVisible = false
        Timber.i(exception, "Fetching contacts data has failed")
    }

    private fun showLoading() {
        progressBar.isVisible = true
        detailsContainer.isVisible = false
    }

    private fun showData(state: ContactDetailsViewState.Data) {
        progressBar.isVisible = false
        detailsContainer.isVisible = true

        vCardToShare = state.vCardToShare
        contactId = state.contactId
        decryptedCardType0 = state.vDecryptedCardType0
        decryptedCardType1 = state.vDecryptedCardType1
        decryptedCardType2 = state.vDecryptedCardType2
        decryptedCardType3 = state.vDecryptedCardType3

        setHeaderData(
            state.title,
            state.initials,
            state.photoUrl,
            state.photoBytes
        )

        detailsAdapter.submitList(state.contactDetailsItems)

        state.contactDetailsItems.onEach { item ->
            setContactDataForActionButtons(item)
        }
    }

    private fun setHeaderData(
        title: String,
        initials: String,
        photoUrl: String?,
        photoBytes: List<Byte>?
    ) {
        contactNameView.text = title
        thumbnail.bind(isSelectedActive = false, isMultiselectActive = false, initials = initials)

        if (!photoBytes.isNullOrEmpty()) {
            setThumbnailImage(photoBytes)
        } else if (photoUrl != null) {
            showTwoButtonInfoDialog(
                titleStringId = R.string.contact_details_remote_content_dialog_title,
                messageStringId = R.string.contact_details_remote_content_dialog_message,
                positiveStringId = R.string.contact_details_remote_content_dialog_positive_button,
                cancelable = false,
                cancelOnTouchOutside = false,
                onNegativeButtonClicked = { setPlaceholderThumbnailImage() }
            ) { loadThumbnailImage(photoUrl) }
        }
    }

    private fun setPlaceholderThumbnailImage() {
        thumbnailImage.apply {
            setImageResource(R.drawable.ic_file_image)
            background = ContextCompat.getDrawable(
                this@ContactDetailsActivity,
                R.drawable.circle_background_interaction_weak
            )
            setPadding(resources.getDimensionPixelSize(R.dimen.padding_4xl))
            isVisible = true
        }
        thumbnail.isVisible = false
    }

    private fun setThumbnailImage(photoBytes: List<Byte>) {
        val byteArray = photoBytes.toByteArray()
        val imageBitmap: Bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        val roundedImage = RoundedBitmapDrawableFactory.create(resources, imageBitmap).apply {
            setAntiAlias(true)
            cornerRadius = resources.getDimensionPixelSize(R.dimen.avatar_size) / 2f
        }
        thumbnailImage.apply {
            setImageDrawable(roundedImage)
            isVisible = true
        }
        thumbnail.isVisible = false
    }

    private fun loadThumbnailImage(photoUrl: String?) {
        Timber.v("Loading photo url: $photoUrl")
        val targetSize = resources.getDimensionPixelSize(R.dimen.avatar_size)
        val imageRequest = ImageRequest.Builder(this)
            .data(photoUrl)
            .size(targetSize, targetSize)
            .transformations(CircleCropTransformation())
            .target(
                onSuccess = { drawable ->
                    Timber.d("Thumbnail loading finished")
                    thumbnailImage.apply {
                        setImageDrawable(drawable)
                        isVisible = true
                    }
                    thumbnail.isVisible = false
                },
                onError = {
                    Timber.i("Thumbnail loading error")
                    thumbnailImage.isVisible = false
                    thumbnail.isVisible = true
                }
            )
            .build()
        ImageLoader(this).enqueue(imageRequest)
    }

    private fun setContactDataForActionButtons(item: ContactDetailsUiItem) {
        if (item is ContactDetailsUiItem.Email) {
            contactEmail = item.value
        }
        if (item is ContactDetailsUiItem.TelephoneNumber) {
            contactPhone = item.value
        }
    }

    private fun onWriteToContact(emailAddress: String) {
        if (emailAddress.isNotEmpty()) {
            startComposeLauncher.launch(StartCompose.Input(toRecipients = listOf(emailAddress)))
        } else {
            showToast(R.string.email_empty, Toast.LENGTH_SHORT)
        }
    }

    private fun onCallContact(phoneNumber: String) {
        Timber.v("On Call $phoneNumber")
        if (phoneNumber.isNotEmpty()) {
            val callIntent = Intent(Intent.ACTION_DIAL)
            callIntent.data = Uri.parse("tel:$phoneNumber")
            startActivity(callIntent)
        } else {
            showToast(R.string.contact_vcard_new_row_phone, Toast.LENGTH_SHORT)
        }
    }

    private fun onShare(contactName: String, vCardToShare: String, context: Context) {
        if (vCardToShare.isNotEmpty()) {
            viewModel.saveVcard(vCardToShare, contactName, context)
        } else {
            showToast(R.string.default_error_message, Toast.LENGTH_SHORT)
        }
    }

    private fun shareVcard(vcfFileUri: Uri, contactName: String) {
        if (vcfFileUri != Uri.EMPTY) {
            Timber.v("Share contact uri: $vcfFileUri")
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, vcfFileUri)
                type = ContactsContract.Contacts.CONTENT_VCARD_TYPE
            }
            startActivity(Intent.createChooser(intent, contactName))
        }
    }

    private fun onAddressClicked(address: String) {
        onUrlClicked("geo:0,0?q=$address")
    }

    private fun onUrlClicked(url: String) {
        Timber.v("On Url clicked $url")
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        startActivity(intent)
    }

    companion object {

        const val EXTRA_ARG_CONTACT_ID = "extra_arg_contact_id"
    }
}
