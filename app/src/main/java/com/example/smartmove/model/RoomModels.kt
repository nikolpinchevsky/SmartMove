package com.example.smartmove.model

data class Room(
    val id: String,
    val project_id: String? = null,
    val name: String
)

data class RoomsResponse(
    val rooms: List<Room>
)

data class CreateRoomRequest(
    val project_id: String,
    val name: String
)

data class RoomResponse(
    val id: String,
    val project_id: String? = null,
    val name: String
)