package com.epfl.esl.simongame.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epfl.esl.simongame.data.FirebaseUserRepository
import com.epfl.esl.simongame.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data object Unregistered : AuthState
    data object Registered : AuthState
}

class AuthViewModel(private val repo: UserRepository) : ViewModel() {

    val state: StateFlow<AuthState> = repo.currentUser
        .map { user -> if (user == null) AuthState.Unregistered else AuthState.Registered }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun authenticate(username: String, email: String, password: String, isLogin: Boolean) {
        val firebaseRepo = repo as? FirebaseUserRepository
        if (firebaseRepo == null) {
            viewModelScope.launch {
                repo.register(username, email)
                repo.addSampleChallengesIfNeeded()
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = if (isLogin) {
                firebaseRepo.signIn(email, password)
            } else {
                firebaseRepo.signUp(email, password, username)
            }

            result.fold(
                onSuccess = {
                    _errorMessage.value = null
                    repo.addSampleChallengesIfNeeded()
                },
                onFailure = { exception ->
                    _errorMessage.value = parseFirebaseError(exception)
                }
            )

            _isLoading.value = false
        }
    }

    fun register(username: String, email: String) {
        viewModelScope.launch {
            repo.register(username, email)
            repo.addSampleChallengesIfNeeded()
        }
    }

    fun logout() {
        viewModelScope.launch { repo.logout() }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun parseFirebaseError(exception: Throwable): String {
        val message = exception.message ?: return "An error occurred"
        return when {
            "email address is badly formatted" in message -> "Invalid email format"
            "password is invalid" in message -> "Wrong password"
            "no user record" in message.lowercase() -> "No account found with that email/username"
            "email address is already in use" in message -> "Email already registered"
            "password should be at least 6 characters" in message -> "Password too short"
            "network error" in message.lowercase() -> "Network error"
            "User has no email" in message -> "Account error, contact support"
            else -> "Authentication failed"
        }
    }
}