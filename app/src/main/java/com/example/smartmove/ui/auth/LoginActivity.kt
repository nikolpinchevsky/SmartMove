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
import com.example.smartmove.model.ActiveProjectResponse
import com.example.smartmove.model.LoginRequest
import com.example.smartmove.model.ProjectCreateRequest
import com.example.smartmove.model.ProjectResponse
import com.example.smartmove.model.TokenResponse
import com.example.smartmove.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.SocketTimeoutException

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        RetrofitClient.init(this)
        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)

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
    }

    private fun loginUser(email: String, password: String) {
        btnLogin.isEnabled = false
        btnLogin.text = "Loading..."

        val request = LoginRequest(
            email = email,
            password = password
        )

        RetrofitClient.api.login(request).enqueue(object : Callback<TokenResponse> {

            override fun onResponse(
                call: Call<TokenResponse>,
                response: Response<TokenResponse>
            ) {
                btnLogin.isEnabled = true
                btnLogin.text = "Login"

                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.access_token
                    sessionManager.saveToken(token)
                    createDefaultProjectIfNeeded()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                btnLogin.isEnabled = true
                btnLogin.text = "Login"

                Toast.makeText(
                    this@LoginActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun createDefaultProjectIfNeeded() {
        RetrofitClient.api.getActiveProject().enqueue(object : Callback<ActiveProjectResponse> {

            override fun onResponse(
                call: Call<ActiveProjectResponse>,
                response: Response<ActiveProjectResponse>
            ) {
                if (response.isSuccessful) {
                    val activeProject = response.body()?.project

                    if (activeProject != null) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Login successful",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finishAffinity()
                    } else {
                        createProjectNow()
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Could not check active project",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finishAffinity()
                }
            }

            override fun onFailure(call: Call<ActiveProjectResponse>, t: Throwable) {
                val message = when (t) {
                    is SocketTimeoutException -> "Project check timed out"
                    else -> "Project check failed: ${t.message}"
                }

                Toast.makeText(
                    this@LoginActivity,
                    message,
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finishAffinity()
            }
        })
    }

    private fun createProjectNow() {
        val request = ProjectCreateRequest(
            name = "My First Move"
        )

        RetrofitClient.api.createProject(request).enqueue(object : Callback<ProjectResponse> {

            override fun onResponse(
                call: Call<ProjectResponse>,
                response: Response<ProjectResponse>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Project created successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Failed to create project: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finishAffinity()
            }

            override fun onFailure(call: Call<ProjectResponse>, t: Throwable) {
                val message = when (t) {
                    is SocketTimeoutException -> "Create project timed out"
                    else -> "Error creating project: ${t.message}"
                }

                Toast.makeText(
                    this@LoginActivity,
                    message,
                    Toast.LENGTH_LONG
                ).show()

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finishAffinity()
            }
        })
    }
}