package com.example.navigram.ui.Profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EditProfileViewModelFactory(private val userData: UserData) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            return EditProfileViewModel(userData) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
