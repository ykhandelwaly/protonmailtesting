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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package ch.protonmail.android.activities.messageDetails

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import ch.protonmail.android.details.domain.model.EmbeddedImageWithContent
import ch.protonmail.android.details.domain.model.EmbeddedImageWithOutputStream
import ch.protonmail.android.details.domain.model.MessageBodyDocument
import ch.protonmail.android.details.domain.model.MessageEmbeddedImages
import ch.protonmail.android.details.domain.model.MessageEmbeddedImagesWithContent
import ch.protonmail.android.details.domain.model.MessageEmbeddedImagesWithOutputStream
import ch.protonmail.android.details.presentation.model.RenderedMessage
import ch.protonmail.android.di.AttachmentsDirectory
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.utils.extensions.forEachAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.plus
import me.proton.core.util.kotlin.DispatcherProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A class that will inline the images in the message's body.
 *
 * ## Input
 * For start the process, these functions must be called
 * * [setMessageBody]
 * * [setImagesAndProcess]
 *
 * ## Output
 * [setImagesAndProcess] will return [RenderedMessage]
 *
 *
 * Implements [CoroutineScope] by the constructor scope
 *
 * @param scope [CoroutineScope] which this class inherit from, this should be our `ViewModel`s
 * scope, so when `ViewModel` is cleared all the coroutines for this class will be canceled
 */
