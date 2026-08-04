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
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
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
import com.example.smartmove.util.FormatUtils

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
    private lateinit var switchFragile: SwitchCompat
    private lateinit var switchValuable: SwitchCompat
    private lateinit var btnSaveBox: Button

    private lateinit var btnAnalyzeWithAi: Button
    private lateinit var tvAiStatus: TextView

    private var selectedPriority: String = "green"

    private var pendingQrBitmap: Bitmap? = null
    private var pendingQrFileName: String? = null
    private var pendingRoomSelection: String? = null
    private var pendingAiRequests = 0

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                analyzeBitmapWithAi(bitmap)
            } else {
                tvAiStatus.text = getString(R.string.ai_status_no_photo)
                btnAnalyzeWithAi.isEnabled = true
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                tvAiStatus.text = getString(R.string.ai_status_analyzing_images, uris.size)

                uris.forEach { uri ->
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(
                                requireContext().contentResolver,
                                uri
                            )
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(
                                requireContext().contentResolver,
                                uri
                            )
                        }

                        analyzeBitmapWithAi(bitmap)

                    } catch (_: Exception) {
                        tvAiStatus.text = getString(R.string.ai_status_failed_load)
                        if (pendingAiRequests == 0) btnAnalyzeWithAi.isEnabled = true
                    }
                }

            } else {
                tvAiStatus.text = getString(R.string.ai_status_no_image)
                btnAnalyzeWithAi.isEnabled = true
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                tvAiStatus.text = getString(R.string.ai_status_opening_camera)
                cameraLauncher.launch(null)
            } else {
                tvAiStatus.text = getString(R.string.ai_status_camera_denied)
                btnAnalyzeWithAi.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    getString(R.string.ai_status_camera_denied),
                    Toast.LENGTH_SHORT
                ).show()
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
                    if (saved) getString(R.string.msg_qr_saved) else getString(R.string.error_qr_save_failed),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_storage_denied),
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
                    tvAiStatus.text = getString(R.string.ai_status_opening_camera)
                    cameraLauncher.launch(null)
                } else {
                    tvAiStatus.text = getString(R.string.ai_status_requesting_camera)
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            optionGallery.setOnClickListener {
                dialog.dismiss()
                tvAiStatus.text = getString(R.string.ai_status_opening_gallery)
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
        pendingAiRequests++
        tvAiStatus.text = getString(R.string.ai_status_analyzing)

        val file = bitmapToFile(bitmap) ?: run {
            pendingAiRequests--
            if (pendingAiRequests == 0) btnAnalyzeWithAi.isEnabled = true
            tvAiStatus.text = getString(R.string.ai_status_failed_process)
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
                    pendingAiRequests--
                    if (pendingAiRequests == 0) btnAnalyzeWithAi.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val ai = response.body() ?: return

                        if (ai.ok && ai.formSuggestions != null) {
                            applyAiSuggestions(ai)
                            tvAiStatus.text = getString(R.string.ai_status_success)
                        } else {
                            tvAiStatus.text = ai.message ?: getString(R.string.ai_status_failed)
                        }
                    } else {
                        tvAiStatus.text = getString(R.string.ai_status_error_code, response.code())
                        Log.e("AI", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<AiAnalyzeResponse>, t: Throwable) {
                    if (!isAdded) return
                    file.delete()
                    pendingAiRequests--
                    if (pendingAiRequests == 0) btnAnalyzeWithAi.isEnabled = true
                    tvAiStatus.text = getString(R.string.ai_status_network_error)
                    Log.e("AI", "Failure", t)
                }
            })
    }

    private fun applyAiSuggestions(ai: AiAnalyzeResponse) {

        val suggestions = ai.formSuggestions ?: return

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

        if (suggestions.priorityColor == "red") {
            selectedPriority = "red"
        } else if (selectedPriority != "red" && suggestions.priorityColor == "yellow") {
            selectedPriority = "yellow"
        }

        updatePrioritySelection()

        val suggestedRoom = suggestions.destinationRoom ?: ""

        Log.d("AI_ROOM", "Suggested room: $suggestedRoom")
        Log.d("AI_ROOM", "Available rooms: ${roomsList.map { it.name }}")

        val roomIndex = roomsList.indexOfFirst {
            it.name.equals(suggestedRoom, ignoreCase = true)
        }

        if (roomIndex >= 0) {
            spinnerRooms.setSelection(roomIndex)
            selectedRoom = roomsList[roomIndex].name
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
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_no_active_project),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                activeProjectId = project.id
                loadRooms()
            }

            override fun onFailure(call: Call<ActiveProjectResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_loading_project),
                    Toast.LENGTH_SHORT
                ).show()
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
                roomNames.add(getString(R.string.add_new_room))

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    roomNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                spinnerRooms.onItemSelectedListener = null
                spinnerRooms.adapter = adapter

                val targetRoom = pendingRoomSelection ?: roomsList.firstOrNull()?.name
                pendingRoomSelection = null

                if (!targetRoom.isNullOrEmpty()) {
                    val index = roomsList.indexOfFirst { it.name.equals(targetRoom, ignoreCase = true) }
                    val safeIndex = if (index >= 0) index else 0
                    spinnerRooms.setSelection(safeIndex, false)
                    selectedRoom = roomsList.getOrNull(safeIndex)?.name ?: ""
                }

                spinnerRooms.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selected = roomNames[position]
                        if (selected == getString(R.string.add_new_room)) {
                            showAddRoomDialog()
                        } else {
                            selectedRoom = selected
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }

            override fun onFailure(call: Call<RoomsResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_failed_load_rooms),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showAddRoomDialog() {
        val previousSelection = spinnerRooms.selectedItemPosition

        val input = EditText(requireContext())
        input.hint = getString(R.string.hint_room_hint)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_add_room_title))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
                val roomName = input.text.toString().trim()

                if (roomName.isNotEmpty()) {
                    createRoom(roomName)
                } else {
                    restoreSpinnerSelection(previousSelection)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_room_name_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { _, _ ->
                restoreSpinnerSelection(previousSelection)
            }
            .setOnCancelListener {
                restoreSpinnerSelection(previousSelection)
            }
            .show()
    }

    private fun restoreSpinnerSelection(previousPosition: Int) {
        if (previousPosition < roomsList.size) {
            spinnerRooms.setSelection(previousPosition)
            selectedRoom = roomsList[previousPosition].name
        } else if (roomsList.isNotEmpty()) {
            spinnerRooms.setSelection(0)
            selectedRoom = roomsList.first().name
        }
    }

    private fun createRoom(roomName: String) {
        val projectId = activeProjectId ?: run {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_no_active_project),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val request = CreateRoomRequest(
            projectId = projectId,
            name = roomName
        )

        RetrofitClient.api.createRoom(request).enqueue(object : Callback<RoomResponse> {
            override fun onResponse(call: Call<RoomResponse>, response: Response<RoomResponse>) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    pendingRoomSelection = roomName
                    loadRooms()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.msg_room_added),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_room_already_exists),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<RoomResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_failed_create_room),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun saveBox() {
        val boxName = etBoxName.text.toString().trim()
        val room = selectedRoom.trim()
        val itemsText = etItems.text.toString().trim()
        val fragile = switchFragile.isChecked
        val valuable = switchValuable.isChecked

        if (boxName.isEmpty()) {
            etBoxName.error = getString(R.string.error_enter_box_name)
            return
        }

        if (room.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_select_room),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val itemsList = FormatUtils.parseItemsList(itemsText)

        val projectId = activeProjectId
        if (projectId == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_no_active_project),
                Toast.LENGTH_SHORT
            ).show()
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
        btnSaveBox.isEnabled = false

        val request = BoxCreateRequest(
            projectId = projectId,
            name = boxName,
            fragile = fragile,
            valuable = valuable,
            priorityColor = selectedPriority,
            destinationRoom = room,
            items = items,
            status = "closed"
        )

        RetrofitClient.api.createBox(request).enqueue(object : Callback<BoxResponse> {
            override fun onResponse(call: Call<BoxResponse>, response: Response<BoxResponse>) {
                if (!isAdded) return
                btnSaveBox.isEnabled = true
                if (response.isSuccessful && response.body() != null) {
                    val savedBox = response.body() ?: return

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.msg_box_saved),
                        Toast.LENGTH_SHORT
                    ).show()

                    if (!savedBox.qrIdentifier.isNullOrEmpty()) {
                        showQrDialog(
                            qrIdentifier = savedBox.qrIdentifier,
                            boxNumber = savedBox.boxNumber
                        )
                    }

                    clearForm()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_saving_box),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<BoxResponse>, t: Throwable) {
                if (!isAdded) return
                btnSaveBox.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_saving_box),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun generateQrBitmap(text: String, size: Int = 700): Bitmap {
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap[x, y] = if (bits[x, y]) Color.BLACK else Color.WHITE
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

        tvQrIdentifier.text = getString(R.string.label_box_number, boxNumber?.toString() ?: "-")

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
                    if (saved) getString(R.string.msg_qr_saved) else getString(R.string.error_qr_save_failed),
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
                if (saved) getString(R.string.msg_qr_saved) else getString(R.string.error_qr_save_failed),
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
        tvAiStatus.text = getString(R.string.ai_status_default)
    }
}
