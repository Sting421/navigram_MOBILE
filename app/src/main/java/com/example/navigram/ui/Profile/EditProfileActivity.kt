package com.example.navigram.ui.Profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.navigram.R
import com.example.navigram.databinding.DialogEditProfileBinding
import com.example.navigram.ui.ImagePickerDialog
import com.google.android.material.progressindicator.CircularProgressIndicator

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: DialogEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels { 
        val userData = UserData(
            username = intent.getStringExtra("USERNAME") ?: "",
            email = intent.getStringExtra("EMAIL") ?: "",
            name = intent.getStringExtra("NAME"),
            profilePicture = intent.getStringExtra("PROFILE_PICTURE"),
            phoneNumber = intent.getStringExtra("PHONE"),
            role = intent.getStringExtra("ROLE") ?: "",
            id = intent.getStringExtra("USER_ID") ?: "",
            socialLogin = intent.getBooleanExtra("SOCIAL_LOGIN", false)
        )
        EditProfileViewModelFactory(userData)
    }
    private lateinit var loadingIndicator: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // Setup loading indicator
        loadingIndicator = CircularProgressIndicator(this).apply {
            isIndeterminate = true
            visibility = View.GONE
        }

        // Handle profile image change
        binding.btnChangePhoto.setOnClickListener {
            showImagePicker()
        }

        // Handle save button
        binding.btnSave.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        // Handle cancel button
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun showImagePicker() {
        ImagePickerDialog().apply {
            setOnImageSelectedListener { cloudinaryUrl ->
                viewModel.updateProfileImageUrl(cloudinaryUrl)
            }
        }.show(supportFragmentManager, "image_picker")
    }

    private fun handleImageSelection(imageUrl: String) {
        viewModel.updateProfileImageUrl(imageUrl)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
        }

        viewModel.profileImageUrl.observe(this) { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.profile_placeholder)
                .into(binding.profileImage)
        }

        viewModel.errorMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
