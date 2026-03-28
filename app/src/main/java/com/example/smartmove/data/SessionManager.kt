package com.example.smartmove.data

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("smartmove_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun saveActiveProjectId(projectId: String) {
        prefs.edit().putString("active_project_id", projectId).apply()
    }

    fun getActiveProjectId(): String? {
        return prefs.getString("active_project_id", null)
    }

    fun saveActiveProjectName(projectName: String) {
        prefs.edit().putString("active_project_name", projectName).apply()
    }

    fun getActiveProjectName(): String? {
        return prefs.getString("active_project_name", null)
    }

    fun clearActiveProject() {
        prefs.edit()
            .remove("active_project_id")
            .remove("active_project_name")
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }
}