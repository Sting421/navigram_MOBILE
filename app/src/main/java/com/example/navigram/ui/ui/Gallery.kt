package com.example.navigram.ui.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.navigram.R
import java.io.File

class GalleryFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryAdapter
    private val imageFiles = mutableListOf<File>()
    private val REQUEST_READ_STORAGE = 101


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_gallery, container, false)
        recyclerView = view.findViewById(R.id.recycler_view) // Ensure this ID matches the XML
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = GalleryAdapter()
        recyclerView.adapter = adapter
        checkPermissions()
    }

    private fun loadImages() {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Navigram"
        )
        imageFiles.clear()
        directory.listFiles()?.filter { it.name.endsWith(".jpg") }?.forEach {
            imageFiles.add(it)
        }
        adapter.notifyDataSetChanged()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            loadImages()
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_READ_STORAGE
                )
            } else {
                loadImages()
            }
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
            loadImages()
        }
    }

    inner class GalleryAdapter : RecyclerView.Adapter<GalleryViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery, parent, false)
            return GalleryViewHolder(view)
        }

        override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
            val file = imageFiles[position]
            Glide.with(holder.itemView)
                .load(file)
                .into(holder.imageView)

            holder.itemView.setOnClickListener {
                // Handle image click (preview with location info)
            }
        }

        override fun getItemCount(): Int = imageFiles.size
    }

    class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
    }
}
