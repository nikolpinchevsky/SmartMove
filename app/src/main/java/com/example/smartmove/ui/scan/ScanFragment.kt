package com.example.smartmove.ui.scan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

class ScanFragment : Fragment() {

    private lateinit var btnStartScan: Button
    private lateinit var tvLastScannedName: TextView
    private lateinit var tvLastScannedDetails: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        btnStartScan = view.findViewById(R.id.btnStartScan)
        tvLastScannedName = view.findViewById(R.id.tvLastScannedName)
        tvLastScannedDetails = view.findViewById(R.id.tvLastScannedDetails)

        btnStartScan.setOnClickListener {
            startQrScan()
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
                val qrIdentifier = result.contents
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
                    updateLastScannedUi(box)
                    openBoxDetails(box.id)
                } else {
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

    private fun updateLastScannedUi(box: BoxResponse) {
        tvLastScannedName.text = box.name
        tvLastScannedDetails.text =
            "Priority: ${box.priority_color} • Status: ${box.status} • Room: ${box.destination_room}"
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
}