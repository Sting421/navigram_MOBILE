package com.example.navigram.ui.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.model.User
import kotlinx.coroutines.launch

class SearchViewModel(private val apiService: ApiService) : ViewModel() {
    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.getAllUsers()

                Log.d("SearchViewModel", "Raw Response: ${response.body()}") // Log raw response
                if (response.isSuccessful) {
                    val allUsers = response.body() ?: emptyList()
                    Log.d("SearchViewModel", "All Users Size: ${allUsers.size}") // Log allUsers size
                    val filteredUsers = if (query.isEmpty()) {
                        allUsers
                    } else {
                        allUsers.filter { user ->
                            user.username.contains(query, ignoreCase = true) ||
                            user.email.contains(query, ignoreCase = true) ||
                            (user.name?.contains(query, ignoreCase = true) == true)
                        }
                    }
                    Log.d("SearchViewModel", "Filtered Users Size: ${filteredUsers.size}") // Log filteredUsers size
                    _users.value = filteredUsers.map { userResponse ->
                        User(
                            id = userResponse.id,
                            username = userResponse.username,
                            email = userResponse.email,
                            profileImageUrl = userResponse.profilePicture,
                            bio = userResponse.name
                        )
                    }
                } else {
                    _error.value = "Failed to fetch users"
                    Log.e("SearchViewModel", "Error: ${response.errorBody()?.string()}") // Log error
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
                _users.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
