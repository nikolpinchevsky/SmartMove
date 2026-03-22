package com.example.smartmove.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.model.BoxesResponse
import com.example.smartmove.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var rootView: View? = null
    private var tvBoxes: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_home, container, false)
        tvBoxes = rootView?.findViewById(R.id.tvBoxesCount)
        return rootView!!
    }

    override fun onResume() {
        super.onResume()
        loadBoxes()
    }

    private fun loadBoxes() {
        RetrofitClient.api.getBoxes().enqueue(object : Callback<BoxesResponse> {

            override fun onResponse(
                call: Call<BoxesResponse>,
                response: Response<BoxesResponse>
            ) {
                if (response.isSuccessful) {
                    val boxes = response.body()?.boxes ?: emptyList()
                    Log.d("HOME", "Boxes count: ${boxes.size}")
                    tvBoxes?.text = boxes.size.toString()
                } else {
                    Log.e("HOME", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                Log.e("HOME", "Failed: ${t.message}")
            }
        })
    }
}