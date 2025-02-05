package com.example.navigram.ui.login

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
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.io.OutputStreamWriter

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.navigram.ui.CameraCapture
import com.example.navigram.ui.SignUpResponse
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

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
        val loginasGuest = binding.loginasGuest
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
                loading.visibility = View.GONE

            }
            loginasGuest?.setOnClickListener {
                loading.visibility = View.VISIBLE

                CoroutineScope(Dispatchers.Main).launch {
                    val result = registerToNetworkAsGuest(this@LoginActivity)
                    try {
                        if (result.startsWith("{")) {
                            val post = Gson().fromJson(result, SignUpResponse::class.java)
                            val intent = Intent(this@LoginActivity, CameraCapture::class.java)
                            saveToken(this@LoginActivity,post.token,post.username)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@LoginActivity, "Failed to Register", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: JsonSyntaxException) {
                        Toast.makeText(this@LoginActivity, "Failed to parse API response", Toast.LENGTH_LONG)
                            .show()
                    }
                    loading.visibility = View.GONE

                }
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
//                Toast.makeText(this@LoginActivity, "${isConnectedToWiFi(this@LoginActivity)}", Toast.LENGTH_LONG).show()
//                 Launch coroutine for network call
                CoroutineScope(Dispatchers.Main).launch {
                    val result = loginToNetwork(this@LoginActivity,username.text.toString(), password.text.toString())
                    if (result.startsWith("{")) {
                        try {
                                val post = Gson().fromJson(result, LoginResponse::class.java)
                                //token storage
                                saveToken(context,post.token,post.username)
                                Toast.makeText(this@LoginActivity, "Hello ${post.username}, Welcome to Navigram!", Toast.LENGTH_LONG).show()
                                val intent = Intent(this@LoginActivity, CameraCapture::class.java)
                                startActivity(intent)
                                finish()

                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                            Toast.makeText(this@LoginActivity, "Failed to parse API response", Toast.LENGTH_LONG).show()
                            _text.postValue("Failed to parse API response")
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, result, Toast.LENGTH_LONG).show()
                    }
                loading.visibility = View.GONE
                }
            }
        }
    }


    fun isConnectedToWiFi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }


    // Moved function outside onCreate()
    private suspend fun loginToNetwork(context: Context, username: String, password: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = context.getString(R.string.BaseURL) // Ensure it's accessed safely
                val url = URL("${baseUrl}api/auth/login")

                (url.openConnection() as HttpURLConnection).run {
                    requestMethod = "POST"
                    connectTimeout = 5000 // Increase timeout to avoid premature failures
                    readTimeout = 5000
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true

                    val jsonPayload = JSONObject().apply {
                        put("username", username)
                        put("password", password)
                    }

                    try {
                        outputStream.use { os ->
                            OutputStreamWriter(os).use { writer ->
                                writer.write(jsonPayload.toString())
                                writer.flush()
                            }
                        }

                        val response = inputStream.bufferedReader().use { it.readText() }
                        println("HTTP Response Code: $responseCode")
                        println("API Response: $response")

                        return@run if (responseCode == HttpURLConnection.HTTP_OK) {
                            response
                        } else if (responseCode == 429) {
                            val jsonResponse = JSONObject(response)
                            val errorMessage = jsonResponse.optString("message", "Unknown error")
                            "Error: $errorMessage"
                        } else {
                            "Error: $responseCode - $response"
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        return@run "Network error: ${e.localizedMessage}"
                    } finally {
                        disconnect()
                    }
                }
            } catch (e: MalformedURLException) {
                e.printStackTrace()
                return@withContext "Invalid URL"
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
                return@withContext "Network timeout. Please try again."
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "Unexpected error: ${e.localizedMessage}"
            }
        }
    }


    private suspend fun loginToNetworkAsGuest(username: String, password: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL("${getString(R.string.BaseURL)}/api/guest/auth/login")
            (url.openConnection() as HttpURLConnection).run {
                requestMethod = "POST"
                connectTimeout = 100000
                readTimeout = 100000
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
    private suspend fun registerToNetworkAsGuest(context: Context): String {
        return withContext(Dispatchers.IO) {
            val baseUrl = context.getString(R.string.BaseURL) // Retrieve base URL before the coroutine
            val url = URL("${baseUrl}api/guest/auth/register")

            (url.openConnection() as HttpURLConnection).run {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json")
                doOutput = true // Enable output for POST request, even if no body is sent

                try {
                    val response = inputStream.bufferedReader().use { it.readText() }
                    println("HTTP Response Code: $responseCode") // Debugging
                    println("API Response: $response") // Debugging

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        println("NetworkGuest created: $response") // Use Log.d for debugging in Android
                        response
                    } else {
                        "Error: $responseCode - $response"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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


fun saveToken(context: Context, token: String,username: String) {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    val sharedPreferencesUser = EncryptedSharedPreferences.create(
        context,
        "secure_prefs_username",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    val sharedPreferencesPassword = EncryptedSharedPreferences.create(
        context,
        "secure_prefs_username",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    with(sharedPreferences.edit()) {
        putString("auth_token", token)
        apply()
    }
    with(sharedPreferencesUser.edit()) {
        putString("auth_username", username)
        apply()
    }
//    with(sharedPreferencesPassword.edit()) {
//        putString("auth_password", password)
//        apply()
//    }
}

fun getToken(context: Context): String? {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    return sharedPreferences.getString("auth_token", null)
}

fun clearToken(context: Context) {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    with(sharedPreferences.edit()) {
        remove("auth_token")
        apply()
    }

}
