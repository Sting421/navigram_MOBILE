package com.example.navigram.ui.memory

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.navigram.R
import com.example.navigram.data.api.CommentResponse
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {
    private var comments: List<CommentResponse> = listOf()

    fun updateComments(newComments: List<CommentResponse>) {
        comments = newComments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImageView: ShapeableImageView = itemView.findViewById(R.id.imageViewProfile)
        private val usernameTextView: TextView = itemView.findViewById(R.id.textViewUsername)
        private val commentTextView: TextView = itemView.findViewById(R.id.textViewComment)
        private val timestampTextView: TextView = itemView.findViewById(R.id.textViewTimestamp)

        fun bind(comment: CommentResponse) {
            usernameTextView.text = comment.username
            commentTextView.text = comment.content
            timestampTextView.text = formatTimestamp(comment.createdAt)
            
            // Load profile picture
            if (comment.profilePicture != null) {
                Glide.with(itemView.context)
                    .load(comment.profilePicture)
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.navigramlogo)
            }

            Log.d("CommentAdapter", "Binding comment: ${formatTimestamp(comment.createdAt)}")
        }

        private fun formatTimestamp(timestamp: String): String {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(timestamp)
                val now = Date()
                val diff = now.time - (date?.time ?: now.time)
                val seconds = diff / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                val days = hours / 24
                val weeks = days / 7
                val months = days / 30
                val years = days / 365

                return when {
                    years > 0 -> if (years == 1L) "1 year ago" else "$years years ago"
                    months > 0 -> if (months == 1L) "1 month ago" else "$months months ago"
                    weeks > 0 -> if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
                    days > 0 -> if (days == 1L) "1 day ago" else "$days days ago"
                    hours > 0 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
                    minutes > 0 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
                    seconds > 30 -> "$seconds seconds ago"
                    else -> "Just now"
                }
            } catch (e: Exception) {
                try {
                    // Try parsing with a simpler format
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val date = sdf.parse(timestamp)
                    val now = Date()
                    val diff = now.time - (date?.time ?: now.time)
                    val seconds = diff / 1000
                    val minutes = seconds / 60
                    val hours = minutes / 60
                    val days = hours / 24

                    return when {
                        days > 0 -> if (days == 1L) "1 day ago" else "$days days ago"
                        hours > 0 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
                        minutes > 0 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
                        else -> "Just now"
                    }
                } catch (e2: Exception) {
                    Log.e("CommentAdapter", "Error parsing date: ${e2.message}")
                    return "Invalid date"
                }
            }
        }
    }
}
