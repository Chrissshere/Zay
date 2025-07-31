package com.chrissyx.zay.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.*
import kotlin.random.Random

class LinkSecurityManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "zay_link_security"
        private const val TOKEN_EXPIRY_HOURS = 24
    }
    
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        PREFS_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Generate a temporary access token for a user's profile link
     * This token expires after 24 hours and can only be used once
     */
    fun generateSecureToken(username: String): String {
        val token = generateRandomToken()
        val expiryTime = System.currentTimeMillis() + (TOKEN_EXPIRY_HOURS * 60 * 60 * 1000)
        
        // Store token with expiry and username
        sharedPreferences.edit()
            .putString("token_$token", username)
            .putLong("expiry_$token", expiryTime)
            .putBoolean("used_$token", false)
            .apply()
        
        return token
    }
    
    /**
     * Validate and consume a security token
     * Returns the username if valid, null if invalid/expired/used
     */
    fun validateAndConsumeToken(token: String): String? {
        try {
            val username = sharedPreferences.getString("token_$token", null)
            val expiryTime = sharedPreferences.getLong("expiry_$token", 0)
            val isUsed = sharedPreferences.getBoolean("used_$token", true)
            
            if (username == null || isUsed) {
                return null
            }
            
            if (System.currentTimeMillis() > expiryTime) {
                // Clean up expired token
                cleanupToken(token)
                return null
            }
            
            // Mark token as used (one-time use)
            sharedPreferences.edit()
                .putBoolean("used_$token", true)
                .apply()
            
            return username
            
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Generate the secure URL with token
     */
    fun generateSecureUrl(username: String): String {
        val token = generateSecureToken(username)
        return "zay://profile/$username?token=$token"
    }
    
    /**
     * Clean up expired tokens (should be called periodically)
     */
    fun cleanupExpiredTokens() {
        try {
            val allPrefs = sharedPreferences.all
            val currentTime = System.currentTimeMillis()
            
            allPrefs.keys.filter { it.startsWith("expiry_") }.forEach { expiryKey ->
                val expiryTime = allPrefs[expiryKey] as? Long ?: 0
                if (currentTime > expiryTime) {
                    val token = expiryKey.removePrefix("expiry_")
                    cleanupToken(token)
                }
            }
        } catch (e: Exception) {
        }
    }
    
    private fun cleanupToken(token: String) {
        sharedPreferences.edit()
            .remove("token_$token")
            .remove("expiry_$token")
            .remove("used_$token")
            .apply()
    }
    
    private fun generateRandomToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
} 