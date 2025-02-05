package com.example.navigram.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.navigram.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import android.os.Environment
import java.io.IOException

class CameraCapture : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var previewView: PreviewView
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA  // Default to rear camera
    private var flashMode = ImageCapture.FLASH_MODE_OFF // Default to no flash
    private lateinit var btnFlash: ImageButton

    private val REQUEST_CODE_LOCATION = 100
    private val REQUEST_CODE_CAMERA = 101
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_capture)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check for permissions at runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
        }

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CAMERA)
        }

        previewView = findViewById(R.id.previewView)
        val btnCapture = findViewById<ImageButton>(R.id.btnCapture)
        val btnSwitchCamera = findViewById<ImageButton>(R.id.btnSwitchCamera)
        btnFlash = findViewById(R.id.flashBtn)

        startCamera()

        btnCapture.setOnClickListener {
            takePhoto()
        }

        btnSwitchCamera.setOnClickListener {
            switchCamera()
        }
        btnFlash.setOnClickListener {
            toggleFlash()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setFlashMode(flashMode) // Apply current flash mode
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()  // Restart camera with new selection
    }

    private fun takePhoto() {
        // Get the DCIM directory path (consider using MediaStore for API 29+)
        val dcimDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Navigram")

        // Alternative: Use app-specific storage
        // val dcimDirectory = File(getExternalFilesDir(Environment.DIRECTORY_DCIM), "Navigram")

        // Create the directory if it doesn't exist
        if (!dcimDirectory.exists()) {
            dcimDirectory.mkdirs()
        }

        // Fetch location before saving the image
        fetchLocation { location ->
            val timestamp = System.currentTimeMillis()

            // Create a file for the image


            // Prepare location info if available
            val locationInfo = if (location != null) {
                "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
            } else {
                "No location data available"
            }
            val imageFile = File(dcimDirectory, "navigram_{$timestamp}_$locationInfo.jpg")
            // Create a Map to store metadata (local path and location info)
            val imageMetadata = mutableMapOf<String, String>(
                "Local Path" to imageFile.absolutePath,
                "Location" to locationInfo
            )

            // Create a separate file to store the metadata
            val metadataFile = File(dcimDirectory, "navigram_${timestamp}_metadata.txt")

            // Write metadata to the file
            try {
                metadataFile.writeText("Image Metadata:\n")
                for ((key, value) in imageMetadata) {
                    metadataFile.appendText("$key: $value\n")
                }
            } catch (e: IOException) {
                Log.e("CameraX", "Failed to write metadata: ${e.message}", e)
            }

            // Set up output options for capturing the photo
            val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

            // Capture the photo
            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Toast.makeText(applicationContext, "Photo Saved: ${imageFile.absolutePath}", Toast.LENGTH_LONG).show()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                    }
                }
            )
        } // Closing brace for fetchLocation lambda
    }

    private fun fetchLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    callback(location)
                }
                .addOnFailureListener { e ->
                    Log.e("CameraCapture", "Failed to get location: ${e.message}")
                    callback(null)
                }
        } else {
            callback(null)
        }
    }

    private fun toggleFlash() {
        // Toggle flash mode: OFF -> ON -> AUTO -> OFF
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                btnFlash.setImageResource(R.drawable.flashon)
                ImageCapture.FLASH_MODE_ON
            }
            ImageCapture.FLASH_MODE_AUTO -> {
                btnFlash.setImageResource(R.drawable.flashoff)
                ImageCapture.FLASH_MODE_OFF
            }
            else -> ImageCapture.FLASH_MODE_OFF
        }

        // Restart camera with new flash setting
        startCamera()
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_LOCATION -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_CAMERA -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}