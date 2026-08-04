package com.example.smartmove.ui.home


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import com.example.smartmove.data.SessionManager
import com.example.smartmove.model.ActiveProjectResponse
import com.example.smartmove.model.BoxesResponse
import com.example.smartmove.network.ApiErrorParser
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.boxlist.BoxesListFragment
import com.example.smartmove.util.navigateTo
import com.example.smartmove.util.openBoxDetails
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import android.annotation.SuppressLint

class HomeFragment : Fragment() {

    private var tvBoxesCount: TextView? = null
    private var tvOpenedCount: TextView? = null
    private var tvUnpackedCount: TextView? = null
    private var tvUrgentCount: TextView? = null
    private var tvProjectName: TextView? = null

    private var tvProgressPercent: TextView? = null
    private var progressMoving: ProgressBar? = null
    private var progressRecentBoxes: ProgressBar? = null
    private var tvRecentEmpty: TextView? = null
    private var recyclerRecentBoxes: RecyclerView? = null

    private var cardBoxes: View? = null
    private var cardOpened: View? = null
    private var cardUnpacked: View? = null
    private var cardUrgent: View? = null

    private var btnSeeAllRecent: View? = null

    private lateinit var recentBoxAdapter: PriorityBoxAdapter
    private lateinit var sessionManager: SessionManager

    private val projectChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshHomeData()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        sessionManager = SessionManager(requireContext())

        tvBoxesCount = rootView.findViewById(R.id.tvBoxesCount)
        tvOpenedCount = rootView.findViewById(R.id.tvOpenedCount)
        tvUnpackedCount = rootView.findViewById(R.id.tvUnpackedCount)
        tvUrgentCount = rootView.findViewById(R.id.tvUrgentCount)
        tvProjectName = rootView.findViewById(R.id.tvProjectName)
        tvProgressPercent = rootView.findViewById(R.id.tvProgressPercent)
        progressMoving = rootView.findViewById(R.id.progressMoving)
        progressRecentBoxes = rootView.findViewById(R.id.progressRecentBoxes)
        tvRecentEmpty = rootView.findViewById(R.id.tvRecentEmpty)
        recyclerRecentBoxes = rootView.findViewById(R.id.recyclerRecentBoxes)

        cardBoxes = rootView.findViewById(R.id.cardBoxes)
        cardOpened = rootView.findViewById(R.id.cardOpened)
        cardUnpacked = rootView.findViewById(R.id.cardUnpacked)
        cardUrgent = rootView.findViewById(R.id.cardUrgent)

        btnSeeAllRecent = rootView.findViewById(R.id.btnSeeAllRecent)

        setupRecentRecycler()
        setupSummaryCardClicks()
        setupSeeAllButton()

        tvProjectName?.text = sessionManager.getActiveProjectName()
            ?: getString(R.string.home_no_active_project)

