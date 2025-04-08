package com.example.navigram.ui

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.navigram.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class UserDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_details)

        val userNameTextView: TextView = findViewById(R.id.tv_user_name)
        val userEmailTextView: TextView = findViewById(R.id.tv_user_email)
        val followButton: Button = findViewById(R.id.btn_follow)

        val userId = intent.getStringExtra("user_id")
        val userName = intent.getStringExtra("user_name")
        val userEmail = intent.getStringExtra("user_email")

        userNameTextView.text = userName
        userEmailTextView.text = userEmail

        followButton.setOnClickListener {
            userId?.let { id ->
                followUser(this,id)
            }
        }
    }

    private fun followUser(context: Context, userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val baseUrl = context.getString(R.string.BaseURL)
                val url = URL("$baseUrl/api/users/$userId/follow")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    println("User followed successfully")
                } else {
                    println("Failed to follow user: $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
