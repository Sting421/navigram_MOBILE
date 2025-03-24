package com.example.navigram.ui.Profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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

// RecyclerView Adapter for Gallery Images
class GalleryAdapter(private var imageItems: List<ImageItem>) : 
    RecyclerView.Adapter<GalleryAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.gallery_item_image)
        val videoIndicator: View = view.findViewById(R.id.gallery_item_video_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gallery_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageItem = imageItems[position]

        // Load image using Glide
        Glide.with(holder.imageView.context)
            .load(imageItem.file)
            .centerCrop()
            .into(holder.imageView)

        // Show video indicator if it's a video
        holder.videoIndicator.visibility = when (imageItem.type) {
            ImageItem.MediaType.VIDEO -> View.VISIBLE
            ImageItem.MediaType.IMAGE -> View.GONE
        }
    }

    override fun getItemCount() = imageItems.size

    // Method to update the list of images
    fun updateImages(newImageItems: List<ImageItem>) {
        imageItems = newImageItems
        notifyDataSetChanged()
    }
}

class ProfileFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels()

    // UI Elements
    private lateinit var profileImage: ImageView
    private lateinit var profileUsername: TextView
    private lateinit var postCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var editProfileButton: Button
    private lateinit var postsRecyclerView: RecyclerView

    // Gallery Adapter
    private lateinit var galleryAdapter: GalleryAdapter

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

        // Setup RecyclerView
        galleryAdapter = GalleryAdapter(emptyList())
        postsRecyclerView.layoutManager = GridLayoutManager(context, 3)
        postsRecyclerView.adapter = galleryAdapter

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
                        profileUsername.text = userData.username
                        followersCount.text = userData.followerCount.toString()
                        followingCount.text = userData.followingCount.toString()

                        // Load profile image if available
                        userData.profileImageUrl?.let { url ->
                            Glide.with(requireContext())
                                .load(url)
                                .centerCrop()
                                .into(profileImage)
                        }
                    }
                }
                
                launch {
                    viewModel.galleryData.collect { imageItems ->
                        postCount.text = imageItems.size.toString()
                        galleryAdapter.updateImages(imageItems)
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
            // TODO: Implement edit profile functionality
        }

        // Optional: Implement pagination or load more functionality
        postsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (totalItemCount <= lastVisibleItem + 3) {
                    viewModel.loadImages(loadMore = true)
                }
            }
        })
    }

    companion object {
        private const val TAG = "ProfileFragment"
    }
}
