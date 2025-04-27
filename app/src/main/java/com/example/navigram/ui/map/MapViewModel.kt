package com.example.navigram.ui.map

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.navigram.R
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.api.AuthInterceptor
import com.example.navigram.data.api.CreateMemoryResponse
import com.example.navigram.data.api.UserResponse
import com.example.navigram.ui.login.getToken
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import io.github.cdimascio.dotenv.dotenv

class MapViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MapViewModel(context: Context) : ViewModel() {
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

    private val _apiService = retrofit.create(ApiService::class.java)
    val apiService: ApiService get() = _apiService

    private val _publicMemories = MutableStateFlow<List<CreateMemoryResponse>>(emptyList())
    val publicMemories: StateFlow<List<CreateMemoryResponse>> = _publicMemories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Cache for user details
    private val userCache = mutableMapOf<String, UserResponse>()
    private val _currentUser = MutableStateFlow<UserResponse?>(null)
    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow()

    private val _clusterMemories = MutableStateFlow<List<CreateMemoryResponse>>(emptyList())
    val clusterMemories: StateFlow<List<CreateMemoryResponse>> = _clusterMemories.asStateFlow()

    private var lastFetchTime = 0L
    private val backoffTime = 2000L // 2 seconds backoff
    private var errorCount = 0
    private var memoryFetched = false

    suspend fun getUserDetails(userId: String): UserResponse? {
        return userCache[userId] ?: try {
            val response = apiService.getPublicUserProfile(userId)
            if (response.isSuccessful && response.body() != null) {
                response.body()?.also { user ->
                    userCache[userId] = user
                }
            } else {
                // Try getting user from all users list if individual fetch fails
                try {
                    val allUsersResponse = apiService.getAllUsers()
                    if (allUsersResponse.isSuccessful) {
                        allUsersResponse.body()?.find { it.id == userId }?.also { user ->
                            userCache[userId] = user
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("MapViewModel", "Error fetching all users", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Error fetching user details", e)
            // Try getting user from all users list as fallback
            try {
                val allUsersResponse = apiService.getAllUsers()
                if (allUsersResponse.isSuccessful) {
                    allUsersResponse.body()?.find { it.id == userId }?.also { user ->
                        userCache[userId] = user
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching all users", e)
                null
            }
        }
    }

    init {
        loadPublicMemories()
    }

    private fun loadPublicMemories() {
        if (_isLoading.value) return

        // Check if we need to respect backoff time
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFetchTime < backoffTime && memoryFetched) {
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.getMemories()
                if (response.isSuccessful) {
                    val memories = response.body() ?: emptyList()
                    // Filter for public memories only
                    _publicMemories.value = memories.filter { it.visibility == "PUBLIC" }
                    
                    memoryFetched = true
                    lastFetchTime = System.currentTimeMillis()
                    errorCount = 0
                } else {
                    errorCount++
                    _error.value = "Failed to load memories"
                }
            } catch (e: Exception) {
                errorCount++
                _error.value = "Error: ${e.message}"
                Log.e("MapViewModel", "Error loading memories", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshMemories() {
        memoryFetched = false
        _publicMemories.value = emptyList()
        loadPublicMemories()
    }

    fun setClusterMemories(memories: List<CreateMemoryResponse>) {
        _clusterMemories.value = memories
    }

    fun clearClusterMemories() {
        _clusterMemories.value = emptyList()
    }
}
