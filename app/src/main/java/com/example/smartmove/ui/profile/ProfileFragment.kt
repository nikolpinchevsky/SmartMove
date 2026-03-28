package com.example.smartmove.ui.profile

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.data.SessionManager
import com.example.smartmove.model.ProjectItem
import com.example.smartmove.model.ProjectResponse
import com.example.smartmove.model.ProjectsResponse
import com.example.smartmove.model.ProjectUpdateRequest
import com.example.smartmove.model.UserResponse
import com.example.smartmove.network.RetrofitClient
import com.example.smartmove.ui.auth.LoginActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvChangeAvatar: TextView
    private lateinit var btnLogout: Button

    private lateinit var tvCurrentProject: TextView
    private lateinit var layoutProjectsList: LinearLayout
    private lateinit var ivProfileAvatar: ImageView

    private lateinit var sessionManager: SessionManager
    private lateinit var prefs: SharedPreferences

    private val avatar1 = R.drawable.avatar1
    private val avatar2 = R.drawable.avatar2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        initViews(view)

        sessionManager = SessionManager(requireContext())
        prefs = requireContext().getSharedPreferences("profile_prefs", 0)

        loadSavedAvatar()
        setupAvatarPicker()
        loadCurrentUser()
        loadProjects()
        setupLogout()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadProjects()
        loadSavedAvatar()
    }

    private fun initViews(view: View) {
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvChangeAvatar = view.findViewById(R.id.tvChangeAvatar)
        btnLogout = view.findViewById(R.id.btnLogout)

        tvCurrentProject = view.findViewById(R.id.tvCurrentProject)
        layoutProjectsList = view.findViewById(R.id.layoutProjectsList)
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar)
    }

    private fun loadCurrentUser() {
        RetrofitClient.api.getCurrentUser().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (!isAdded) return

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    bindUser(user)
                } else {
                    showFallbackUser()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                if (!isAdded) return
                showFallbackUser()
            }
        })
    }

    private fun bindUser(user: UserResponse) {
        val displayName = user.name ?: user.email.substringBefore("@")
        val firstName = displayName.split(" ").first()

        tvGreeting.text = "Hello, $firstName"
        tvUserName.text = displayName
    }

    private fun showFallbackUser() {
        tvGreeting.text = "Hello, User"
        tvUserName.text = "Unknown User"
    }

    private fun loadProjects() {
        RetrofitClient.api.getProjects().enqueue(object : Callback<ProjectsResponse> {
            override fun onResponse(
                call: Call<ProjectsResponse>,
                response: Response<ProjectsResponse>
            ) {
                if (!isAdded) return

                if (response.isSuccessful) {
                    val projects = response.body()?.projects ?: emptyList()

                    if (projects.isEmpty()) {
                        tvCurrentProject.text = "No projects yet"
                        layoutProjectsList.removeAllViews()
                        return
                    }

                    val active = projects.find { it.is_active }

                    if (active != null) {
                        bindProjects(active, projects)
                    } else {
                        setProjectActive(projects.first())
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load projects", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ProjectsResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Projects error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun bindProjects(current: ProjectItem, all: List<ProjectItem>) {
        sessionManager.saveActiveProjectId(current.id)
        sessionManager.saveActiveProjectName(current.name)

        tvCurrentProject.text = current.name
        layoutProjectsList.removeAllViews()

        val others = all.filter { it.id != current.id }

        others.forEach { project ->
            val view = layoutInflater.inflate(
                R.layout.item_other_project,
                layoutProjectsList,
                false
            )

            val tvName = view.findViewById<TextView>(R.id.tvProjectName)
            tvName.text = project.name

            view.setOnClickListener {
                setProjectActive(project)
            }

            layoutProjectsList.addView(view)
        }

        notifyProjectChanged()
    }

    private fun setProjectActive(project: ProjectItem) {
        RetrofitClient.api.updateProject(
            project.id,
            ProjectUpdateRequest(is_active = true)
        ).enqueue(object : Callback<ProjectResponse> {

            override fun onResponse(
                call: Call<ProjectResponse>,
                response: Response<ProjectResponse>
            ) {
                if (!isAdded) return

                if (response.isSuccessful) {
                    sessionManager.saveActiveProjectId(project.id)
                    sessionManager.saveActiveProjectName(project.name)
                    loadProjects()
                } else {
                    Toast.makeText(requireContext(), "Switch failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ProjectResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Switch failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun notifyProjectChanged() {
        activity?.sendBroadcast(Intent("ACTIVE_PROJECT_CHANGED"))
    }

    private fun setupLogout() {
        btnLogout.setOnClickListener {
            sessionManager.clearSession()
            RetrofitClient.init(requireContext())

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupAvatarPicker() {
        tvChangeAvatar.setOnClickListener {
            showAvatarChooser()
        }
    }

    private fun showAvatarChooser() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_avatar_picker, null)
        val ivAvatarOption = dialogView.findViewById<ImageView>(R.id.ivAvatarOption)

        val currentAvatar = prefs.getInt("selected_avatar", avatar2)
        val avatarToShow = if (currentAvatar == avatar1) avatar2 else avatar1

        ivAvatarOption.setImageResource(avatarToShow)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        ivAvatarOption.setOnClickListener {
            saveAvatar(avatarToShow)
            ivProfileAvatar.setImageResource(avatarToShow)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveAvatar(avatarResId: Int) {
        prefs.edit()
            .putInt("selected_avatar", avatarResId)
            .apply()
    }

    private fun loadSavedAvatar() {
        val savedAvatar = prefs.getInt("selected_avatar", avatar2)
        ivProfileAvatar.setImageResource(savedAvatar)
    }
}