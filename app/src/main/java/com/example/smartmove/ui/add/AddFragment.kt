package com.example.smartmove.ui.add

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.model.ActiveProjectResponse
import com.example.smartmove.model.BoxCreateRequest
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddFragment : Fragment() {

    private lateinit var etBoxName: EditText
    private lateinit var etRoom: EditText
    private lateinit var tvPriorityGreen: TextView
    private lateinit var tvPriorityYellow: TextView
    private lateinit var tvPriorityRed: TextView
    private lateinit var switchFragile: Switch
    private lateinit var switchValuable: Switch
    private lateinit var btnSaveBox: Button

    private var selectedPriority: String = "green"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add, container, false)

        initViews(view)
        setupPrioritySelection()
        setupSaveButton()

        return view
    }

    private fun initViews(view: View) {
        etBoxName = view.findViewById(R.id.etBoxName)
        etRoom = view.findViewById(R.id.etRoom)
        tvPriorityGreen = view.findViewById(R.id.tvPriorityGreen)
        tvPriorityYellow = view.findViewById(R.id.tvPriorityYellow)
        tvPriorityRed = view.findViewById(R.id.tvPriorityRed)
        switchFragile = view.findViewById(R.id.switchFragile)
        switchValuable = view.findViewById(R.id.switchValuable)
        btnSaveBox = view.findViewById(R.id.btnSaveBox)
    }

    private fun setupPrioritySelection() {
        tvPriorityGreen.setOnClickListener {
            selectedPriority = "green"
            Toast.makeText(requireContext(), "Priority: Green", Toast.LENGTH_SHORT).show()
        }

        tvPriorityYellow.setOnClickListener {
            selectedPriority = "yellow"
            Toast.makeText(requireContext(), "Priority: Yellow", Toast.LENGTH_SHORT).show()
        }

        tvPriorityRed.setOnClickListener {
            selectedPriority = "red"
            Toast.makeText(requireContext(), "Priority: Red", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSaveButton() {
        btnSaveBox.setOnClickListener {
            saveBox()
        }
    }

    private fun saveBox() {
        val boxName = etBoxName.text.toString().trim()
        val room = etRoom.text.toString().trim()
        val fragile = switchFragile.isChecked
        val valuable = switchValuable.isChecked

        if (boxName.isEmpty()) {
            etBoxName.error = "Please enter a box name"
            return
        }

        if (room.isEmpty()) {
            etRoom.error = "Please enter a destination room"
            return
        }

        getActiveProjectAndCreateBox(
            boxName = boxName,
            room = room,
            fragile = fragile,
            valuable = valuable
        )
    }

    private fun getActiveProjectAndCreateBox(
        boxName: String,
        room: String,
        fragile: Boolean,
        valuable: Boolean
    ) {
        RetrofitClient.api.getActiveProject().enqueue(object : Callback<ActiveProjectResponse> {
            override fun onResponse(
                call: Call<ActiveProjectResponse>,
                response: Response<ActiveProjectResponse>
            ) {
                if (!response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load active project",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ADD_BOX", "Active project error: ${response.code()}")
                    return
                }

                val activeProject = response.body()?.project

                if (activeProject == null) {
                    Toast.makeText(
                        requireContext(),
                        "No active project found. Create a project first.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                createBox(
                    projectId = activeProject.id,
                    boxName = boxName,
                    room = room,
                    fragile = fragile,
                    valuable = valuable
                )
            }

            override fun onFailure(call: Call<ActiveProjectResponse>, t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "Error loading project: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("ADD_BOX", "getActiveProject failure", t)
            }
        })
    }

    private fun createBox(
        projectId: String,
        boxName: String,
        room: String,
        fragile: Boolean,
        valuable: Boolean
    ) {
        val request = BoxCreateRequest(
            project_id = projectId,
            name = boxName,
            fragile = fragile,
            valuable = valuable,
            priority_color = selectedPriority,
            destination_room = room,
            items = emptyList(),
            status = "closed"
        )

        RetrofitClient.api.createBox(request).enqueue(object : Callback<BoxResponse> {
            override fun onResponse(call: Call<BoxResponse>, response: Response<BoxResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Box saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    clearForm()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to save box",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ADD_BOX", "createBox error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BoxResponse>, t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "Error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("ADD_BOX", "createBox failure", t)
            }
        })
    }

    private fun clearForm() {
        etBoxName.text.clear()
        etRoom.text.clear()
        switchFragile.isChecked = false
        switchValuable.isChecked = false
        selectedPriority = "green"
    }
}