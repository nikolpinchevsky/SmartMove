package com.example.smartmove.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartmove.MainActivity
import com.example.smartmove.R
import com.example.smartmove.data.SessionManager
import com.example.smartmove.model.RegisterRequest
import com.example.smartmove.model.TokenResponse
import com.example.smartmove.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnCreateAccount: Button
    private lateinit var tvBackToLogin: TextView

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        sessionManager = SessionManager(this)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        btnCreateAccount.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty()) {
                etFullName.error = "Please enter full name"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etEmail.error = "Please enter email"
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            registerUser(name, email, password)
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        val request = RegisterRequest(
            name = name,
            email = email,
            password = password
        )

        RetrofitClient.api.register(request).enqueue(object : Callback<TokenResponse> {
            override fun onResponse(
                call: Call<TokenResponse>,
                response: Response<TokenResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.access_token
                    sessionManager.saveToken(token)

                    Toast.makeText(
                        this@SignUpActivity,
                        "Registration successful",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                } else {
                    Toast.makeText(
                        this@SignUpActivity,
                        "Registration failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                Toast.makeText(
                    this@SignUpActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}