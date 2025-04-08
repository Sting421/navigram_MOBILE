package com.example.navigram.ui.Profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.Dialog
import android.util.Log
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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.navigram.R
import com.example.navigram.ui.Profile.ProfileViewModel
import com.example.navigram.ui.Profile.UserData
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ProfileFragment : Fragment() {
    private val client = OkHttpClient()
    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(requireContext())
    }

    // UI Elements
    private lateinit var profileImage: ImageView
    private lateinit var profileUsername: TextView
    private lateinit var postCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var editProfileButton: Button
    private lateinit var postsRecyclerView: RecyclerView

    // Memory Adapter
    private lateinit var memoryAdapter: MemoryAdapter

    // Memory list type with fully qualified name
    private var memories: List<com.example.navigram.data.api.CreateMemoryResponse> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.activity_profile, container, false)

        // Initialize UI elements
        profileImage = view.findViewById(R.id.profile_image)
        profileUsername = view.findViewById(R.id.profile_username)
        postCount = view.findViewById(R.id.profile_post_count)
        followersCount = view.findViewById(R.id.profile_followers_count)
        followingCount = view.findViewById(R.id.profile_following_count)
        editProfileButton = view.findViewById(R.id.edit_profile_button)
        postsRecyclerView = view.findViewById(R.id.profile_posts_recycler_view)

        // Setup RecyclerView with MemoryAdapter using fully qualified type
        memoryAdapter = MemoryAdapter(memories) { memory: com.example.navigram.data.api.CreateMemoryResponse ->
            showMemoryDetailsDialog(memory)
        }
        postsRecyclerView.layoutManager = GridLayoutManager(context, 3)
        postsRecyclerView.adapter = memoryAdapter

        return view
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

                            // Hide follower stats for now as they're not part of the API
                            followersCount.text = "0"
                            followingCount.text = "0"

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
                    viewModel.memories.collect { memories ->
                        memoryAdapter.updateMemories(memories)
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

        // Set up edit profile button click listener
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
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
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_edit_profile)

        // Initialize dialog views
        val nameInput = dialog.findViewById<TextInputEditText>(R.id.edit_name)
        val usernameInput = dialog.findViewById<TextInputEditText>(R.id.edit_username)
        val emailInput = dialog.findViewById<TextInputEditText>(R.id.edit_email)
        val phoneInput = dialog.findViewById<TextInputEditText>(R.id.edit_phone)

        // Pre-fill current user data
        viewModel.userData.value?.let { user ->
            nameInput.setText(user.name)
            usernameInput.setText(user.username)
            emailInput.setText(user.email)
            phoneInput.setText(user.phoneNumber)
        }

        // Set up dialog buttons
        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            viewModel.userData.value?.let { currentUser ->
                val updatedUser = UserData(
                    username = usernameInput.text.toString(),
                    email = emailInput.text.toString(),
                    name = nameInput.text.toString(),
                    profilePicture = currentUser.profilePicture,
                    phoneNumber = phoneInput.text.toString(),
                    role = currentUser.role,
                    id = currentUser.id,
                    socialLogin = currentUser.socialLogin
                )
                viewModel.updateUserProfile(updatedUser)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showMemoryDetailsDialog(memory: com.example.navigram.data.api.CreateMemoryResponse) {
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
        memoryDate.text = memory.createdAt

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

    companion object {
        private const val TAG = "ProfileFragment"
    }
}
