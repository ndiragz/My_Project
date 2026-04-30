package com.example.lostandfoundapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lostandfoundapp.data.AppDatabase
import com.example.lostandfoundapp.model.User
import com.example.lostandfoundapp.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val prefs = application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private val _isCheckingAuth = MutableStateFlow(true)
    val isCheckingAuth = _isCheckingAuth.asStateFlow()

    // Theme state
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val ADMIN_EMAIL = "ryanmacharia@gmail.com"
    private val ADMIN_PASS = "1234"

    init {
        checkPersistedAuth()
    }

    private fun checkPersistedAuth() {
        viewModelScope.launch {
            try {
                val savedEmail = prefs.getString("user_email", null)
                if (savedEmail != null) {
                    val normalizedEmail = savedEmail.trim().lowercase()
                    var user = userDao.getUserByEmail(normalizedEmail)
                    if (user != null) {
                        // Ensure admin role for specific account on session restore
                        if (normalizedEmail == ADMIN_EMAIL && user.role != UserRole.ADMIN) {
                            userDao.updateUserRole(user.email, UserRole.ADMIN)
                            user = user.copy(role = UserRole.ADMIN)
                        }
                        _currentUser.value = user
                    } else if (normalizedEmail == ADMIN_EMAIL) {
                        // If it's the admin but not in DB yet
                        val admin = User(ADMIN_EMAIL, "Admin Ryan", ADMIN_PASS, UserRole.ADMIN)
                        try { userDao.registerUser(admin) } catch (e: Exception) {}
                        _currentUser.value = admin
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCheckingAuth.value = false
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val normalizedEmail = email.trim().lowercase()
                
                // Special check for Admin account
                if (normalizedEmail == ADMIN_EMAIL && pass == ADMIN_PASS) {
                    var user = userDao.getUserByEmail(normalizedEmail)
                    if (user == null) {
                        // Seed the admin user if it doesn't exist
                        val adminUser = User(normalizedEmail, "Admin Ryan", pass, UserRole.ADMIN)
                        try {
                            userDao.registerUser(adminUser)
                        } catch (e: Exception) {
                            // Already exists or other DB error, try to fetch again
                            user = userDao.getUserByEmail(normalizedEmail)
                        }
                        if (user == null) user = adminUser
                    }
                    
                    val finalUser = user!!.copy(role = UserRole.ADMIN, password = ADMIN_PASS)
                    saveUserEmail(finalUser.email)
                    _currentUser.value = finalUser
                    _authState.value = AuthState.Success
                    return@launch
                }

                val user = userDao.getUserByEmail(normalizedEmail)
                if (user != null && user.password == pass) {
                    saveUserEmail(user.email)
                    _currentUser.value = user
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Invalid email or password")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login failed: ${e.message}")
            }
        }
    }

    fun register(name: String, email: String, pass: String, profilePicUri: String? = null) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val normalizedEmail = email.trim().lowercase()
                val existingUser = userDao.getUserByEmail(normalizedEmail)
                if (existingUser != null) {
                    _authState.value = AuthState.Error("Email already registered")
                } else {
                    // Set role to ADMIN if registering the special account
                    val role = if (normalizedEmail == ADMIN_EMAIL) UserRole.ADMIN else UserRole.USER
                    val newUser = User(normalizedEmail, name, pass, role, profilePicUri)
                    userDao.registerUser(newUser)
                    saveUserEmail(newUser.email)
                    _currentUser.value = newUser
                    _authState.value = AuthState.Success
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Registration failed: ${e.message}")
            }
        }
    }

    private fun saveUserEmail(email: String) {
        prefs.edit().putString("user_email", email).apply()
    }

    fun logout() {
        prefs.edit().remove("user_email").apply()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
