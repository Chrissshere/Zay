package com.chrissyx.zay.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chrissyx.zay.data.models.Platform
import com.chrissyx.zay.data.models.User
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.network.InstagramService
import com.chrissyx.zay.utils.DeviceHelper
import com.chrissyx.zay.utils.DeviceAuthManager
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class AuthViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val instagramService: InstagramService,
    private val deviceHelper: DeviceHelper,
    private val userPreferences: UserPreferences,
    private val deviceAuthManager: DeviceAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // Instagram login launcher - to be set by the fragment
    var instagramLoginLauncher: ActivityResultLauncher<Intent>? = null
    
    // Snapchat login launcher - to be set by the fragment
    var snapchatLoginLauncher: ActivityResultLauncher<Intent>? = null

    init {
        ensureAdminAccount()
    }
    
    private fun ensureAdminAccount() {
        viewModelScope.launch {
            try {
                // Check if your admin account exists
                val adminUsername = "_c_ssyx" // Your username
                val existingUser = firebaseRepository.getUserByUsername(adminUsername)
                
                if (existingUser != null && existingUser.role != "admin") {
                    // Update existing user to admin
                    val updatedUser = existingUser.copy(
                        role = "admin",
                        isPro = true
                    )
                    firebaseRepository.createOrUpdateUser(updatedUser)
                } else if (existingUser == null) {
                    // Create admin account if it doesn't exist
                    val adminUser = User(
                        username = adminUsername,
                        token = userPreferences.generateToken(),
                        deviceID = deviceHelper.getDeviceId(),
                        platform = Platform.INSTAGRAM.displayName,
                        role = "admin",
                        isPro = true,
                        prompt = "send me anonymous messages!"
                    )
                    firebaseRepository.createOrUpdateUser(adminUser)
                }
            } catch (e: Exception) {
            }
        }
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(
            username = username,
            errorMessage = null
        )
    }

    fun updatePlatform(platform: Platform) {
        _uiState.value = _uiState.value.copy(selectedPlatform = platform)
    }

    fun handleContinue() {
        val currentState = _uiState.value
        val trimmedUsername = currentState.username.trim()
        
        if (trimmedUsername.isEmpty()) {
            _uiState.value = currentState.copy(errorMessage = "Please enter a username.")
            return
        }

        _uiState.value = currentState.copy(
            isLoading = true,
            errorMessage = null,
            loginSuccess = false
        )

        viewModelScope.launch {
            try {
                // Check if user already exists on the selected platform
                val existingUser = firebaseRepository.getUserByUsernameAndPlatform(
                    trimmedUsername, 
                    currentState.selectedPlatform.displayName
                )
                
                if (existingUser != null) {
                    // User exists - check if device is trusted
                    val isDeviceTrusted = deviceAuthManager.isDeviceTrusted(trimmedUsername)
                    
                    if (isDeviceTrusted) {
                        // Device is trusted - auto-login without verification
                        saveLoginData(trimmedUsername, existingUser)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            loginSuccess = true,
                            showAccountVerification = false
                        )
                    } else {
                        // Device not trusted - show verification dialog
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showAccountVerification = true,
                            existingAccountUsername = trimmedUsername
                        )
                    }
                } else {
                    // Check if username exists on other platforms for informational purposes
                    val usersOnOtherPlatforms = firebaseRepository.getUsersWithUsername(trimmedUsername)
                    val platformsWithUser = usersOnOtherPlatforms.map { it.platform }
                    
                    if (platformsWithUser.isNotEmpty()) {
                    }
                    
                    // New user on this platform - proceed with creation
                    createNewUser(trimmedUsername, currentState.selectedPlatform)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error checking user account: ${e.message}"
                )
            }
        }
    }
    
    // Instagram login methods
    fun startInstagramVerification() {
        
        // Launch Instagram verification activity if launcher is available
        instagramLoginLauncher?.let { launcher ->
            try {
                val intent = Intent().apply {
                    setClassName("com.chrissyx.zay", "com.chrissyx.zay.ui.auth.InstagramLoginActivity")
                }
                launcher.launch(intent)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Unable to start Instagram verification. Please try again."
                )
            }
        } ?: run {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Instagram verification not available. Please try again."
            )
        }
    }
    
    fun handleInstagramLoginResult(data: Intent?) {
        val success = data?.getBooleanExtra(InstagramLoginActivity.EXTRA_SUCCESS, false) ?: false
        
        if (success) {
            val instagramUsername = data?.getStringExtra(InstagramLoginActivity.EXTRA_USERNAME)
            val instagramId = data?.getStringExtra(InstagramLoginActivity.EXTRA_INSTAGRAM_ID)
            
            if (instagramUsername != null) {
                handleInstagramVerificationSuccess(instagramUsername, instagramId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to get Instagram username"
                )
            }
        } else {
            val error = data?.getStringExtra(InstagramLoginActivity.EXTRA_ERROR) ?: "Instagram login was cancelled"
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = error
            )
        }
    }
    
    private fun handleInstagramVerificationSuccess(instagramUsername: String, instagramId: String?) {
        viewModelScope.launch {
            val expectedUsername = _uiState.value.existingAccountUsername
            
            // Check if Instagram username matches the expected username
            if (instagramUsername.equals(expectedUsername, ignoreCase = true)) {
                // Verification successful - show trust device dialog
                val user = firebaseRepository.getUserByUsernameWithFallback(expectedUsername!!, "INSTAGRAM")
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showAccountVerification = false,
                        showTrustDeviceDialog = true,
                        pendingTrustUser = user
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Account verification failed. Please try again."
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Instagram username doesn't match. Expected: @$expectedUsername, Got: @$instagramUsername"
                )
            }
        }
    }
    
    // Snapchat login methods
    fun startSnapchatVerification() {
        
        // Launch Snapchat verification activity if launcher is available
        snapchatLoginLauncher?.let { launcher ->
            try {
                val intent = Intent().apply {
                    setClassName("com.chrissyx.zay", "com.chrissyx.zay.ui.auth.SnapchatLoginActivity")
                }
                launcher.launch(intent)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Unable to start Snapchat verification. Please try again."
                )
            }
        } ?: run {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Snapchat verification not available. Please try again."
            )
        }
    }
    
    fun handleSnapchatLoginResult(data: Intent?) {
        val resultCode = data?.extras?.keySet()?.contains("snapchat_username") ?: false
        
        if (resultCode) {
            val snapchatUsername = data?.getStringExtra("snapchat_username")
            val snapchatExternalId = data?.getStringExtra("snapchat_external_id")
            
            if (snapchatUsername != null) {
                handleSnapchatVerificationSuccess(snapchatUsername, snapchatExternalId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to get Snapchat username"
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Snapchat login was cancelled"
            )
        }
    }
    
    private fun handleSnapchatVerificationSuccess(snapchatUsername: String, snapchatExternalId: String?) {
        viewModelScope.launch {
            val expectedUsername = _uiState.value.existingAccountUsername
            
            // Check if Snapchat username matches the expected username
            if (snapchatUsername.equals(expectedUsername, ignoreCase = true)) {
                // Verification successful - show trust device dialog
                val user = firebaseRepository.getUserByUsernameWithFallback(expectedUsername!!, "SNAPCHAT")
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showAccountVerification = false,
                        showTrustDeviceDialog = true,
                        pendingTrustUser = user
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Account verification failed. Please try again."
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Snapchat username doesn't match. Expected: @$expectedUsername, Got: @$snapchatUsername"
                )
            }
        }
    }

    private suspend fun createNewUser(username: String, platform: Platform) {
        val token = userPreferences.generateToken()
        val newUser = User(
            username = username,
            token = token,
            deviceID = deviceHelper.getDeviceId(),
            platform = platform.displayName
        )

        // Use platform-specific creation to allow same username across platforms
        val success = firebaseRepository.createOrUpdateUserWithPlatform(newUser)
        if (success) {
            // Trust this device for the new user
            deviceAuthManager.trustCurrentDevice(username)
            
            saveLoginData(username, newUser)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                loginSuccess = true
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Failed to create account. Please try again."
            )
        }
    }

    private fun generateAlternativeUsername(originalUsername: String): String {
        val baseUsername = originalUsername.substringBefore("@")
        val randomNumber = Random.nextInt(100, 999)
        return "$baseUsername$randomNumber"
    }

    private fun saveLoginData(username: String, user: User) {
        userPreferences.username = username
        userPreferences.token = user.token
        userPreferences.platform = user.platform
        userPreferences.prompt = user.prompt
        userPreferences.role = user.role
        userPreferences.isPro = user.isPro
        
    }

    fun handleCreateNewAccount() {
        // User wants to create a new account with numbers
        val currentState = _uiState.value
        val baseUsername = currentState.existingAccountUsername ?: ""
        val newUsername = generateAlternativeUsername(baseUsername)
        
        _uiState.value = currentState.copy(
            showAccountVerification = false,
            username = newUsername,
            isLoading = true
        )
        
        viewModelScope.launch {
            createNewUser(newUsername, currentState.selectedPlatform)
        }
    }
    
    fun handleDevBypass(username: String) {
        // Development bypass - login without Instagram verification
        viewModelScope.launch {
            val user = firebaseRepository.getUserByUsername(username)
            if (user != null) {
                userPreferences.username = user.username
                userPreferences.isPro = user.isPro
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loginSuccess = true,
                    showAccountVerification = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Account not found"
                )
            }
        }
    }
    
    fun dismissVerificationDialog() {
        _uiState.value = _uiState.value.copy(
            showAccountVerification = false,
            existingAccountUsername = ""
        )
    }

    fun handleVerifyAccount() {
        val currentState = _uiState.value
        val username = currentState.existingAccountUsername
        
        if (username.isEmpty()) return
        
        // For personal accounts that can't use Instagram Business API,
        // offer manual verification option
        _uiState.value = currentState.copy(
            showAccountVerification = false,
            showManualVerification = true,
            existingAccountUsername = username
        )
    }
    
    fun handleManualVerification(contactMethod: String) {
        val currentState = _uiState.value
        val username = currentState.existingAccountUsername
        
        if (username.isEmpty()) return
        
        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                // Create a verification request in Firebase
                val verificationRequest = mapOf(
                    "username" to username,
                    "contactMethod" to contactMethod,
                    "requestedAt" to System.currentTimeMillis() / 1000.0,
                    "status" to "pending",
                    "deviceId" to deviceHelper.getDeviceId(),
                    "deviceModel" to deviceHelper.getDeviceModel()
                )
                
                firebaseRepository.createVerificationRequest(username, verificationRequest)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showManualVerification = false,
                    verificationRequestSent = true,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to send verification request: ${e.message}"
                )
            }
        }
    }
    
    fun dismissManualVerification() {
        _uiState.value = _uiState.value.copy(
            showManualVerification = false,
            verificationRequestSent = false
        )
    }

    fun logout() {
        userPreferences.logout()
    }

    // TikTok integration - TODO: Implement later
    
    fun handleTrustDeviceDecision(trust: Boolean) {
        val user = _uiState.value.pendingTrustUser ?: return
        
        viewModelScope.launch {
            if (trust) {
                // Trust this device for future logins
                deviceAuthManager.trustCurrentDevice(user.username)
            }
            
            // Complete login regardless of trust decision
            saveLoginData(user.username, user)
            
            _uiState.value = _uiState.value.copy(
                showTrustDeviceDialog = false,
                pendingTrustUser = null,
                loginSuccess = true
            )
        }
    }
}

data class AuthUiState(
    val username: String = "",
    val selectedPlatform: Platform = Platform.INSTAGRAM,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val showAccountVerification: Boolean = false,
    val existingAccountUsername: String = "",
    val showManualVerification: Boolean = false,
    val verificationRequestSent: Boolean = false,
    val showTrustDeviceDialog: Boolean = false,
    val pendingTrustUser: User? = null
) 