package com.utility.cam.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Manages the storage and lifecycle of utility photos
 */
class PhotoStorageManager(private val context: Context) {
    
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
     * Save a photo to the sandbox storage
     */
    suspend fun savePhoto(
        imageFile: File,
        ttlDuration: TTLDuration,
        description: String? = null
    ): UtilityPhoto = withContext(Dispatchers.IO) {
        val photoId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val expirationTime = timestamp + ttlDuration.toMilliseconds()
        
        // Create permanent file in sandbox
        val permanentFile = File(photosDir, "$photoId.jpg")
        imageFile.copyTo(permanentFile, overwrite = true)
        
        // Create thumbnail
        val thumbnailFile = File(photosDir, "${photoId}_thumb.jpg")
        createThumbnail(permanentFile, thumbnailFile)
        
        val photo = UtilityPhoto(
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
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Get all active photos (non-expired)
     */
    suspend fun getAllPhotos(): List<UtilityPhoto> = withContext(Dispatchers.IO) {
        loadPhotoMetadata().filterNot { it.isExpired() }
    }
    
    /**
     * Get a specific photo by ID
     */
    suspend fun getPhoto(photoId: String): UtilityPhoto? = withContext(Dispatchers.IO) {
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
        
        true
    }
    
    /**
     * Delete all expired photos
     */
    suspend fun deleteExpiredPhotos(): Int = withContext(Dispatchers.IO) {
        val photos = loadPhotoMetadata()
        val (expired, active) = photos.partition { it.isExpired() }
        
        // Delete expired files
        expired.forEach { photo ->
            File(photo.filePath).delete()
            photo.thumbnailPath?.let { File(it).delete() }
        }
        
        // Save active photos metadata
        saveAllPhotoMetadata(active)
        
        expired.size
    }
    
    /**
     * Save a photo permanently to the device gallery
     */
    suspend fun saveToGallery(photoId: String): Boolean = withContext(Dispatchers.IO) {
        val photo = getPhoto(photoId) ?: return@withContext false
        val sourceFile = File(photo.filePath)
        
        if (!sourceFile.exists()) return@withContext false
        
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "utility_cam_${photo.id}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/UtilityCam")
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
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
    private fun loadPhotoMetadata(): List<UtilityPhoto> {
        return try {
            if (metadataFile.exists()) {
                val json = metadataFile.readText()
                val type = object : TypeToken<List<UtilityPhoto>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun savePhotoMetadata(photo: UtilityPhoto) {
        val photos = loadPhotoMetadata().toMutableList()
        photos.add(photo)
        saveAllPhotoMetadata(photos)
    }
    
    private fun saveAllPhotoMetadata(photos: List<UtilityPhoto>) {
        try {
            val json = gson.toJson(photos)
            metadataFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
