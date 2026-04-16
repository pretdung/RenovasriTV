package com.example.renovasritv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class DeviceFlowStarted(val userCode: String, val verificationUri: String) : AuthState()
    data class Authenticated(val email: String?, val avatarUrl: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()
    
    private var pollingJob: Job? = null

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            SupabaseConfig.client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        _authState.value = AuthState.Authenticated(
                            email = user?.email,
                            avatarUrl = user?.userMetadata?.get("avatar_url")?.toString()?.trim('\"')
                        )
                        pollingJob?.cancel()
                    }
                    is SessionStatus.NotAuthenticated -> {
                        if (_authState.value !is AuthState.DeviceFlowStarted) {
                            _authState.value = AuthState.Idle
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun startGoogleDeviceFlow() {
        pollingJob?.cancel()
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Simulate fetching device code from Edge Function
                delay(1000)
                val userCode = "RENO-${(1000..9999).random()}"
                val verificationUri = "https://renovasri.com/tv-auth"
                
                _authState.value = AuthState.DeviceFlowStarted(userCode, verificationUri)
                
                // Start polling for session
                startPolling()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    private fun startPolling() {
        pollingJob = viewModelScope.launch {
            repeat(60) { // Poll for 5 minutes
                delay(5000)
                try {
                    SupabaseConfig.client.auth.refreshCurrentSession()
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
            }
            if (_authState.value is AuthState.DeviceFlowStarted) {
                _authState.value = AuthState.Error("Session timeout. Please try again.")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                SupabaseConfig.client.auth.signOut()
            } catch (e: Exception) {
                // Ignore sign out errors or handle them
            }
            _authState.value = AuthState.Idle
        }
    }
}
