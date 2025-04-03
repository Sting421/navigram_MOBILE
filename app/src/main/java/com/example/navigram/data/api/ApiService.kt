package com.example.navigram.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.Response

class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(
        chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
    )
}

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
    val profilePicture: String?,
    val phoneNumber: String?,
    val role: String,
    val name: String?,
    val id: String,
    val socialLogin: Boolean,
    val email: String,
    val username: String
)

interface ApiService {
    @GET("api/auth/me")
    suspend fun getUserProfile(): Response<UserResponse>

    @PUT("api/users/{id}")
    suspend fun updateUserProfile(
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>
}
