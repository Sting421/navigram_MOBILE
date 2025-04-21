package com.example.navigram.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.navigram.R
import com.example.navigram.databinding.FragmentDashboardBinding
import com.example.navigram.data.model.Story
import com.example.navigram.data.model.FeedPost
import com.example.navigram.ui.CreateMemoryActivity
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStoryRecyclerView()
        setupFeedRecyclerView()
        setupAddPostButton()
    }

    private fun setupStoryRecyclerView() {
        // TODO: Replace with actual data from backend
        val stories = listOf(
            Story("1", "user1", "", System.currentTimeMillis()),
            Story("2", "user2", "", System.currentTimeMillis()),
            Story("3", "user3", "", System.currentTimeMillis())
        )

        storyAdapter = StoryAdapter(stories) { story ->
            // TODO: Handle story click
            Snackbar.make(binding.root, "Story clicked: ${story.username}", Snackbar.LENGTH_SHORT).show()
        }

        binding.storiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = storyAdapter
        }
    }

    private fun setupFeedRecyclerView() {
        // TODO: Replace with actual data from backend
        val posts = listOf(
            FeedPost(
                "1", "user1", "John Doe", "",
                "", "Beautiful sunset! 🌅", 
                42, 5, System.currentTimeMillis()
            ),
            FeedPost(
                "2", "user2", "Jane Smith", "",
                "", "Having a great time! 😊",
                78, 12, System.currentTimeMillis()
            )
        )

        feedAdapter = FeedAdapter(
            posts,
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
