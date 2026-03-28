package com.example.smartmove.ui.scan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.boxdetails.BoxDetailsFragment
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class ScanFragment : Fragment() {

    private lateinit var btnStartScan: Button
    private lateinit var btnOpenFullDetails: Button

    private lateinit var tvEmptyState: TextView
    private lateinit var layoutBoxDetails: LinearLayout

    private lateinit var tvBoxName: TextView
    private lateinit var tvBoxQr: TextView
    private lateinit var tvBoxPriority: TextView
    private lateinit var tvBoxStatus: TextView
    private lateinit var tvBoxRoom: TextView
    private lateinit var tvBoxFragile: TextView
    private lateinit var tvBoxValuable: TextView
    private lateinit var tvBoxItems: TextView

    private var lastScannedBoxId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        btnStartScan = view.findViewById(R.id.btnStartScan)
        btnOpenFullDetails = view.findViewById(R.id.btnOpenFullDetails)

        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        layoutBoxDetails = view.findViewById(R.id.layoutBoxDetails)

        tvBoxName = view.findViewById(R.id.tvBoxName)
        tvBoxQr = view.findViewById(R.id.tvBoxQr)
        tvBoxPriority = view.findViewById(R.id.tvBoxPriority)
        tvBoxStatus = view.findViewById(R.id.tvBoxStatus)
        tvBoxRoom = view.findViewById(R.id.tvBoxRoom)
        tvBoxFragile = view.findViewById(R.id.tvBoxFragile)
        tvBoxValuable = view.findViewById(R.id.tvBoxValuable)
        tvBoxItems = view.findViewById(R.id.tvBoxItems)

        btnStartScan.setOnClickListener {
            startQrScan()
        }

        btnOpenFullDetails.setOnClickListener {
            lastScannedBoxId?.let { boxId ->
                openBoxDetails(boxId)
            }
        }

        return view
    }

    private fun startQrScan() {
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan a SmartMove box QR")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(requireContext(), "Scan cancelled", Toast.LENGTH_SHORT).show()
            } else {
                val qrIdentifier = result.contents.trim()
                Log.d("SCAN", "Scanned QR: $qrIdentifier")
                loadBoxByQr(qrIdentifier)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun loadBoxByQr(qrIdentifier: String) {
        RetrofitClient.api.getBoxByQr(qrIdentifier).enqueue(object : Callback<BoxResponse> {
            override fun onResponse(call: Call<BoxResponse>, response: Response<BoxResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val box = response.body()!!
                    lastScannedBoxId = box.id
                    updateScannedBoxUi(box)

                    Toast.makeText(
                        requireContext(),
                        "Box scanned successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showEmptyState()
                    Toast.makeText(
                        requireContext(),
                        "Box not found for scanned QR",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<BoxResponse>, t: Throwable) {
                Log.e("SCAN", "QR lookup failed", t)
                Toast.makeText(
                    requireContext(),
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun updateScannedBoxUi(box: BoxResponse) {
        tvEmptyState.visibility = View.GONE
        layoutBoxDetails.visibility = View.VISIBLE

        tvBoxName.text = box.name
        tvBoxQr.text = "QR: ${box.qr_identifier}"
        tvBoxPriority.text = "Priority: ${formatText(box.priority_color)}"
        tvBoxStatus.text = "Status: ${formatText(box.status)}"
        tvBoxRoom.text = "Room: ${formatText(box.destination_room)}"
        tvBoxFragile.text = "Fragile: ${yesNo(box.fragile)}"
        tvBoxValuable.text = "Valuable: ${yesNo(box.valuable)}"

        val itemsText = if (box.items.isNullOrEmpty()) {
            "No items added"
        } else {
            box.items.joinToString(", ")
        }

        tvBoxItems.text = itemsText
    }

    private fun showEmptyState() {
        lastScannedBoxId = null
        tvEmptyState.visibility = View.VISIBLE
        layoutBoxDetails.visibility = View.GONE
    }

    private fun openBoxDetails(boxId: String) {
        val fragment = BoxDetailsFragment().apply {
            arguments = Bundle().apply {
                putString("box_id", boxId)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun yesNo(value: Boolean?): String {
        return if (value == true) "Yes" else "No"
    }

    private fun formatText(value: String?): String {
        if (value.isNullOrBlank()) return "-"
        return value.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase(Locale.getDefault())
                    .replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
            }
    }
}