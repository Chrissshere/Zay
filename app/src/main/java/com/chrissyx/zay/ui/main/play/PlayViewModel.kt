package com.chrissyx.zay.ui.main.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.network.TinyUrlService
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val tinyUrlService = TinyUrlService()
    
    private val _uiState = MutableStateFlow(PlayUiState())
    val uiState: StateFlow<PlayUiState> = _uiState.asStateFlow()

    fun forceReloadUserData() {
        loadUserData()
    }
    
    fun loadUserData() {
        val username = userPreferences.username ?: ""
        val prompt = userPreferences.prompt ?: "send me anonymous messages!"
        
        
        _uiState.value = _uiState.value.copy(
            username = username,
            prompt = prompt
        )
        
        // Fetch latest data from Firebase
        viewModelScope.launch {
            val user = firebaseRepository.getUserByUsername(username)
            user?.let {
                
                _uiState.value = _uiState.value.copy(
                    prompt = it.prompt,
                    profilePictureURL = it.profilePictureURL
                )
                // Update local cache
                userPreferences.prompt = it.prompt
            } ?: run {
            }
        }
    }

    fun generateShareableLink(): String? {
        val username = userPreferences.username ?: return null
        return "zay://profile/$username"
    }

    fun generateTinyUrl(onResult: (String?) -> Unit) {
        val username = userPreferences.username ?: return
        
        _uiState.value = _uiState.value.copy(
            isGeneratingLink = true,
            tinyUrlError = false
        )
        
        viewModelScope.launch {
            val tinyUrl = tinyUrlService.createShortUrl(username)
            
            _uiState.value = _uiState.value.copy(
                isGeneratingLink = false,
                generatedTinyUrl = tinyUrl,
                tinyUrlError = tinyUrl == null
            )
            
            onResult(tinyUrl)
        }
    }

    fun startEditingPrompt() {
        _uiState.value = _uiState.value.copy(isEditingPrompt = true)
    }

    fun cancelEditingPrompt() {
        _uiState.value = _uiState.value.copy(isEditingPrompt = false)
    }

    fun savePrompt(newPrompt: String) {
        val username = userPreferences.username ?: return
        
        _uiState.value = _uiState.value.copy(
            isSavingPrompt = true,
            isEditingPrompt = false
        )
        
        viewModelScope.launch {
            val success = firebaseRepository.updateUserPrompt(username, newPrompt)
            
            if (success) {
                _uiState.value = _uiState.value.copy(
                    prompt = newPrompt,
                    isSavingPrompt = false,
                    successMessage = "Prompt updated! ✨"
                )
                userPreferences.prompt = newPrompt
            } else {
                _uiState.value = _uiState.value.copy(
                    isSavingPrompt = false,
                    errorMessage = "Failed to update prompt. Please try again."
                )
            }
        }
    }

    fun uploadProfilePicture(imageBytes: ByteArray) {
        val username = userPreferences.username ?: return
        
        _uiState.value = _uiState.value.copy(isUploadingImage = true)
        
        viewModelScope.launch {
            val imageUrl = firebaseRepository.uploadProfilePicture(username, imageBytes)
            
            if (imageUrl != null) {
                val success = firebaseRepository.updateUserProfilePicture(username, imageUrl)
                
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        profilePictureURL = imageUrl,
                        isUploadingImage = false,
                        successMessage = "Profile picture updated! 📸"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isUploadingImage = false,
                        errorMessage = "Failed to save profile picture. Please try again."
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isUploadingImage = false,
                    errorMessage = "Failed to upload image. Please try again."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

data class PlayUiState(
    val username: String = "",
    val prompt: String = "send me anonymous messages!",
    val profilePictureURL: String? = null,
    val isEditingPrompt: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isSavingPrompt: Boolean = false,
    val isGeneratingLink: Boolean = false,
    val generatedTinyUrl: String? = null,
    val tinyUrlError: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) 