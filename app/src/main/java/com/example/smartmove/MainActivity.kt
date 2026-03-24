package com.example.smartmove

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smartmove.ui.home.HomeFragment
import com.example.smartmove.ui.search.SearchFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.add.AddFragment
import com.example.smartmove.ui.scan.ScanFragment
import com.example.smartmove.ui.profile.ProfileFragment
import android.content.Intent
import com.example.smartmove.data.SessionManager
import com.example.smartmove.ui.auth.LoginActivity
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        RetrofitClient.init(this)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            bottomNavigation.selectedItemId = R.id.nav_home
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_search -> {
                    replaceFragment(SearchFragment())
                    true
                }
                R.id.nav_add -> {
                    replaceFragment(AddFragment())
                    true
                }
                R.id.nav_scan -> {
                    replaceFragment(ScanFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}