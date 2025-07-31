package com.chrissyx.zay.ui.messaging

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chrissyx.zay.data.models.Message
import com.chrissyx.zay.data.models.SenderInfo
import com.chrissyx.zay.data.models.User
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.utils.DeviceAuthManager
import com.chrissyx.zay.utils.DeviceHelper
import com.chrissyx.zay.utils.DeviceUtils
import com.chrissyx.zay.utils.UserPreferences
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MessageViewModel(
    private val userPreferences: UserPreferences,
    private val context: Context
) : ViewModel() {

    private val firebaseRepository = FirebaseRepository()
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val deviceAuthManager = DeviceAuthManager(context, firebaseRepository)
    
    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    fun loadUserInfo(username: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val user = firebaseRepository.getUserByUsername(username)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userInfo = user
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load user info"
                )
            }
        }
    }

    fun sendMessage(recipientUsername: String, messageText: String) {
        
        _uiState.value = _uiState.value.copy(isSending = true)
        
        viewModelScope.launch {
            try {
                // Get current location first
                val currentLocation = getCurrentLocation()
                
                // Update sender's location in Firebase
                updateSenderLocation(currentLocation)
                
                // Create message with appropriate device info based on sender's Pro status
                val currentUsername = userPreferences.username ?: "anonymous"
                val isProUser = userPreferences.isPro
                val deviceInfo = deviceAuthManager.getDeviceInfoForMessage(isProUser)
                
                
                val message = Message(
                    text = messageText,
                    timestamp = System.currentTimeMillis() / 1000.0, // Convert to seconds with decimal
                    sender = currentUsername, // Use actual sender username
                    device = deviceInfo // Real device for Pro users, random for non-Pro
                )
                
                
                val success = firebaseRepository.sendMessage(recipientUsername, message)
                
                
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        messageSent = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        errorMessage = "Failed to send message. Please try again."
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    errorMessage = "Failed to send message. Please try again."
                )
            }
        }
    }
    
    private suspend fun getCurrentLocation(): Location? {
        return try {
            // Check permissions
            val hasFineLocation = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (!hasFineLocation && !hasCoarseLocation) {
                return null
            }
            
            // Try to get last known location first
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                return lastLocation
            }
            
            val defaultLocation = Location("default").apply {
                latitude = 40.7128  // New York City as default
                longitude = -74.0060
            }
            return defaultLocation
            
        } catch (e: Exception) {
            // Return default location on error
            val defaultLocation = Location("default").apply {
                latitude = 40.7128  // New York City as default
                longitude = -74.0060
            }
            return defaultLocation
        }
    }
    
    private suspend fun updateSenderLocation(location: Location?) {
        val username = userPreferences.username ?: return
        
        try {
            // Always update device info, even without location
            val deviceModel = DeviceUtils.getCurrentDeviceModel()
            
            if (location != null) {
                // Update both location and device
                firebaseRepository.updateUserLocation(username, location.latitude, location.longitude, deviceModel)
            } else {
                // Update only device info when location is not available
                firebaseRepository.updateUserDevice(username, deviceModel)
            }
        } catch (e: Exception) {
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearMessageSent() {
        _uiState.value = _uiState.value.copy(messageSent = false)
    }
}

data class MessageUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val messageSent: Boolean = false,
    val userInfo: User? = null,
    val errorMessage: String? = null
) 