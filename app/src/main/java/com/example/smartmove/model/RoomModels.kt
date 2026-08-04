package com.example.smartmove.model

import com.google.gson.annotations.SerializedName

data class Room(
    val id: String,
    @SerializedName("project_id")
    val projectId: String? = null,
    val name: String
)

data class RoomsResponse(
    val rooms: List<Room>
)

data class CreateRoomRequest(
    @SerializedName("project_id")
    val projectId: String,
    val name: String
)

data class RoomResponse(
    val id: String,
    @SerializedName("project_id")
    val projectId: String? = null,
    val name: String
)
