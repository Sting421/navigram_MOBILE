package com.example.navigram.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.navigram.MainActivity
import com.example.navigram.R
import com.example.navigram.databinding.ActivityLogoutTestBinding
import com.example.navigram.databinding.ActivitySignUpBinding
import com.example.navigram.ui.login.LoginActivity
import com.example.navigram.ui.login.clearToken
import com.example.navigram.ui.login.getToken


import android.os.Build;

class LogoutTest : AppCompatActivity() {

    private lateinit var binding: ActivityLogoutTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        print("The Token is ${getToken(this@LogoutTest)}")
        enableEdgeToEdge()
        setContentView(R.layout.activity_logout_test)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityLogoutTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val logoutButton = binding.logOut
        val getTokenBtn = binding.getTokenBtn

        logoutButton.setOnClickListener {
            clearToken(this@LogoutTest)
            val intent = Intent(this@LogoutTest, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        getTokenBtn.setOnClickListener {
            val id = Build.SERIAL;
            Toast.makeText(this@LogoutTest, "Hello ${id}, Welcome to Navigram!", Toast.LENGTH_LONG).show()


        }

    }
}