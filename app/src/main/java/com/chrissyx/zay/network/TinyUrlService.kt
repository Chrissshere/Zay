package com.chrissyx.zay.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TinyUrlService {
    
    companion object {
        private const val API_KEY = "YOUR_TINYURL_API_KEY"
        private const val BASE_URL = "https://api.tinyurl.com/create"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    suspend fun createShortUrl(username: String): String? = withContext(Dispatchers.IO) {
        try {
            // Create the zay:// deep link URL
            val deepLinkUrl = "zay://profile/$username"
            
            
            // Create JSON payload according to official API documentation
            val jsonPayload = JSONObject().apply {
                put("url", deepLinkUrl)
                put("domain", "tinyurl.com")
                // Optional fields can be omitted according to the docs
            }
            
            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(BASE_URL)
                .header("Authorization", "Bearer $API_KEY")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            
            val response = client.newCall(request).execute()
            
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                
                responseBody?.let { body ->
                    val jsonResponse = JSONObject(body)
                    
                    // Check the response code field (0 = success according to docs)
                    val responseCode = jsonResponse.optInt("code", -1)
                    
                    // Check for errors array
                    if (jsonResponse.has("errors")) {
                        val errorsArray = jsonResponse.getJSONArray("errors")
                        if (errorsArray.length() > 0) {
                            val errors = mutableListOf<String>()
                            for (i in 0 until errorsArray.length()) {
                                errors.add(errorsArray.getString(i))
                            }
                            return@withContext null
                        }
                    }
                    
                    // Get the tiny_url from data object if response code is 0 (success)
                    if (responseCode == 0 && jsonResponse.has("data")) {
                        val data = jsonResponse.getJSONObject("data")
                        if (data.has("tiny_url")) {
                            val tinyUrl = data.getString("tiny_url")
                            return@withContext tinyUrl
                        } else {
                        }
                    } else {
                    }
                }
            } else {
                val errorBody = response.body?.string()
                
                // Try to parse error response
                errorBody?.let { body ->
                    try {
                        val errorJson = JSONObject(body)
                        if (errorJson.has("errors")) {
                            val errorsArray = errorJson.getJSONArray("errors")
                            val errors = mutableListOf<String>()
                            for (i in 0 until errorsArray.length()) {
                                errors.add(errorsArray.getString(i))
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 