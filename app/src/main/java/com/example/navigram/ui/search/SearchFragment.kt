package com.example.navigram.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.navigram.NavigramApplication
import com.example.navigram.R
import com.example.navigram.data.api.ApiService
import com.example.navigram.data.model.User

class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels {
        val app = (requireActivity().applicationContext as NavigramApplication)
        SearchViewModelFactory(app.apiService)
    }
    private lateinit var userAdapter: UserAdapter
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchView = view.findViewById(R.id.searchView)
        recyclerView = view.findViewById(R.id.userRecyclerView)

        setupRecyclerView()
        setupSearchView()
        observeViewModel()
        
        // Load all users initially
        viewModel.searchUsers("")
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(emptyList(), ::onUserClick)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = userAdapter
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchUsers(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.searchUsers(it) }
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            userAdapter.updateUsers(users)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onUserClick(user: User) {
        val bundle = Bundle()
        bundle.putString("userId", user.id)
        findNavController().navigate(R.id.navigation_profile, bundle)
    }
}
