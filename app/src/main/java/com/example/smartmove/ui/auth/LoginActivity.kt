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
import com.example.smartmove.model.LoginRequest
import com.example.smartmove.model.TokenResponse
import com.example.smartmove.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var tvGuest: TextView

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        tvGuest = findViewById(R.id.tvGuest)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Please enter email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Please enter password"
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        tvGuest.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    private fun loginUser(email: String, password: String) {
        val request = LoginRequest(
            email = email,
            password = password
        )

        RetrofitClient.api.login(request).enqueue(object : Callback<TokenResponse> {
            override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
                val rawBody = response.body()
                val errorText = response.errorBody()?.string()

                if (response.isSuccessful && rawBody != null) {
                    val token = rawBody.access_token
                    sessionManager.saveToken(token)

                    Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                Toast.makeText(
                    this@LoginActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}