package com.example.navigram

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.navigram.ui.CameraCapture
import com.example.navigram.ui.Dashboard
import com.example.navigram.ui.login.LoginActivity
import com.example.navigram.ui.login.getToken
import com.example.navigram.ui.home.HomeFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        lifecycleScope.launch {
            delay(2000) // Wait for 2 seconds instead of using Thread.sleep()

            val isValid = validateToken(this@MainActivity)
            val intent = if (isValid) {
                Intent(this@MainActivity, Dashboard::class.java)
            } else {
                Intent(this@MainActivity, LoginActivity::class.java)
            }
//            val intent = Intent(this@MainActivity, Dashboard::class.java)
            startActivity(intent)
            finish() // Close MainActivity after launching the new activity
        }
    }

    private suspend fun validateToken(context: Context): Boolean {
        val baseUrl = context.getString(R.string.BaseURL)
        val url = URL("$baseUrl/api/auth/me")
        val token = getToken(context) ?: return false

        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (url.openConnection() as HttpURLConnection).apply {
                    Log.e("Running", "test")
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                val responseCode = connection.responseCode
                responseCode == 200
            } catch (e: Exception) {
                Log.e("BADvalidateToken", "Error", e)
                false
            } finally {
                connection?.disconnect()
            }
        }
    }
}

