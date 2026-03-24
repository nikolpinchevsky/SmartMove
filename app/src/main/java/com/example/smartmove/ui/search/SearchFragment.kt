package com.example.smartmove.ui.search

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmove.R
import com.example.smartmove.model.BoxesResponse
import com.example.smartmove.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var recyclerSearchResults: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var boxAdapter: BoxAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        etSearch = view.findViewById(R.id.etSearch)
        recyclerSearchResults = view.findViewById(R.id.recyclerSearchResults)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        setupRecyclerView()
        setupSearch()

        return view
    }

    private fun setupRecyclerView() {
        boxAdapter = BoxAdapter(emptyList()) { selectedBox ->
            val fragment = com.example.smartmove.ui.boxdetails.BoxDetailsFragment()
            fragment.arguments = Bundle().apply {
                putString("box_id", selectedBox.id)
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerSearchResults.layoutManager = LinearLayoutManager(requireContext())
        recyclerSearchResults.adapter = boxAdapter
    }

    private fun setupSearch() {
        etSearch.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }

        etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = etSearch.text.toString().trim()

        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "Type something to search", Toast.LENGTH_SHORT).show()
            return
        }

        tvEmptyState.visibility = View.GONE

        RetrofitClient.api.getBoxes(query = query).enqueue(object : Callback<BoxesResponse> {
            override fun onResponse(
                call: Call<BoxesResponse>,
                response: Response<BoxesResponse>
            ) {
                if (response.isSuccessful) {
                    val boxes = response.body()?.boxes ?: emptyList()

                    Log.d("SEARCH", "Results count: ${boxes.size}")

                    if (boxes.isNotEmpty()) {
                        boxAdapter.updateData(boxes)
                        recyclerSearchResults.visibility = View.VISIBLE
                        tvEmptyState.visibility = View.GONE
                    } else {
                        boxAdapter.updateData(emptyList())
                        recyclerSearchResults.visibility = View.GONE
                        tvEmptyState.visibility = View.VISIBLE
                    }
                } else {
                    Log.e("SEARCH", "Search error: ${response.code()}")
                    Toast.makeText(
                        requireContext(),
                        "Search failed: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                Log.e("SEARCH", "Search failure", t)
                Toast.makeText(
                    requireContext(),
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}