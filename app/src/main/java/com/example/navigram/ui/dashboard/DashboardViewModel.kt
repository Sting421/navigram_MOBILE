package com.example.navigram.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.api.CreateMemoryResponse
import com.example.navigram.data.model.Story
import kotlinx.coroutines.launch
import retrofit2.Response

class DashboardViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _stories = MutableLiveData<List<Story>>()
    val stories: LiveData<List<Story>> = _stories

    private val _feed = MutableLiveData<List<CreateMemoryResponse>>()
    val feed: LiveData<List<CreateMemoryResponse>> = _feed

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadStories() {
        viewModelScope.launch {
            try {
                val response = apiService.getAllFollowing()
                if (response.isSuccessful && response.body() != null) {
                    val users = response.body()!!.data
                    // Convert UserResponse to Story objects

                    val storyList = users.map { user ->
                        val rawUsername = user.username.trim()
                        val usernameF = if (rawUsername.length > 6) rawUsername.take(6) + "..." else rawUsername
                        Story(
                            id = user.id, // Use the actual user ID
                            username = usernameF,
                            imageUrl = user.profilePicture ?: "", // Use profile picture as story image
                            timestamp = System.currentTimeMillis(),
                            isViewed = false
                        )
                    }
                    _stories.value = storyList
                } else {
                    _error.value = "Failed to load stories"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            }
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            try {
                val response = apiService.getMemories()
                if (response.isSuccessful && response.body() != null) {
                    _feed.value = response.body()!!
                } else {
                    _error.value = "Failed to load memories"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            }
        }
    }
}
