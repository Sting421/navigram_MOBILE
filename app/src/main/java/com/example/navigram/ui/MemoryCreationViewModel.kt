package com.example.navigram.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.api.CreateMemoryRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch

data class MemoryCreationState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedMediaUri: Uri? = null,
    val selectedMediaUrl: String? = null,
    val selectedLocation: MemoryLocation? = null,
    val isSuccess: Boolean = false,
        val visibility: String = "PUBLIC", // Default visibility
    val mediaType: String = "IMAGE"
)

data class MemoryLocation(
    val latitude: Double,
    val longitude: Double
)

class MemoryCreationViewModel(
    private val apiService: ApiService
) : ViewModel() {
    private val _state = MutableLiveData(MemoryCreationState())
    val state: LiveData<MemoryCreationState> = _state

    fun setMediaUri(uri: Uri?) {
        _state.value = _state.value?.copy(
            selectedMediaUri = uri,
            selectedMediaUrl = null,
            error = null
        )
    }

    fun setMediaUrl(url: String) {
        _state.value = _state.value?.copy(
            selectedMediaUri = null,
            selectedMediaUrl = url,
            error = null
        )
    }

    fun setVisibility(visibility: String) {
        _state.value = _state.value?.copy(
            visibility = visibility,
            error = null
        )
    }

    fun setLocation(latitude: Double, longitude: Double) {
        _state.value = _state.value?.copy(
            selectedLocation = MemoryLocation(latitude, longitude),
            error = null
        )
    }

    fun uploadMemory(description: String) {
        val currentState = _state.value ?: return
        
        if (currentState.selectedMediaUrl == null) {
            _state.value = currentState.copy(error = "Please select a media file")
            return
        }
        
        if (currentState.selectedLocation == null) {
            _state.value = currentState.copy(error = "Please select a location")
            return
        }

        viewModelScope.launch {
            try {
                _state.value = currentState.copy(isLoading = true, error = null)
                
                // Create and log the request
                val request = CreateMemoryRequest(
                    latitude = currentState.selectedLocation.latitude,
                    longitude = currentState.selectedLocation.longitude,
                    mediaUrl = currentState.selectedMediaUrl,
                    mediaType = currentState.mediaType,
                    description = description,
                    visibility = currentState.visibility
                )
                
                Log.d("MemoryCreation", "Creating memory with coordinates: (${request.latitude}, ${request.longitude})")
                val response = apiService.createMemory(request)
                // Log response
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    val gson = Gson()
                    val jsonResponse = gson.toJson(responseBody)
                    Log.d("MemoryCreation", "Response: $jsonResponse")
                    
                    _state.value = currentState.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                } else {
                    Log.e("MemoryCreation", "Error response: ${response.errorBody()?.string()}")
                    _state.value = currentState.copy(
                        isLoading = false,
                        error = "Failed to create memory: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create memory"
                )
            }
        }
    }
}
