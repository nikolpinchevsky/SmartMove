package com.example.smartmove.ui.boxdetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.model.BoxUpdateRequest
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.util.FormatUtils
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
    private lateinit var btnSaveBoxChanges: Button
    private lateinit var switchFragile: SwitchCompat
    private lateinit var switchValuable: SwitchCompat

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

        val boxId = currentBoxId
        if (boxId.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_missing_box_id),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            loadBoxDetails(boxId)
        }

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnSaveBoxChanges.setOnClickListener {
            saveChanges()
        }

        setupPriorityChips()

        return view
    }

    private fun loadBoxDetails(boxId: String) {
        RetrofitClient.api.getBoxById(boxId)
            .enqueue(object : Callback<BoxResponse> {

                override fun onResponse(
                    call: Call<BoxResponse>,
                    response: Response<BoxResponse>
                ) {
                    if (!isAdded) return

                    if (response.isSuccessful && response.body() != null) {
                        bindBoxToForm(response.body() ?: return)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_failed_load_box),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<BoxResponse>, t: Throwable) {
                    if (!isAdded) return

                    Log.e("EDIT_BOX", "Load failure", t)

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_network, t.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun bindBoxToForm(box: BoxResponse) {

        etEditBoxName.setText(box.name)
        etEditRoom.setText(box.destinationRoom)

        val items = box.items.orEmpty()
        etEditItems.setText(items.joinToString(", "))

        switchFragile.isChecked = box.fragile
        switchValuable.isChecked = box.valuable

        currentStatus = box.status

        selectedPriority = box.priorityColor.lowercase()

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
        val activeDrawable = mapOf(
            "red" to R.drawable.bg_priority_red,
            "yellow" to R.drawable.bg_priority_yellow,
            "green" to R.drawable.bg_priority_green
        )

        val inactiveDrawable = mapOf(
            "red" to R.drawable.bg_priority_red,
            "yellow" to R.drawable.bg_priority_yellow,
            "green" to R.drawable.bg_priority_green
        )

        val textColor = mapOf(
            "red" to R.color.priority_red_text,
            "yellow" to R.color.priority_yellow_text,
            "green" to R.color.priority_green_text
        )

        listOf(
            chipEditRed to "red",
            chipEditYellow to "yellow",
            chipEditGreen to "green"
        ).forEach { (chip, priority) ->

            val selected = selectedPriority == priority

            chip.setBackgroundResource(
                if (selected) activeDrawable.getValue(priority)
                else inactiveDrawable.getValue(priority)
            )

            chip.setTextColor(
                requireContext().getColor(textColor.getValue(priority))
            )

            chip.alpha = if (selected) 1f else 0.45f
        }
    }

    private fun saveChanges() {

        val boxId = currentBoxId ?: return

        val name = etEditBoxName.text.toString().trim()
        val room = etEditRoom.text.toString().trim()
        val itemsText = etEditItems.text.toString().trim()

        if (name.isEmpty()) {
            etEditBoxName.error =
                getString(R.string.error_enter_box_name_edit)
            return
        }

        if (room.isEmpty()) {
            etEditRoom.error =
                getString(R.string.error_enter_room)
            return
        }

        val itemsList = FormatUtils.parseItemsList(itemsText)

        val request = BoxUpdateRequest(
            name = name,
            fragile = switchFragile.isChecked,
            valuable = switchValuable.isChecked,
            priorityColor = selectedPriority,
            destinationRoom = room,
            items = itemsList,
            status = currentStatus
        )

        RetrofitClient.api.updateBox(boxId, request)
            .enqueue(object : Callback<BoxResponse> {

                override fun onResponse(
                    call: Call<BoxResponse>,
                    response: Response<BoxResponse>
                ) {
                    if (!isAdded) return

                    if (response.isSuccessful) {

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_box_updated),
                            Toast.LENGTH_SHORT
                        ).show()

                        requireActivity()
                            .onBackPressedDispatcher
                            .onBackPressed()

                    } else {

                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.error_update_box_failed,
                                response.code()
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<BoxResponse>,
                    t: Throwable
                ) {
                    if (!isAdded) return

                    Log.e("EDIT_BOX", "Save failure", t)

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_network, t.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}