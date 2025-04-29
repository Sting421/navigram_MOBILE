package com.example.navigram.ui.map

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.imageview.ShapeableImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.navigram.R
import com.example.navigram.data.api.CreateMemoryResponse
import java.text.SimpleDateFormat
import java.util.Locale

class MemoryAdapter(
    private val memories: List<CreateMemoryResponse>,
    private val onItemClick: (CreateMemoryResponse) -> Unit
) : RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder>() {

    class MemoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ShapeableImageView = view.findViewById(R.id.memory_image)
        val description: TextView = view.findViewById(R.id.memory_description)
        val date: TextView = view.findViewById(R.id.memory_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cluster_memory, parent, false)
        return MemoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val memory = memories[position]
        
        holder.description.text = memory.description
        
        // Format the date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val date = inputFormat.parse(memory.createdAt.trim())
            holder.date.text = date?.let { outputFormat.format(it) } ?: memory.createdAt
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: ${e.message}")
            holder.date.text = memory.createdAt
        }

        // Load image using Glide with better error handling
        Glide.with(holder.image.context)
            .load(memory.mediaUrl.takeIf { it.isNotEmpty() } ?: R.drawable.navigramlogo)
            .centerCrop()
            .placeholder(R.drawable.navigramlogo)
            .error(R.drawable.navigramlogo)
            .into(holder.image)

        holder.itemView.setOnClickListener {
            onItemClick(memory)
        }
    }

    companion object {
        private const val TAG = "MemoryAdapter"
    }

    override fun getItemCount() = memories.size
}