        return rootView
    }

    override fun onStart() {
        super.onStart()

        val filter = IntentFilter("ACTIVE_PROJECT_CHANGED")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(
                projectChangedReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.registerReceiver(
                requireActivity(),
                projectChangedReceiver,
                filter,
                null,
                null,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onStop() {
        super.onStop()

        try {
            requireContext().unregisterReceiver(projectChangedReceiver)
        } catch (_: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHomeData()
    }

    private fun refreshHomeData() {
        tvProjectName?.text = sessionManager.getActiveProjectName()
            ?: getString(R.string.home_no_active_project)
        loadActiveProject()
        loadBoxes()
    }

    private fun setupSeeAllButton() {
        btnSeeAllRecent?.setOnClickListener {
            openBoxesList("all")
        }
    }

    private fun setupSummaryCardClicks() {
        cardBoxes?.setOnClickListener {
            openBoxesList("all")
        }

        cardOpened?.setOnClickListener {
            openBoxesList("opened")
        }

        cardUnpacked?.setOnClickListener {
            openBoxesList("to_open")
        }

        cardUrgent?.setOnClickListener {
            openBoxesList("urgent")
        }
    }

    private fun openBoxesList(listType: String) {
        navigateTo(BoxesListFragment().apply {
            arguments = Bundle().apply { putString("list_type", listType) }
        })
    }

    private fun setupRecentRecycler() {
        recentBoxAdapter = PriorityBoxAdapter(emptyList()) { selectedBox ->
            openBoxDetails(selectedBox.id)
        }

        recyclerRecentBoxes?.layoutManager = LinearLayoutManager(requireContext())
        recyclerRecentBoxes?.adapter = recentBoxAdapter
    }

    private fun pickRecentBoxes(boxes: List<com.example.smartmove.model.BoxResponse>): List<com.example.smartmove.model.BoxResponse> {
        val grouped = boxes.groupBy { it.priorityColor.lowercase() }
        val selected = mutableListOf<com.example.smartmove.model.BoxResponse>()
        listOf("red", "yellow", "green").forEach { priority ->
            grouped[priority]?.randomOrNull()?.let { selected.add(it) }
        }
        val remaining = boxes.filter { it !in selected }.shuffled()
        selected.addAll(remaining.take(4 - selected.size))
        return selected.take(4)
    }

    private fun loadActiveProject() {
        RetrofitClient.api.getActiveProject().enqueue(object : Callback<ActiveProjectResponse> {
            override fun onResponse(
                call: Call<ActiveProjectResponse>,
                response: Response<ActiveProjectResponse>
            ) {
                if (!isAdded) return

                if (response.isSuccessful) {
                    val project = response.body()?.project

                    if (project != null) {
                        sessionManager.saveActiveProjectId(project.id)
                        sessionManager.saveActiveProjectName(project.name)
                        tvProjectName?.text = project.name
                    } else {
                        sessionManager.clearActiveProject()
                        tvProjectName?.text = getString(R.string.home_no_active_project)
                    }
                } else {
                    Log.e("HOME", "Project error: ${response.code()}")
                    tvProjectName?.text =
                        sessionManager.getActiveProjectName()
                            ?: getString(R.string.home_no_active_project)
                }
            }

            override fun onFailure(call: Call<ActiveProjectResponse>, t: Throwable) {
                if (!isAdded) return

                Log.e("HOME", "Project failed: ${t.message}")
                tvProjectName?.text =
                    sessionManager.getActiveProjectName()
                        ?: getString(R.string.home_no_active_project)
            }
        })
    }

    private fun loadBoxes() {
        progressRecentBoxes?.visibility = View.VISIBLE
        recyclerRecentBoxes?.visibility = View.GONE
        tvRecentEmpty?.visibility = View.GONE

        RetrofitClient.api.getBoxes().enqueue(object : Callback<BoxesResponse> {
            override fun onResponse(call: Call<BoxesResponse>, response: Response<BoxesResponse>) {
                if (!isAdded) return
                progressRecentBoxes?.visibility = View.GONE

                if (response.isSuccessful) {
                    val boxes = response.body()?.boxes ?: emptyList()
                    updateStats(boxes)
                    updateRecentBoxes(boxes)
                } else {
                    Log.e("HOME", "Error: ${response.code()}")
                    showFallbackValues()
                    recyclerRecentBoxes?.visibility = View.GONE
                    tvRecentEmpty?.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), ApiErrorParser.parse(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                if (!isAdded) return
                Log.e("HOME", "Failed: ${t.message}")
                progressRecentBoxes?.visibility = View.GONE
                showFallbackValues()
                recyclerRecentBoxes?.visibility = View.GONE
                tvRecentEmpty?.visibility = View.VISIBLE
                Toast.makeText(requireContext(), ApiErrorParser.parse(t), Toast.LENGTH_LONG).show()
            }
        })
    }

    @SuppressLint("DefaultLocale")
    private fun updateStats(boxes: List<com.example.smartmove.model.BoxResponse>) {
        val total = boxes.size
        val opened = boxes.count {
            it.status.lowercase() == "opened" || it.status.lowercase() == "unpacked"
        }
        val toOpen = boxes.count {
            it.status.lowercase() != "opened" && it.status.lowercase() != "unpacked"
        }
        val urgent = boxes.count {
            it.priorityColor.lowercase() == "red" &&
                    it.status.lowercase() != "opened" &&
                    it.status.lowercase() != "unpacked"
        }
        val progress = if (total > 0) (opened * 100) / total else 0

        tvBoxesCount?.text = total.toString()
        tvOpenedCount?.text = opened.toString()
        tvUnpackedCount?.text = toOpen.toString()
        tvUrgentCount?.text = urgent.toString()
        tvProgressPercent?.text = getString(R.string.home_progress_format, progress)
        progressMoving?.progress = progress
    }

    private fun updateRecentBoxes(boxes: List<com.example.smartmove.model.BoxResponse>) {
        val recent = pickRecentBoxes(boxes)
        if (recent.isNotEmpty()) {
            recentBoxAdapter.updateData(recent)
            recyclerRecentBoxes?.visibility = View.VISIBLE
            tvRecentEmpty?.visibility = View.GONE
        } else {
            recentBoxAdapter.updateData(emptyList())
            recyclerRecentBoxes?.visibility = View.GONE
            tvRecentEmpty?.visibility = View.VISIBLE
        }
    }

    private fun showFallbackValues() {
        tvBoxesCount?.text = "-"
        tvOpenedCount?.text = "-"
        tvUnpackedCount?.text = "-"
        tvUrgentCount?.text = "-"
        tvProgressPercent?.text = getString(R.string.home_progress_format, 0)
        progressMoving?.progress = 0
    }
}
