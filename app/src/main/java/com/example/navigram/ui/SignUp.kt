package com.example.navigram.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.navigram.MainActivity
import com.example.navigram.R
import com.example.navigram.databinding.ActivitySignUpBinding
import com.example.navigram.ui.login.LoginActivity
import com.example.navigram.ui.login.LoginResponse
import com.example.navigram.ui.login.clearToken
import com.example.navigram.ui.login.saveToken
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Data class for Sign-Up Response
data class SignUpResponse(
    val token: String,
    val username: String,
    val status: Int
)

class SignUp : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val _text = MutableLiveData<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                if (result.startsWith("{")) {
                    try {
                        val post = Gson().fromJson(result, SignUpResponse::class.java)
                        if (post.status == 200) {
                            Toast.makeText(this@SignUp, "Account Created Successfully!", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@SignUp, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@SignUp, "Error: ${post.status}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: JsonSyntaxException) {
                        Toast.makeText(this@SignUp, "Failed to parse API response", Toast.LENGTH_LONG).show()
                    }
                }

                else {
                    Toast.makeText(this@SignUp, result, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Function to send registration data to server
    private suspend fun registerToNetwork(username: String, password: String, email: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL("${getString(R.string.BaseURL)}api/auth/register")
            (url.openConnection() as HttpURLConnection).run {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json")
                doOutput = true

                val jsonPayload = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                    put("email", email)
                }

                try {
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonPayload.toString())
                        writer.flush()
                    }

                    val response = inputStream.bufferedReader().use { it.readText() }

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        response
                    } else {
                        "Error: $responseCode - $response"
                    }
                } catch (e: Exception) {
                    if(responseCode == 500)
                        "User already exist"
                    else
                        "Failed to fetch Data"
                } finally {
                    disconnect()
                }
            }
        }
    }



}
