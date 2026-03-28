package com.example.smartmove.network

import com.example.smartmove.model.ActiveProjectResponse
import com.example.smartmove.model.AiAnalyzeResponse
import com.example.smartmove.model.BoxCreateRequest
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.model.BoxesResponse
import com.example.smartmove.model.BoxStatusUpdateRequest
import com.example.smartmove.model.BoxUpdateRequest
import com.example.smartmove.model.LoginRequest
import com.example.smartmove.model.ProjectCreateRequest
import com.example.smartmove.model.ProjectResponse
import com.example.smartmove.model.ProjectsResponse
import com.example.smartmove.model.ProjectUpdateRequest
import com.example.smartmove.model.RegisterRequest
import com.example.smartmove.model.TokenResponse
import com.example.smartmove.model.UserResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<TokenResponse>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<TokenResponse>

    @GET("auth/me")
    fun getCurrentUser(): Call<UserResponse>

    @POST("projects")
    fun createProject(@Body request: ProjectCreateRequest): Call<ProjectResponse>

    @GET("projects")
    fun getProjects(): Call<ProjectsResponse>

    @GET("projects/active")
    fun getActiveProject(): Call<ActiveProjectResponse>

    @PATCH("projects/{project_id}")
    fun updateProject(
        @Path("project_id") projectId: String,
        @Body request: ProjectUpdateRequest
    ): Call<ProjectResponse>

    @POST("boxes")
    fun createBox(@Body request: BoxCreateRequest): Call<BoxResponse>

    @GET("boxes")
    fun getBoxes(
        @Query("project_id") projectId: String? = null,
        @Query("q") query: String? = null,
        @Query("room") room: String? = null,
        @Query("priority_color") priorityColor: String? = null,
        @Query("status") status: String? = null
    ): Call<BoxesResponse>

    @GET("boxes/{box_id}")
    fun getBoxById(@Path("box_id") boxId: String): Call<BoxResponse>

    @GET("boxes/by-qr/{qr_identifier}")
    fun getBoxByQr(@Path("qr_identifier") qrIdentifier: String): Call<BoxResponse>

    @GET("boxes/priority/open-first")
    fun getPriorityBoxes(
        @Query("project_id") projectId: String? = null
    ): Call<BoxesResponse>

    @PATCH("boxes/{box_id}")
    fun updateBox(
        @Path("box_id") boxId: String,
        @Body request: BoxUpdateRequest
    ): Call<BoxResponse>

    @PATCH("boxes/{box_id}/status")
    fun updateBoxStatus(
        @Path("box_id") boxId: String,
        @Body request: BoxStatusUpdateRequest
    ): Call<Map<String, Any>>

    @Multipart
    @POST("ai/analyze-box-image")
    fun analyzeBoxImageForForm(
        @Part file: MultipartBody.Part
    ): Call<AiAnalyzeResponse>
}