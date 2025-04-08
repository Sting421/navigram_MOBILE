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
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.api.AuthInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import android.view.View
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.textfield.TextInputEditText
import com.bumptech.glide.Glide
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.views.overlay.Marker

class MemoryCreationActivity : AppCompatActivity() {
    
    private lateinit var viewModel: MemoryCreationViewModel
    private lateinit var mediaTypeSpinner: Spinner
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var uploadButton: Button
    private lateinit var submitButton: Button
    private lateinit var previewImage: ImageView
    private lateinit var mapView: MapView
    private var currentMarker: Marker? = null
    
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
        // Initialize OSMDroid configuration
        Configuration.getInstance().userAgentValue = packageName

        mapView = findViewById(R.id.mapView)
        mapView.setMultiTouchControls(true)
        
        val mapController = mapView.controller
        mapController.setZoom(6.0) // More zoomed out to show larger area
        
        // Set initial position to Philippines (approximate center)
        val startPoint = GeoPoint(13.20, 125.85)
        mapController.setCenter(startPoint)

        // Set up map click listener
        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay(this) {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val projection = mapView.projection
                val point = projection.fromPixels(e.x.toInt(), e.y.toInt())
                val geoPoint = GeoPoint(point.latitude, point.longitude)
                
                // Remove previous marker
                currentMarker?.let { mapView.overlays.remove(it) }
                
                // Add new marker with custom icon
                val marker = Marker(mapView).apply {
                    position = geoPoint
                    // Create smaller marker icon
                    val drawable = resources.getDrawable(R.drawable.navigramlogo, theme)
                    drawable.setBounds(0, 0, 48, 48) // Fixed size in pixels for marker
                    icon = drawable
                    setAnchor(0.5f, 1.0f) // Center horizontally, bottom aligned
                }
                currentMarker = marker
                mapView.overlays.add(marker)
                
                // Update ViewModel
                viewModel.setLocation(geoPoint.latitude, geoPoint.longitude)
                
                // Log the coordinates
                Log.d("MemoryCreation", "Selected location: ${geoPoint.latitude}, ${geoPoint.longitude}")
                
                mapView.invalidate()
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
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
        
        // Initialize OSMDroid configuration before setting content view
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(this@MemoryCreationActivity, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@MemoryCreationActivity))
        }
        
        setContentView(R.layout.activity_memory_creation)
        
        // Initialize Retrofit and ApiService
        val token = getStoredToken()
        if (token == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(token))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.196.8:8080") // Using Android emulator localhost
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val factory = MemoryCreationViewModelFactory(apiService)
        viewModel = ViewModelProvider(this, factory)[MemoryCreationViewModel::class.java]
        
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
            // Ensure description is not empty after trim
            val trimmedDescription = descriptionInput.text.toString().trim()
            if (trimmedDescription.isEmpty()) {
                Toast.makeText(this, "Please add a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadMemory(trimmedDescription)
            }
        }
    }
    private fun showImagePicker() {
        val imagePickerDialog = ImagePickerDialog().apply {
            setOnImageSelectedListener { url ->
                viewModel.setMediaUrl(url)
                Glide.with(this@MemoryCreationActivity)
                    .load(url)
                    .centerCrop()
                    .into(previewImage)
                previewImage.visibility = View.VISIBLE
            }
        }
        imagePickerDialog.show(supportFragmentManager, "imagePicker")
    }

    private fun validateForm(): Boolean {
        val state = viewModel.state.value ?: return false
        
        if (state.selectedMediaUri == null && state.selectedMediaUrl == null) {
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

    private fun getStoredToken(): String? {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return sharedPreferences.getString("auth_token", null)
    }

    private fun uploadMemory(description: String) {
        viewModel.uploadMemory(description)
        Log.d("MemoryCreation", "Uploading memory with description: ${descriptionInput.text}")
    }
}
