package com.example.smartmove.ui.boxdetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.network.RetrofitClient
import android.widget.Button
import com.example.smartmove.model.BoxStatusUpdateRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BoxDetailsFragment : Fragment() {

    private lateinit var tvBoxName: TextView
    private lateinit var tvBoxNumber: TextView
    private lateinit var tvRoom: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvPriority: TextView
    private lateinit var tvFragile: TextView
    private lateinit var tvValuable: TextView
    private lateinit var tvItems: TextView
    private lateinit var tvQr: TextView

    private lateinit var btnMarkOpened: Button

    private var currentBoxId: String? = null

    private lateinit var btnEditBox: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_box_details, container, false)

        tvBoxName = view.findViewById(R.id.tvDetailsBoxName)
        tvBoxNumber = view.findViewById(R.id.tvDetailsBoxNumber)
        tvRoom = view.findViewById(R.id.tvDetailsRoom)
        tvStatus = view.findViewById(R.id.tvDetailsStatus)
        tvPriority = view.findViewById(R.id.tvDetailsPriority)
        tvFragile = view.findViewById(R.id.tvDetailsFragile)
        tvValuable = view.findViewById(R.id.tvDetailsValuable)
        tvItems = view.findViewById(R.id.tvDetailsItems)
        tvQr = view.findViewById(R.id.tvDetailsQr)
        btnMarkOpened = view.findViewById(R.id.btnMarkOpened)
        btnEditBox = view.findViewById(R.id.btnEditBox)
        val boxId = arguments?.getString("box_id")
        currentBoxId = boxId

        if (boxId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing box id", Toast.LENGTH_SHORT).show()
        } else {
            loadBoxDetails(boxId)
        }
        btnMarkOpened.setOnClickListener {
            val boxId = currentBoxId
            if (boxId != null) {
                updateStatus(boxId, "opened")
            }
        }
        btnEditBox.setOnClickListener {
            val boxId = currentBoxId
            if (boxId != null) {
                val fragment = com.example.smartmove.ui.boxdetails.EditBoxFragment()
                fragment.arguments = Bundle().apply {
                    putString("box_id", boxId)
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
        return view
    }

    private fun loadBoxDetails(boxId: String) {
        RetrofitClient.api.getBoxById(boxId).enqueue(object : Callback<BoxResponse> {
            override fun onResponse(call: Call<BoxResponse>, response: Response<BoxResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    bindBox(response.body()!!)
                } else {
                    Log.e("BOX_DETAILS", "Error: ${response.code()}")
                    Toast.makeText(
                        requireContext(),
                        "Failed to load box details",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<BoxResponse>, t: Throwable) {
                Log.e("BOX_DETAILS", "Failure", t)
                Toast.makeText(
                    requireContext(),
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
    private fun updateStatus(boxId: String, newStatus: String) {
        val request = BoxStatusUpdateRequest(status = newStatus)

        RetrofitClient.api.updateBoxStatus(boxId, request)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "Status updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadBoxDetails(boxId)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to update status: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    Toast.makeText(
                        requireContext(),
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
    private fun bindBox(box: BoxResponse) {
        tvBoxName.text = box.name
        tvBoxNumber.text = "Box number: ${box.box_number}"
        tvRoom.text = "Room: ${box.destination_room}"
        tvStatus.text = box.status.replaceFirstChar { it.uppercase() }
        tvPriority.text = box.priority_color.replaceFirstChar { it.uppercase() }
        tvFragile.text = "Fragile: ${if (box.fragile) "Yes" else "No"}"
        tvValuable.text = "Valuable: ${if (box.valuable) "Yes" else "No"}"
        tvItems.text = if (box.items.isNotEmpty()) box.items.joinToString(", ") else "No items"
        tvQr.text = "QR: ${box.qr_identifier}"

        when (box.priority_color.lowercase()) {
            "red" -> tvPriority.setBackgroundResource(R.drawable.bg_priority_red)
            "yellow" -> tvPriority.setBackgroundResource(R.drawable.bg_priority_yellow)
            "green" -> tvPriority.setBackgroundResource(R.drawable.bg_priority_green)
            else -> tvPriority.setBackgroundResource(R.drawable.bg_chip_soft)
        }

        if (box.status.lowercase() == "opened") {
            btnMarkOpened.isEnabled = false
            btnMarkOpened.text = "Already Opened"
        } else {
            btnMarkOpened.isEnabled = true
            btnMarkOpened.text = "Mark as Opened"
        }
    }
}