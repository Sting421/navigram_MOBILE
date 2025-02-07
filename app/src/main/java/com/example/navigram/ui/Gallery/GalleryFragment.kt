package com.example.navigram.ui.Gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.navigram.R

class GalleryFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: GalleryAdapter
    private val REQUEST_READ_STORAGE = 101
    val viewModel: GalleryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_gallery, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView with GridLayoutManager
        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.layoutManager = gridLayoutManager
        
        adapter = GalleryAdapter()
        recyclerView.adapter = adapter

        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val visibleItemCount = gridLayoutManager.childCount
                val totalItemCount = gridLayoutManager.itemCount
                val firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition()

                // Load more when scrolled near the end
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount 
                    && firstVisibleItemPosition >= 0) {
                    viewModel.loadImages(loadMore = true)
                }
            }
        })

        // Observe ViewModel data
        viewModel.galleryData.observe(viewLifecycleOwner) { imageItems ->
            adapter.submitList(imageItems)
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Initial image load
        viewModel.loadImages()

        checkPermissions()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10 and above, storage access is different
            return
        }
        
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_READ_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_READ_STORAGE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Reload images if permission granted
            viewModel.loadImages()
        }
    }

    inner class GalleryAdapter : RecyclerView.Adapter<GalleryViewHolder>() {
        private val imageItems = mutableListOf<ImageItem>()

        fun submitList(newItems: List<ImageItem>) {
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = imageItems.size
                override fun getNewListSize() = newItems.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = 
                    imageItems[oldItemPosition].path == newItems[newItemPosition].path
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = 
                    imageItems[oldItemPosition] == newItems[newItemPosition]
            })

            imageItems.clear()
            imageItems.addAll(newItems)
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery, parent, false)
            return GalleryViewHolder(view)
        }

        override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
            val imageItem = imageItems[position]
            
            // Optimized Glide loading with caching and placeholder
            Glide.with(holder.itemView.context)
                .load(imageItem.file)
                .apply(
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()
                        .placeholder(R.drawable.navigramlogo)
                        .error(R.drawable.navigramlogo)
                )
                .into(holder.imageView)

            holder.itemView.setOnClickListener {
                // TODO: Implement image preview or details view
            }
        }

        override fun getItemCount(): Int = imageItems.size
    }

    class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
    }
}
