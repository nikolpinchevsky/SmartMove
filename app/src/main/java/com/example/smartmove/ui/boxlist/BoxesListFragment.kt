package com.example.smartmove.ui.boxlist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmove.R
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.model.BoxesResponse
import com.example.smartmove.network.ApiErrorParser
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.search.BoxAdapter
import com.example.smartmove.util.openBoxDetails
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BoxesListFragment : Fragment() {

    private lateinit var tvListTitle: TextView
    private lateinit var tvListSubtitle: TextView
    private lateinit var tvEmptyList: TextView
    private lateinit var progressBoxesList: ProgressBar
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
        progressBoxesList = view.findViewById(R.id.progressBoxesList)
        recyclerBoxesList = view.findViewById(R.id.recyclerBoxesList)

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupRecycler()
        setupTitle()
        loadBoxes()

        return view
    }

    private fun setupRecycler() {
        boxAdapter = BoxAdapter(emptyList()) { selectedBox ->
            openBoxDetails(selectedBox.id)
        }

        recyclerBoxesList.layoutManager = LinearLayoutManager(requireContext())
        recyclerBoxesList.adapter = boxAdapter
    }

    private fun setupTitle() {
        when (listType) {
            "all" -> {
                tvListTitle.text = getString(R.string.list_title_all)
                tvListSubtitle.text = getString(R.string.list_subtitle_all)
                tvEmptyList.text = getString(R.string.list_empty_all)
            }
            "opened" -> {
                tvListTitle.text = getString(R.string.list_title_opened)
                tvListSubtitle.text = getString(R.string.list_subtitle_opened)
                tvEmptyList.text = getString(R.string.list_empty_opened)
            }
            "unpacked" -> {
                tvListTitle.text = getString(R.string.list_title_unpacked)
                tvListSubtitle.text = getString(R.string.list_subtitle_unpacked)
                tvEmptyList.text = getString(R.string.list_empty_opened)
            }
            "priority" -> {
                tvListTitle.text = getString(R.string.list_title_priority)
                tvListSubtitle.text = getString(R.string.list_subtitle_priority)
                tvEmptyList.text = getString(R.string.list_empty_urgent)
            }
            "urgent" -> {
                tvListTitle.text = getString(R.string.list_title_urgent)
                tvListSubtitle.text = getString(R.string.list_subtitle_urgent)
                tvEmptyList.text = getString(R.string.list_empty_urgent)
            }
            "to_open" -> {
                tvListTitle.text = getString(R.string.list_title_to_open)
                tvListSubtitle.text = getString(R.string.list_subtitle_to_open)
                tvEmptyList.text = getString(R.string.list_empty_to_open)
            }
            else -> {
                tvEmptyList.text = getString(R.string.list_empty)
            }
        }
    }

    private fun loadBoxes() {
        progressBoxesList.visibility = View.VISIBLE
        recyclerBoxesList.visibility = View.GONE
        tvEmptyList.visibility = View.GONE

        when (listType) {
            "all" -> loadAllBoxes()
            "opened" -> loadBoxesByStatus("opened")
            "unpacked" -> loadBoxesByStatus("unpacked")
            "priority" -> loadUrgentBoxes()
            "urgent" -> loadUrgentBoxes()
            "to_open" -> loadBoxesToOpen()
            else -> loadAllBoxes()
        }
    }

    private fun fetchBoxes(
        call: Call<BoxesResponse>,
        transform: (List<BoxResponse>) -> List<BoxResponse> = { it }
    ) {
        call.enqueue(object : Callback<BoxesResponse> {
            override fun onResponse(call: Call<BoxesResponse>, response: Response<BoxesResponse>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    updateList(transform(response.body()?.boxes ?: emptyList()))
                } else {
                    showError(response)
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                if (!isAdded) return
                showError(t)
            }
        })
    }

    private fun loadAllBoxes() = fetchBoxes(RetrofitClient.api.getBoxes())

    private fun loadBoxesByStatus(status: String) = fetchBoxes(RetrofitClient.api.getBoxes(status = status))

    private fun loadBoxesToOpen() = fetchBoxes(RetrofitClient.api.getBoxes()) { boxes ->
        boxes.filter {
            it.status.lowercase() != "opened" && it.status.lowercase() != "unpacked"
        }
    }

    private fun loadUrgentBoxes() = fetchBoxes(RetrofitClient.api.getPriorityBoxes())

    private fun updateList(boxes: List<BoxResponse>) {
        progressBoxesList.visibility = View.GONE
        if (boxes.isEmpty()) {
            recyclerBoxesList.visibility = View.GONE
            tvEmptyList.visibility = View.VISIBLE
        } else {
            recyclerBoxesList.visibility = View.VISIBLE
            tvEmptyList.visibility = View.GONE
            boxAdapter.updateData(boxes)
        }
    }

    private fun showError(response: Response<*>) {
        val message = ApiErrorParser.parse(response)
        Log.e("BOXES_LIST", "HTTP ${response.code()}: $message")
        progressBoxesList.visibility = View.GONE
        recyclerBoxesList.visibility = View.GONE
        tvEmptyList.visibility = View.VISIBLE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(t: Throwable) {
        val message = ApiErrorParser.parse(t)
        Log.e("BOXES_LIST", "Failure: ${t.message}")
        progressBoxesList.visibility = View.GONE
        recyclerBoxesList.visibility = View.GONE
        tvEmptyList.visibility = View.VISIBLE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}
