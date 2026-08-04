package com.example.smartmove.ui.search

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmove.R
import com.example.smartmove.model.BoxesResponse
import com.example.smartmove.network.ApiErrorParser
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.util.openBoxDetails
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var recyclerSearchResults: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressSearch: ProgressBar
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
        progressSearch = view.findViewById(R.id.progressSearch)

        setupRecyclerView()
        setupSearch()
        loadAllBoxes()

        return view
    }

    private fun setupRecyclerView() {
        boxAdapter = BoxAdapter(emptyList()) { selectedBox ->
            openBoxDetails(selectedBox.id)
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

    private fun loadAllBoxes() {
        progressSearch.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        recyclerSearchResults.visibility = View.GONE
        fetchBoxes(RetrofitClient.api.getBoxes())
    }

    private fun performSearch() {
        val query = etSearch.text.toString().trim()

        if (query.isEmpty()) {
            loadAllBoxes()
            return
        }

        progressSearch.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        recyclerSearchResults.visibility = View.GONE
        fetchBoxes(RetrofitClient.api.getBoxes(query = query))
    }

    private fun fetchBoxes(call: Call<BoxesResponse>) {
        call.enqueue(object : Callback<BoxesResponse> {
            override fun onResponse(call: Call<BoxesResponse>, response: Response<BoxesResponse>) {
                if (!isAdded) return
                progressSearch.visibility = View.GONE
                if (response.isSuccessful) {
                    updateResults(response.body()?.boxes ?: emptyList())
                } else {
                    Log.e("SEARCH", "Error: ${response.code()}")
                    Toast.makeText(requireContext(), ApiErrorParser.parse(response), Toast.LENGTH_SHORT).show()
                    tvEmptyState.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                if (!isAdded) return
                progressSearch.visibility = View.GONE
                Log.e("SEARCH", "Failure", t)
                Toast.makeText(requireContext(), ApiErrorParser.parse(t), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateResults(boxes: List<com.example.smartmove.model.BoxResponse>) {
        if (boxes.isNotEmpty()) {
            boxAdapter.updateData(boxes)
            recyclerSearchResults.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        } else {
            boxAdapter.updateData(emptyList())
            recyclerSearchResults.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        }
    }
}