package com.example.navigram.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.navigram.NavigramApplication
import com.example.navigram.R
import com.example.navigram.databinding.FragmentDashboardBinding
import com.example.navigram.data.model.Story
import com.example.navigram.data.model.FeedPost
import com.example.navigram.ui.CreateMemoryActivity
import com.example.navigram.ui.UserDetailsActivity
import com.google.android.material.snackbar.Snackbar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var storyAdapter: StoryAdapter
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    private lateinit var viewModel: DashboardViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupStoryRecyclerView()
        setupFeedRecyclerView()
        setupAddPostButton()
        observeViewModel()
        
        viewModel.loadFeed() // Load feed posts
    }

    private fun setupViewModel() {
        val factory = DashboardViewModelFactory((requireActivity().application as NavigramApplication).apiService)
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]
        viewModel.loadStories()
    }

    private fun setupStoryRecyclerView() {
        storyAdapter = StoryAdapter(emptyList()) { story ->
            // Launch UserDetailsActivity when story is clicked
            val intent = Intent(requireContext(), UserDetailsActivity::class.java).apply {
                putExtra("user_id", story.id)
            }
            startActivity(intent)
        }

        binding.storiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = storyAdapter
        }
    }

    private fun setupFeedRecyclerView() {
        feedAdapter = FeedAdapter(
            emptyList(),
            onLikeClick = { post ->
                // TODO: Implement like functionality
                Snackbar.make(binding.root, "Liked post", Snackbar.LENGTH_SHORT).show()
            },
            onCommentClick = { post ->
                // TODO: Open comments screen
                Snackbar.make(binding.root, "Comments clicked", Snackbar.LENGTH_SHORT).show()
            },
            onShareClick = { post ->
                // TODO: Implement share functionality
                Snackbar.make(binding.root, "Share clicked", Snackbar.LENGTH_SHORT).show()
            },
            onPostClick = { post ->
                // TODO: Open post details
                Snackbar.make(binding.root, "Post clicked", Snackbar.LENGTH_SHORT).show()
            },
            onProfileClick = { userId ->
                // TODO: Open profile screen
                Snackbar.make(binding.root, "Profile clicked", Snackbar.LENGTH_SHORT).show()
            }
        )

        binding.feedRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = feedAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.stories.observe(viewLifecycleOwner) { stories ->
            storyAdapter.updateStories(stories)
        }

        viewModel.feed.observe(viewLifecycleOwner) { posts ->
            feedAdapter.updatePosts(posts)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setupAddPostButton() {
        binding.fabAddPost.setOnClickListener {
            startActivity(Intent(requireContext(), CreateMemoryActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
