package com.utility.cam.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.utility.cam.analytics.AnalyticsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.core.graphics.scale

/**
 * Manages the storage and lifecycle of utility photos and videos
 */
class PhotoStorageManager(private val context: Context) {

    companion object {
        private const val TAG = "PhotoStorageManager"
    }

    private val photosDir = File(context.filesDir, "utility_photos")
    private val metadataFile = File(context.filesDir, "photos_metadata.json")
    private val gson = Gson()

    init {
        // Ensure photos directory exists
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
    }

    /**
     * Save a photo or video to the sandbox storage
     */
    suspend fun savePhoto(
        imageFile: File,
        ttlDuration: TTLDuration,
        description: String? = null
    ): UtilityMedia = savePhoto(imageFile, ttlDuration.toMilliseconds(), description)

    /**
     * Save a photo or video to the sandbox storage with custom TTL in milliseconds
     */
    suspend fun savePhoto(
        imageFile: File,
        ttlMilliseconds: Long,
        description: String? = null
    ): UtilityMedia = withContext(Dispatchers.IO) {
        val photoId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val expirationTime = timestamp + ttlMilliseconds

        // Detect if it's a video or image
        val isVideo = imageFile.name.endsWith(".mp4", ignoreCase = true)
        val fileExtension = if (isVideo) ".mp4" else ".jpg"

        // Create permanent file in sandbox
        val permanentFile = File(photosDir, "$photoId$fileExtension")
        imageFile.copyTo(permanentFile, overwrite = true)

        // Create thumbnail
        val thumbnailFile = File(photosDir, "${photoId}_thumb.jpg")
        if (isVideo) {
            createVideoThumbnail(permanentFile, thumbnailFile)
        } else {
            createThumbnail(permanentFile, thumbnailFile)
        }

        val photo = UtilityMedia(
            id = photoId,
            fileName = permanentFile.name,
            filePath = permanentFile.absolutePath,
            captureTimestamp = timestamp,
            expirationTimestamp = expirationTime,
            thumbnailPath = thumbnailFile.absolutePath,
            description = description
        )

        // Save metadata
        savePhotoMetadata(photo)

        // Track photo count for feedback
        val feedbackManager = FeedbackManager(context)
        feedbackManager.incrementPhotoCount()

        // Track analytics
        AnalyticsHelper.logPhotoCaptured(
            ttlDuration = "CUSTOM_${ttlMilliseconds / (60 * 60 * 1000)}H",
            hasDescription = !description.isNullOrBlank()
        )

        // Emit event
        PhotoEventBus.emit(PhotoEvent.PhotoAdded)

        photo
    }

