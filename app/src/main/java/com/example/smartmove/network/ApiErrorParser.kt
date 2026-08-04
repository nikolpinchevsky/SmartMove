package com.example.smartmove.network

import org.json.JSONObject
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ApiErrorParser {

    fun parse(response: Response<*>): String {
        return try {
            val body = response.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                val json = JSONObject(body)
                when {
                    json.has("detail") -> json.getString("detail")
                    json.has("message") -> json.getString("message")
                    json.has("error") -> json.getString("error")
                    else -> httpMessage(response.code())
                }
            } else {
                httpMessage(response.code())
            }
        } catch (_: Exception) {
            httpMessage(response.code())
        }
    }

    fun parse(t: Throwable): String = when (t) {
        is SocketTimeoutException -> "Connection timed out. Please try again."
        is UnknownHostException -> "No internet connection. Please check your network."
        else -> "Something went wrong. Please try again."
    }

    private fun httpMessage(code: Int): String = when (code) {
        400 -> "Invalid request. Please check your input."
        401 -> "Session expired. Please log in again."
        403 -> "You don't have permission to do this."
        404 -> "Not found."
        409 -> "This already exists."
        in 500..599 -> "Server error. Please try again later."
        else -> "Something went wrong ($code)."
    }
}


