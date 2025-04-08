package com.example.navigram

import android.app.Application
import com.example.navigram.data.api.ApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context
import androidx.preference.PreferenceManager
import android.util.Log
import com.example.navigram.data.api.AuthInterceptor // Import AuthInterceptor
import com.example.navigram.ui.login.getToken

class NavigramApplication : Application() {
    lateinit var apiService: ApiService
        private set

    override fun onCreate() {
        super.onCreate()

        // Retrieve the token from SharedPreferences
        val authToken = getToken(this).toString()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authToken)) // Add AuthInterceptor
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.BaseURL))
            .client(okHttpClient) // Set OkHttpClient
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    fun provideApiService(): ApiService = apiService
}
