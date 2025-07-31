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

data class CreateExploreProfileUiState(
    val isLoading: Boolean = false,
    val prompt: String = "",
    val bannerImageUri: String? = null,
    val isUploadingBanner: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isEditMode: Boolean = false,
    val originalProfile: ExploreProfile? = null
)

class CreateExploreProfileViewModel(application: Application) : AndroidViewModel(application) {
    
    private val firebaseRepository = FirebaseRepository()
    private val userPreferences = UserPreferences(application)
    
    private val _uiState = MutableStateFlow(CreateExploreProfileUiState())
    val uiState: StateFlow<CreateExploreProfileUiState> = _uiState.asStateFlow()
    
    fun initializeForEdit(profile: ExploreProfile) {
        _uiState.value = _uiState.value.copy(
            isEditMode = true,
            originalProfile = profile,
            prompt = profile.prompt,
            bannerImageUri = profile.bannerImageURL
        )
    }
    
    fun updatePrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(prompt = prompt)
    }
    
    fun uploadBannerImage(imageUri: String) {
        _uiState.value = _uiState.value.copy(isUploadingBanner = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                
                // Convert URI to ByteArray for upload
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(imageUri))
                val originalBytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (originalBytes != null) {
                    
                    // Compress the image before upload
                    val compressedBytes = compressImageBytes(originalBytes)
                    
                    val downloadUrl = firebaseRepository.uploadBannerImage(
                        userPreferences.username.orEmpty(),
                        compressedBytes
                    )
                    
                    if (downloadUrl != null) {
                        _uiState.value = _uiState.value.copy(
                            bannerImageUri = downloadUrl,
                            isUploadingBanner = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isUploadingBanner = false,
                            errorMessage = "Failed to upload banner image"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isUploadingBanner = false,
                        errorMessage = "Failed to read image file"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isUploadingBanner = false,
                    errorMessage = "Error uploading image: ${e.message}"
                )
            }
        }
    }
    
    private fun compressImageBytes(imageBytes: ByteArray): ByteArray {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            // Resize if too large (max 1024px on longest side)
            val resizedBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
                val scaleFactor = 1024.0f / maxOf(bitmap.width, bitmap.height)
                val newWidth = (bitmap.width * scaleFactor).toInt()
                val newHeight = (bitmap.height * scaleFactor).toInt()
                android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }
            
            val outputStream = java.io.ByteArrayOutputStream()
            resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val compressedBytes = outputStream.toByteArray()
            
            // Clean up
            if (resizedBitmap != bitmap) {
                bitmap.recycle()
            }
            resizedBitmap.recycle()
            
            compressedBytes
        } catch (e: Exception) {
            imageBytes // Return original if compression fails
        }
    }
    
    fun saveProfile() {
        val currentState = _uiState.value
        val username = userPreferences.username
        
        if (username.isEmpty()) {
            _uiState.value = currentState.copy(errorMessage = "User not logged in")
            return
        }
        
        if (currentState.prompt.trim().isEmpty()) {
            _uiState.value = currentState.copy(errorMessage = "Prompt is required")
            return
        }
        
        _uiState.value = currentState.copy(isSaving = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                val profile = ExploreProfile(
                    username = username,
                    prompt = currentState.prompt.trim(),
                    bannerImageURL = currentState.bannerImageUri ?: "",
                    isActive = true,
                    createdAt = System.currentTimeMillis().toDouble(),
                    lastUpdated = System.currentTimeMillis().toDouble(),
                    tags = extractHashtags(currentState.prompt),
                    location = null, // Can be added later
                    isVerified = false, // Will be set based on user verification status
                    isPro = userPreferences.isPro // Include pro status for prioritization
                )
                
                val success = if (currentState.isEditMode) {
                    firebaseRepository.updateExploreProfile(profile)
                } else {
                    firebaseRepository.createExploreProfile(profile)
                }
                
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isSuccess = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Failed to save profile. Please try again."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Error saving profile: ${e.message}"
                )
            }
        }
    }
    
    private fun extractHashtags(text: String): List<String> {
        val hashtagPattern = "#\\w+".toRegex()
        return hashtagPattern.findAll(text)
            .map { it.value.lowercase() }
            .toList()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
} 