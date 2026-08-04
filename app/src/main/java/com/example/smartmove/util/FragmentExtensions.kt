package com.example.smartmove.util

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.ui.boxdetails.BoxDetailsFragment

fun Fragment.navigateTo(fragment: Fragment) {
    requireActivity().supportFragmentManager.beginTransaction()
        .replace(R.id.fragmentContainer, fragment)
        .addToBackStack(null)
        .commitAllowingStateLoss()
}

fun Fragment.openBoxDetails(boxId: String) {
    navigateTo(BoxDetailsFragment().apply {
        arguments = Bundle().apply { putString("box_id", boxId) }
    })
}
