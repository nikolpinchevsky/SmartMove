package com.example.smartmove

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smartmove.ui.home.HomeFragment
import com.example.smartmove.ui.search.SearchFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.add.AddFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(HomeFragment())
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