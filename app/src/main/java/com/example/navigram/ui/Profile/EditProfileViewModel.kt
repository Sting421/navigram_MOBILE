package com.example.navigram.ui.Profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.navigram.data.CloudinaryUploader
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val userData: UserData
) : ViewModel() {
    private val _profileImageUrl = MutableLiveData<String>()
    val profileImageUrl: LiveData<String> = _profileImageUrl

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        _profileImageUrl.value = userData.profilePicture
    }

    fun updateProfileImageUrl(url: String) {
        _isLoading.value = true
        try {
            _profileImageUrl.value = url
            _isLoading.value = false
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Failed to update profile image"
            _isLoading.value = false
        }
    }
}
