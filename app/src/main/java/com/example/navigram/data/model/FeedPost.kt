package com.example.navigram.data.model

data class FeedPost(
    val id: String,
    val userId: String,
    val username: String,
    val name: String?,
    val mediaUrl: String,
    val mediaType: String,
    val title: String?,
    val description: String?,
    val comments: List<Comment>? = null,
    val flagReason: String? = null,
    val totalFlags: Int = 0,
    val createdAt: String,
    val upvoteCount: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val visibility: String,
    val distanceInMeters: Double? = null,
    val audioUrl: String? = null
)

data class Comment(
    val id: String,
    val userId: String,
    val username: String,
    val text: String,
    val timestamp: Long
)
