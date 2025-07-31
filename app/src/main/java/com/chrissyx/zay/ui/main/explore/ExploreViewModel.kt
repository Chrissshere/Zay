package com.chrissyx.zay.ui.main.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chrissyx.zay.data.models.ExploreProfile
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val profiles: List<ExploreProfile> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val hasExistingProfile: Boolean = false,
    val existingProfile: ExploreProfile? = null
)

class ExploreViewModel(application: Application) : AndroidViewModel(application) {
    
    private val firebaseRepository = FirebaseRepository()
    private val userPreferences = UserPreferences(application)
    
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()
    
    init {
        loadProfiles()
        checkForExistingProfile()
    }
    
    fun loadProfiles() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                val profiles = firebaseRepository.getAllExploreProfiles()
                
                // Sort profiles to prioritize pro users
                val sortedProfiles = profiles.sortedWith { profile1, profile2 ->
                    when {
                        // Pro users first
                        profile1.isPro && !profile2.isPro -> -1
                        !profile1.isPro && profile2.isPro -> 1
                        // If both are pro or both are not pro, sort by creation time (newest first)
                        else -> profile2.createdAt.compareTo(profile1.createdAt)
                    }
                }
                
                sortedProfiles.take(5).forEach { profile ->
                }
                
                _uiState.value = _uiState.value.copy(
                    profiles = sortedProfiles,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load explore profiles: ${e.message}"
                )
            }
        }
    }
    
    // Debug method to load all profiles including inactive ones
    fun loadAllProfilesDebug() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                val profiles = firebaseRepository.getAllExploreProfilesDebug()
                
                _uiState.value = _uiState.value.copy(
                    profiles = profiles,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Debug load failed: ${e.message}"
                )
            }
        }
    }
    
    private fun checkForExistingProfile() {
        val currentUsername = userPreferences.username
        if (currentUsername.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val existingProfile = firebaseRepository.getExploreProfileByUsername(currentUsername)
                    _uiState.value = _uiState.value.copy(
                        hasExistingProfile = existingProfile != null,
                        existingProfile = existingProfile
                    )
                } catch (e: Exception) {
                    // If we can't check, assume no existing profile
                    _uiState.value = _uiState.value.copy(hasExistingProfile = false)
                }
            }
        }
    }
    
    fun searchProfiles(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, isLoading = true)
        
        viewModelScope.launch {
            try {
                val profiles = if (query.isEmpty()) {
                    firebaseRepository.getAllExploreProfiles()
                } else {
                    firebaseRepository.searchExploreProfiles(query)
                }
                
                _uiState.value = _uiState.value.copy(
                    profiles = profiles,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to search profiles"
                )
            }
        }
    }
    
    fun refreshProfiles() {
        loadProfiles()
        checkForExistingProfile()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
} 