package com.sandeep.newsreader

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sandeep.newsreader.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

// Single Activity — the only Activity in the entire app.
// All screens are Fragments navigated via NavController.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Wire BottomNavigationView to NavController.
        // Handles back stack, selection state, and reselect behaviour automatically.
        binding.bottomNavigation.setupWithNavController(navController)
    }
}
