package com.example.navigram.ui.Gallery

import android.app.Application
import android.content.ContentUris
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

    private var currentPage = 0
    private val itemsPerPage = 20
    private var hasMoreItems = true
    private var isImagesLoaded = false

    fun loadImages(loadMore: Boolean = false) {
        if (!hasMoreItems && !loadMore) return

        if (!loadMore) {
            currentPage = 0
            hasMoreItems = true
            isImagesLoaded = false
        }

        if (!isImagesLoaded || loadMore) {
            _isLoading.value = true

            viewModelScope.launch {
                val images = withContext(Dispatchers.IO) {
                    getImagesFromMediaStore(currentPage, itemsPerPage)
                }

                val currentList = if (loadMore) _galleryData.value.orEmpty() else listOf()
                val updatedList = currentList + images

                _galleryData.value = updatedList
                _isLoading.value = false

                hasMoreItems = images.size == itemsPerPage
                currentPage++

                if (!loadMore) {
                    isImagesLoaded = true
                }
            }
        }
    }

    private fun getImagesFromMediaStore(page: Int, pageSize: Int): List<ImageItem> {
        val images = mutableListOf<ImageItem>()
        val offset = page * pageSize

        try {
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Images.Media.SIZE} > 0"
            } else {
                null
            }

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_MODIFIED
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                try {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                    // Handle pagination
                    if (cursor.moveToPosition(offset)) {
                        var count = 0
                        do {
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn) ?: continue
                            val path = cursor.getString(dataColumn) ?: continue
                            val date = cursor.getLong(dateColumn)

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
                                    lastModified = date * 1000 // Convert to milliseconds
                                )
                            )

                            count++
                        } while (count < pageSize && cursor.moveToNext())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return images
    }

    fun resetImagesLoaded() {
        isImagesLoaded = false
    }

    fun clearGalleryData() {
        _galleryData.value = emptyList()
    }
}
