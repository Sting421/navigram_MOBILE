package com.example.navigram.ui.Profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.navigram.R

class MemoryAdapter(
    private var memories: List<com.example.navigram.data.api.CreateMemoryResponse>,
    private val onMemoryClick: (com.example.navigram.data.api.CreateMemoryResponse) -> Unit
) : RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder>() {

    class MemoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.gallery_item_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gallery_item, parent, false)
        return MemoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val memory = memories[position]

        // Load image using Glide
        Glide.with(holder.imageView.context)
            .load(memory.mediaUrl)
            .centerCrop()
            .placeholder(R.drawable.navigramlogo)
            .error(R.drawable.navigramlogo)
            .into(holder.imageView)

        // Set click listener
        holder.itemView.setOnClickListener {
            onMemoryClick(memory)
        }
    }

    override fun getItemCount() = memories.size

    fun updateMemories(newMemories: List<com.example.navigram.data.api.CreateMemoryResponse>) {
        memories = newMemories
        notifyDataSetChanged()
    }
}
