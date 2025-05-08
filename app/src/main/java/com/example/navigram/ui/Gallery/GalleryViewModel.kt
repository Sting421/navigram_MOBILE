package com.example.navigram.ui.Gallery

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log

data class ImageItem(
    val id: Long,
    val uri: String,
    val name: String,
    val path: String,
    val type: MediaType,
    val lastModified: Long
) {
    enum class MediaType {
        IMAGE, VIDEO
    }
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val _galleryData = MutableLiveData<List<ImageItem>>()
    val galleryData: LiveData<List<ImageItem>> = _galleryData
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Cache for images to avoid repeatedly loading them
    private var cachedImages: List<ImageItem>? = null
    private val TAG = "GalleryViewModel"

    fun loadImages(loadMore: Boolean = false) {
        viewModelScope.launch {
            try {
                // If we already have cached images and are not explicitly trying to load more, use them
                if (cachedImages != null && !loadMore && cachedImages!!.isNotEmpty()) {
                    _galleryData.value = cachedImages
                    return@launch
                }
                
                _isLoading.value = true
                Log.d(TAG, "Starting to load images")

                val images = withContext(Dispatchers.IO) {
                    getImagesFromMediaStore()
                }
                
                Log.d(TAG, "Found ${images.size} images")
                cachedImages = images
                _galleryData.value = images
            } catch (e: Exception) {
                Log.e(TAG, "Error loading images: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getImagesFromMediaStore(): List<ImageItem> {
        return withContext(Dispatchers.IO) {
            val images = mutableListOf<ImageItem>()

            try {
                // Query for all images in both internal and external storage
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_MODIFIED
                )

                val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
                Log.d(TAG, "Querying MediaStore for images")
                
                // Get images from external storage
                queryMediaStore(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    sortOrder,
                    images
                )
                
                // Get images from internal storage
                queryMediaStore(
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    projection,
                    sortOrder,
                    images
                )
                
                Log.d(TAG, "Total images found: ${images.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error querying MediaStore: ${e.message}")
                e.printStackTrace()
            }

            images
        }
    }
    
    private fun queryMediaStore(
        uri: android.net.Uri,
        projection: Array<String>,
        sortOrder: String,
        images: MutableList<ImageItem>
    ) {
        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                extractImagesFromCursor(cursor, images)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying $uri: ${e.message}")
        }
    }
    
    private fun extractImagesFromCursor(cursor: Cursor, images: MutableList<ImageItem>) {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

        Log.d(TAG, "Processing cursor with ${cursor.count} images")

        while (cursor.moveToNext()) {
            try {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val path = cursor.getString(dataColumn) ?: ""
                val date = cursor.getLong(dateColumn)

                // Skip invalid entries
                if (path.isBlank()) continue
                
                // Check if file exists
                val file = File(path)
                if (!file.exists() || file.length() == 0L) continue

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                images.add(
                    ImageItem(
                        id = id,
                        uri = contentUri.toString(),
                        name = name,
                        path = path,
                        type = ImageItem.MediaType.IMAGE,
                        lastModified = date * 1000
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}")
            }
        }
    }

    fun clearGalleryData() {
        cachedImages = null
        _galleryData.value = emptyList()
    }
    
    // Force a reload of images, bypassing the cache
    fun refreshImages() {
        cachedImages = null
        loadImages(true)
    }
}