internal class MessageRenderer(
    private val dispatchers: DispatcherProvider,
    private val documentParser: DocumentParser,
    private val bitmapImageDecoder: ImageDecoder,
    private val attachmentsDirectory: File,
    scope: CoroutineScope
) : CoroutineScope by scope + dispatchers.Comp {

    private val renderedMessagesCache = mutableMapOf<String, RenderedMessage>()

    private val messagesBodiesById = mutableMapOf<String, String>()
    // keep track of ids of the already inlined images across the threads
    private val inlinedImagesIdsByMessageId = mutableMapOf<String, MutableList<String>>()

    // region Actors
    private val resultsChannel = Channel<RenderedMessage>()

    /**
     * Actor that will compress images.
     *
     * This actor will work concurrently: it will push all the [EmbeddedImage]s to be processed in
     * `imageSelector`, then it will create a pool of tasks ( which pool's size is [WORKERS_COUNT] )
     * and each task will collect and process items - concurrently - from `imageSelector` until it's
     * empty.
     */
    private val imageCompressor = actor<MessageEmbeddedImages> {
        for ((messageId, embeddedImages) in channel) {
            val outputsChannel = Channel<EmbeddedImageWithOutputStream>(capacity = embeddedImages.size)

            /** This [Channel] works as a queue for handle [EmbeddedImage]s concurrently */
            val imageSelector = Channel<EmbeddedImage>(capacity = embeddedImages.size)

            // Queue all the embeddedImages
            for (embeddedImage in embeddedImages) {
                val contentId = embeddedImage.contentId.formatContentId()

                // Skip if we don't have a content id or already rendered
                if (contentId.isNotBlank() && contentId !in getInlinedImagesIds(messageId))
                    imageSelector.send(embeddedImage)
            }

            // For each worker in WORKERS_COUNT start an "async task"
            (1..WORKERS_COUNT).forEachAsync {
                // Each "task" will iterate and collect a single embeddedImage until imageSelector
                // is empty and process it asynchronously
                while (!imageSelector.isEmpty) {
                    val embeddedImage = imageSelector.receive()

                    // Process the image
                    val child = embeddedImage.localFileName ?: continue
                    val file = File(messageDirectory(embeddedImage.messageId), child)
                    // Skip if file does not exist
                    if (!file.exists() || file.length() == 0L) continue

                    val size = (MAX_IMAGES_TOTAL_SIZE / embeddedImages.size)
                        .coerceAtMost(MAX_IMAGE_SINGLE_SIZE)

                    val compressed = try {
                        ByteArrayOutputStream().also {
                            // The file could be corrupted even if exists and it's not empty
                            val bitmap = bitmapImageDecoder(file, size)
                            // Could throw `IllegalStateException` if for some reason the
                            // Bitmap is already recycled
                            bitmap.compress(Bitmap.CompressFormat.WEBP, 80, it)
                        }
                    } catch (t: Throwable) {
                        Timber.i(t, "Skip the image")
                        // Skip the image
                        continue
                    }

                    // Add the processed image to outputs
                    outputsChannel.send(EmbeddedImageWithOutputStream(embeddedImage, compressed))
                }
            }

            imageSelector.close()
            outputsChannel.close()
            imageStringifier.send(MessageEmbeddedImagesWithOutputStream(messageId, outputsChannel.toList()))
        }
    }

    /** Actor that will stringify images */
    private val imageStringifier = actor<MessageEmbeddedImagesWithOutputStream> {
        for ((messageId, imagesWithStreams) in channel) {

            val imageStrings = imagesWithStreams.map { imageWithStream ->
                val content = Base64.encodeToString(imageWithStream.stream.toByteArray(), Base64.DEFAULT)
                EmbeddedImageWithContent(imageWithStream.image, content)
            }
            imageInliner.send(MessageEmbeddedImagesWithContent(messageId, imageStrings))
        }
    }

    /** Actor that will inline images into [Document] */
    private val imageInliner = actor<MessageEmbeddedImagesWithContent> {
        for ((messageId, imagesWithContents) in channel) {
            val messageBody = messagesBodiesById.getValue(messageId)
            val document = documentParser(messageBody)

            for (imageWithContent in imagesWithContents) {

                val (embeddedImage, image64) = imageWithContent
                val contentId = embeddedImage.contentId.formatContentId()

                // Skip if we don't have a content id or already rendered
                if (contentId.isBlank() || contentId in getInlinedImagesIds(messageId)) continue
                idsListUpdater.send(messageId to contentId)

                val encoding = embeddedImage.encoding.formatEncoding()
                val contentType = embeddedImage.contentType.formatContentType()

                document.findImageElements(contentId)
                    ?.attr("src", "data:$contentType;$encoding,$image64")
            }

            documentStringifier.send(MessageBodyDocument(messageId, document))
        }
    }

    /** Actor that will stringify the Document of the message body */
    private val documentStringifier = actor<MessageBodyDocument> {
        for ((messageId, document) in channel) {
            resultsChannel.send(RenderedMessage(messageId, document.toString()))
        }
    }

    /** `CoroutineContext` for [idsListUpdater] for update [inlinedImagesIdsByMessageId] of a single thread */
    private val idsListContext = newSingleThreadContext("idsListContext")

    /** Actor that will update [inlinedImagesIdsByMessageId] */
    private val idsListUpdater = actor<Pair<String, String>>(idsListContext) {
        for ((messageId, imageId) in channel) {
            inlinedImagesIdsByMessageId.getOrPut(messageId) { mutableListOf() }
                .add(imageId)
        }
    }
    // endregion

    /**
     * Set the [messageBody] for the message with the given [messageId]
     * The [messageBody] will be used when [setImagesAndProcess] is called for a message with the same [messageId]
     *
     * @param messageBody [String] representation of the HTML message's body
     */
    fun setMessageBody(messageId: String, messageBody: String) {
        messagesBodiesById[messageId] = messageBody
        inlinedImagesIdsByMessageId[messageId]?.clear()
    }

    /**
     * Set [EmbeddedImage]s to be inlined in the message with the given [messageId]
     *
     * @throws IllegalStateException if no message body has been set for the message
     *  @see setMessageBody
     */
    suspend fun setImagesAndProcess(messageId: String, images: List<EmbeddedImage>): RenderedMessage {
        return coroutineScope {
            val fromCache = renderedMessagesCache.remove(messageId)

            if (fromCache != null) {
                return@coroutineScope fromCache
            } else {
                val cacheJob = launch {
                    checkNotNull(messagesBodiesById[messageId]) { "No message body set for id: $messageId" }
                    imageCompressor.send(MessageEmbeddedImages(messageId, images))
                    for (result in resultsChannel) {
                        renderedMessagesCache[result.messageId] = result
                    }
                }
                var renderedMessage: RenderedMessage? = renderedMessagesCache.remove(messageId)
                while (renderedMessage == null) {
                    renderedMessage = renderedMessagesCache.remove(messageId)
                    delay(1)
                }
                cacheJob.cancel()
                return@coroutineScope renderedMessage
            }
        }
    }

    private fun getInlinedImagesIds(messageId: String): List<String> =
        inlinedImagesIdsByMessageId[messageId] ?: emptyList()

    private fun messageDirectory(messageId: String) = File(attachmentsDirectory, messageId)

    /**
     * A Factory for create [MessageRenderer]
     * We use this because [MessageRenderer] needs a message body that will be retrieved lazily,
     * but we still can mock [MessageRenderer] by injecting a mocked [Factory] in the `ViewModel`
     *
     * @param imageDecoder [ImageDecoder]
     */
    class Factory @Inject constructor(
        private val dispatchers: DispatcherProvider,
        @AttachmentsDirectory private val attachmentsDirectory: File,
        private val documentParser: DocumentParser = DefaultDocumentParser(),
        private val imageDecoder: ImageDecoder = DefaultImageDecoder()
    ) {

        /** @return new instance of [MessageRenderer] */
        fun create(scope: CoroutineScope) =
            MessageRenderer(dispatchers, documentParser, imageDecoder, attachmentsDirectory, scope)
    }

}

