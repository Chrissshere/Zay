package com.chrissyx.zay.utils

import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object TicketUtils {
    
    // Generate unique ticket ID like "JH13BNK"
    fun generateTicketId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..7)
            .map { chars.random() }
            .joinToString("")
    }
    
    // Generate secure login link key like "872977ndokn928ndo93bdbla"
    fun generateLoginKey(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..27)
            .map { chars.random() }
            .joinToString("")
    }
    
    // Generate deep link URL for support ticket login
    fun generateSupportTicketLoginUrl(ticketId: String, loginKey: String): String {
        return "zay://zayapi/supportticket/id?=$ticketId/key?=$loginKey"
    }
    
    // Generate TinyURL using actual API service
    suspend fun generateTinyUrl(originalUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            
            // Create a short URL for the provided original URL
            val jsonPayload = org.json.JSONObject().apply {
                put("url", originalUrl)
                put("domain", "tinyurl.com")
            }
            
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            
            val request = okhttp3.Request.Builder()
                .url("https://api.tinyurl.com/create")
                .header("Authorization", "Bearer YOUR_TINYURL_API_KEY")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            
            val response = client.newCall(request).execute()
            
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                
                responseBody?.let { body ->
                    val jsonResponse = org.json.JSONObject(body)
                    val responseCode = jsonResponse.optInt("code", -1)
                    
                    
                    if (responseCode == 0 && jsonResponse.has("data")) {
                        val data = jsonResponse.getJSONObject("data")
                        if (data.has("tiny_url")) {
                            val tinyUrl = data.getString("tiny_url")
                            return@withContext tinyUrl
                        } else {
                        }
                    } else {
                        if (jsonResponse.has("errors")) {
                            val errors = jsonResponse.getJSONArray("errors")
                        }
                    }
            } else {
                val errorBody = response.body?.string()
            }
            
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    // Test method to verify TinyURL API is working
    suspend fun testTinyUrlConnection(): String {
        return try {
            val testUrl = "https://www.google.com"
            val result = generateTinyUrl(testUrl)
            if (result != null) {
                "✅ TinyURL API is working! Generated: $result"
            } else {
                "❌ TinyURL API failed to generate URL - check logs for details"
            }
        } catch (e: Exception) {
            "❌ TinyURL API error: ${e.message}"
        }
    }
    
    // Parse ticket ID from deep link
    fun parseTicketIdFromUrl(url: String): String? {
        return try {
            val regex = Regex("id\\?=([A-Z0-9]+)")
            regex.find(url)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
    
    // Parse login key from deep link
    fun parseLoginKeyFromUrl(url: String): String? {
        return try {
            val regex = Regex("key\\?=([a-z0-9]+)")
            regex.find(url)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
} 