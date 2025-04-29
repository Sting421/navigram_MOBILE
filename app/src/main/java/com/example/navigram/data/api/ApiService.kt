package com.example.navigram.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.Response
import com.example.navigram.data.model.User

class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(
        chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
    )
}

data class CreateMemoryRequest(
    val latitude: Double,
    val longitude: Double,
    val mediaUrl: String,
    val mediaType: String,
    val description: String,
    val visibility: String
)

data class CreateMemoryResponse(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val mediaUrl: String,
    val mediaType: String,
    val description: String,
    val visibility: String,
    val createdAt: String,
    val userId: String,
    val username: String,
)

data class UpdateUserRequest(
    val profilePicture: String?,
    val phoneNumber: String?,
    val role: String,
    val name: String,
    val socialLogin: Boolean,
    val email: String,
    val username: String
)

data class UserResponse(
    val profilePicture: String,
    val phoneNumber: String?,
    val role: String,
    val name: String?,
    val id: String,
    val socialLogin: Boolean,
    val email: String,
    val username: String
)

data class Auth0TokenRequest(
    val email: String,
    val name: String,
    val googleId: String,
    val idToken: String,
    val serverAuthCode: String?,
    val grantType: String = "authorization_code"
)

data class AuthResponse(
    val token: String,
    val username: String,
    val status: Int
)

data class FlagMemoryRequest(
    val memoryId: String,
    val reason: String
)

data class FlagMemoryResponse(
    val success: Boolean,
    val message: String
)

data class CreateCommentRequest(
    val memoryId: String,
    val content: String
)

data class UpdateMemoryRequest(
    val latitude: Double,
    val longitude: Double,
    val mediaUrl: String,
    val mediaType: String,
    val description: String,
    val visibility: String
)

data class CommentResponse(
    val id: String,
    val memoryId: String,
    val content: String,
    val userId: String,
    val username: String,
    val profilePicture: String?,
    val createdAt: String
)

data class CommentsResponse(
    val data: List<CommentResponse>,
    val success: Boolean,
    val message: String
)

data class FollowCountsResponse(
    val data: FollowCounts,
    val success: Boolean,
    val message: String
)

data class FollowCounts(
    val followers: Int,
    val following: Int
)

interface ApiService {
    @PUT("api/memories/{id}")
    suspend fun updateMemory(
        @Path("id") memoryId: String,
        @Body request: UpdateMemoryRequest
    ): Response<CreateMemoryResponse>

    @retrofit2.http.DELETE("api/memories/{id}")
    suspend fun deleteMemory(@Path("id") memoryId: String): Response<Unit>

    @GET("api/comments/memory/{memoryId}")
    suspend fun getMemoryComments(@Path("memoryId") memoryId: String): Response<CommentsResponse>

    @POST("api/comments")
    suspend fun createComment(@Body request: CreateCommentRequest): Response<CommentResponse>

    @POST("api/auth/social/auth0/exchange")
    suspend fun exchangeAuth0Token(@Body request: Auth0TokenRequest): Response<AuthResponse>

    @POST("api/memories")
    suspend fun createMemory(@Body request: CreateMemoryRequest): Response<CreateMemoryResponse>

    @GET("api/auth/me")
    suspend fun getUserProfile(): Response<UserResponse>

    @PUT("api/users/{id}")
    suspend fun updateUserProfile(
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>

    @GET("api/memories")
    suspend fun getMemories(): Response<List<CreateMemoryResponse>>

    @GET("api/users/all")
    suspend fun getAllUsers(): Response<List<UserResponse>>

    @GET("api/users/{userId}")
    suspend fun getPublicUserProfile(@Path("userId") userId: String): Response<UserResponse>

    @POST("api/users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: String): Response<Unit>

    @POST("api/flags")
    suspend fun flagMemory(@Body request: FlagMemoryRequest): Response<FlagMemoryResponse>

    @GET("api/users/{userId}/follow-counts")
    suspend fun getUserFollowCounts(@Path("userId") userId: String): Response<FollowCountsResponse>
}
