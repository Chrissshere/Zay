package com.chrissyx.zay.network

import com.chrissyx.zay.config.InstagramConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

data class InstagramUser(
    val id: String,
    val username: String,
    val accountType: String? = null
)

class InstagramAuthService {
    
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    fun getAuthorizationUrl(): String {
        return InstagramConfig.getAuthorizationUrl()
    }
    
    suspend fun exchangeCodeForToken(code: String): String? = withContext(Dispatchers.IO) {
        try {
            
            val formBody = FormBody.Builder()
                .add("client_id", InstagramConfig.CLIENT_ID)
                .add("client_secret", InstagramConfig.CLIENT_SECRET)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", InstagramConfig.REDIRECT_URI)
                .add("code", code)
                .build()
            
            val request = Request.Builder()
                .url(InstagramConfig.TOKEN_URL)
                .post(formBody)
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            
            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                
                // Handle both old and new response formats
                val accessToken = if (json.has("data")) {
                    // New format with data array
                    val dataArray = json.getJSONArray("data")
                    if (dataArray.length() > 0) {
                        dataArray.getJSONObject(0).getString("access_token")
                    } else {
                        null
                    }
                } else {
                    // Direct format
                    json.optString("access_token").takeIf { it.isNotEmpty() }
                }
                
                if (accessToken != null) {
                    accessToken
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getUserProfile(accessToken: String): InstagramUser? = withContext(Dispatchers.IO) {
        try {
            
            // Use the Instagram Graph API to get user info
            val url = "${InstagramConfig.USER_INFO_URL}?fields=id,username,account_type&access_token=$accessToken"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            
            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                
                val user = InstagramUser(
                    id = json.getString("id"),
                    username = json.getString("username"),
                    accountType = json.optString("account_type", "business")
                )
                user
            } else {
                
                // Fallback: try to get basic user info
                val fallbackUrl = "${InstagramConfig.USER_INFO_URL}?access_token=$accessToken"
                val fallbackRequest = Request.Builder()
                    .url(fallbackUrl)
                    .addHeader("Accept", "application/json")
                    .build()
                
                val fallbackResponse = client.newCall(fallbackRequest).execute()
                val fallbackBody = fallbackResponse.body?.string()
                
                
                if (fallbackResponse.isSuccessful && fallbackBody != null) {
                    val fallbackJson = JSONObject(fallbackBody)
                    val user = InstagramUser(
                        id = fallbackJson.getString("id"),
                        username = fallbackJson.optString("username", "user_${fallbackJson.getString("id")}"),
                        accountType = "instagram_business_verified"
                    )
                    user
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun verifyUsername(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simple check to see if Instagram username exists
            val url = "https://www.instagram.com/$username/"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:91.0) Gecko/91.0 Firefox/91.0")
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful && response.code != 404
        } catch (e: Exception) {
            false
        }
    }
} 