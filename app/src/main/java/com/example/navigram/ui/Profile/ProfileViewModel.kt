package com.example.navigram.ui.Profile

import android.content.Context
import android.util.Log
import com.example.navigram.ui.login.getToken
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.navigram.R
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.api.AuthInterceptor
import com.example.navigram.data.api.UpdateUserRequest
import com.example.navigram.data.api.CreateMemoryResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

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

    private val _memories = MutableStateFlow<List<CreateMemoryResponse>>(emptyList())
    val memories: StateFlow<List<CreateMemoryResponse>> = _memories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _memoriesCount = MutableStateFlow(0)
    val memoriesCount: StateFlow<Int> = _memoriesCount.asStateFlow()

    private val _selectedMemory = MutableStateFlow<CreateMemoryResponse?>(null)
    val selectedMemory: StateFlow<CreateMemoryResponse?> = _selectedMemory.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadMemories() {
        viewModelScope.launch {
            try {
                val response = apiService.getMemories()
                if (response.isSuccessful) {
                    val allMemories = response.body() ?: emptyList()
                    // Filter memories by current user's ID
                    val currentUserId = _userData.value?.id
                    if (currentUserId != null) {
                        val userMemories = allMemories.filter { it.userId == currentUserId }
                        _memories.value = userMemories
                        _memoriesCount.value = userMemories.size
                    }
                } else {
                    Log.e(TAG, "Error loading memories: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading memories", e)
            }
        }
    }

    fun selectMemory(memory: CreateMemoryResponse) {
        _selectedMemory.value = memory
    }

    fun clearSelectedMemory() {
        _selectedMemory.value = null
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
                        // Reload memories after user data is loaded to apply filtering
                        loadMemories()
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

    // State to track update operation status
    private val _updateStatus = MutableStateFlow<UpdateStatus?>(null)
    val updateStatus: StateFlow<UpdateStatus?> = _updateStatus.asStateFlow()

    sealed class UpdateStatus {
        object Success : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }

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
                        // Reload memories after profile update to ensure correct filtering
                        loadMemories()
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

    fun clearUpdateStatus() {
        _updateStatus.value = null
    }
}
