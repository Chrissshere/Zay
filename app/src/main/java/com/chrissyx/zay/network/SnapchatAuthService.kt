package com.chrissyx.zay.network

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.chrissyx.zay.config.SnapchatConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

class SnapchatAuthService(private val context: Context) {
    
    data class SnapchatUser(
        val displayName: String,
        val externalId: String,
        val bitmojiAvatar: String? = null
    )
    
    data class PKCEParams(
        val codeVerifier: String,
        val codeChallenge: String,
        val state: String
    )
    
    fun generatePKCEParams(): PKCEParams {
        // Generate code verifier (43-128 characters)
        val codeVerifier = generateCodeVerifier()
        
        // Generate code challenge (SHA256 hash of verifier, base64url encoded)
        val codeChallenge = generateCodeChallenge(codeVerifier)
        
        // Generate state for CSRF protection
        val state = generateRandomString(32)
        
        return PKCEParams(codeVerifier, codeChallenge, state)
    }
    
    fun buildAuthorizationUrl(pkceParams: PKCEParams): String {
        val scopeString = SnapchatConfig.DEFAULT_SCOPES.joinToString(" ")
        
        val authUrl = Uri.Builder()
            .scheme("https")
            .authority("accounts.snapchat.com")
            .path("/accounts/oauth2/auth")
            .appendQueryParameter("response_type", SnapchatConfig.RESPONSE_TYPE_CODE)
            .appendQueryParameter("client_id", SnapchatConfig.CLIENT_ID)
            .appendQueryParameter("redirect_uri", SnapchatConfig.REDIRECT_URI)
            .appendQueryParameter("scope", scopeString)
            .appendQueryParameter("state", pkceParams.state)
            .appendQueryParameter("code_challenge", pkceParams.codeChallenge)
            .appendQueryParameter("code_challenge_method", SnapchatConfig.CODE_CHALLENGE_METHOD)
            .build()
            .toString()
            
        
        return authUrl
    }
    
    suspend fun exchangeCodeForToken(
        authorizationCode: String,
        codeVerifier: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(SnapchatConfig.TOKEN_ENDPOINT)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            
            val postData = buildString {
                append("grant_type=${SnapchatConfig.GRANT_TYPE_AUTHORIZATION_CODE}")
                append("&code=$authorizationCode")
                append("&redirect_uri=${Uri.encode(SnapchatConfig.REDIRECT_URI)}")
                append("&client_id=${SnapchatConfig.CLIENT_ID}")
                append("&code_verifier=$codeVerifier")
            }
            
            
            connection.outputStream.use { outputStream ->
                outputStream.write(postData.toByteArray())
            }
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                val accessToken = jsonResponse.optString("access_token").takeIf { it.isNotEmpty() }
                accessToken
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun refreshAccessToken(refreshToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(SnapchatConfig.TOKEN_ENDPOINT)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            
            val postData = buildString {
                append("grant_type=${SnapchatConfig.GRANT_TYPE_REFRESH_TOKEN}")
                append("&refresh_token=$refreshToken")
                append("&client_id=${SnapchatConfig.CLIENT_ID}")
            }
            
            connection.outputStream.use { outputStream ->
                outputStream.write(postData.toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                jsonResponse.optString("access_token").takeIf { it.isNotEmpty() }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getUserProfile(accessToken: String): SnapchatUser? = withContext(Dispatchers.IO) {
        try {
            val url = URL(SnapchatConfig.USER_PROFILE_ENDPOINT)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                
                // Parse user data according to Snapchat API response format
                val data = jsonResponse.optJSONObject("data")
                if (data != null) {
                    val displayName = data.optString("display_name", "")
                    val externalId = data.optString("external_id", "")
                    val bitmojiAvatar = data.optJSONObject("bitmoji")?.optString("avatar")?.takeIf { it.isNotEmpty() }
                    
                    SnapchatUser(
                        displayName = displayName,
                        externalId = externalId,
                        bitmojiAvatar = bitmojiAvatar
                    )
                } else {
                    null
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(codeVerifier.toByteArray())
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
} 