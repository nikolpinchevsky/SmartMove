package com.example.smartmove.model

data class ProjectCreateRequest(
    val name: String
)

data class ProjectResponse(
    val id: String,
    val name: String,
    val is_active: Boolean
)

data class ProjectItem(
    val id: String,
    val name: String,
    val is_active: Boolean,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class ProjectsResponse(
    val projects: List<ProjectItem>
)

data class ActiveProjectResponse(
    val project: ProjectItem?
)

data class ProjectUpdateRequest(
    val name: String? = null,
    val is_active: Boolean? = null
)