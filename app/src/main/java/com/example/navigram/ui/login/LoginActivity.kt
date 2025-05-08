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
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import android.util.Base64
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
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.navigram.ui.CameraCapture
import com.example.navigram.data.api.SignUpResponse
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.navigram.ui.Dashboard
import io.github.cdimascio.dotenv.dotenv
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.api.Auth0TokenRequest
import com.example.navigram.data.api.LoginRequest

import com.example.navigram.data.api.LoginResponse
import org.json.JSONObject

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

        // Configure Google Sign-In with simplified options
        val gso = try {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestId()
                .requestIdToken(getString(R.string.google_client_id))
                .setHostedDomain("*") // Allow any domain
                .build()
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Failed to build GSO", e)
            Toast.makeText(this, "Failed to initialize Google Sign-In configuration", Toast.LENGTH_LONG).show()
            return
        }

        try {
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            
            // Clear any previous sign-in state
            googleSignInClient.signOut().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("GoogleSignIn", "Previous sign-in state cleared")
                    // Silently attempt to sign in with cached credentials
                    googleSignInClient.silentSignIn()
                        .addOnSuccessListener { account ->
                            Log.d("GoogleSignIn", "Silent sign-in successful: ${account.email}")
                        }
                        .addOnFailureListener { e ->
                            Log.d("GoogleSignIn", "Silent sign-in failed, user interaction required", e)
                        }
                } else {
                    Log.e("GoogleSignIn", "Failed to clear previous sign-in state", task.exception)
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Failed to initialize Google Sign-In", e)
            Toast.makeText(this, "Failed to initialize Google Sign-In", Toast.LENGTH_LONG).show()
        }

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


    private suspend fun loginToNetwork(context: Context, username: String, password: String): String {
        return withContext(Dispatchers.IO) {
            val maxRetries = 3
            var currentAttempt = 0
            var delayMs = 1000L // Initial delay of 1 second

            while (currentAttempt < maxRetries) {
                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl(context.getString(R.string.BaseURL))
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val apiService = retrofit.create(ApiService::class.java)
                    val loginRequest = LoginRequest(username, password)

                    val response = apiService.login(loginRequest)

                    return@withContext when {
                        response.isSuccessful -> {
                            val loginResponse = response.body()
                            if (loginResponse != null) {
                                // Convert the response to JSON string to maintain compatibility
                                Gson().toJson(loginResponse)
                            } else {
                                "Error: Empty response from server"
                            }
                        }
                        response.code() == 429 -> {
                            if (currentAttempt == maxRetries - 1) {
                                "Error: Too many requests. Please try again later."
                            } else {
                                // Retry for 429 errors
                                delay(delayMs)
                                currentAttempt++
                                delayMs *= 2 // Exponential backoff
                                continue
                            }
                        }
                        response.code() == 500 -> {
                            "Invalid Credentials, User not Found!"
                        }
                        else -> {
                            val errorBody = response.errorBody()?.string() ?: "Unknown error"
                            "Error: ${response.code()} - $errorBody"
                        }
                    }
                } catch (e: Exception) {
                    if (currentAttempt == maxRetries - 1) {
                        return@withContext when (e) {
                            is IOException -> "Network error: Please check your internet connection"
                            else -> "Unexpected error: ${e.localizedMessage}"
                        }
                    } else {
                        delay(delayMs)
                        currentAttempt++
                        delayMs *= 2 // Exponential backoff
                        continue
                    }
                }
            }
            "Error: Max retry attempts reached"
        }
    }


    private suspend fun loginToNetworkAsGuest(username: String, password: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(getString(R.string.BaseURL))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(ApiService::class.java)
                val loginRequest = LoginRequest(username, password)
                val response = apiService.loginAsGuest(loginRequest)
                
                when {
                    response.isSuccessful -> {
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            Gson().toJson(loginResponse)
                        } else {
                            "Error: Empty response from server"
                        }
                    }
                    response.code() == 429 -> {
                        "Error: Too many requests. Please try again later."
                    }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            var account: GoogleSignInAccount? = null
            try {
                account = task.getResult(ApiException::class.java)
                // Log account details for debugging
                Log.d("GoogleSignIn", "Email: ${account?.email}")
                Log.d("GoogleSignIn", "Display Name: ${account?.displayName}")
                Log.d("GoogleSignIn", "ID: ${account?.id}")
                Log.d("GoogleSignIn", "ID Token: ${account?.idToken}")
                // Successfully signed in
                CoroutineScope(Dispatchers.Main).launch {
                    // Create Retrofit instance for the API call
                    val retrofit = Retrofit.Builder()
                        .baseUrl(getString(R.string.BaseURL))
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val apiService = retrofit.create(ApiService::class.java)

                    try {
                        // Prepare the request data
                        val request = Auth0TokenRequest(
                            email = account.email ?: "",
                            name = account.displayName ?: "",
                            googleId = account.id ?: "",
                            idToken = account.idToken ?: "",
                            serverAuthCode = null,
                            grantType = "authorization_code"
                        )

                        // Log request for debugging
                        Log.d("GoogleSignIn", "Sending auth request for email: ${request.email}")

                        // Make the API call
                        val response = apiService.exchangeAuth0Token(request)
                        Log.d("GoogleSignIn", "Return response: $response")
                        when {
                            response.isSuccessful -> {
                                val authResponse = response.body()
                                if (authResponse != null && authResponse.token.isNotEmpty()) {
                                    Log.d("GoogleSignIn", "Login successful with username: ${authResponse.username}")
                                    saveToken(this@LoginActivity, authResponse.token, authResponse.username)
                                    
                                    // Navigate to Dashboard
                                    val intent = Intent(this@LoginActivity, Dashboard::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Log.e("GoogleSignIn", "Empty or invalid response from server")
                                    runOnUiThread {
                                        Toast.makeText(this@LoginActivity, "Server returned an invalid response", Toast.LENGTH_LONG).show()
                                        binding.loading.visibility = View.GONE
                                    }
                                    // Clear sign in state and retry
                                    googleSignInClient.signOut()
                                }
                            }
                            response.code() == 401 -> {
                                Log.e("GoogleSignIn", "Authorization failed. Code: 401")
                                throw Exception("Authorization failed. Please try again.")
                            }
                            response.code() == 400 -> {
                                val errorBody = response.errorBody()?.string()
                                Log.e("GoogleSignIn", "Bad request. Code: 400, Error: $errorBody")
                                throw Exception("Invalid request. Please try again.")
                            }
                            else -> {
                                val errorBody = response.errorBody()?.string()
                                Log.e("GoogleSignIn", "Error response: $errorBody")
                                throw Exception(errorBody ?: "Authentication failed")
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

            } catch (e: ApiException) {
                // Log detailed error information
                val errorMessage = when(e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign in cancelled"
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign in failed"
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign in already in progress"
                    GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Invalid account"
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Sign in required"
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error"
                    10 -> {
                        Log.e("GoogleSignIn", "Developer error: Client ID mismatch or invalid configuration", e)
                        // Get the client ID from resources to verify in logs
                        val configuredClientId = getString(R.string.google_client_id)
                        Log.d("GoogleSignIn", "Configured client ID: $configuredClientId")
                        try {
                            // Try to extract and decode returned client ID from token
                            account?.idToken?.split(".")?.get(1)?.let { payload ->
                                // Convert Base64URL to Base64 and add padding if necessary
                                val base64 = payload.replace("-", "+").replace("_", "/")
                                val paddedPayload = when (base64.length % 4) {
                                    0 -> base64
                                    2 -> "$base64=="
                                    3 -> "$base64="
                                    else -> base64
                                }
                                val decodedBytes = Base64.decode(paddedPayload, Base64.NO_WRAP)
                                val decodedPayload = String(decodedBytes)
                                Log.d("GoogleSignIn", "Decoded token payload: $decodedPayload")
                                val jsonPayload = JSONObject(decodedPayload)
                                val aud = jsonPayload.optString("aud")
                                Log.d("GoogleSignIn", "Token audience (client ID): $aud")
                            }
                        } catch (e: Exception) {
                            Log.e("GoogleSignIn", "Could not extract returned client ID", e)
                        }
                        "Google Sign-In configuration error. Please verify app credentials in Google Cloud Console."
                    }
                    else -> {
                        Log.e("GoogleSignIn", "Unknown error with code: ${e.statusCode}", e)
                        "Unknown error: ${e.statusCode}"
                    }
                }

                // Clear sign-in state and retry with reinitialized client
                if (e.statusCode == 10) {
                    googleSignInClient.signOut().addOnCompleteListener {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestIdToken(getString(R.string.google_client_id))
                            .requestServerAuthCode(getString(R.string.google_client_id))
                            .build()
                        googleSignInClient = GoogleSignIn.getClient(this, gso)
                    }
                }
                Log.e("GoogleSignIn", "Sign in failed: $errorMessage", e )
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                binding.loading.visibility = View.GONE
            }
        }
    }

    private fun signInWithGoogle() {
        try {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
            Log.d("GoogleSignIn", "Sign-in intent started")
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Failed to start sign-in intent", e)
            Toast.makeText(this, "Failed to start Google Sign-In", Toast.LENGTH_LONG).show()
            binding.loading.visibility = View.GONE
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
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(context.getString(R.string.BaseURL))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(ApiService::class.java)
                val response = apiService.registerGuest()
                
                when {
                    response.isSuccessful -> {
                        val guestResponse = response.body()
                        if (guestResponse != null) {
                            Gson().toJson(guestResponse)
                        } else {
                            "Error: Empty response from server"
                        }
                    }
                    response.code() == 500 -> {
                        "User already exists"
                    }
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
