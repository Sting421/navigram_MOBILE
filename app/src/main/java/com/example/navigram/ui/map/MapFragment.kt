package com.example.navigram.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.navigram.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.app.Dialog
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.widget.Button
import android.widget.TextView
import kotlin.math.pow
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import com.bumptech.glide.Glide
import android.widget.ImageView
import com.example.navigram.data.api.CreateMemoryResponse

class MapFragment : Fragment() {
    companion object {
        private const val ZOOM_LEVEL = 7.0
        private const val CLUSTER_DISTANCE_THRESHOLD = 0.05 // Base threshold in kilometers
        private const val LOCATION_ZOOM = 10.0
    }
    private val LOCATION_PERMISSION_REQUEST = 1
    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(requireContext())
    }
    
    private lateinit var map: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var locationPermissionGranted = false

    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var refreshButton: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Initialize progress indicator
        progressIndicator = view.findViewById<CircularProgressIndicator>(R.id.loading_indicator).apply {
            isIndeterminate = true
            visibility = GONE
        }

        // Initialize refresh button
        refreshButton = view.findViewById<FloatingActionButton>(R.id.refresh_button).apply {
            setOnClickListener { viewModel.refreshMemories() }
        }

        // Initialize OSMDroid configuration
        Configuration.getInstance().userAgentValue = requireContext().packageName
        
        map = view.findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        
        // Setup location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)


        // Set up my location button
        view.findViewById<FloatingActionButton>(R.id.my_location_button).setOnClickListener {
            checkLocationPermission()
        }

        return view
    }

    private fun handleMarkerClick(clickedMarker: Marker, memories: List<CreateMemoryResponse>) {
        val currentZoom = map.zoomLevelDouble
        val clickedMemory = memories.find { 
            GeoPoint(it.latitude, it.longitude) == clickedMarker.position 
        } ?: return

        // Calculate zoom-based threshold
        val baseThreshold = CLUSTER_DISTANCE_THRESHOLD
        val zoomFactor = 0.5.pow((currentZoom - 15).toDouble())
        val proximityThreshold = baseThreshold * zoomFactor

        // Find nearby memories
        val closeMemories = memories.filter { memory ->
            val distance = calculateDistance(
                clickedMemory.latitude, clickedMemory.longitude,
                memory.latitude, memory.longitude
            )
            distance <= proximityThreshold
        }

        if (closeMemories.size > 1) {
            showMemoryClusterDialog(closeMemories)
        } else {
            showMemoryDetailsDialog(clickedMemory)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon/2) * Math.sin(dLon/2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
        return R * c
    }

    private fun showMemoryClusterDialog(memories: List<CreateMemoryResponse>) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_memory_cluster)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.memory_list)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Set up adapter for the recycler view
        val adapter = MemoryAdapter(memories) { memory ->
            dialog.dismiss()
            showMemoryDetailsDialog(memory)
        }
        recyclerView.adapter = adapter

        dialog.findViewById<Button>(R.id.close_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private lateinit var memoryCountView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        memoryCountView = view.findViewById(R.id.memory_count)
        super.onViewCreated(view, savedInstanceState)

        checkLocationPermission()

        // Initial map setup
        map.controller.setZoom(ZOOM_LEVEL)
        map.controller.setCenter(GeoPoint(14.5995, 120.9842)) // Default to Manila, Philippines

        // Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    progressIndicator.visibility = if (isLoading) VISIBLE else GONE
                    refreshButton.isEnabled = !isLoading
                }
            }
        }

        // Observe errors
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { error ->
                    error?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Observe public memories
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.publicMemories.collect { memories ->
                    // Update memory count
                    if (memories.isNotEmpty()) {
                        memoryCountView.text = "${memories.size} memories nearby"
                        memoryCountView.visibility = VISIBLE
                    } else {
                        memoryCountView.visibility = GONE
                    }
                    // Clear existing markers
                    map.overlays.removeAll { it is Marker }
                    map.overlays.add(myLocationOverlay)

                    // Add markers for memories and set up clustering
                    memories.forEach { memory ->
                        val marker = Marker(map).apply {
                            position = GeoPoint(memory.latitude, memory.longitude)
                            title = memory.description
                            snippet = "Created: ${memory.createdAt}"
                            icon = requireContext().resources.getDrawable(R.drawable.mappin2, null)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { clickedMarker, _ ->
                                handleMarkerClick(clickedMarker, memories)
                                true
                            }
                        }
                        map.overlays.add(marker)
                    }
                    map.invalidate()
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
            enableLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private fun fetchLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

    private fun enableLocation() {
        if (!locationPermissionGranted) return

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fetchLocation { location ->
                location?.let {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    map.controller.animateTo(geoPoint)
                    Log.d("MapFragment", "Current location - Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Error getting location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true
                enableLocation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        viewModel.refreshMemories()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun showMemoryDetailsDialog(memory: CreateMemoryResponse) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_memory_details)

        // Initialize dialog views
        val memoryImage = dialog.findViewById<ImageView>(R.id.memory_image)
        val memoryDescription = dialog.findViewById<TextView>(R.id.memory_description)
        val memoryDate = dialog.findViewById<TextView>(R.id.memory_date)
        val memoryLocation = dialog.findViewById<TextView>(R.id.memory_location)

        // Load memory data
        Glide.with(requireContext())
            .load(memory.mediaUrl)
            .centerCrop()
            .placeholder(R.drawable.navigramlogo)
            .error(R.drawable.navigramlogo)
            .into(memoryImage)

        memoryDescription.text = memory.description
        memoryDate.text = memory.createdAt
        memoryLocation.text = "📍 ${memory.latitude}, ${memory.longitude}"

        // Set up close button
        dialog.findViewById<Button>(R.id.close_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
