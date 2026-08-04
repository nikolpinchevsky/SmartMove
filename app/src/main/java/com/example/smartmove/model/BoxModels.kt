package com.example.smartmove.model

import com.google.gson.annotations.SerializedName
//
data class BoxCreateRequest(
    @SerializedName("project_id")
    val projectId: String,
    val name: String,
    val fragile: Boolean,
    val valuable: Boolean,
    @SerializedName("priority_color")
    val priorityColor: String,
    @SerializedName("destination_room")
    val destinationRoom: String,
    val items: List<String>,
    val status: String
)

data class BoxResponse(
    val id: String,
    @SerializedName("project_id")
    val projectId: String,
    @SerializedName("box_number")
    val boxNumber: Int,
    val name: String,
    val fragile: Boolean,
    val valuable: Boolean,
    @SerializedName("priority_color")
    val priorityColor: String,
    @SerializedName("destination_room")
    val destinationRoom: String,
    val items: List<String>? = null,
    val status: String,
    @SerializedName("qr_identifier")
    val qrIdentifier: String?,
    @SerializedName("image_url")
    val imageUrl: String?
)

data class BoxesResponse(
    val boxes: List<BoxResponse>
)

data class BoxStatusUpdateRequest(
    val status: String
)

data class BoxUpdateRequest(
    val name: String? = null,
    val fragile: Boolean? = null,
    val valuable: Boolean? = null,
    @SerializedName("priority_color")
    val priorityColor: String? = null,
    @SerializedName("destination_room")
    val destinationRoom: String? = null,
    val items: List<String>? = null,
    val status: String? = null
)

data class AiFormSuggestions(
    val name: String? = null,
    val items: List<String>? = null,
    @SerializedName("destination_room")
    val destinationRoom: String? = null,
    @SerializedName("priority_color")
    val priorityColor: String? = null,
    val fragile: Boolean? = null,
    val valuable: Boolean? = null
)

data class AiMetadata(
    @SerializedName("detected_categories")
    val detectedCategories: List<String>? = null,
    @SerializedName("suggested_fragile")
    val suggestedFragile: Boolean? = null,
    @SerializedName("suggested_valuable")
    val suggestedValuable: Boolean? = null,
    @SerializedName("suggested_priority_color")
    val suggestedPriorityColor: String? = null,
    @SerializedName("suggested_destination_room")
    val suggestedDestinationRoom: String? = null,
    @SerializedName("suggested_box_name")
    val suggestedBoxName: String? = null,
    val reason: String? = null,
    val approved: Boolean? = null,
    @SerializedName("saved_at")
    val savedAt: String? = null
)

data class AiAnalyzeResponse(
    val ok: Boolean,
    val message: String? = null,
    @SerializedName("form_suggestions")
    val formSuggestions: AiFormSuggestions? = null,
    @SerializedName("ai_metadata")
    val aiMetadata: AiMetadata? = null
)
