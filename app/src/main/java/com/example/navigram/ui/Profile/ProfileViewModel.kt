package com.example.navigram.ui.Profile

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

// Data class to represent user profile information
data class UserData(
    val username: String,
    val profileImageUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0
)

class ProfileViewModel : ViewModel() {
    private val _userData = MutableStateFlow(
        UserData(
            username = "Navigram User", // Default username
            profileImageUrl = null,
            followerCount = 0,
            followingCount = 0
        )
    )
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    private val _galleryData = MutableStateFlow<List<ImageItem>>(emptyList())
    val galleryData: StateFlow<List<ImageItem>> = _galleryData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentPage = 0
    private val itemsPerPage = 20
    private var hasMoreItems = true

    init {
        loadImages()
    }

    /**
     * Loads images from storage.
     * @param loadMore: Whether to load additional images (pagination).
     */
    fun loadImages(loadMore: Boolean = false) {
        if (!hasMoreItems && loadMore) return

        if (!loadMore) {
            currentPage = 0
            hasMoreItems = true
            _galleryData.value = emptyList()
        }

        _isLoading.value = true

        viewModelScope.launch {
            val images = withContext(Dispatchers.IO) {
                getImagesFromStorage(currentPage, itemsPerPage)
            }

            if (images.isNotEmpty()) {
                _galleryData.value = _galleryData.value + images
            }

            _isLoading.value = false
            hasMoreItems = images.size == itemsPerPage
            currentPage++
        }
    }

    /**
     * Fetches images from device storage.
     */
    private fun getImagesFromStorage(page: Int, pageSize: Int): List<ImageItem> {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Camera"
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

    // Method to update user data
    fun updateUserData(userData: UserData) {
        _userData.value = userData
    }
}
