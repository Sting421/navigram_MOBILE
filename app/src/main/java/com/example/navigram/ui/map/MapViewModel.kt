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
import com.example.navigram.ui.login.getToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

    private val apiService = retrofit.create(ApiService::class.java)

    private val _publicMemories = MutableStateFlow<List<CreateMemoryResponse>>(emptyList())
    val publicMemories: StateFlow<List<CreateMemoryResponse>> = _publicMemories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _clusterMemories = MutableStateFlow<List<CreateMemoryResponse>>(emptyList())
    val clusterMemories: StateFlow<List<CreateMemoryResponse>> = _clusterMemories.asStateFlow()

    private var lastFetchTime = 0L
    private val backoffTime = 2000L // 2 seconds backoff
    private var errorCount = 0
    private var memoryFetched = false

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
