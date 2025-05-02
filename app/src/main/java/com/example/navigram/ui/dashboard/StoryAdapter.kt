package com.example.navigram.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.navigram.R
import com.example.navigram.data.model.Story

class StoryAdapter(
    private var stories: List<Story>,
    private val onStoryClick: (Story) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val storyImage: ImageView = view.findViewById(R.id.storyImage)
        val storyUsername: TextView = view.findViewById(R.id.storyUsername)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        holder.storyUsername.text = story.username

        // Load story image using Glide
        com.bumptech.glide.Glide.with(holder.itemView.context)
            .load(story.imageUrl)
            .placeholder(R.drawable.profile_placeholder)
            .error(R.drawable.navigramlogo)
            .circleCrop()
            .into(holder.storyImage)

        holder.itemView.setOnClickListener {
            onStoryClick(story)
        }
    }

    override fun getItemCount() = stories.size

    fun updateStories(newStories: List<Story>) {
        stories = newStories
        notifyDataSetChanged()
    }
}
