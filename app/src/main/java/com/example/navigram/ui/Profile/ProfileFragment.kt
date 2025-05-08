package com.example.navigram.ui.Profile

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.navigram.R
import com.example.navigram.ui.Profile.ProfileViewModel
import com.example.navigram.ui.Profile.UserData
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProfileFragment : Fragment() {
    private val client = OkHttpClient()
    private val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val outputFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(requireContext())
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1
        private const val ZOOM_LEVEL = 7.0
        private const val CLUSTER_DISTANCE_THRESHOLD = 0.05 // Base threshold in kilometers
        private const val LOCATION_ZOOM = 10.0
        private const val TAG = "ProfileFragment"
    }

    // UI Elements
    private lateinit var profileImage: ImageView
    private lateinit var profileUsername: TextView
    private lateinit var postCount: TextView
    private lateinit var editProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var viewToggleGroup: com.google.android.material.button.MaterialButtonToggleGroup

    // Map related properties
    private lateinit var map: org.osmdroid.views.MapView
    private lateinit var myLocationOverlay: org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
    private var locationPermissionGranted = false

    // Memory Adapter
    private lateinit var memoryAdapter: MemoryAdapter

    // Register activity result launcher for edit profile
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Reload user data after successful profile update
            viewModel.loadUserProfile()
        }
    }

    // Memory list type with fully qualified name
    private var memories: List<com.example.navigram.data.api.CreateMemoryResponse> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.activity_profile, container, false)
        
        // Initialize map first since other views depend on it
        initializeMap(view)
        setupViews(view)
        
        return view
    }

    private fun setupViews(view: View) {
        // Map must be initialized before setting up views
        if (!::map.isInitialized) {
            Log.e(TAG, "Map not initialized before setting up views")
            return
        }

        // Initialize UI elements
        profileImage = view.findViewById(R.id.profile_image)
        profileUsername = view.findViewById(R.id.profile_username)
        postCount = view.findViewById(R.id.profile_post_count)
        editProfileButton = view.findViewById(R.id.edit_profile_button)
        logoutButton = view.findViewById(R.id.logout_button)
        postsRecyclerView = view.findViewById(R.id.profile_posts_recycler_view)
        viewToggleGroup = view.findViewById(R.id.view_toggle_group)

        // Setup toggle group
        viewToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.grid_view_button -> {
                        postsRecyclerView.visibility = VISIBLE
                        map.visibility = GONE
                    }
                    R.id.map_view_button -> {
                        postsRecyclerView.visibility = GONE
                        map.visibility = VISIBLE
                        updateMapMarkers()
                    }
                }
            }
        }

        // Select grid view by default
        viewToggleGroup.check(R.id.grid_view_button)

        // Setup RecyclerView with MemoryAdapter using fully qualified type
        memoryAdapter = MemoryAdapter(memories) { memory: com.example.navigram.data.api.CreateMemoryResponse ->
            showMemoryDetailsDialog(memory)
        }
        postsRecyclerView.layoutManager = GridLayoutManager(context, 3)
        postsRecyclerView.adapter = memoryAdapter
    }

    private fun initializeMap(view: View) {
        // Initialize OSMDroid configuration
        org.osmdroid.config.Configuration.getInstance().userAgentValue = requireContext().packageName
        
        map = view.findViewById(R.id.profile_map_view)
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        
        // Setup location overlay
        myLocationOverlay = org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay(
            org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider(context), 
            map
        )
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)

        // Set initial map position and zoom
        map.controller.setZoom(ZOOM_LEVEL)
        map.controller.setCenter(org.osmdroid.util.GeoPoint(14.5995, 120.9842)) // Default to Manila, Philippines
    }

    private fun updateMapMarkers() {
        // Clear existing markers
        map.overlays.removeAll { it is org.osmdroid.views.overlay.Marker }
        map.overlays.add(myLocationOverlay) // Add back location overlay

        memories.forEach { memory ->
            val marker = org.osmdroid.views.overlay.Marker(map).apply {
                position = org.osmdroid.util.GeoPoint(memory.latitude, memory.longitude)
                title = memory.description
                val date = LocalDateTime.parse(memory.createdAt, inputFormat).toLocalDate()
                snippet = outputFormat.format(date)
                icon = requireContext().resources.getDrawable(R.drawable.mappin2, null)
                setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { marker, _ ->
                    handleMarkerClick(marker, memories)
                    true
                }
            }
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    private fun handleMarkerClick(clickedMarker: org.osmdroid.views.overlay.Marker, memories: List<com.example.navigram.data.api.CreateMemoryResponse>) {
        val clickedMemory = memories.find { 
            org.osmdroid.util.GeoPoint(it.latitude, it.longitude) == clickedMarker.position 
        } ?: return

        // Calculate zoom-based threshold
        val currentZoom = map.zoomLevelDouble
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Enable edge-to-edge
        requireActivity().window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }

        // Observe ViewModel data and update UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userData.collect { userData ->
                        userData?.let { user ->
                            profileUsername.text = "@${user.username}"
                            val displayName = user.name ?: user.username
                            view.findViewById<TextView>(R.id.profile_name).text = displayName
                            
                            // Set email in bio
                            view.findViewById<TextView>(R.id.profile_bio).text = user.email

                            // Load profile image if available
                            user.profilePicture?.let { url ->
                                Glide.with(requireContext())
                                    .load(url)
                                    .centerCrop()
                                    .placeholder(R.drawable.navigramlogo)
                                    .error(R.drawable.navigramlogo)
                                    .into(profileImage)
                            }
                        }
                    }
                }
                
                launch {
                    viewModel.memoriesCount.collect { count ->
                        postCount.text = count.toString()
                    }
                }

                launch {
                    viewModel.memories.collect { newMemories ->
                        memories = newMemories // Update main memories list
                        memoryAdapter.updateMemories(newMemories)
                        if (map.visibility == VISIBLE) {
                            updateMapMarkers()
                        }
                    }
                }

                launch {
                    viewModel.selectedMemory.collect { memory ->
                        // Memory selection is handled by showMemoryDetailsDialog
                    }
                }
            }
        }

        // Set up create memory button click listener
        view.findViewById<View>(R.id.create_memory_button)?.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_memory_creation)
        }

        // Set up button click listeners
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        logoutButton.setOnClickListener {
            // Clear secure preferences
            requireContext().getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            // Navigate to login screen
            findNavController().navigate(R.id.action_navigation_profile_to_loginActivity)
            requireActivity().finish() // Close the current activity to prevent going back
        }

        // Observe update status
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateStatus.collect { status ->
                    when (status) {
                        is ProfileViewModel.UpdateStatus.Success -> {
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            viewModel.clearUpdateStatus()
                        }
                        is ProfileViewModel.UpdateStatus.Error -> {
                            Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                            viewModel.clearUpdateStatus()
                        }
                        null -> {} // Do nothing
                    }
                }
            }
        }

    }

    private fun showEditProfileDialog() {
        // Launch EditProfileActivity
        val intent = Intent(requireContext(), EditProfileActivity::class.java)
        viewModel.userData.value?.let { user ->
            intent.putExtra("NAME", user.name)
            intent.putExtra("USERNAME", user.username)
            intent.putExtra("EMAIL", user.email)
            intent.putExtra("PHONE", user.phoneNumber)
            intent.putExtra("PROFILE_PICTURE", user.profilePicture)
            intent.putExtra("USER_ID", user.id)
            intent.putExtra("ROLE", user.role)
            intent.putExtra("SOCIAL_LOGIN", user.socialLogin)
        }
        editProfileLauncher.launch(intent)
    }

     fun showMemoryDetailsDialog(memory: com.example.navigram.data.api.CreateMemoryResponse) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_memory_details)

        // Initialize dialog views
        val memoryImage = dialog.findViewById<ImageView>(R.id.memory_image)
        val memoryDescription = dialog.findViewById<TextView>(R.id.memory_description)
        val memoryDate = dialog.findViewById<TextView>(R.id.memory_date)

        // Load memory data
        Glide.with(requireContext())
            .load(memory.mediaUrl)
            .centerCrop()
            .placeholder(R.drawable.navigramlogo)
            .error(R.drawable.navigramlogo)
            .into(memoryImage)

        memoryDescription.text = memory.description
        val date = LocalDateTime.parse(memory.createdAt.trim(), inputFormat).toLocalDate()
        memoryDate.text = outputFormat.format(date)

        // Get location data
        val memoryLocation = dialog.findViewById<TextView>(R.id.memory_location)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val locationText = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://address-from-to-latitude-longitude.p.rapidapi.com/geolocationapi?lat=${memory.latitude}&lng=${memory.longitude}")
                        .get()
                        .addHeader("x-rapidapi-key", "fc33d176bdmsh77abb4787653b11p100a6cjsn63a64fd53e22")
                        .addHeader("x-rapidapi-host", "address-from-to-latitude-longitude.p.rapidapi.com")
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    Log.d(TAG, "API Response: $responseBody")
                    val jsonResponse = JSONObject(responseBody ?: "{}")

                    val results = jsonResponse.optJSONArray("Results")
                    Log.d(TAG, "Results array: ${results?.toString(2)}")
                    val address = if (results != null && results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        firstResult.optString("address", "Location not available")
                    } else {
                        "Location not available"
                    }
                    "📍 $address"
                }
                memoryLocation.text = locationText
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching location: ${e.message}", e)
                memoryLocation.text = "📍 Location not available"
            }
        }

        // Set up close button
        dialog.findViewById<Button>(R.id.close_button).setOnClickListener {
            dialog.dismiss()
            viewModel.clearSelectedMemory()
        }

        dialog.show()
    }

    private fun showMemoryClusterDialog(memories: List<com.example.navigram.data.api.CreateMemoryResponse>) {
        val dialog = Dialog(requireContext(), R.style.CustomDialog)
        dialog.setContentView(R.layout.dialog_memory_cluster)

        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            
            val displayMetrics = resources.displayMetrics
            val maxHeight = (displayMetrics.heightPixels * 0.8).toInt()
            attributes?.apply {
                height = maxHeight
            }
            
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.memory_list).apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
        
        val adapter = com.example.navigram.ui.map.MemoryAdapter(memories) { memory ->
            dialog.dismiss()
            showMemoryDetailsDialog(memory)
        }
        recyclerView.adapter = adapter

        dialog.findViewById<Button>(R.id.close_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
