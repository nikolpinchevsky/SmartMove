package com.example.smartmove.ui.boxlist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmove.R
import com.example.smartmove.model.BoxesResponse
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.boxdetails.BoxDetailsFragment
import com.example.smartmove.ui.search.BoxAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BoxesListFragment : Fragment() {

    private lateinit var tvListTitle: TextView
    private lateinit var tvListSubtitle: TextView
    private lateinit var tvEmptyList: TextView
    private lateinit var recyclerBoxesList: RecyclerView
    private lateinit var boxAdapter: BoxAdapter

    private var listType: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listType = arguments?.getString("list_type") ?: "all"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_boxes_list, container, false)

        tvListTitle = view.findViewById(R.id.tvListTitle)
        tvListSubtitle = view.findViewById(R.id.tvListSubtitle)
        tvEmptyList = view.findViewById(R.id.tvEmptyList)
        recyclerBoxesList = view.findViewById(R.id.recyclerBoxesList)

        setupRecycler()
        setupTitle()
        loadBoxes()

        return view
    }

    private fun setupRecycler() {
        boxAdapter = BoxAdapter(emptyList()) { selectedBox ->
            val fragment = BoxDetailsFragment()
            fragment.arguments = Bundle().apply {
                putString("box_id", selectedBox.id)
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerBoxesList.layoutManager = LinearLayoutManager(requireContext())
        recyclerBoxesList.adapter = boxAdapter
    }

    private fun setupTitle() {
        when (listType) {
            "all" -> {
                tvListTitle.text = "All Boxes"
                tvListSubtitle.text = "All boxes in the active project"
            }
            "opened" -> {
                tvListTitle.text = "Opened Boxes"
                tvListSubtitle.text = "Boxes that were already opened"
            }
            "unpacked" -> {
                tvListTitle.text = "Unpacked Boxes"
                tvListSubtitle.text = "Boxes that were already unpacked"
            }
            "urgent" -> {
                tvListTitle.text = "Urgent Boxes"
                tvListSubtitle.text = "Priority boxes that should be handled first"
            }
        }
    }

    private fun loadBoxes() {
        when (listType) {
            "all" -> loadAllBoxes()
            "opened" -> loadBoxesByStatus("opened")
            "unpacked" -> loadBoxesByStatus("unpacked")
            "urgent" -> loadUrgentBoxes()
        }
    }

    private fun loadAllBoxes() {
        RetrofitClient.api.getBoxes().enqueue(object : Callback<BoxesResponse> {
            override fun onResponse(call: Call<BoxesResponse>, response: Response<BoxesResponse>) {
                if (response.isSuccessful) {
                    val boxes = response.body()?.boxes ?: emptyList()
                    updateList(boxes)
                } else {
                    showError("Failed to load boxes: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun loadBoxesByStatus(status: String) {
        RetrofitClient.api.getBoxes(status = status).enqueue(object : Callback<BoxesResponse> {
            override fun onResponse(call: Call<BoxesResponse>, response: Response<BoxesResponse>) {
                if (response.isSuccessful) {
                    val boxes = response.body()?.boxes ?: emptyList()
                    updateList(boxes)
                } else {
                    showError("Failed to load boxes: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun loadUrgentBoxes() {
        RetrofitClient.api.getPriorityBoxes().enqueue(object : Callback<BoxesResponse> {
            override fun onResponse(call: Call<BoxesResponse>, response: Response<BoxesResponse>) {
                if (response.isSuccessful) {
                    val boxes = response.body()?.boxes ?: emptyList()
                    updateList(boxes)
                } else {
                    showError("Failed to load urgent boxes: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun updateList(boxes: List<com.example.smartmove.model.BoxResponse>) {
        if (boxes.isEmpty()) {
            recyclerBoxesList.visibility = View.GONE
            tvEmptyList.visibility = View.VISIBLE
        } else {
            recyclerBoxesList.visibility = View.VISIBLE
            tvEmptyList.visibility = View.GONE
            boxAdapter.updateData(boxes)
        }
    }

    private fun showError(message: String) {
        Log.e("BOXES_LIST", message)
        recyclerBoxesList.visibility = View.GONE
        tvEmptyList.visibility = View.VISIBLE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}