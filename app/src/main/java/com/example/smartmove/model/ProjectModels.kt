package com.example.smartmove.model

import com.google.gson.annotations.SerializedName

data class ProjectCreateRequest(
    val name: String
)

data class ProjectResponse(
    val id: String,
    val name: String,
    @SerializedName("is_active")
    val isActive: Boolean
)

data class ProjectItem(
    val id: String,
    val name: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class ProjectsResponse(
    val projects: List<ProjectItem>
)

data class ActiveProjectResponse(
    val project: ProjectItem?
)

data class ProjectUpdateRequest(
    val name: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null
)
