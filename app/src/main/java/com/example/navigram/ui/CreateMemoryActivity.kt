package com.example.navigram.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.bumptech.glide.Glide
import com.example.navigram.R
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.api.AuthInterceptor
import com.google.android.material.textfield.TextInputEditText
import okhttp3.OkHttpClient
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.view.MotionEvent

class CreateMemoryActivity : AppCompatActivity() {
    private lateinit var viewModel: MemoryCreationViewModel
    private lateinit var mediaTypeSpinner: Spinner
    private lateinit var visibilitySpinner: Spinner
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var uploadButton: Button
    private lateinit var createButton: Button
    private lateinit var previewImage: ImageView
    private lateinit var mapView: MapView
    private lateinit var progressBar: ProgressBar
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
                    previewImage.visibility = View.VISIBLE
                }
                it.toString().contains("video") -> {
                    // TODO: Handle video preview
                }
                it.toString().contains("audio") -> {
                    // TODO: Handle audio preview
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(this@CreateMemoryActivity, PreferenceManager.getDefaultSharedPreferences(this@CreateMemoryActivity))
        }

        setContentView(R.layout.activity_create_memory)

        // Initialize views
        mediaTypeSpinner = findViewById(R.id.mediaTypeSpinner)
        visibilitySpinner = findViewById(R.id.visibilitySpinner)
        descriptionInput = findViewById(R.id.descriptionInput)
        uploadButton = findViewById(R.id.uploadButton)
        createButton = findViewById(R.id.createButton)
        previewImage = findViewById(R.id.previewImage)
        mapView = findViewById(R.id.mapView)
        progressBar = findViewById(R.id.progressBar)

        // Set up toolbar
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { finish() }
        }

        // Initialize ViewModel with API service
        setupViewModel()
        
        // Check permissions and initialize map
        checkLocationPermission()
        
        // Set up UI listeners
        setupListeners()
        
        // Set up visibility spinner listener
        visibilitySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedVisibility = parent?.getItemAtPosition(position) as String
                viewModel.setVisibility(selectedVisibility)
                Log.d("MemoryCreation", "Selected visibility: $selectedVisibility")
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing, keep default visibility
            }
        }
        
        // Observe ViewModel state
        observeViewModel()
    }

    private fun setupViewModel() {
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
    }

    private fun setupListeners() {
        uploadButton.setOnClickListener {
            when (mediaTypeSpinner.selectedItem.toString().lowercase()) {
                "photo" -> showImagePicker()
                "video" -> getContent.launch("video/*")
                "audio" -> getContent.launch("audio/*")
            }
        }

        createButton.setOnClickListener {
            if (validateForm()) {
                val trimmedDescription = descriptionInput.text.toString().trim()
                if (trimmedDescription.isEmpty()) {
                    Toast.makeText(this, "Please add a description", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                uploadMemory(trimmedDescription)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            
            if (state.error != null) {
                Toast.makeText(this, state.error, Toast.LENGTH_SHORT).show()
            }
            
            if (state.isSuccess) {
                Toast.makeText(this, "Memory created successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initializeMap() {
        mapView.setMultiTouchControls(true)
        
        val mapController = mapView.controller
        mapController.setZoom(6.0)
        
        val startPoint = GeoPoint(13.20, 125.85) // Philippines center
        mapController.setCenter(startPoint)

        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay(this) {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val projection = mapView.projection
                val point = projection.fromPixels(e.x.toInt(), e.y.toInt())
                val geoPoint = GeoPoint(point.latitude, point.longitude)
                
                currentMarker?.let { mapView.overlays.remove(it) }
                
                val marker = Marker(mapView).apply {
                    position = geoPoint
                    val drawable = resources.getDrawable(R.drawable.navigramlogo, theme)
                    drawable.setBounds(0, 0, 48, 48)
                    icon = drawable
                    setAnchor(0.5f, 1.0f)
                }
                currentMarker = marker
                mapView.overlays.add(marker)
                
                viewModel.setLocation(geoPoint.latitude, geoPoint.longitude)
                Log.d("MemoryCreation", "Selected location: ${geoPoint.latitude}, ${geoPoint.longitude}")
                
                mapView.invalidate()
                return true
            }
        })
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
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
                requestPermissions()
            }
            else -> {
                requestPermissions()
            }
        }
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun showImagePicker() {
        val imagePickerDialog = ImagePickerDialog().apply {
            setOnImageSelectedListener { url ->
                viewModel.setMediaUrl(url)
                Glide.with(this@CreateMemoryActivity)
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
        Log.d("MemoryCreation", "Uploading memory with description: $description")
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
