package com.example.navigram

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.content.Context
import com.example.navigram.ui.CameraCapture
import com.example.navigram.ui.Dashboard
import com.example.navigram.ui.login.LoginActivity
import com.example.navigram.ui.ui.GalleryFragment
import com.example.navigram.ui.ui.dashboard.DashboardFragment
import kotlinx.coroutines.Dispatchers
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
        Thread {
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
//                if(getToken(this@MainActivity)!= null){
//                    startActivity(Intent(this, LogoutTest::class.java))
//                }
//                else
//                    startActivity(Intent(this, LoginActivity::class.java))
//                finish() // Close MainActivity after launching LoginRegisterAct
//                startActivity(Intent(this, CameraCapture::class.java))
                startActivity(Intent(this, LoginActivity::class.java))
//                val galleryFragment = GalleryFragment()
//
//                supportFragmentManager.beginTransaction()
//                    .replace(R.id.fragment_container, galleryFragment)
//                    .commit()
            }

        }.start()
    }

    private suspend fun validateToken(context: Context): String {
        return withContext(Dispatchers.IO) {
            val baseUrl = context.getString(R.string.BaseURL) // Retrieve base URL before the coroutine
            val url = URL("${baseUrl}/api/auth/me")

            (url.openConnection() as HttpURLConnection).run {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json")
                doOutput = true // Enable output for POST request, even if no body is sent

                try {
                    val response = inputStream.bufferedReader().use { it.readText() }
                    println("HTTP Response Code: $responseCode") // Debugging
                    println("API Response: $response") // Debugging

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        println("Auth is valid: $response") // Use Log.d for debugging in Android
                        response
                    } else {
                        "Error: $responseCode - $response"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if(responseCode == 500)
                        "test"
                    else
                        "Failed to fetch Data"
                } finally {
                    disconnect()
                }
            }
        }
    }
}
