package com.chrissyx.zay.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class InstagramService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    suspend fun checkUsernameExists(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://instagram.com/$username"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    val html = response.body?.string() ?: ""
                    !html.contains("Sorry, this page isn't available", ignoreCase = true)
                }
                302 -> {
                    // Handle redirect to login page
                    val location = response.header("Location")
                    if (location?.contains("/accounts/login/") == true) {
                        // Follow redirect to check if user exists
                        val redirectRequest = Request.Builder()
                            .url(location)
                            .get()
                            .build()
                        
                        val redirectResponse = client.newCall(redirectRequest).execute()
                        val redirectHtml = redirectResponse.body?.string() ?: ""
                        !redirectHtml.contains("Sorry, this page isn't available", ignoreCase = true)
                    } else {
                        true // Assume user exists if redirected elsewhere
                    }
                }
                404 -> false
                else -> false
            }
        } catch (e: Exception) {
            false // Return false on any network error
        }
    }
} 