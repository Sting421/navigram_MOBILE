package com.example.navigram.ui.dashboard

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.navigram.R
import com.example.navigram.data.model.FeedPost

class FeedAdapter(
    private val posts: List<FeedPost>,
    private val onLikeClick: (FeedPost) -> Unit,
    private val onCommentClick: (FeedPost) -> Unit,
    private val onShareClick: (FeedPost) -> Unit,
    private val onPostClick: (FeedPost) -> Unit,
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
        val post = posts[position]

        // TODO: Load user profile image using Glide
        // Glide.with(holder.itemView.context)
        //     .load(post.userProfileImage)
        //     .circleCrop()
        //     .into(holder.userProfileImage)

        // TODO: Load post image using Glide
        // Glide.with(holder.itemView.context)
        //     .load(post.imageUrl)
        //     .into(holder.postImage)

        holder.username.text = post.username
        holder.likeCount.text = "${post.likeCount} likes"
        holder.postDescription.text = post.description
        holder.viewAllComments.text = "View all ${post.commentCount} comments"
        holder.timestamp.text = DateUtils.getRelativeTimeSpanString(
            post.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )

        holder.likeButton.setImageResource(
            if (post.isLiked) R.drawable.ic_like_filled else R.drawable.ic_like
        )

        // Click listeners
        holder.likeButton.setOnClickListener { onLikeClick(post) }
        holder.commentButton.setOnClickListener { onCommentClick(post) }
        holder.shareButton.setOnClickListener { onShareClick(post) }
        holder.postImage.setOnClickListener { onPostClick(post) }
        holder.userProfileImage.setOnClickListener { onProfileClick(post.userId) }
        holder.username.setOnClickListener { onProfileClick(post.userId) }
    }

    override fun getItemCount() = posts.size
}
