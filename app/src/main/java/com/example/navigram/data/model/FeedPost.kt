package com.example.navigram.data.model

data class FeedPost(
    val id: String,
    val userId: String,
    val username: String,
    val userProfileImage: String,
    val imageUrl: String,
    val description: String,
    val likeCount: Int,
    val commentCount: Int,
    val timestamp: Long,
    val isLiked: Boolean = false,
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val id: String,
    val userId: String,
    val username: String,
    val text: String,
    val timestamp: Long
)
