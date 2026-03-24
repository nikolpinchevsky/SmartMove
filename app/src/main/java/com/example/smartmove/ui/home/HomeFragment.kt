package com.example.smartmove.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmove.R
import com.example.smartmove.model.ActiveProjectResponse
import com.example.smartmove.model.BoxesResponse
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.boxdetails.BoxDetailsFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var rootView: View? = null

    private var tvBoxesCount: TextView? = null
    private var tvOpenedCount: TextView? = null
    private var tvUnpackedCount: TextView? = null
    private var tvUrgentCount: TextView? = null
    private var tvProjectName: TextView? = null
    private var tvPriorityEmpty: TextView? = null
    private var recyclerPriorityBoxes: RecyclerView? = null

    private lateinit var priorityBoxAdapter: PriorityBoxAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_home, container, false)

        tvBoxesCount = rootView?.findViewById(R.id.tvBoxesCount)
        tvOpenedCount = rootView?.findViewById(R.id.tvOpenedCount)
        tvUnpackedCount = rootView?.findViewById(R.id.tvUnpackedCount)
        tvUrgentCount = rootView?.findViewById(R.id.tvUrgentCount)
        tvProjectName = rootView?.findViewById(R.id.tvProjectName)
        tvPriorityEmpty = rootView?.findViewById(R.id.tvPriorityEmpty)
        recyclerPriorityBoxes = rootView?.findViewById(R.id.recyclerPriorityBoxes)

        setupPriorityRecycler()

        return rootView!!
    }

    override fun onResume() {
        super.onResume()
        loadActiveProject()
        loadBoxes()
        loadPriorityBoxes()
    }

    private fun setupPriorityRecycler() {
        priorityBoxAdapter = PriorityBoxAdapter(emptyList()) { selectedBox ->
            val fragment = BoxDetailsFragment()
            fragment.arguments = Bundle().apply {
                putString("box_id", selectedBox.id)
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerPriorityBoxes?.layoutManager = LinearLayoutManager(requireContext())
        recyclerPriorityBoxes?.adapter = priorityBoxAdapter
    }

    private fun loadActiveProject() {
        RetrofitClient.api.getActiveProject().enqueue(object : Callback<ActiveProjectResponse> {
            override fun onResponse(
                call: Call<ActiveProjectResponse>,
                response: Response<ActiveProjectResponse>
            ) {
                if (response.isSuccessful) {
                    val project = response.body()?.project
                    tvProjectName?.text = project?.name ?: "No Active Project"
                } else {
                    Log.e("HOME", "Project error: ${response.code()}")
                    tvProjectName?.text = "No Active Project"
                }
            }

            override fun onFailure(call: Call<ActiveProjectResponse>, t: Throwable) {
                Log.e("HOME", "Project failed: ${t.message}")
                tvProjectName?.text = "No Active Project"
            }
        })
    }

    private fun loadBoxes() {
        RetrofitClient.api.getBoxes().enqueue(object : Callback<BoxesResponse> {

            override fun onResponse(
                call: Call<BoxesResponse>,
                response: Response<BoxesResponse>
            ) {
                if (response.isSuccessful) {
                    val boxes = response.body()?.boxes ?: emptyList()

                    val total = boxes.size
                    val opened = boxes.count { it.status.lowercase() == "opened" }
                    val unpacked = boxes.count { it.status.lowercase() == "unpacked" }
                    val urgent = boxes.count { it.priority_color.lowercase() == "red" }

                    tvBoxesCount?.text = total.toString()
                    tvOpenedCount?.text = opened.toString()
                    tvUnpackedCount?.text = unpacked.toString()
                    tvUrgentCount?.text = urgent.toString()

                } else {
                    Log.e("HOME", "Error: ${response.code()}")
                    showFallbackValues()
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                Log.e("HOME", "Failed: ${t.message}")
                showFallbackValues()
            }
        })
    }

    private fun loadPriorityBoxes() {
        RetrofitClient.api.getPriorityBoxes().enqueue(object : Callback<BoxesResponse> {
            override fun onResponse(
                call: Call<BoxesResponse>,
                response: Response<BoxesResponse>
            ) {
                if (response.isSuccessful) {
                    val boxes = response.body()?.boxes ?: emptyList()

                    if (boxes.isNotEmpty()) {
                        priorityBoxAdapter.updateData(boxes)
                        recyclerPriorityBoxes?.visibility = View.VISIBLE
                        tvPriorityEmpty?.visibility = View.GONE
                    } else {
                        priorityBoxAdapter.updateData(emptyList())
                        recyclerPriorityBoxes?.visibility = View.GONE
                        tvPriorityEmpty?.visibility = View.VISIBLE
                    }
                } else {
                    Log.e("HOME", "Priority error: ${response.code()}")
                    recyclerPriorityBoxes?.visibility = View.GONE
                    tvPriorityEmpty?.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                Log.e("HOME", "Priority failed: ${t.message}")
                recyclerPriorityBoxes?.visibility = View.GONE
                tvPriorityEmpty?.visibility = View.VISIBLE
            }
        })
    }

    private fun showFallbackValues() {
        tvBoxesCount?.text = "-"
        tvOpenedCount?.text = "-"
        tvUnpackedCount?.text = "-"
        tvUrgentCount?.text = "-"
    }
}