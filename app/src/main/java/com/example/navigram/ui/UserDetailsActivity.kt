package com.example.navigram.ui

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.navigram.NavigramApplication
import com.example.navigram.R
import com.example.navigram.data.api.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class UserDetailsActivity : AppCompatActivity() {
    private lateinit var apiService: ApiService
    
    // UI Elements
    private lateinit var userNameTextView: TextView
    private lateinit var userUsernameTextView: TextView
    private lateinit var userBioTextView: TextView
    private lateinit var followButton: Button
    private lateinit var profileImageView: ImageView
    private lateinit var postCountTextView: TextView
    private lateinit var followersCountTextView: TextView
    private lateinit var followingCountTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_details)

        apiService = (application as NavigramApplication).apiService
        initializeViews()

        val userId = intent.getStringExtra("user_id")
        userId?.let { id ->
            loadUserDetails(id)
            followButton.setOnClickListener {
                followUser(this, id)
            }
        }
    }

    private fun initializeViews() {
        userNameTextView = findViewById(R.id.user_name)
        userUsernameTextView = findViewById(R.id.user_username)
        userBioTextView = findViewById(R.id.profile_bio)
        followButton = findViewById(R.id.follow_user_button)
        profileImageView = findViewById(R.id.profile_image)
        postCountTextView = findViewById(R.id.profile_post_count)
        followersCountTextView = findViewById(R.id.profile_followers_count)
        followingCountTextView = findViewById(R.id.profile_following_count)
    }

    private fun loadUserDetails(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getPublicUserProfile(userId)
                
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let { userResponse ->
                        runOnUiThread {
                            // Update UI with user details
                            userNameTextView.text = userResponse.name ?: userResponse.username
                            userUsernameTextView.text = "@${userResponse.username}"

                            // Load profile image if available
                            userResponse.profilePicture?.let { profilePicUrl ->
                                Glide.with(this@UserDetailsActivity)
                                    .load(profilePicUrl)
                                    .placeholder(R.drawable.profile_placeholder)
                                    .into(profileImageView)
                            }


                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@UserDetailsActivity,
                            "Failed to load user details",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@UserDetailsActivity,
                        "Error loading user details",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun followUser(context: Context, userId: String) {
        followButton.isEnabled = false // Disable button while request is in progress

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.followUser(userId)
                
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "User followed successfully", Toast.LENGTH_SHORT).show()
                        followButton.text = "Following"
                    } else {
                        Toast.makeText(context, "Failed to follow user", Toast.LENGTH_SHORT).show()
                        followButton.isEnabled = true // Re-enable button on failure
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(context, "Error following user", Toast.LENGTH_SHORT).show()
                    followButton.isEnabled = true // Re-enable button on error
                }
            }
        }
    }
}
