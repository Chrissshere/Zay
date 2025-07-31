package com.chrissyx.zay.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.UUID

class UserPreferences(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_TOKEN = "token"
        private const val KEY_IS_PRO = "is_pro"
        private const val KEY_PLATFORM = "platform"
        private const val KEY_PROMPT = "prompt"
        private const val KEY_ROLE = "role"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_SUBSCRIPTION_TYPE = "subscription_type"
        private const val KEY_SUBSCRIPTION_RENEWAL_DATE = "subscription_renewal_date"
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    var username: String
        get() = sharedPreferences.getString(KEY_USERNAME, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_USERNAME, value).apply()
    
    var isPro: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_PRO, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_IS_PRO, value).apply()
    
    var deviceId: String
        get() = sharedPreferences.getString(KEY_DEVICE_ID, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_DEVICE_ID, value).apply()
    
    var token: String?
        get() = sharedPreferences.getString(KEY_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_TOKEN, value).apply()
    
    var platform: String?
        get() = sharedPreferences.getString(KEY_PLATFORM, null)
        set(value) = sharedPreferences.edit().putString(KEY_PLATFORM, value).apply()
    
    var prompt: String?
        get() = sharedPreferences.getString(KEY_PROMPT, "send me anonymous messages!")
        set(value) = sharedPreferences.edit().putString(KEY_PROMPT, value).apply()
    
    var role: String
        get() = sharedPreferences.getString(KEY_ROLE, "user") ?: "user"
        set(value) = sharedPreferences.edit().putString(KEY_ROLE, value).apply()
    
    var subscriptionType: String?
        get() = sharedPreferences.getString(KEY_SUBSCRIPTION_TYPE, null)
        set(value) = sharedPreferences.edit().putString(KEY_SUBSCRIPTION_TYPE, value).apply()
    
    var subscriptionRenewalDate: String?
        get() = sharedPreferences.getString(KEY_SUBSCRIPTION_RENEWAL_DATE, null)
        set(value) = sharedPreferences.edit().putString(KEY_SUBSCRIPTION_RENEWAL_DATE, value).apply()
    
    fun isLoggedIn(): Boolean {
        return username.isNotEmpty()
    }
    
    fun generateToken(): String {
        val newToken = UUID.randomUUID().toString()
        token = newToken
        return newToken
    }
    
    fun logout() {
        sharedPreferences.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_TOKEN)
            .remove(KEY_PLATFORM)
            .remove(KEY_PROMPT)
            .apply()
    }
    
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
} 