package com.example.navigram.ui.dashboard

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.navigram.R
import com.example.navigram.data.api.CreateMemoryResponse
import java.text.SimpleDateFormat
import java.util.Locale

class FeedAdapter(
    private var posts: List<CreateMemoryResponse>,
    private val onLikeClick: (CreateMemoryResponse) -> Unit,
    private val onCommentClick: (CreateMemoryResponse) -> Unit,
    private val onShareClick: (CreateMemoryResponse) -> Unit,
    private val onPostClick: (CreateMemoryResponse) -> Unit,
    private val onProfileClick: (String) -> Unit
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    class FeedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userProfileImage: ImageView = view.findViewById(R.id.userProfileImage)
        val username: TextView = view.findViewById(R.id.username)
        val postImage: ImageView = view.findViewById(R.id.postImage)
        val likeButton: ImageButton = view.findViewById(R.id.likeButton)
        val commentButton: ImageButton = view.findViewById(R.id.commentButton)
        val shareButton: ImageButton = view.findViewById(R.id.shareButton)
        val likeCount: TextView = view.findViewById(R.id.likeCount)
        val postDescription: TextView = view.findViewById(R.id.postDescription)
        val viewAllComments: TextView = view.findViewById(R.id.viewAllComments)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_post, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val memory = posts[position]

        // Load profile image placeholder (since profile pics not included in API response)
        Glide.with(holder.itemView.context)
            .load(R.drawable.profile_placeholder)
            .circleCrop()
            .into(holder.userProfileImage)

        // Load memory media
        Glide.with(holder.itemView.context)
            .load(memory.mediaUrl)
            .into(holder.postImage)

        // Set username. If user has a name, show "name (@username)", otherwise just "@username"
        val displayName = if (memory.name != null && memory.name.isNotEmpty()) {
            "${memory.name} (@${memory.username})"
        } else {
            "@${memory.username}"
        }
        holder.username.text = displayName

        // Set likes count
        holder.likeCount.text = "${memory.upvoteCount} likes"

        // Set description
        val displayText = if (memory.title != null && memory.title.isNotEmpty()) {
            "${memory.title}\n${memory.description ?: ""}"
        } else {
            memory.description ?: ""
        }
        holder.postDescription.text = displayText

        // Set comments count
        val commentCount = memory.comments?.size ?: 0
        holder.viewAllComments.text = "View all $commentCount comments"

        // Parse and display timestamp
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        try {
            val date = parser.parse(memory.createdAt)
            holder.timestamp.text = DateUtils.getRelativeTimeSpanString(
                date?.time ?: 0,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
        } catch (e: Exception) {
            holder.timestamp.text = memory.createdAt
        }

        // Set like button state (always unfilled for now)
        holder.likeButton.setImageResource(R.drawable.ic_like)

        // Click listeners
        holder.likeButton.setOnClickListener { onLikeClick(memory) }
        holder.commentButton.setOnClickListener { onCommentClick(memory) }
        holder.shareButton.setOnClickListener { onShareClick(memory) }
        holder.postImage.setOnClickListener { onPostClick(memory) }
        holder.userProfileImage.setOnClickListener { onProfileClick(memory.userId) }
        holder.username.setOnClickListener { onProfileClick(memory.userId) }
    }

    override fun getItemCount() = posts.size

    fun updatePosts(newPosts: List<CreateMemoryResponse>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
