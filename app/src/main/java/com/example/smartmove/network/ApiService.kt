package com.example.smartmove.network

import com.example.smartmove.model.LoginRequest
import com.example.smartmove.model.RegisterRequest
import com.example.smartmove.model.TokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<TokenResponse>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<TokenResponse>
}