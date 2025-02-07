package com.example.navigram.ui.Gallery

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Data class to represent an image or video item
data class ImageItem(
    val file: File,
    val type: MediaType,
    val lastModified: Long = file.lastModified()
) {
    enum class MediaType {
        IMAGE, VIDEO
    }

    val name: String = file.name
    val path: String = file.absolutePath
}

class GalleryViewModel : ViewModel() {
    // Private mutable live data that can be modified within the ViewModel
    private val _galleryData = MutableLiveData<List<ImageItem>>()

    // Public immutable live data exposed to observers
    val galleryData: LiveData<List<ImageItem>> = _galleryData
    
    // Private mutable live data for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Pagination parameters
    private var currentPage = 0
    private val itemsPerPage = 20
    private var hasMoreItems = true

    // Flag to ensure images are loaded only once per session
    private var isImagesLoaded = false

    // Function to load images from the device
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
                    getImagesFromStorage(currentPage, itemsPerPage)
                }

                val currentList = if (loadMore) _galleryData.value.orEmpty() else listOf()
                val updatedList = currentList + images

                _galleryData.value = updatedList
                _isLoading.value = false

                // Update pagination state
                hasMoreItems = images.size == itemsPerPage
                currentPage++

                if (!loadMore) {
                    isImagesLoaded = true
                }
            }
        }
    }

    // Private function to fetch images from storage
    private fun getImagesFromStorage(page: Int, pageSize: Int): List<ImageItem> {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Navigram"
        )

        return directory.listFiles()
            ?.filter { file ->
                file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png", "mp4", "gif", "bmp")
            }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(page * pageSize)
            ?.take(pageSize)
            ?.map { file ->
                ImageItem(
                    file = file,
                    type = when (file.extension.lowercase()) {
                        "mp4" -> ImageItem.MediaType.VIDEO
                        else -> ImageItem.MediaType.IMAGE
                    }
                )
            }
            ?: emptyList()
    }

    // Optional: Function to reset loading flag (e.g., when needed)
    fun resetImagesLoaded() {
        isImagesLoaded = false
    }

    // Optional: Function to clear gallery data
    fun clearGalleryData() {
        _galleryData.value = emptyList()
    }
}
