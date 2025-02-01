package com.example.navigram.ui.login

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.navigram.databinding.ActivityLoginBinding
import com.example.navigram.R
import com.example.navigram.ui.SignUp
import com.example.navigram.ui.dashboard.DashboardFragment
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.io.OutputStreamWriter

data class LoginResponse(
    val token: String,
    val username:String,
    val status: Int
)

class LoginActivity : AppCompatActivity() {
    private val _text = MutableLiveData<String>()

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val signUp = binding.signUp
        val loading = binding.loading

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // Disable login button unless both username & password are valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            // Complete and destroy login activity once successful
            finish()
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                    }
                }
                false
            }
            signUp?.setOnClickListener {
                loading.visibility = View.VISIBLE
                val intent = Intent(this@LoginActivity, SignUp::class.java)
                startActivity(intent)
                finish() // Close MainActivity after launching SignUp
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE

                // Launch coroutine for network call
                CoroutineScope(Dispatchers.Main).launch {
                    val result = loginToNetwork(username.text.toString(), password.text.toString())
                    if (result.startsWith("{")) {
                        try {
                            val post = Gson().fromJson(result, LoginResponse::class.java)
                            print("The status is ${post.status}")
                            if(post.status == 429){
                                Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_LONG).show()
                            }
                            else{
                                Toast.makeText(this@LoginActivity, "Hello ${post.username}, Welcome to Navigram!", Toast.LENGTH_LONG).show()
                                val intent = Intent(this@LoginActivity, DashboardFragment::class.java)
                                startActivity(intent)
                                finish() // Close MainActivity after launching SignUp
                            }
                            loading.visibility = View.GONE
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                            Toast.makeText(this@LoginActivity, "Failed to parse API response", Toast.LENGTH_LONG).show()
                            _text.postValue("Failed to parse API response")
                        }
                    } else {
                        _text.postValue("Unexpected response: $result")
                    }


                }


            }
        }
    }

    // Moved function outside onCreate()
    private suspend fun loginToNetwork(username: String, password: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL("${getString(R.string.BaseURL)}api/auth/login")
            (url.openConnection() as HttpURLConnection).run {
                requestMethod = "POST"
                connectTimeout = 1000
                readTimeout = 1000
                setRequestProperty("Content-Type", "application/json")
                doOutput = true // Enable output for request body

                // Create JSON payload
                val jsonPayload = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }

                // Write JSON payload to request body
                try {
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonPayload.toString())
                        writer.flush()
                    }

                    val response = inputStream.bufferedReader().use { it.readText() }
                    println("HTTP Response Code: $responseCode") // Debugging
                    println("API Response: $response") // Debugging

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        response
                    } else if (responseCode == 429) {
                        // Parse the JSON response to extract the message
                        val jsonResponse = JSONObject(response)
                        val errorMessage = jsonResponse.optString("message", "Unknown error")
                        println("Error 500: $errorMessage")
                        "Error: $errorMessage"
                    } else {
                        "Error: $responseCode - $response"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    "Failed to fetch data"
                } finally {
                    disconnect()
                }

            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO: initiate successful logged-in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
