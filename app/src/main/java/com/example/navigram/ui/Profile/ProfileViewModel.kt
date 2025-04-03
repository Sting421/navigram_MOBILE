package com.example.navigram.ui.Profile

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.navigram.ui.login.getToken
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.navigram.R
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.api.AuthInterceptor
import com.example.navigram.data.api.UpdateUserRequest
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Data class to represent user profile information
data class UserData(
    val username: String,
    val email: String,
    val name: String?,
    val profilePicture: String?,
    val phoneNumber: String?,
    val role: String,
    val id: String,
    val socialLogin: Boolean
)

class ProfileViewModel(private val context: Context) : ViewModel() {
    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val retrofit by lazy {
        val token = getToken(context) ?: throw IllegalStateException("No auth token found")
        val baseUrl = context.getString(R.string.BaseURL)
        
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(token))
            .build()

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService = retrofit.create(ApiService::class.java)

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    private val _galleryData = MutableStateFlow<List<ImageItem>>(emptyList())
    val galleryData: StateFlow<List<ImageItem>> = _galleryData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentPage = 0
    private val itemsPerPage = 20
    private var hasMoreItems = true

    init {
        loadUserProfile()
        loadImages()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val response = apiService.getUserProfile()
                if (response.isSuccessful) {
                    response.body()?.let { userResponse ->
                        Log.d(TAG, "User profile response: $userResponse")
                        
                        val userData = UserData(
                            username = userResponse.username,
                            email = userResponse.email,
                            name = userResponse.name,
                            profilePicture = userResponse.profilePicture,
                            phoneNumber = userResponse.phoneNumber,
                            role = userResponse.role,
                            id = userResponse.id,
                            socialLogin = userResponse.socialLogin
                        )
                        
                        Log.d(TAG, "Created UserData: $userData")
                        _userData.value = userData
                    }
                } else {
                    Log.e(TAG, "Error loading user profile: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                // Handle error
                Log.e(TAG, "Error loading user profile", e)
            }
        }
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

    // State to track update operation status
    private val _updateStatus = MutableStateFlow<UpdateStatus?>(null)
    val updateStatus: StateFlow<UpdateStatus?> = _updateStatus.asStateFlow()

    sealed class UpdateStatus {
        object Success : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }

    // Method to update user profile
    fun updateUserProfile(userData: UserData) {
        viewModelScope.launch {
            try {
                val request = UpdateUserRequest(
                    profilePicture = userData.profilePicture,
                    phoneNumber = userData.phoneNumber,
                    role = userData.role,
                    name = userData.name ?: "",
                    socialLogin = userData.socialLogin,
                    email = userData.email,
                    username = userData.username
                )

                val response = apiService.updateUserProfile(
                    userId = userData.id,
                    request = request
                )

                if (response.isSuccessful) {
                    response.body()?.let { userResponse ->
                        val updatedUserData = UserData(
                            username = userResponse.username,
                            email = userResponse.email,
                            name = userResponse.name,
                            profilePicture = userResponse.profilePicture,
                            phoneNumber = userResponse.phoneNumber,
                            role = userResponse.role,
                            id = userResponse.id,
                            socialLogin = userResponse.socialLogin
                        )
                        _userData.value = updatedUserData
                        _updateStatus.value = UpdateStatus.Success
                    }
                } else {
                    _updateStatus.value = UpdateStatus.Error("Failed to update profile: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error("Error updating profile: ${e.message}")
                Log.e(TAG, "Error updating user profile", e)
            }
        }
    }

    // Clear update status
    fun clearUpdateStatus() {
        _updateStatus.value = null
    }
}
