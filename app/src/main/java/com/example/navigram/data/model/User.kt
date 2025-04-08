package com.example.navigram.data.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val memoriesCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val name: String? = null
)
