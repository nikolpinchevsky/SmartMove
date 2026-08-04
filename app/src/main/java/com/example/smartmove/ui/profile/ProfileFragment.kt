package com.example.smartmove.ui.profile

import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.smartmove.R
import com.example.smartmove.data.SessionManager
import com.example.smartmove.model.ProjectCreateRequest
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
    private lateinit var btnNewProject: Button

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
        setupLogout()
        setupNewProjectButton()

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
        btnNewProject = view.findViewById(R.id.btnNewProject)

        tvCurrentProject = view.findViewById(R.id.tvCurrentProject)
        layoutProjectsList = view.findViewById(R.id.layoutProjectsList)
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar)
    }

    private fun loadCurrentUser() {
        RetrofitClient.api.getCurrentUser().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (!isAdded) return

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body() ?: return
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

        tvGreeting.text = getString(R.string.profile_greeting, firstName)
        tvUserName.text = displayName
    }

    private fun showFallbackUser() {
        tvGreeting.text = getString(R.string.profile_greeting_default)
        tvUserName.text = getString(R.string.profile_unknown_user)
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
                        tvCurrentProject.text = getString(R.string.profile_no_projects)
                        layoutProjectsList.removeAllViews()
                        return
                    }

                    val active = projects.find { it.isActive }

                    if (active != null) {
                        bindProjects(active, projects)
                    } else {
                        setProjectActive(projects.first())
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_load_projects_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ProjectsResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_projects_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun bindProjects(current: ProjectItem, all: List<ProjectItem>) {
        val previousActiveId = sessionManager.getActiveProjectId()

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

            val tvName = view.findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvProjectName)
            tvName.text = project.name

            view.setOnClickListener {
                setProjectActive(project)
            }

            layoutProjectsList.addView(view)
        }

        if (previousActiveId != current.id) {
            notifyProjectChanged()
        }
    }

    private fun setProjectActive(project: ProjectItem) {
        RetrofitClient.api.updateProject(
            project.id,
            ProjectUpdateRequest(isActive = true)
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
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_switch_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ProjectResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_switch_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun notifyProjectChanged() {
        activity?.sendBroadcast(
            Intent("ACTIVE_PROJECT_CHANGED").setPackage(requireContext().packageName)
        )
    }

    private fun setupNewProjectButton() {
        btnNewProject.setOnClickListener {
            showCreateProjectDialog()
        }
    }

    private fun showCreateProjectDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_project, null, false)

        val etProjectName = dialogView.findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.etProjectName)
        val btnCancelProject = dialogView.findViewById<Button>(R.id.btnCancelProject)
        val btnCreateProject = dialogView.findViewById<Button>(R.id.btnCreateProject)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelProject.setOnClickListener { dialog.dismiss() }

        btnCreateProject.setOnClickListener {
            val projectName = etProjectName.text.toString().trim()
            if (projectName.isEmpty()) {
                etProjectName.error = getString(R.string.home_enter_project_name)
                return@setOnClickListener
            }
            createNewProject(projectName, dialog)
        }

        dialog.show()
    }

    private fun createNewProject(projectName: String, dialog: AlertDialog) {
        RetrofitClient.api.createProject(
            ProjectCreateRequest(name = projectName)
        ).enqueue(object : Callback<ProjectResponse> {

            override fun onResponse(call: Call<ProjectResponse>, response: Response<ProjectResponse>) {
                if (!isAdded) return

                if (response.isSuccessful && response.body() != null) {
                    val project = response.body() ?: return
                    sessionManager.saveActiveProjectId(project.id)
                    sessionManager.saveActiveProjectName(project.name)
                    dialog.dismiss()
                    Toast.makeText(requireContext(), getString(R.string.msg_project_created), Toast.LENGTH_SHORT).show()
                    loadProjects()
                    notifyProjectChanged()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_create_project_failed_code, response.code()),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ProjectResponse>, t: Throwable) {
                if (!isAdded) return
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_create_project_error, t.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
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

        val ivAvatar1 = dialogView.findViewById<ImageView>(R.id.ivAvatar1)
        val ivAvatar2 = dialogView.findViewById<ImageView>(R.id.ivAvatar2)
        val container1 = dialogView.findViewById<FrameLayout>(R.id.containerAvatar1)
        val container2 = dialogView.findViewById<FrameLayout>(R.id.containerAvatar2)
        val btnCancelAvatar = dialogView.findViewById<Button>(R.id.btnCancelAvatar)
        val btnApplyAvatar = dialogView.findViewById<Button>(R.id.btnApplyAvatar)

        ivAvatar1.setImageResource(avatar1)
        ivAvatar2.setImageResource(avatar2)

        var pendingSelection = prefs.getInt("selected_avatar", avatar2)

        fun refreshSelection() {
            container1.setBackgroundResource(
                if (pendingSelection == avatar1) R.drawable.bg_avatar_selected else R.drawable.bg_avatar_unselected
            )
            container2.setBackgroundResource(
                if (pendingSelection == avatar2) R.drawable.bg_avatar_selected else R.drawable.bg_avatar_unselected
            )
        }
        refreshSelection()

        container1.setOnClickListener { pendingSelection = avatar1; refreshSelection() }
        container2.setOnClickListener { pendingSelection = avatar2; refreshSelection() }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelAvatar.setOnClickListener { dialog.dismiss() }

        btnApplyAvatar.setOnClickListener {
            saveAvatar(pendingSelection)
            ivProfileAvatar.setImageResource(pendingSelection)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveAvatar(avatarResId: Int) {
        prefs.edit { putInt("selected_avatar", avatarResId) }
    }

    private fun loadSavedAvatar() {
        val savedAvatar = prefs.getInt("selected_avatar", avatar2)
        ivProfileAvatar.setImageResource(savedAvatar)
    }
}
