package com.example.smartmove.model


data class BoxCreateRequest(
    val project_id: String,
    val name: String,
    val fragile: Boolean,
    val valuable: Boolean,
    val priority_color: String,
    val destination_room: String,
    val items: List<String>,
    val status: String
)

data class BoxResponse(
    val id: String,
    val project_id: String,
    val box_number: Int,
    val name: String,
    val fragile: Boolean,
    val valuable: Boolean,
    val priority_color: String,
    val destination_room: String,
    val items: List<String>,
    val status: String,
    val qr_identifier: String,
    val image_url: String?
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
    val priority_color: String? = null,
    val destination_room: String? = null,
    val items: List<String>? = null,
    val status: String? = null
)