package com.example.navigram.ui.memory

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.navigram.R
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.api.CreateMemoryResponse
import com.example.navigram.data.api.CreateCommentRequest
import com.example.navigram.data.api.FlagMemoryRequest
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class MemoryDetailsDialog(
    private val context: Context,
    private val memory: CreateMemoryResponse,
    private val apiService: ApiService,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    companion object {
        private const val TAG = "MemoryDetailsDialog"
    }

    private lateinit var dialog: Dialog
    private lateinit var commentsAdapter: CommentAdapter
    private val client = okhttp3.OkHttpClient()

    fun show() {
        val dialogView = View.inflate(context, R.layout.dialog_memory_details, null)
        setupViews(dialogView)
        
        dialog = Dialog(context, R.style.CustomDialog)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        
        // Set dialog window attributes
        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.5f)
        }
        
        dialog.show()
    }

    private fun setupViews(view: View) {
        // Set up RecyclerView and adapter for comments
        val commentsRecyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.comments_recycler_view)
        commentsAdapter = CommentAdapter()
        commentsRecyclerView.apply {
            adapter = commentsAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context).apply {
                isAutoMeasureEnabled = false
            }
            setHasFixedSize(true)
            isNestedScrollingEnabled = true
        }

        // Fetch comments when dialog is shown
        fetchComments()

        // Set up comment views
        val commentInput = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.comment_input)
        val sendCommentButton = view.findViewById<ImageButton>(R.id.send_comment_button)

        sendCommentButton.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        val request = CreateCommentRequest(
                            memoryId = memory.id,
                            content = commentText
                        )
                        val response = apiService.createComment(request)
                        
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Comment posted successfully", Toast.LENGTH_SHORT).show()
                            commentInput.text?.clear()
                            // Refresh comments after posting
                            fetchComments()
                        } else {
                            Toast.makeText(context, "Failed to post comment", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Set up memory views
        setupMemoryViews(view)
    }

    private fun setupMemoryViews(view: View) {
        val memoryImage = view.findViewById<ImageView>(R.id.memory_image)
        val memoryDescription = view.findViewById<TextView>(R.id.memory_description)
        val memoryDate = view.findViewById<TextView>(R.id.memory_date)
        val memoryUsername = view.findViewById<TextView>(R.id.memory_username)
        val profileImage = view.findViewById<ImageView>(R.id.profile_image)
        val optionsButton = view.findViewById<ImageButton>(R.id.memory_options_button)
        val shareButton = view.findViewById<MaterialButton>(R.id.share_button)
        val closeButton = view.findViewById<MaterialButton>(R.id.close_button)

        // Set memory data
        memoryDescription.text = memory.description
        memoryUsername.text = memory.username

        // Set up location
        setupLocation(view)

        // Set up date
        setupDate(memoryDate)

        // Load images
        loadImages(memoryImage, profileImage)

        // Set up click listeners
        profileImage.setOnClickListener {
            val intent = android.content.Intent(context, com.example.navigram.ui.UserDetailsActivity::class.java).apply {
                putExtra("username", memory.username)
            }
            context.startActivity(intent)
        }

        optionsButton.setOnClickListener { view ->
            showOptionsMenu(view)
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        shareButton.setOnClickListener {
            // TODO: Implement share functionality
        }
    }

    private fun setupLocation(view: View) {
        val memoryLocation = view.findViewById<TextView>(R.id.memory_location)
        lifecycleScope.launch {
            try {
                val locationText = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://address-from-to-latitude-longitude.p.rapidapi.com/geolocationapi?lat=${memory.latitude}&lng=${memory.longitude}")
                        .get()
                        .addHeader("x-rapidapi-key", "fc33d176bdmsh77abb4787653b11p100a6cjsn63a64fd53e22")
                        .addHeader("x-rapidapi-host", "address-from-to-latitude-longitude.p.rapidapi.com")
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    Log.d(TAG, "API Response: $responseBody")
                    val jsonResponse = JSONObject(responseBody ?: "{}")

                    val results = jsonResponse.optJSONArray("Results")
                    Log.d(TAG, "Results array: ${results?.toString(2)}")
                    val address = if (results != null && results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        firstResult.optString("address", "Location not available")
                    } else {
                        "Location not available"
                    }
                    "$address"
                }
                memoryLocation.text = locationText
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching location: ${e.message}", e)
                memoryLocation.text = "Location not available"
            }
        }
    }

    private fun setupDate(memoryDate: TextView) {
        try {
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            inputDateFormat.isLenient = true
            val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val date = inputDateFormat.parse(memory.createdAt.trim())
            if (date != null) {
                memoryDate.text = outputDateFormat.format(date)
            } else {
                memoryDate.text = memory.createdAt
                Log.e(TAG, "Failed to parse date: null result")
            }
        } catch (e: Exception) {
            memoryDate.text = memory.createdAt // Fallback to raw date string
            Log.e(TAG, "Error parsing date: ${e.message}")
        }
    }

    private fun loadImages(memoryImage: ImageView, profileImage: ImageView) {
        if (memory.mediaUrl.isNotEmpty()) {
            Glide.with(context)
                .load(memory.mediaUrl)
                .centerCrop()
                .placeholder(R.drawable.navigramlogo)
                .error(R.drawable.navigramlogo)
                .into(memoryImage)
        }

        Glide.with(context)
            .load(R.drawable.navigramlogo)
            .centerCrop()
            .placeholder(R.drawable.navigramlogo)
            .error(R.drawable.navigramlogo)
            .into(profileImage)
    }

    private fun showOptionsMenu(view: View) {
        val popup = PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.memory_options_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_flag_memory -> {
                    showFlagConfirmationDialog()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    private fun showFlagConfirmationDialog() {
        AlertDialog.Builder(context)
            .setTitle(R.string.flag_memory_title)
            .setMessage(R.string.flag_memory_message)
            .setPositiveButton(R.string.flag_memory_confirm) { _, _ ->
                flagMemory()
            }
            .setNegativeButton(R.string.flag_memory_cancel, null)
            .show()
    }

    private fun flagMemory() {
        lifecycleScope.launch {
            try {
                val request = FlagMemoryRequest(
                    memoryId = memory.id,
                    reason = "Inappropriate content"
                )
                val response = apiService.flagMemory(request)
                
                if (response.isSuccessful) {
                    Toast.makeText(context, "Memory has been flagged", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Failed to flag memory", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchComments() {
        lifecycleScope.launch {
            try {
                val response = apiService.getMemoryComments(memory.id)
                if (response.isSuccessful) {
                    response.body()?.let { comments ->
                        commentsAdapter.updateComments(comments)
                    }
                } else {
                    Toast.makeText(context, "Failed to load comments", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching comments: ${e.message}", e)
                Toast.makeText(context, "Error loading comments", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
