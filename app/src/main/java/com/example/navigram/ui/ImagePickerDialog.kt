package com.example.navigram.ui

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.navigram.R
import com.example.navigram.ui.Gallery.GalleryViewModel
import com.example.navigram.ui.Gallery.ImageItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ImagePickerDialog : DialogFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ImageAdapter
    private lateinit var viewModel: GalleryViewModel
    private var onImageSelected: ((Uri) -> Unit)? = null

    fun setOnImageSelectedListener(listener: (Uri) -> Unit) {
        onImageSelected = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_image_picker)
            .create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return dialog?.window?.decorView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        
        viewModel = ViewModelProvider(this)[GalleryViewModel::class.java]
        setupRecyclerView()
        observeViewModel()
        checkPermissionAndLoadImages()
    }

    private fun setupRecyclerView() {
        adapter = ImageAdapter { imageItem ->
            onImageSelected?.invoke(Uri.fromFile(imageItem.file))
            dismiss()
        }
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.galleryData.observe(viewLifecycleOwner) { images ->
            adapter.submitList(images.filter { it.type == ImageItem.MediaType.IMAGE })
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun checkPermissionAndLoadImages() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadImages()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_READ_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_READ_STORAGE && grantResults.isNotEmpty() && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadImages()
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
                .load(item.file)
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
