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
import android.net.Uri
import android.graphics.ImageDecoder
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.smartmove.model.CreateRoomRequest
import com.example.smartmove.model.Room
import com.example.smartmove.model.RoomResponse
import com.example.smartmove.model.RoomsResponse

class AddFragment : Fragment() {

    private lateinit var etBoxName: EditText
    private lateinit var etItems: EditText
    private lateinit var spinnerRooms: Spinner
    private var roomsList: MutableList<Room> = mutableListOf()
    private var selectedRoom: String = ""
    private var activeProjectId: String? = null

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

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->

            if (uris.isNotEmpty()) {
                tvAiStatus.text = "Analyzing ${uris.size} images..."

                uris.forEach { uri ->
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(
                                requireContext().contentResolver,
                                uri
                            )
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            MediaStore.Images.Media.getBitmap(
                                requireContext().contentResolver,
                                uri
                            )
                        }

                        analyzeBitmapWithAi(bitmap)

                    } catch (e: Exception) {
                        tvAiStatus.text = "Failed to load one of the images"
                        btnAnalyzeWithAi.isEnabled = true
                    }
                }

            } else {
                tvAiStatus.text = "No image selected"
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
        loadActiveProjectAndRooms()

        return view
    }

    private fun initViews(view: View) {
        etBoxName = view.findViewById(R.id.etBoxName)
        etItems = view.findViewById(R.id.etItems)
        spinnerRooms = view.findViewById(R.id.spinnerRooms)

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

            val dialogView = layoutInflater.inflate(R.layout.dialog_image_source, null)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val optionCamera = dialogView.findViewById<View>(R.id.optionCamera)
            val optionGallery = dialogView.findViewById<View>(R.id.optionGallery)
            val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)

            optionCamera.setOnClickListener {
                dialog.dismiss()

                if (ContextCompat.checkSelfPermission(
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

            optionGallery.setOnClickListener {
                dialog.dismiss()
                tvAiStatus.text = "Opening gallery..."
                galleryLauncher.launch("image/*")
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
                btnAnalyzeWithAi.isEnabled = true
            }

            dialog.setOnCancelListener {
                btnAnalyzeWithAi.isEnabled = true
            }

            dialog.show()
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
                    if (!isAdded) return
                    file.delete()
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
                    if (!isAdded) return
                    file.delete()
                    btnAnalyzeWithAi.isEnabled = true
                    tvAiStatus.text = "Network error"
                    Log.e("AI", "Failure", t)
                }
            })
    }

    private fun applyAiSuggestions(ai: AiAnalyzeResponse) {

        val suggestions = ai.form_suggestions ?: return

        if (!suggestions.name.isNullOrBlank()) {
            etBoxName.setText(suggestions.name)
        }

        val existingItems = etItems.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        val newItems = suggestions.items
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        val mergedItems = (existingItems + newItems)
            .distinctBy { it.lowercase() }

        etItems.setText(mergedItems.joinToString(", "))

        if (suggestions.fragile == true) {
            switchFragile.isChecked = true
        }

        if (suggestions.valuable == true) {
            switchValuable.isChecked = true
        }

        if (suggestions.priority_color == "red") {
            selectedPriority = "red"
        } else if (selectedPriority != "red" && suggestions.priority_color == "yellow") {
            selectedPriority = "yellow"
        }

        updatePrioritySelection()

        val suggestedRoom = suggestions.destination_room ?: ""

        if (selectedRoom.isBlank() || selectedRoom.equals("general", ignoreCase = true)) {
            val roomIndex = roomsList.indexOfFirst {
                it.name.equals(suggestedRoom, ignoreCase = true)
            }

            if (roomIndex >= 0) {
                spinnerRooms.setSelection(roomIndex)
                selectedRoom = roomsList[roomIndex].name
            }
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

    private fun loadActiveProjectAndRooms() {
        RetrofitClient.api.getActiveProject().enqueue(object : Callback<ActiveProjectResponse> {
            override fun onResponse(
                call: Call<ActiveProjectResponse>,
                response: Response<ActiveProjectResponse>
            ) {
                if (!isAdded) return
                val project = response.body()?.project ?: run {
                    Toast.makeText(requireContext(), "No active project found", Toast.LENGTH_SHORT).show()
                    return
                }
                activeProjectId = project.id
                loadRooms()
            }

            override fun onFailure(call: Call<ActiveProjectResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Error loading project", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadRooms() {
        RetrofitClient.api.getRooms().enqueue(object : Callback<RoomsResponse> {
            override fun onResponse(
                call: Call<RoomsResponse>,
                response: Response<RoomsResponse>
            ) {
                if (!isAdded) return
                roomsList = response.body()?.rooms?.toMutableList() ?: mutableListOf()

                val roomNames = roomsList.map { it.name }.toMutableList()
                roomNames.add("+ Add new room")

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    roomNames
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerRooms.adapter = adapter

                spinnerRooms.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selected = roomNames[position]

                        if (selected == "+ Add new room") {
                            showAddRoomDialog()
                        } else {
                            selectedRoom = selected
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

                if (roomsList.isNotEmpty()) {
                    selectedRoom = roomsList.first().name
                }
            }

            override fun onFailure(call: Call<RoomsResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Failed to load rooms", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddRoomDialog() {
        val input = EditText(requireContext())
        input.hint = "e.g. Balcony"

        AlertDialog.Builder(requireContext())
            .setTitle("Add new room")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val roomName = input.text.toString().trim()

                if (roomName.isNotEmpty()) {
                    createRoom(roomName)
                } else {
                    Toast.makeText(requireContext(), "Room name is empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                if (roomsList.isNotEmpty()) spinnerRooms.setSelection(0)
            }
            .show()
    }

    private fun createRoom(roomName: String) {
        val projectId = activeProjectId ?: run {
            Toast.makeText(requireContext(), "No active project", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CreateRoomRequest(
            project_id = projectId,
            name = roomName
        )

        RetrofitClient.api.createRoom(request).enqueue(object : Callback<RoomResponse> {
            override fun onResponse(call: Call<RoomResponse>, response: Response<RoomResponse>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    selectedRoom = roomName
                    loadRooms()
                    Toast.makeText(requireContext(), "Room added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Room already exists or error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RoomResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Failed to create room", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun selectRoomIfExists(roomName: String) {
        selectedRoom = roomName

        val index = roomsList.indexOfFirst {
            it.name.equals(roomName, ignoreCase = true)
        }

        if (index >= 0) {
            spinnerRooms.setSelection(index)
        }
    }

    private fun saveBox() {
        val boxName = etBoxName.text.toString().trim()
        val room = selectedRoom.trim()
        val itemsText = etItems.text.toString().trim()
        val fragile = switchFragile.isChecked
        val valuable = switchValuable.isChecked

        if (boxName.isEmpty()) {
            etBoxName.error = "Please enter a box name"
            return
        }

        if (room.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a destination room", Toast.LENGTH_SHORT).show()
            return
        }

        val itemsList = if (itemsText.isEmpty()) {
            emptyList()
        } else {
            itemsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        val projectId = activeProjectId
        if (projectId == null) {
            Toast.makeText(requireContext(), "No active project found", Toast.LENGTH_SHORT).show()
            return
        }

        createBox(projectId, boxName, room, itemsList, fragile, valuable)
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
                if (!isAdded) return
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
                if (!isAdded) return
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

        val ivQrCode = dialogView.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.ivQrCode)
        val tvQrIdentifier = dialogView.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvQrIdentifier)
        val btnSave = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnSave)
        val btnClose = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnClose)

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
        if (roomsList.isNotEmpty()) {
            spinnerRooms.setSelection(0)
            selectedRoom = roomsList.first().name
        }
        etItems.text.clear()
        switchFragile.isChecked = false
        switchValuable.isChecked = false
        selectedPriority = "green"
        updatePrioritySelection()
        tvAiStatus.text = "Use AI to fill automatically"
    }
}