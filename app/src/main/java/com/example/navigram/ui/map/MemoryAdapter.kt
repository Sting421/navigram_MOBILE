package com.example.navigram.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        val image: ImageView = view.findViewById(R.id.memory_image)
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
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        holder.date.text = memory.createdAt

        // Load image using Glide
        if (memory.mediaUrl.isNotEmpty()) {
            Glide.with(holder.image.context)
                .load(memory.mediaUrl)
                .centerCrop()
                .placeholder(R.drawable.navigramlogo)
                .error(R.drawable.navigramlogo)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.navigramlogo)
        }

        holder.itemView.setOnClickListener {
            onItemClick(memory)
        }
    }

    override fun getItemCount() = memories.size
}
