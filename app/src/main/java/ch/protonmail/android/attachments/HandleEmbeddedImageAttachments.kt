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

package ch.protonmail.android.attachments

import android.content.Context
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.work.ListenableWorker
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.model.AttachmentMetadata
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.storage.AttachmentClearingServiceHelper
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.TryWithRetry
import me.proton.core.util.kotlin.forEachAsync
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Handles downloads of image attachments embedded in HTML code , part of [DownloadEmbeddedAttachmentsWorker].
 */
class HandleEmbeddedImageAttachments @Inject constructor(
    private val context: Context,
    private val userManager: UserManager,
    private val databaseProvider: DatabaseProvider,
    private val clearingServiceHelper: AttachmentClearingServiceHelper,
    private val attachmentsRepository: AttachmentsRepository,
    private val tryWithRetry: TryWithRetry
) {

    private val attachmentMetadataDao: AttachmentMetadataDao
        get() = databaseProvider.provideAttachmentMetadataDao(userManager.requireCurrentUserId())

    suspend operator fun invoke(
        embeddedImages: List<EmbeddedImage>,
        crypto: AddressCrypto,
        messageId: String
    ): ListenableWorker.Result {

        if (embeddedImages.isEmpty()) {
            Timber.i("Cannot download empty embedded attachments list")
            AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.FAILED))
            return ListenableWorker.Result.failure()
        }

        val pathname = context.filesDir.toString() + Constants.DIR_EMB_ATTACHMENT_DOWNLOADS + messageId
        Timber.v("Embedded attachments path: $pathname")
        val attachmentsDirectoryFile = File(pathname)
        if (!createAttachmentFolderIfNeeded(attachmentsDirectoryFile)) {
            return ListenableWorker.Result.failure()
        }

        Timber.v("handleEmbeddedImages images:${embeddedImages.size} directory:$attachmentsDirectoryFile")
        // short-circuit if all attachments are already downloaded
        val attachmentsAlreadyDownloaded = getAllAttachmentsAlreadyDownloaded(
            attachmentsDirectoryFile,
            messageId,
            embeddedImages,
            attachmentMetadataDao
        )
        if (attachmentsAlreadyDownloaded.isNotEmpty()) {
            Timber.v("All attachments already downloaded")
            AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.SUCCESS, attachmentsAlreadyDownloaded))
            return ListenableWorker.Result.success()
        }

        AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.STARTED))

        var hasFailed = false

        val embeddedImagesWithLocalFile = mutableListOf<EmbeddedImage>()
        embeddedImages.withIndex().forEachAsync {
            val (index, embeddedImage) = it.index to it.value

            val filename = calculateFilename(embeddedImage.fileNameFormatted, index)
            val attachmentFile = File(attachmentsDirectoryFile, filename)
            Timber.v("Trying to download file: ${embeddedImage.fileNameFormatted} calculated file: $filename")

            tryWithRetry {
                val decryptedByteArray = attachmentsRepository.getAttachmentDataOrNull(
                    crypto,
                    embeddedImage.attachmentId,
                    embeddedImage.key
                )

                decryptedByteArray?.let { bytes ->
                    attachmentFile.sink().buffer().use { sink ->
                        sink.write(bytes)
                    }
                }

                val embeddedImageWithFile = embeddedImage.copy(localFileName = filename)
                embeddedImagesWithLocalFile.add(embeddedImageWithFile)

                val uri = FileProvider.getUriForFile(
                    context, context.applicationContext.packageName + ".provider", attachmentFile
                )

                val attachmentMetadata = AttachmentMetadata(
                    embeddedImageWithFile.attachmentId,
                    embeddedImageWithFile.fileNameFormatted,
                    embeddedImageWithFile.size,
                    embeddedImageWithFile.messageId + "/" + filename,
                    embeddedImageWithFile.messageId,
                    System.currentTimeMillis(),
                    uri
                )
                Timber.v(
                    "Insert embd attachment id: ${embeddedImageWithFile.attachmentId} messageId: ${embeddedImageWithFile.messageId}"
                )
                attachmentMetadataDao.insertAttachmentMetadata(attachmentMetadata)
            }.mapLeft { error ->
                Timber.e(error, "handleEmbeddedImages exception")
                hasFailed = true
            }
        }

        return if (hasFailed) {
            AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.FAILED))
            ListenableWorker.Result.failure()
        } else {
            clearingServiceHelper.startRegularClearUpService() // TODO don't call it every time we download attachments
            AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.SUCCESS, embeddedImagesWithLocalFile))
            ListenableWorker.Result.success()
        }
    }

    private fun createAttachmentFolderIfNeeded(path: File): Boolean = try {
        if (!path.exists()) {
            path.mkdirs()
        }
        true
    } catch (exception: SecurityException) {
        Timber.e(exception, "createAttachmentFolderIfNeeded exception")
        AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.FAILED))
        false
    }

    private fun getAllAttachmentsAlreadyDownloaded(
        attachmentsDirectoryFile: File,
        messageId: String,
        embeddedImages: List<EmbeddedImage>,
        attachmentMetadataDao: AttachmentMetadataDao
    ): List<EmbeddedImage> {

        if (attachmentsDirectoryFile.exists()) {

            val attachmentMetadataList = attachmentMetadataDao.getAllAttachmentsForMessage(messageId)

            val embeddedImagesWithLocalFiles = mutableListOf<EmbeddedImage>()
            embeddedImages.forEach { embeddedImage ->
                attachmentMetadataList.find { it.id == embeddedImage.attachmentId }?.let {
                    embeddedImagesWithLocalFiles.add(
                        embeddedImage.copy(localFileName = it.localLocation.substringAfterLast("/"))
                    )
                } ?: return emptyList() // a file is not downloaded
            }

            if (
                embeddedImagesWithLocalFiles.isNotEmpty() &&
                embeddedImagesWithLocalFiles.all { it.localFileName != null }
            ) return embeddedImagesWithLocalFiles // all embedded images are in the local file storage already
        }

        return emptyList()
    }

    private fun calculateFilename(originalFilename: String, position: Int): String {
        var filename = originalFilename.replace(" ", "_").replace("/", ":")
        val filenameArray = filename.split(".").toTypedArray()
        filenameArray[0] = filenameArray[0] + "(" + position + ")"
        filename = TextUtils.join(".", filenameArray).replace(" ", "_").replace("/", ":")
        return filename
    }
}
