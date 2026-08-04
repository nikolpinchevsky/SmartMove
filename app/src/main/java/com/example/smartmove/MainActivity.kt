package com.example.smartmove

import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.smartmove.data.SessionManager
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.add.AddFragment
import com.example.smartmove.ui.auth.LoginActivity
import com.example.smartmove.ui.home.HomeFragment
import com.example.smartmove.ui.profile.ProfileFragment
import com.example.smartmove.ui.scan.ScanFragment
import com.example.smartmove.ui.search.SearchFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private val fragmentCache = SparseArray<Fragment>()

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
            switchTab(R.id.nav_home)
            bottomNavigation.selectedItemId = R.id.nav_home
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            switchTab(item.itemId)
            true
        }
    }

    private fun switchTab(itemId: Int) {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        val fragment = fragmentCache[itemId] ?: createFragment(itemId).also { fragmentCache.put(itemId, it) }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun createFragment(itemId: Int): Fragment = when (itemId) {
        R.id.nav_home -> HomeFragment()
        R.id.nav_search -> SearchFragment()
        R.id.nav_add -> AddFragment()
        R.id.nav_scan -> ScanFragment()
        R.id.nav_profile -> ProfileFragment()
        else -> HomeFragment()
    }
}
