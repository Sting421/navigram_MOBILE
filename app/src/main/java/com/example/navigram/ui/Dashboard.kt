package com.example.navigram.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.navigram.ui.Gallery.GalleryViewModel
import com.example.navigram.R
import com.example.navigram.databinding.ActivityDashboardBinding

class Dashboard : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    val navController = null
     val viewModel: GalleryViewModel by viewModels()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadImages()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_dashboard)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_camera, R.id.navigation_gallery,
                R.id.navigation_map, R.id.navigation_profile
            )
        )
        
        // Remove setupActionBarWithNavController
        navView.setupWithNavController(navController)

        // Add onclick listener for bottom navigation
        navView.setOnItemSelectedListener { item ->
            val navController = findNavController(R.id.nav_host_fragment_activity_dashboard)
            val currentDestination = navController.currentDestination?.id

            when (item.itemId) {
                R.id.navigation_camera -> {
                    // Start Camera Activity
                    if (currentDestination != R.id.navigation_camera) {
                        val intent = Intent(this@Dashboard, CameraCapture::class.java)
                        startActivity(intent)
                    }
                    true
                }
                R.id.navigation_gallery -> {
                    // Navigate to Gallery (reuse existing instance)
                    if (currentDestination != R.id.navigation_gallery) {
                        navController.navigate(
                            R.id.navigation_gallery,
                            null,
                            NavOptions.Builder()
                                .setLaunchSingleTop(true) // Reuse existing instance
                                .setPopUpTo(R.id.navigation_gallery, false)
                                .build()
                        )
                    }
                    true
                }
                R.id.navigation_home -> {
                    // Navigate to Home (reuse existing instance)
                    if (currentDestination != R.id.navigation_home) {
                        navController.navigate(
                            R.id.navigation_home,
                            null,
                            NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .setPopUpTo(R.id.navigation_home, false)
                                .build()
                        )
                    }
                    true
                }

                R.id.navigation_profile -> {
                    // Navigate to Profile (reuse existing instance)
                    if (currentDestination != R.id.navigation_profile) {
                        navController.navigate(
                            R.id.navigation_profile,
                            null,
                            NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .setPopUpTo(R.id.navigation_profile, false)
                                .build()
                        )
                    }
                    true
                }
                R.id.navigation_map -> {
                    // Navigate to Map (reuse existing instance)
                    if (currentDestination != R.id.navigation_map) {
                        navController.navigate(
                            R.id.navigation_map,
                            null,
                            NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .setPopUpTo(R.id.navigation_map, false)
                                .build()
                        )
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_dashboard)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
