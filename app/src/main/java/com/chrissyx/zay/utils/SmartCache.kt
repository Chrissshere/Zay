package com.chrissyx.zay.utils

import android.content.Context
import android.content.SharedPreferences

class SmartCache(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_cache", Context.MODE_PRIVATE)
    
    companion object {
        private const val CACHE_DURATION = 3 * 60 * 1000L // 3 minutes in milliseconds
        private const val LAST_LOAD_PREFIX = "last_load_"
        private const val FORCE_REFRESH_PREFIX = "force_refresh_"
    }
    
    /**
     * Check if data should be refreshed for a given key
     * Returns true if:
     * - Never loaded before
     * - Last load was more than 3 minutes ago
     * - Force refresh was requested
     */
    fun shouldRefresh(key: String): Boolean {
        val lastLoadTime = prefs.getLong("$LAST_LOAD_PREFIX$key", 0L)
        val forceRefresh = prefs.getBoolean("$FORCE_REFRESH_PREFIX$key", false)
        val currentTime = System.currentTimeMillis()
        
        // Clear force refresh flag if it was set
        if (forceRefresh) {
            prefs.edit().putBoolean("$FORCE_REFRESH_PREFIX$key", false).apply()
            return true
        }
        
        // Check if cache expired (3 minutes)
        return (currentTime - lastLoadTime) > CACHE_DURATION
    }
    
    /**
     * Mark data as loaded for a given key
     */
    fun markAsLoaded(key: String) {
        prefs.edit().putLong("$LAST_LOAD_PREFIX$key", System.currentTimeMillis()).apply()
    }
    
    /**
     * Force refresh on next load for a given key
     */
    fun forceRefresh(key: String) {
        prefs.edit().putBoolean("$FORCE_REFRESH_PREFIX$key", true).apply()
    }
    
    /**
     * Get last load time for debugging
     */
    fun getLastLoadTime(key: String): Long {
        return prefs.getLong("$LAST_LOAD_PREFIX$key", 0L)
    }
    
    /**
     * Clear all cache data
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Common cache keys
     */
    object Keys {
        const val EXPLORE_PROFILES = "explore_profiles"
        const val USER_PROFILE = "user_profile"
        const val SUPPORT_TICKETS = "support_tickets"
        const val ADMIN_USERS = "admin_users"
        const val INBOX_MESSAGES = "inbox_messages"
    }
}