package com.example.smartmove.ui.boxdetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.model.BoxStatusUpdateRequest
import com.example.smartmove.network.ApiErrorParser
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.util.FormatUtils
import com.example.smartmove.util.PriorityChipHelper
import com.example.smartmove.util.navigateTo
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

    private lateinit var btnMarkOpened: Button
    private lateinit var btnEditBox: Button
    private lateinit var progressBoxDetails: ProgressBar
    private lateinit var scrollBoxDetails: ScrollView

    private var currentBoxId: String? = null

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
        btnMarkOpened = view.findViewById(R.id.btnMarkOpened)
        btnEditBox = view.findViewById(R.id.btnEditBox)
        progressBoxDetails = view.findViewById(R.id.progressBoxDetails)
        scrollBoxDetails = view.findViewById(R.id.scrollBoxDetails)

        currentBoxId = arguments?.getString("box_id")

        if (currentBoxId.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_missing_box_id),
                Toast.LENGTH_SHORT
            ).show()
        }

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnMarkOpened.setOnClickListener {
            currentBoxId?.let { id ->
                updateStatus(id, "opened")
            }
        }

        btnEditBox.setOnClickListener {
            currentBoxId?.let { id ->
                navigateTo(EditBoxFragment().apply {
                    arguments = Bundle().apply { putString("box_id", id) }
                })
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        currentBoxId?.let { loadBoxDetails(it) }
    }

    private fun loadBoxDetails(boxId: String) {
        progressBoxDetails.visibility = View.VISIBLE
        scrollBoxDetails.visibility = View.GONE

        RetrofitClient.api.getBoxById(boxId).enqueue(object : Callback<BoxResponse> {
            override fun onResponse(call: Call<BoxResponse>, response: Response<BoxResponse>) {
                if (!isAdded) return
                progressBoxDetails.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    scrollBoxDetails.visibility = View.VISIBLE
                    bindBox(response.body() ?: return)
                } else {
                    Log.e("BOX_DETAILS", "Error: ${response.code()}")
                    Toast.makeText(requireContext(), ApiErrorParser.parse(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BoxResponse>, t: Throwable) {
                if (!isAdded) return
                progressBoxDetails.visibility = View.GONE
                Log.e("BOX_DETAILS", "Failure", t)
                Toast.makeText(requireContext(), ApiErrorParser.parse(t), Toast.LENGTH_LONG).show()
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
                    if (!isAdded) return

                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_status_updated),
                            Toast.LENGTH_SHORT
                        ).show()
                        loadBoxDetails(boxId)
                    } else {
                        Toast.makeText(requireContext(), ApiErrorParser.parse(response), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    if (!isAdded) return
                    Toast.makeText(requireContext(), ApiErrorParser.parse(t), Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun bindBox(box: BoxResponse) {
        tvBoxName.text = box.name
        tvBoxNumber.text = getString(R.string.details_box_number, box.boxNumber.toString())
        tvRoom.text = getString(R.string.details_room, box.destinationRoom)
        tvStatus.text = FormatUtils.formatStatus(box.status)
        tvPriority.text = FormatUtils.formatPriority(box.priorityColor)
        tvFragile.text = getString(R.string.details_fragile, FormatUtils.yesNo(box.fragile, requireContext()))
        tvValuable.text = getString(R.string.details_valuable, FormatUtils.yesNo(box.valuable, requireContext()))

        val items = box.items.orEmpty()

        tvItems.text = if (items.isNotEmpty()) {
            items.joinToString(", ")
        } else {
            getString(R.string.details_no_items)
        }

        PriorityChipHelper.applyChipStyle(tvPriority, box.priorityColor, requireContext())

        if (box.status.lowercase() == "opened") {
            btnMarkOpened.isEnabled = false
            btnMarkOpened.setText(R.string.btn_already_opened)
        } else {
            btnMarkOpened.isEnabled = true
            btnMarkOpened.setText(R.string.btn_mark_opened)
        }
    }
}