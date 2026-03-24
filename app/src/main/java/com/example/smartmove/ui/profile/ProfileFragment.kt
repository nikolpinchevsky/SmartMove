package com.example.smartmove.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.data.SessionManager
import com.example.smartmove.model.UserResponse
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.auth.LoginActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnLogout: Button
    private lateinit var sessionManager: SessionManager

    private lateinit var tvGreeting: TextView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        btnLogout = view.findViewById(R.id.btnLogout)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        sessionManager = SessionManager(requireContext())

        loadCurrentUser()

        btnLogout.setOnClickListener {
            sessionManager.clearSession()
            RetrofitClient.init(requireContext())

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        return view
    }

    private fun loadCurrentUser() {
        RetrofitClient.api.getCurrentUser().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    bindUser(user)
                } else {
                    Log.e("PROFILE", "Error: ${response.code()}")
                    tvUserName.text = "Unknown User"
                    tvUserEmail.text = "-"
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("PROFILE", "Failure", t)
                tvUserName.text = "Unknown User"
                tvUserEmail.text = "-"
                Toast.makeText(
                    requireContext(),
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun bindUser(user: UserResponse) {
        val displayName = when {
            !user.name.isNullOrBlank() -> user.name
            user.email.isNotBlank() -> user.email.substringBefore("@")
            else -> "SmartMove User"
        }

        val firstName = displayName
            .trim()
            .split(" ")
            .firstOrNull()
            ?.replaceFirstChar { it.uppercase() }
            ?: "User"

        tvGreeting.text = "Hello, $firstName"
        tvUserName.text = displayName
        tvUserEmail.text = user.email
    }
}