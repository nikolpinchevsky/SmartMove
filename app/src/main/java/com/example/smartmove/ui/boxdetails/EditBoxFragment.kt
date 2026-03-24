package com.example.smartmove.ui.boxdetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.model.BoxUpdateRequest
import com.example.smartmove.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.TextView

class EditBoxFragment : Fragment() {

    private lateinit var etEditBoxName: EditText
    private lateinit var etEditRoom: EditText
    private lateinit var etEditItems: EditText

    private lateinit var chipEditRed: TextView

    private lateinit var chipEditYellow: TextView

    private lateinit var chipEditGreen: TextView
    private lateinit var cbEditFragile: CheckBox
    private lateinit var cbEditValuable: CheckBox
    private lateinit var btnSaveBoxChanges: Button
    private lateinit var switchFragile: Switch
    private lateinit var switchValuable: Switch
    private var selectedPriority: String = "yellow"
    private var currentBoxId: String? = null
    private var currentStatus: String = "closed"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_edit_box, container, false)

        etEditBoxName = view.findViewById(R.id.etEditBoxName)
        etEditRoom = view.findViewById(R.id.etEditRoom)
        etEditItems = view.findViewById(R.id.etEditItems)
        chipEditRed = view.findViewById(R.id.chipEditRed)
        chipEditYellow = view.findViewById(R.id.chipEditYellow)
        chipEditGreen = view.findViewById(R.id.chipEditGreen)
        btnSaveBoxChanges = view.findViewById(R.id.btnSaveBoxChanges)
        switchFragile = view.findViewById(R.id.switchFragile)
        switchValuable = view.findViewById(R.id.switchValuable)
        currentBoxId = arguments?.getString("box_id")

        if (currentBoxId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing box id", Toast.LENGTH_SHORT).show()
        } else {
            loadBoxDetails(currentBoxId!!)
        }

        btnSaveBoxChanges.setOnClickListener {
            saveChanges()
        }
        setupPriorityChips()
        return view
    }

    private fun loadBoxDetails(boxId: String) {
        RetrofitClient.api.getBoxById(boxId).enqueue(object : Callback<BoxResponse> {
            override fun onResponse(call: Call<BoxResponse>, response: Response<BoxResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    bindBoxToForm(response.body()!!)
                } else {
                    Toast.makeText(requireContext(), "Failed to load box", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BoxResponse>, t: Throwable) {
                Log.e("EDIT_BOX", "Load failure", t)
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun bindBoxToForm(box: BoxResponse) {

        etEditBoxName.setText(box.name)
        etEditRoom.setText(box.destination_room)
        etEditItems.setText(box.items.joinToString(", "))
        switchFragile.isChecked = box.fragile
        switchValuable.isChecked = box.valuable
        currentStatus = box.status

        selectedPriority = box.priority_color.lowercase()
        updatePrioritySelection()
    }
    private fun setupPriorityChips() {
        chipEditRed.setOnClickListener {
            selectedPriority = "red"
            updatePrioritySelection()
        }

        chipEditYellow.setOnClickListener {
            selectedPriority = "yellow"
            updatePrioritySelection()
        }

        chipEditGreen.setOnClickListener {
            selectedPriority = "green"
            updatePrioritySelection()
        }
    }

    private fun updatePrioritySelection() {
        chipEditRed.setBackgroundResource(
            if (selectedPriority == "red") R.drawable.bg_priority_red else R.drawable.bg_chip_soft
        )
        chipEditYellow.setBackgroundResource(
            if (selectedPriority == "yellow") R.drawable.bg_priority_yellow else R.drawable.bg_chip_soft
        )
        chipEditGreen.setBackgroundResource(
            if (selectedPriority == "green") R.drawable.bg_priority_green else R.drawable.bg_chip_soft
        )

        chipEditRed.setTextColor(
            if (selectedPriority == "red") requireContext().getColor(android.R.color.white)
            else requireContext().getColor(R.color.smartmove_text_primary)
        )
        chipEditYellow.setTextColor(
            if (selectedPriority == "yellow") requireContext().getColor(android.R.color.white)
            else requireContext().getColor(R.color.smartmove_text_primary)
        )
        chipEditGreen.setTextColor(
            if (selectedPriority == "green") requireContext().getColor(android.R.color.white)
            else requireContext().getColor(R.color.smartmove_text_primary)
        )
    }
    private fun saveChanges() {
        val boxId = currentBoxId ?: return

        val name = etEditBoxName.text.toString().trim()
        val room = etEditRoom.text.toString().trim()
        val itemsText = etEditItems.text.toString().trim()

        if (name.isEmpty()) {
            etEditBoxName.error = "Enter box name"
            return
        }

        if (room.isEmpty()) {
            etEditRoom.error = "Enter destination room"
            return
        }

        val priority = selectedPriority

        val itemsList = if (itemsText.isEmpty()) {
            emptyList()
        } else {
            itemsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        val request = BoxUpdateRequest(
            name = name,
            fragile = switchFragile.isChecked,
            valuable = switchValuable.isChecked,
            priority_color = priority,
            destination_room = room,
            items = itemsList,
            status = currentStatus
        )

        RetrofitClient.api.updateBox(boxId, request).enqueue(object : Callback<BoxResponse> {
            override fun onResponse(call: Call<BoxResponse>, response: Response<BoxResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Box updated successfully", Toast.LENGTH_SHORT).show()

                    val fragment = BoxDetailsFragment().apply {
                        arguments = Bundle().apply {
                            putString("box_id", boxId)
                        }
                    }

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commit()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to update box: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<BoxResponse>, t: Throwable) {
                Log.e("EDIT_BOX", "Save failure", t)
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}