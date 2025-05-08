package com.example.navigram.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.example.navigram.MainActivity
import com.example.navigram.R
import com.example.navigram.databinding.ActivitySignUpBinding
import com.example.navigram.ui.login.LoginActivity
import com.example.navigram.ui.login.saveToken
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.api.SignUpRequest
import com.example.navigram.data.api.SignUpResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class SignUp : AppCompatActivity() {
    private lateinit var apiService: ApiService

    private lateinit var binding: ActivitySignUpBinding
    private val _text = MutableLiveData<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Retrofit and ApiService
        val retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.BaseURL))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(ApiService::class.java)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val email = binding.email
        val password = binding.password
        val confirmPassword = binding.confirmPassword
        val signUpButton = binding.signUp
        val loading = binding.loading
        val loginBtn = binding.login

        loginBtn.setOnClickListener {
            val intent = Intent(this@SignUp, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        // Sign-Up Click Listener
        signUpButton.setOnClickListener {
            val user = username.text.toString().trim()
            val emailAdd = email.text.toString().trim()
            val pass = password.text.toString().trim()
            val confirmPass = confirmPassword.text.toString().trim()

            if (user.isEmpty() || emailAdd.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this@SignUp, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loading.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.Main).launch {
                val result = registerToNetwork(user, pass, emailAdd)
                loading.visibility = View.GONE

                println("Debug - Raw Result: $result") // Debug the raw response
                try {
                    if (result.contains("error")) {
                        val errorMessage = if (result.contains("already exists")) {
                            "Username or email already exists"
                        } else if (result.contains("invalid")) {
                            "Invalid input data"
                        } else {
                            result
                        }
                        Toast.makeText(this@SignUp, errorMessage, Toast.LENGTH_LONG).show()
                    } else {
                        val post = Gson().fromJson(result, SignUpResponse::class.java)
                        if (post != null && post.token.isNotEmpty()) {
                            saveToken(this@SignUp, post.token, post.username)
                            Toast.makeText(this@SignUp, "Account Created Successfully!", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@SignUp, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@SignUp, "Invalid response from server", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: JsonSyntaxException) {
                    println("Debug - Parse Error: ${e.message}") // Debug parse errors
                    Toast.makeText(this@SignUp, "Failed to parse server response: $result", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    println("Debug - General Error: ${e.message}") // Debug general errors
                    Toast.makeText(this@SignUp, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun registerToNetwork(username: String, password: String, email: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val signUpRequest = SignUpRequest(username, password, email)
                val response = apiService.register(signUpRequest)
                
                when {
                    response.isSuccessful -> {
                        val signUpResponse = response.body()
                        if (signUpResponse != null) {
                            Gson().toJson(signUpResponse)
                        } else {
                            "Error: Empty response from server"
                        }
                    }
                    response.code() == 409 -> "User already exists"
                    response.code() == 400 -> "Invalid input data"
                    response.code() == 500 -> "Server error occurred"
                    else -> {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        "Error: ${response.code()} - $errorBody"
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is IOException -> "Network error: Please check your internet connection"
                    else -> "Unexpected error: ${e.localizedMessage}"
                }
            }
        }
    }



}
