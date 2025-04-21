package com.example.navigram.data.model

data class Story(
    val id: String,
    val username: String,
    val imageUrl: String,
    val timestamp: Long,
    val duration: Long = 15000, // Default story duration in milliseconds
    val isViewed: Boolean = false
)