    /**
     * Create a thumbnail from the original image
     */
    private fun createThumbnail(sourceFile: File, thumbnailFile: File) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            val targetSize = 200
            val scale = minOf(
                options.outWidth / targetSize,
                options.outHeight / targetSize
            ).coerceAtLeast(1)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }

            var bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)

            // Handle rotation
            val exif = ExifInterface(sourceFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            bitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }

            FileOutputStream(thumbnailFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            bitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Create a thumbnail from a video file
     */
    private fun createVideoThumbnail(sourceFile: File, thumbnailFile: File) {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(sourceFile.absolutePath)

            // Get frame at 1 second (or first frame if video is shorter)
            val bitmap = retriever.getFrameAtTime(1000000) // 1 second in microseconds

            if (bitmap != null) {
                // Scale down to thumbnail size
                val targetSize = 200
                val scale = minOf(
                    bitmap.width.toFloat() / targetSize,
                    bitmap.height.toFloat() / targetSize
                ).coerceAtLeast(1f)

                val scaledWidth = (bitmap.width / scale).toInt()
                val scaledHeight = (bitmap.height / scale).toInt()

                val scaledBitmap = bitmap.scale(scaledWidth, scaledHeight)

                FileOutputStream(thumbnailFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }

                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }
                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Failed to create video thumbnail", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Get all active photos (non-expired)
     */
    suspend fun getAllPhotos(): List<UtilityMedia> = withContext(Dispatchers.IO) {
        loadPhotoMetadata().filterNot { it.isExpired() }
    }

    /**
     * Get a specific photo by ID
     */
    suspend fun getPhoto(photoId: String): UtilityMedia? = withContext(Dispatchers.IO) {
        loadPhotoMetadata().find { it.id == photoId }
    }

    /**
     * Delete a specific photo
     */
    suspend fun deletePhoto(photoId: String): Boolean = withContext(Dispatchers.IO) {
        val photos = loadPhotoMetadata().toMutableList()
        val photo = photos.find { it.id == photoId } ?: return@withContext false

        // Delete files
        File(photo.filePath).delete()
        photo.thumbnailPath?.let { File(it).delete() }

        // Update metadata
        photos.remove(photo)
        saveAllPhotoMetadata(photos)

        // Track analytics
        AnalyticsHelper.logPhotoDeleted(photoId, manualDelete = true)

        // Emit event
        PhotoEventBus.emit(PhotoEvent.PhotosDeleted)

        true
    }

    /**
     * Delete all expired photos
     */
    suspend fun deleteExpiredPhotos(): Int = withContext(Dispatchers.IO) {
        Log.d(TAG, "deleteExpiredPhotos: Starting cleanup process")
        val currentTime = System.currentTimeMillis()
        Log.d(TAG, "deleteExpiredPhotos: Current time = $currentTime")

        val photos = loadPhotoMetadata()
        Log.d(TAG, "deleteExpiredPhotos: Loaded ${photos.size} total photos from metadata")

        // Log each photo's expiration status
        photos.forEachIndexed { index, photo ->
            val isExpired = photo.isExpired()
            val timeRemaining = photo.getTimeRemaining()
            Log.d(TAG, "deleteExpiredPhotos: Photo $index - ID: ${photo.id}, " +
                    "Expiration: ${photo.expirationTimestamp}, " +
                    "IsExpired: $isExpired, " +
                    "TimeRemaining: ${timeRemaining}ms (${timeRemaining / 1000}s)")
        }

        val (expired, active) = photos.partition { it.isExpired() }
        Log.d(TAG, "deleteExpiredPhotos: Found ${expired.size} expired photos and ${active.size} active photos")

        // Delete expired files
        expired.forEachIndexed { index, photo ->
            Log.d(TAG, "deleteExpiredPhotos: Deleting expired photo ${index + 1}/${expired.size} - ID: ${photo.id}")
            val mainFileDeleted = File(photo.filePath).delete()
            Log.d(TAG, "deleteExpiredPhotos: Main file deleted: $mainFileDeleted - ${photo.filePath}")

            photo.thumbnailPath?.let { thumbPath ->
                val thumbDeleted = File(thumbPath).delete()
                Log.d(TAG, "deleteExpiredPhotos: Thumbnail deleted: $thumbDeleted - $thumbPath")
            }
        }

        // Save active photos metadata
        Log.d(TAG, "deleteExpiredPhotos: Saving ${active.size} active photos to metadata")
        saveAllPhotoMetadata(active)

        Log.d(TAG, "deleteExpiredPhotos: Cleanup complete. Deleted ${expired.size} photos")

        // Emit event if photos were deleted
        if (expired.isNotEmpty()) {
            Log.d(TAG, "deleteExpiredPhotos: Emitting PhotosDeleted event")
            PhotoEventBus.emit(PhotoEvent.PhotosDeleted)
        }

        expired.size
    }

    /**
     * Save a photo or video permanently to the device gallery
     */
    suspend fun saveToGallery(photoId: String): Boolean = withContext(Dispatchers.IO) {
        val photo = getPhoto(photoId) ?: return@withContext false
        val sourceFile = File(photo.filePath)

        if (!sourceFile.exists()) return@withContext false

        try {
            // Detect if it's a video or image
            val isVideo = sourceFile.name.endsWith(".mp4", ignoreCase = true)
            val fileExtension = if (isVideo) ".mp4" else ".jpg"
            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
            val mediaUri = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "utility_cam_${photo.id}$fileExtension")
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/UtilityCam")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(mediaUri, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Track analytics
                AnalyticsHelper.logPhotoSavedToGallery(photoId)

                // Delete from sandbox after successful save
                deletePhoto(photoId)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Update photo description
     */
    suspend fun updateDescription(photoId: String, description: String) = withContext(Dispatchers.IO) {
        val photos = loadPhotoMetadata().toMutableList()
        val index = photos.indexOfFirst { it.id == photoId }

        if (index != -1) {
            photos[index] = photos[index].copy(description = description)
            saveAllPhotoMetadata(photos)
        }
    }

    // Metadata management
    private fun loadPhotoMetadata(): List<UtilityMedia> {
        return try {
            if (metadataFile.exists()) {
                val json = metadataFile.readText()
                val type = object : TypeToken<List<UtilityMedia>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun savePhotoMetadata(photo: UtilityMedia) {
        val photos = loadPhotoMetadata().toMutableList()
        photos.add(photo)
        saveAllPhotoMetadata(photos)
    }

    private fun saveAllPhotoMetadata(photos: List<UtilityMedia>) {
        try {
            val json = gson.toJson(photos)
            metadataFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
