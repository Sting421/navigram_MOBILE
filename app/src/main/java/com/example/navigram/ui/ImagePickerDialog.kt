package com.example.navigram.ui

import android.Manifest
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.navigram.R
import com.example.navigram.data.CloudinaryUploader
import com.example.navigram.ui.Gallery.GalleryViewModel
import com.example.navigram.ui.Gallery.ImageItem
import com.example.navigram.ui.Gallery.GalleryViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class ImagePickerDialog : DialogFragment() {
    private val TAG = "ImagePickerDialog"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog)
    }

    override fun onStart() {
        super.onStart()
        try {
            dialog?.let { dialog ->
                dialog.window?.let { window ->
                    window.attributes?.apply {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    window.setDimAmount(0.5f)
                }
                dialog.setCanceledOnTouchOutside(false)
                dialog.setCancelable(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var noImagesText: TextView
    private lateinit var adapter: ImageAdapter
    private lateinit var viewModel: GalleryViewModel
    private var onImageSelected: ((String) -> Unit)? = null

    fun setOnImageSelectedListener(listener: (String) -> Unit) {
        onImageSelected = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_image_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        noImagesText = view.findViewById(R.id.noImagesText)
        
        view.findViewById<View>(R.id.closeButton).setOnClickListener {
            dismiss()
        }
        
        try {
            viewModel = ViewModelProvider(this, GalleryViewModelFactory(requireActivity().application))[GalleryViewModel::class.java]
            setupRecyclerView()
            setupSwipeRefresh()
            observeViewModel()
            checkPermissionAndLoadImages()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error initializing image picker: ${e.message}", Toast.LENGTH_LONG).show()
            dismiss()
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshImages()
        }
        swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.teal_200
        )
    }

    private fun setupRecyclerView() {
        // Set up adapter
        adapter = ImageAdapter { imageItem ->
            val imageUri = Uri.parse(imageItem.uri)
            progressBar.visibility = View.VISIBLE
            
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val cloudinaryUrl = withContext(Dispatchers.IO) {
                        CloudinaryUploader.uploadImage(requireContext(), imageUri)
                    }
                    onImageSelected?.invoke(cloudinaryUrl)
                    dismiss()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_LONG).show()
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
        // Set up RecyclerView
        val layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.galleryData.observe(viewLifecycleOwner) { images ->
            val imageList = images.filter { it.type == ImageItem.MediaType.IMAGE }
            adapter.submitList(imageList)
            
            if (imageList.isEmpty()) {
                noImagesText.visibility = View.VISIBLE
            } else {
                noImagesText.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading && adapter.itemCount == 0) View.VISIBLE else View.GONE
            swipeRefreshLayout.isRefreshing = isLoading && adapter.itemCount > 0
        }
    }

    private fun checkPermissionAndLoadImages() {
        Log.d(TAG, "Checking permissions")
        val context = requireContext()
        val hasPermission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        Log.d(TAG, "Has permission: $hasPermission")

        if (hasPermission) {
            viewModel.loadImages()
        } else {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            requestPermissions(arrayOf(permission), REQUEST_READ_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(TAG, "Permission result: ${grantResults.firstOrNull()}")
        if (requestCode == REQUEST_READ_STORAGE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.loadImages()
            } else {
                Toast.makeText(context, "Permission required to access images", Toast.LENGTH_LONG).show()
                dismiss()
            }
        }
    }

    private inner class ImageAdapter(
        private val onItemClick: (ImageItem) -> Unit
    ) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
        private var items = listOf<ImageItem>()

        fun submitList(newItems: List<ImageItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val item = items[position]
            Glide.with(holder.itemView)
                .load(Uri.parse(item.uri))
                .apply(
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()
                        .placeholder(R.drawable.navigramlogo)
                        .error(R.drawable.navigramlogo)
                )
                .into(holder.imageView)

            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size

        inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.image_view)
        }
    }

    companion object {
        private const val REQUEST_READ_STORAGE = 101
    }
}
