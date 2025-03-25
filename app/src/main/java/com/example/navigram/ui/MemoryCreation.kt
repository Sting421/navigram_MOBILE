package com.example.navigram.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.navigram.R
import android.net.Uri
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.textfield.TextInputEditText
import com.bumptech.glide.Glide
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MemoryCreationActivity : AppCompatActivity() {
    
    private lateinit var viewModel: MemoryCreationViewModel
    private lateinit var mediaTypeSpinner: Spinner
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var uploadButton: Button
    private lateinit var submitButton: Button
    private lateinit var previewImage: ImageView
    private var googleMap: GoogleMap? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            initializeMap()
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
        }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.setMediaUri(it)
            when {
                it.toString().contains("image") -> {
                    Glide.with(this)
                        .load(it)
                        .centerCrop()
                        .into(previewImage)
                }
                it.toString().contains("video") -> {
                    // TODO: Handle video preview
                }
                it.toString().contains("audio") -> {
                    // TODO: Handle audio preview
                }
            }
            previewImage.visibility = android.view.View.VISIBLE
        }
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapContainer) as? SupportMapFragment
        
        mapFragment?.getMapAsync { map ->
            googleMap = map
            
            // Enable location if permission is granted
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
            }
            
            map.setOnMapClickListener { latLng ->
                // Clear previous markers
                map.clear()
                // Add new marker
                map.addMarker(MarkerOptions().position(latLng))
                // Update ViewModel
                viewModel.setLocation(latLng.latitude, latLng.longitude)
            }
        }
    }
    
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializeMap()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    this,
                    "Location permission is required to create memories",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_creation)
        
        viewModel = ViewModelProvider(this)[MemoryCreationViewModel::class.java]
        
        // Initialize views and check location permission
        mediaTypeSpinner = findViewById(R.id.mediaTypeSpinner)
        descriptionInput = findViewById(R.id.descriptionInput)
        uploadButton = findViewById(R.id.uploadButton)
        submitButton = findViewById(R.id.submitButton)
        previewImage = findViewById(R.id.previewImage)
        
        checkLocationPermission()
        
        // Observe ViewModel state
        viewModel.state.observe(this) { state ->
            if (state.error != null) {
                Toast.makeText(this, state.error, Toast.LENGTH_SHORT).show()
            }
            
            if (state.isLoading) {
                submitButton.isEnabled = false
                submitButton.text = "Creating Memory..."
            } else {
                submitButton.isEnabled = true
                submitButton.text = "Create Memory"
            }
            
            if (state.isSuccess) {
                Toast.makeText(this, "Memory created successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        
        // Set up media upload button
        uploadButton.setOnClickListener {
            when (mediaTypeSpinner.selectedItem.toString().lowercase()) {
                "photo" -> showImagePicker()
                "video" -> getContent.launch("video/*")
                "audio" -> getContent.launch("audio/*")
            }
        }
        

        
        // Set up submit button
        submitButton.setOnClickListener {
            if (validateForm()) {
                uploadMemory()
            }
        }
    }
    private fun showImagePicker() {
        val imagePickerDialog = ImagePickerDialog().apply {
            setOnImageSelectedListener { uri ->
                viewModel.setMediaUri(uri)
                Glide.with(this@MemoryCreationActivity)
                    .load(uri)
                    .centerCrop()
                    .into(previewImage)
                previewImage.visibility = View.VISIBLE
            }
        }
        imagePickerDialog.show(supportFragmentManager, "imagePicker")
    }

    private fun validateForm(): Boolean {
        val state = viewModel.state.value ?: return false
        
        if (state.selectedMediaUri == null) {
            Toast.makeText(this, "Please select a media file", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (state.selectedLocation == null) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (descriptionInput.text.toString().isEmpty()) {
            Toast.makeText(this, "Please add a description", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun uploadMemory() {
        viewModel.uploadMemory(descriptionInput.text.toString())
    }
}
