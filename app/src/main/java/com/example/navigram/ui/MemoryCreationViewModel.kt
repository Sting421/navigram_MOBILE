package com.example.navigram.ui

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class MemoryCreationState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedMediaUri: Uri? = null,
    val selectedLocation: MemoryLocation? = null,
    val isSuccess: Boolean = false
)

data class MemoryLocation(
    val latitude: Double,
    val longitude: Double
)

class MemoryCreationViewModel : ViewModel() {
    private val _state = MutableLiveData(MemoryCreationState())
    val state: LiveData<MemoryCreationState> = _state

    fun setMediaUri(uri: Uri?) {
        _state.value = _state.value?.copy(
            selectedMediaUri = uri,
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
        
        if (currentState.selectedMediaUri == null) {
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
                
                // TODO: Implement actual upload logic here
                // For now, just simulate a delay
                kotlinx.coroutines.delay(2000)
                
                _state.value = currentState.copy(
                    isLoading = false,
                    isSuccess = true,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to upload memory"
                )
            }
        }
    }
}
