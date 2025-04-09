package com.example.navigram.ui.login

import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
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
import com.example.navigram.ui.Dashboard
import io.github.cdimascio.dotenv.dotenv

data class LoginResponse(
    val token: String,
    val username:String,
    val status: Int
)
// Load environment variables
/*
val dotenv = dotenv()  // This will load the environment variables from the .env file
val baseUrl = dotenv["BASE_URL"]  // Retrieve the BASE_URL environment variable

*/


class LoginActivity : AppCompatActivity() {
    private val _text = MutableLiveData<String>()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestServerAuthCode(getString(R.string.google_client_id), false)
            .requestIdToken(getString(R.string.google_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check for existing Google Sign In account and valid token
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)
        val savedToken = getToken(this)
        
        Log.d("GoogleSignIn", "Checking existing sign in - Account: ${lastSignedInAccount?.email}, Token exists: ${savedToken != null}")
        
        if (lastSignedInAccount != null) {
            if (savedToken != null) {
                // User is already signed in with both Google account and valid token
                Log.d("GoogleSignIn", "Found valid sign in, proceeding to Dashboard")
                startActivity(Intent(this, Dashboard::class.java))
                finish()
                return
            } else {
                // We have Google account but no valid token, sign out from Google
                Log.d("GoogleSignIn", "Found Google account but no valid token, signing out")
                googleSignInClient.signOut().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("GoogleSignIn", "Sign out completed successfully")
                    } else {
                        Log.e("GoogleSignIn", "Sign out failed: ${task.exception}")
                    }
                }
            }
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val signUp = binding.signUp
        val loading = binding.loading
        val loginasGuest = binding.loginasGuest
        val loginWithGoogle = binding.loginWithGoogle
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



        loginWithGoogle?.setOnClickListener {
            loading.visibility = View.VISIBLE
            signInWithGoogle()
        }

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
                            val intent = Intent(this@LoginActivity, Dashboard::class.java)
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
                                val intent = Intent(this@LoginActivity, Dashboard::class.java)
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
                val url = URL("$baseUrl/api/auth/login")

                (url.openConnection() as HttpURLConnection).run {
                    requestMethod = "POST"
                    connectTimeout = 10000 // Increase timeout to avoid premature failures
                    readTimeout = 10000
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                // Log account details for debugging
                Log.d("GoogleSignIn", "Email: ${account.email}")
                Log.d("GoogleSignIn", "Display Name: ${account.displayName}")
                Log.d("GoogleSignIn", "ID: ${account.id}")
                Log.d("GoogleSignIn", "ID Token: ${account.idToken}")
                // Successfully signed in
                CoroutineScope(Dispatchers.Main).launch {
                    val baseUrl = getString(R.string.BaseURL)
                    val url = URL("$baseUrl/api/auth/google")
                    val result = withContext(Dispatchers.IO) {
                        try {
                            (url.openConnection() as HttpURLConnection).run {
                                requestMethod = "POST"
                                setRequestProperty("Content-Type", "application/json")
                                doOutput = true

                                val jsonPayload = JSONObject().apply {
                                    put("email", account.email)
                                    put("name", account.displayName)
                                    put("googleId", account.id)
                                    put("idToken", account.idToken)
                                    account.serverAuthCode?.let { code ->
                                        put("serverAuthCode", code)
                                    }
                                }
                                // Log the payload for debugging
                                Log.d("GoogleSignIn", "Sending payload: ${jsonPayload.toString()}")

                                OutputStreamWriter(outputStream).use { writer ->
                                    writer.write(jsonPayload.toString())
                                    writer.flush()
                                }

                                val response = inputStream.bufferedReader().use { it.readText() }
                                Log.d("GoogleSignIn", "API Response Code: $responseCode")
                                Log.d("GoogleSignIn", "API Response: $response")
                                
                                when (responseCode) {
                                    HttpURLConnection.HTTP_OK -> try {
                                        val loginResponse = Gson().fromJson(response, LoginResponse::class.java)
                                        if (loginResponse.token.isNotEmpty()) {
                                            Log.d("GoogleSignIn", "Login successful, token: ${loginResponse.token}")
                                            saveToken(this@LoginActivity, loginResponse.token, loginResponse.username)
                                            
                                            // Start Dashboard activity
                                            val intent = Intent(this@LoginActivity, Dashboard::class.java)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            throw Exception("Invalid token received")
                                        }
                                    } catch (e: JsonSyntaxException) {
                                        Log.e("GoogleSignIn", "Failed to parse response", e)
                                        throw Exception("Invalid response format from server")
                                    }
                                    
                                    HttpURLConnection.HTTP_UNAUTHORIZED -> {
                                        Log.e("GoogleSignIn", "Authentication failed")
                                        throw Exception("Authentication failed. Please try again.")
                                    }
                                    
                                    else -> {
                                        var errorMessage = "Server error"
                                        try {
                                            // Try to read from error stream first
                                            val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: response
                                            Log.d("GoogleSignIn", "Error response: $errorResponse")
                                            
                                            val errorJson = JSONObject(errorResponse)
                                            errorMessage = errorJson.optString("message", errorMessage)
                                            if (errorMessage == "Server error") {
                                                // If no specific message found, include the full error response
                                                errorMessage = "Server error: $errorResponse"
                                            }
                                        } catch (e: Exception) {
                                            Log.e("GoogleSignIn", "Error parsing error response", e)
                                            errorMessage = "Server error ($responseCode)"
                                        }
                                        throw Exception(errorMessage)
                                    }
                                }
                            }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("GoogleSignIn", "Server error details:", e)
                                var errorMessage = when {
                                    e is IOException -> "Network error. Please check your connection."
                                    e.message?.contains("401") == true -> "Authentication failed. Please try again."
                                    e.message?.contains("400") == true -> "Invalid request. Please try again."
                                    else -> "Sign-in failed: ${e.message}"
                                }
                                runOnUiThread {
                                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                                    binding.loading.visibility = View.GONE
                                }
                                // Sign out from Google to ensure a fresh sign-in next time
                                googleSignInClient.signOut()
                            }
                    }
                }
            } catch (e: ApiException) {
                // Log detailed error information
                val errorMessage = when(e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign in cancelled"
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign in failed"
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign in already in progress"
                    GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Invalid account"
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Sign in required"
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error"
                    else -> "Unknown error: ${e.statusCode}"
                }
                Log.e("GoogleSignIn", "Sign in failed: $errorMessage", e)
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                binding.loading.visibility = View.GONE
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
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
            val url = URL("$baseUrl/api/guest/auth/register")

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
