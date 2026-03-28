package com.example.smartmove.ui.add

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.model.ActiveProjectResponse
import com.example.smartmove.model.AiAnalyzeResponse
import com.example.smartmove.model.BoxCreateRequest
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.network.RetrofitClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class AddFragment : Fragment() {

    private lateinit var etBoxName: EditText
    private lateinit var etRoom: EditText
    private lateinit var etItems: EditText
    private lateinit var tvPriorityGreen: TextView
    private lateinit var tvPriorityYellow: TextView
    private lateinit var tvPriorityRed: TextView
    private lateinit var switchFragile: Switch
    private lateinit var switchValuable: Switch
    private lateinit var btnSaveBox: Button

    private lateinit var btnAnalyzeWithAi: Button
    private lateinit var tvAiStatus: TextView

    private var selectedPriority: String = "green"

    private var pendingQrBitmap: Bitmap? = null
    private var pendingQrFileName: String? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                analyzeBitmapWithAi(bitmap)
            } else {
                tvAiStatus.text = "No photo captured"
                btnAnalyzeWithAi.isEnabled = true
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                tvAiStatus.text = "Opening camera..."
                cameraLauncher.launch(null)
            } else {
                tvAiStatus.text = "Camera permission denied"
                btnAnalyzeWithAi.isEnabled = true
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            val bitmap = pendingQrBitmap
            val fileName = pendingQrFileName

            if (isGranted && bitmap != null && fileName != null) {
                val saved = saveBitmapToGallery(bitmap, fileName)
                Toast.makeText(
                    requireContext(),
                    if (saved) "QR saved to gallery" else "Failed to save QR",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Storage permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }

            pendingQrBitmap = null
            pendingQrFileName = null
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add, container, false)

        initViews(view)
        setupPrioritySelection()
        setupSaveButton()
        setupAiButton()
        updatePrioritySelection()

        return view
    }

    private fun initViews(view: View) {
        etBoxName = view.findViewById(R.id.etBoxName)
        etRoom = view.findViewById(R.id.etRoom)
        etItems = view.findViewById(R.id.etItems)
        tvPriorityGreen = view.findViewById(R.id.tvPriorityGreen)
        tvPriorityYellow = view.findViewById(R.id.tvPriorityYellow)
        tvPriorityRed = view.findViewById(R.id.tvPriorityRed)
        switchFragile = view.findViewById(R.id.switchFragile)
        switchValuable = view.findViewById(R.id.switchValuable)
        btnSaveBox = view.findViewById(R.id.btnSaveBox)

        btnAnalyzeWithAi = view.findViewById(R.id.btnAnalyzeWithAi)
        tvAiStatus = view.findViewById(R.id.tvAiStatus)
    }

    private fun setupAiButton() {
        btnAnalyzeWithAi.setOnClickListener {
            btnAnalyzeWithAi.isEnabled = false

            if (
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                tvAiStatus.text = "Opening camera..."
                cameraLauncher.launch(null)
            } else {
                tvAiStatus.text = "Requesting camera permission..."
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun analyzeBitmapWithAi(bitmap: Bitmap) {
        tvAiStatus.text = "Analyzing image..."

        val file = bitmapToFile(bitmap) ?: run {
            tvAiStatus.text = "Failed to process image"
            btnAnalyzeWithAi.isEnabled = true
            return
        }

        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val multipart = MultipartBody.Part.createFormData("file", file.name, requestFile)

        RetrofitClient.api.analyzeBoxImageForForm(multipart)
            .enqueue(object : Callback<AiAnalyzeResponse> {

                override fun onResponse(
                    call: Call<AiAnalyzeResponse>,
                    response: Response<AiAnalyzeResponse>
                ) {
                    btnAnalyzeWithAi.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val ai = response.body()!!

                        if (ai.ok && ai.form_suggestions != null) {
                            applyAiSuggestions(ai)
                            tvAiStatus.text = "AI filled the form ✔"
                        } else {
                            tvAiStatus.text = ai.message ?: "AI failed"
                        }
                    } else {
                        tvAiStatus.text = "Error: ${response.code()}"
                        Log.e("AI", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<AiAnalyzeResponse>, t: Throwable) {
                    btnAnalyzeWithAi.isEnabled = true
                    tvAiStatus.text = "Network error"
                    Log.e("AI", "Failure", t)
                }
            })
    }

    private fun applyAiSuggestions(ai: AiAnalyzeResponse) {
        val s = ai.form_suggestions ?: return

        s.name?.let { etBoxName.setText(it) }
        s.destination_room?.let { etRoom.setText(it) }
        etItems.setText(s.items?.joinToString(", ") ?: "")

        switchFragile.isChecked = s.fragile ?: false
        switchValuable.isChecked = s.valuable ?: false

        s.priority_color?.let {
            selectedPriority = it.lowercase()
            updatePrioritySelection()
        }
    }

    private fun bitmapToFile(bitmap: Bitmap): File? {
        return try {
            val file = File(requireContext().cacheDir, "ai_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            file
        } catch (e: Exception) {
            Log.e("AI", "bitmapToFile failed", e)
            null
        }
    }

    private fun setupPrioritySelection() {
        tvPriorityGreen.setOnClickListener {
            selectedPriority = "green"
            updatePrioritySelection()
        }

        tvPriorityYellow.setOnClickListener {
            selectedPriority = "yellow"
            updatePrioritySelection()
        }

        tvPriorityRed.setOnClickListener {
            selectedPriority = "red"
            updatePrioritySelection()
        }
    }

    private fun updatePrioritySelection() {
        tvPriorityGreen.alpha = if (selectedPriority == "green") 1f else 0.5f
        tvPriorityYellow.alpha = if (selectedPriority == "yellow") 1f else 0.5f
        tvPriorityRed.alpha = if (selectedPriority == "red") 1f else 0.5f
    }

    private fun setupSaveButton() {
        btnSaveBox.setOnClickListener {
            saveBox()
        }
    }

    private fun saveBox() {
        val boxName = etBoxName.text.toString().trim()
        val room = etRoom.text.toString().trim()
        val itemsText = etItems.text.toString().trim()
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

        val itemsList = if (itemsText.isEmpty()) {
            emptyList()
        } else {
            itemsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        getActiveProjectAndCreateBox(
            boxName,
            room,
            itemsList,
            fragile,
            valuable
        )
    }

    private fun getActiveProjectAndCreateBox(
        boxName: String,
        room: String,
        items: List<String>,
        fragile: Boolean,
        valuable: Boolean
    ) {
        RetrofitClient.api.getActiveProject().enqueue(object : Callback<ActiveProjectResponse> {
            override fun onResponse(
                call: Call<ActiveProjectResponse>,
                response: Response<ActiveProjectResponse>
            ) {
                val project = response.body()?.project ?: run {
                    Toast.makeText(requireContext(), "No active project found", Toast.LENGTH_SHORT).show()
                    return
                }

                createBox(project.id, boxName, room, items, fragile, valuable)
            }

            override fun onFailure(call: Call<ActiveProjectResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error loading project", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createBox(
        projectId: String,
        boxName: String,
        room: String,
        items: List<String>,
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
            items = items,
            status = "closed"
        )

        RetrofitClient.api.createBox(request).enqueue(object : Callback<BoxResponse> {
            override fun onResponse(call: Call<BoxResponse>, response: Response<BoxResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val savedBox = response.body()!!

                    Toast.makeText(requireContext(), "Box saved!", Toast.LENGTH_SHORT).show()

                    if (!savedBox.qr_identifier.isNullOrEmpty()) {
                        showQrDialog(
                            qrIdentifier = savedBox.qr_identifier,
                            boxNumber = savedBox.box_number
                        )
                    }

                    clearForm()
                } else {
                    Toast.makeText(requireContext(), "Error saving box", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BoxResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error saving box", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun generateQrBitmap(text: String, size: Int = 700): Bitmap {
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }

    private fun showQrDialog(qrIdentifier: String, boxNumber: Int?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_qr_code, null)

        val ivQrCode = dialogView.findViewById<ImageView>(R.id.ivQrCode)
        val tvQrIdentifier = dialogView.findViewById<TextView>(R.id.tvQrIdentifier)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        val bitmap = generateQrBitmap(qrIdentifier)
        ivQrCode.setImageBitmap(bitmap)

        tvQrIdentifier.text = "Box number: ${boxNumber ?: "-"}"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            saveQrWithPermissionCheck(bitmap, "QR_$qrIdentifier.png")
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveQrWithPermissionCheck(bitmap: Bitmap, fileName: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val saved = saveBitmapToGallery(bitmap, fileName)
                Toast.makeText(
                    requireContext(),
                    if (saved) "QR saved to gallery" else "Failed to save QR",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                pendingQrBitmap = bitmap
                pendingQrFileName = fileName
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            val saved = saveBitmapToGallery(bitmap, fileName)
            Toast.makeText(
                requireContext(),
                if (saved) "QR saved to gallery" else "Failed to save QR",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap, fileName: String): Boolean {
        val resolver = requireContext().contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/SmartMove"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return false

        return try {
            val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
            outputStream.use { stream ->
                if (stream == null) return false
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            true
        } catch (e: Exception) {
            Log.e("QR_SAVE", "Failed saving QR", e)
            false
        }
    }

    private fun clearForm() {
        etBoxName.text.clear()
        etRoom.text.clear()
        etItems.text.clear()
        switchFragile.isChecked = false
        switchValuable.isChecked = false
        selectedPriority = "green"
        updatePrioritySelection()
        tvAiStatus.text = "Use AI to fill automatically"
    }
}