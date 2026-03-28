package com.example.smartmove.ui.home

import android.app.AlertDialog
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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmove.R
import com.example.smartmove.data.SessionManager
import com.example.smartmove.model.ActiveProjectResponse
import com.example.smartmove.model.BoxesResponse
import com.example.smartmove.model.ProjectCreateRequest
import com.example.smartmove.model.ProjectResponse
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.boxdetails.BoxDetailsFragment
import com.example.smartmove.ui.boxlist.BoxesListFragment
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

    private var cardBoxes: CardView? = null
    private var cardOpened: CardView? = null
    private var cardUnpacked: CardView? = null
    private var cardUrgent: CardView? = null

    private var btnNewProject: Button? = null

    private lateinit var priorityBoxAdapter: PriorityBoxAdapter
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
        rootView = inflater.inflate(R.layout.fragment_home, container, false)

        sessionManager = SessionManager(requireContext())

        tvBoxesCount = rootView?.findViewById(R.id.tvBoxesCount)
        tvOpenedCount = rootView?.findViewById(R.id.tvOpenedCount)
        tvUnpackedCount = rootView?.findViewById(R.id.tvUnpackedCount)
        tvUrgentCount = rootView?.findViewById(R.id.tvUrgentCount)
        tvProjectName = rootView?.findViewById(R.id.tvProjectName)
        tvPriorityEmpty = rootView?.findViewById(R.id.tvPriorityEmpty)
        recyclerPriorityBoxes = rootView?.findViewById(R.id.recyclerPriorityBoxes)

        cardBoxes = rootView?.findViewById(R.id.cardBoxes)
        cardOpened = rootView?.findViewById(R.id.cardOpened)
        cardUnpacked = rootView?.findViewById(R.id.cardUnpacked)
        cardUrgent = rootView?.findViewById(R.id.cardUrgent)

        btnNewProject = rootView?.findViewById(R.id.btnNewProject)

        setupPriorityRecycler()
        setupSummaryCardClicks()
        setupNewProjectButton()

        tvProjectName?.text = sessionManager.getActiveProjectName() ?: "No Active Project"

        return rootView!!
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
            requireActivity().registerReceiver(projectChangedReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            requireActivity().unregisterReceiver(projectChangedReceiver)
        } catch (_: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHomeData()
    }

    private fun refreshHomeData() {
        tvProjectName?.text = sessionManager.getActiveProjectName() ?: "No Active Project"
        loadActiveProject()
        loadBoxes()
        loadPriorityBoxes()
    }

    private fun setupNewProjectButton() {
        btnNewProject?.setOnClickListener {
            showCreateProjectDialog()
        }
    }

    private fun showCreateProjectDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_project, null, false)

        val etProjectName = dialogView.findViewById<EditText>(R.id.etProjectName)
        val btnCancelProject = dialogView.findViewById<Button>(R.id.btnCancelProject)
        val btnCreateProject = dialogView.findViewById<Button>(R.id.btnCreateProject)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelProject.setOnClickListener {
            dialog.dismiss()
        }

        btnCreateProject.setOnClickListener {
            val projectName = etProjectName.text.toString().trim()

            if (projectName.isEmpty()) {
                etProjectName.error = "Enter project name"
                return@setOnClickListener
            }

            createNewProject(projectName, dialog)
        }

        dialog.show()
    }

    private fun createNewProject(projectName: String, dialog: AlertDialog) {
        RetrofitClient.api.createProject(
            ProjectCreateRequest(name = projectName)
        ).enqueue(object : Callback<ProjectResponse> {

            override fun onResponse(
                call: Call<ProjectResponse>,
                response: Response<ProjectResponse>
            ) {
                if (!isAdded) return

                if (response.isSuccessful && response.body() != null) {
                    val project = response.body()!!

                    sessionManager.saveActiveProjectId(project.id)
                    sessionManager.saveActiveProjectName(project.name)

                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Project created", Toast.LENGTH_SHORT).show()

                    refreshHomeData()
                    activity?.sendBroadcast(Intent("ACTIVE_PROJECT_CHANGED"))
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Create project failed: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ProjectResponse>, t: Throwable) {
                if (!isAdded) return

                Toast.makeText(
                    requireContext(),
                    "Create project error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupSummaryCardClicks() {
        cardBoxes?.setOnClickListener {
            openBoxesList("all")
        }

        cardOpened?.setOnClickListener {
            openBoxesList("opened")
        }

        cardUnpacked?.setOnClickListener {
            openBoxesList("unpacked")
        }

        cardUrgent?.setOnClickListener {
            openBoxesList("urgent")
        }
    }

    private fun openBoxesList(listType: String) {
        val fragment = BoxesListFragment()
        fragment.arguments = Bundle().apply {
            putString("list_type", listType)
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
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

    private fun loadPriorityBoxes() {
        RetrofitClient.api.getPriorityBoxes().enqueue(object : Callback<BoxesResponse> {
            override fun onResponse(
                call: Call<BoxesResponse>,
                response: Response<BoxesResponse>
            ) {
                if (!isAdded) return

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
                    priorityBoxAdapter.updateData(emptyList())
                    recyclerPriorityBoxes?.visibility = View.GONE
                    tvPriorityEmpty?.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<BoxesResponse>, t: Throwable) {
                if (!isAdded) return

                Log.e("HOME", "Priority failed: ${t.message}")
                priorityBoxAdapter.updateData(emptyList())
                recyclerPriorityBoxes?.visibility = View.GONE
                tvPriorityEmpty?.visibility = View.VISIBLE
            }
        })
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
                        tvProjectName?.text = "No Active Project"
                    }
                } else {
                    Log.e("HOME", "Project error: ${response.code()}")
                    tvProjectName?.text =
                        sessionManager.getActiveProjectName() ?: "No Active Project"
                }
            }

            override fun onFailure(call: Call<ActiveProjectResponse>, t: Throwable) {
                if (!isAdded) return

                Log.e("HOME", "Project failed: ${t.message}")
                tvProjectName?.text =
                    sessionManager.getActiveProjectName() ?: "No Active Project"
            }
        })
    }

    private fun loadBoxes() {
        RetrofitClient.api.getBoxes().enqueue(object : Callback<BoxesResponse> {

            override fun onResponse(
                call: Call<BoxesResponse>,
                response: Response<BoxesResponse>
            ) {
                if (!isAdded) return

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
                if (!isAdded) return

                Log.e("HOME", "Failed: ${t.message}")
                showFallbackValues()
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