// region constants
/** A count of bytes representing the maximum total size of the images to inline */
private const val MAX_IMAGES_TOTAL_SIZE = 9_437_184 // 9 MB

/** A count of bytes representing the maximum size of a single images to inline */
private const val MAX_IMAGE_SINGLE_SIZE = 1_048_576 // 1 MB

/** Max number of concurrent workers. It represents the available processors */
private val WORKERS_COUNT get() = Runtime.getRuntime().availableProcessors()

/** Placeholder for image's id */
private const val ID_PLACEHOLDER = "%id"

/** [Array] of html attributes that could contain an image */
private val IMAGE_ATTRIBUTES =
    arrayOf("img[src=$ID_PLACEHOLDER]", "img[src=cid:$ID_PLACEHOLDER]", "img[rel=$ID_PLACEHOLDER]")
// endregion

// region extensions
private fun String.formatEncoding() = toLowerCase()
private fun String.formatContentId() = trimStart('<').trimEnd('>')
private fun String.formatContentType() = toLowerCase()
    .replace("\r", "").replace("\n", "")
    .replaceFirst(";.*$".toRegex(), "")

/**
 * Flatten the receiver [Document] by removing the indentation and disabling prettyPrint.
 * @return [Document]
 */
private fun Document.flatten() = apply { outputSettings().indentAmount(0).prettyPrint(false) }

/** @return [Elements] matching the image attribute for the given [id] */
private fun Document.findImageElements(id: String): Elements? {
    return IMAGE_ATTRIBUTES
        .map { attr -> attr.replace(ID_PLACEHOLDER, id) }
        // with `asSequence` iteration will stop when the first usable element
        // is found and so avoid to make too many calls to document.select
        .asSequence()
        .map { select(it) }
        .find { it.isNotEmpty() }
}
// endregion

// region DocumentParser
/**
 * Parses a document as [String] and returns a [Document] model
 */
internal interface DocumentParser {
    suspend operator fun invoke(body: String): Document
}

/**
 * Default implementation of [DocumentParser]
 */
internal class DefaultDocumentParser @Inject constructor() : DocumentParser {
    override suspend fun invoke(body: String): Document = Jsoup.parse(body).flatten()
}
// endregion

// region ImageDecoder
/**
 * Decodes to [Bitmap] the image provided by the given [File] to fit the max size provided
 */
internal interface ImageDecoder {
    operator fun invoke(file: File, maxBytes: Int): Bitmap
}

/**
 * Default implementation of [ImageDecoder]
 */
internal class DefaultImageDecoder @Inject constructor() : ImageDecoder {
    override fun invoke(file: File, maxBytes: Int): Bitmap {
        // https://stackoverflow.com/a/8497703/6372379

        // Decode image size
        val boundOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundOptions)

        var scale = 1
        while (boundOptions.outWidth * boundOptions.outHeight * (1 / scale.toDouble().pow(2.0)) > maxBytes) {
            scale++
        }

        return if (scale > 1) {
            scale--
            // scale to max possible inSampleSize that still yields an image larger than target
            val options = BitmapFactory.Options().apply { inSampleSize = scale }
            val tempBitmap = BitmapFactory.decodeFile(file.absolutePath, options)

            // resize to desired dimensions
            val height = tempBitmap.height
            val width = tempBitmap.width

            val y = sqrt(maxBytes / (width.toDouble() / height))
            val x = (y / height) * width

            val scaledBitmap = Bitmap.createScaledBitmap(tempBitmap, x.toInt(), y.toInt(), true)
            tempBitmap.recycle()

            scaledBitmap

        } else {
            BitmapFactory.decodeFile(file.absolutePath)
        }
    }
}
// endregion
