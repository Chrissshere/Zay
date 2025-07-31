package com.chrissyx.zay.utils

import android.content.Context
import android.provider.Settings
import com.chrissyx.zay.data.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class DeviceAuthManager(
    private val context: Context,
    private val firebaseRepository: FirebaseRepository
) {
    
    /**
     * Get a unique device identifier that persists across app reinstalls
     * Uses Android ID which is unique per device and app combination
     */
    fun getDeviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        // Hash the Android ID for additional security
        return hashString(androidId ?: "unknown_device")
    }
    
    /**
     * Get device info for display purposes
     */
    fun getDeviceInfo(): String {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        val version = android.os.Build.VERSION.RELEASE
        return "$manufacturer $model (Android $version)"
    }
    
    /**
     * Get actual device info for the current device
     */
    fun getRealDeviceInfo(): String {
        return try {
            val manufacturer = android.os.Build.MANUFACTURER
            val model = android.os.Build.MODEL
            
            // Clean up the device name
            val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) {
                model
            } else {
                "$manufacturer $model"
            }
            
            deviceName.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        } catch (e: Exception) {
            "Android Device"
        }
    }
    
    /**
     * Generate random device info for anonymous messaging (for non-Pro users)
     */
    fun generateRandomDeviceInfo(): String {
        val devices = listOf(
            "iPhone 15 Pro",
            "Samsung Galaxy S24",
            "Google Pixel 8",
            "OnePlus 12",
            "Xiaomi 14",
            "iPhone 14",
            "Samsung Galaxy A54",
            "Google Pixel 7a",
            "Nothing Phone 2",
            "Sony Xperia 1 V",
            "iPhone 13 mini",
            "Samsung Galaxy Z Flip5",
            "Motorola Edge 40",
            "Realme GT3",
            "OPPO Find X6"
        )
        
        val random = kotlin.random.Random(System.currentTimeMillis())
        return devices[random.nextInt(devices.size)]
    }
    
    /**
     * Get device info for messaging - real device for Pro users, random for non-Pro
     */
    fun getDeviceInfoForMessage(isProUser: Boolean): String {
        return if (isProUser) {
            getRealDeviceInfo()
        } else {
            generateRandomDeviceInfo()
        }
    }
    
    /**
     * Check if current device is trusted for the given username
     */
    suspend fun isDeviceTrusted(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceId()
                val user = firebaseRepository.getUserByUsername(username)
                user?.trustedDevices?.contains(deviceId) == true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Add current device to trusted devices for the user
     */
    suspend fun trustCurrentDevice(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceId()
                val deviceInfo = getDeviceInfo()
                firebaseRepository.addTrustedDevice(username, deviceId, deviceInfo)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Remove current device from trusted devices
     */
    suspend fun untrustCurrentDevice(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceId()
                firebaseRepository.removeTrustedDevice(username, deviceId)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Get all trusted devices for a user (for settings/management)
     */
    suspend fun getTrustedDevices(username: String): List<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val user = firebaseRepository.getUserByUsername(username)
                user?.trustedDeviceInfo?.toList() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}