package com.chrissyx.zay.config

object InstagramConfig {
    const val CLIENT_ID = "YOUR_INSTAGRAM_CLIENT_ID"
    const val CLIENT_SECRET = "YOUR_INSTAGRAM_CLIENT_SECRET"
    const val REDIRECT_URI = "https://tinyurl.com/ZayVerifyInsta"
    
    // Updated Instagram Business API endpoints (2024)
    const val AUTHORIZATION_URL = "https://www.instagram.com/oauth/authorize"
    const val TOKEN_URL = "https://api.instagram.com/oauth/access_token"
    const val USER_INFO_URL = "https://graph.instagram.com/me"
    
    // Current Instagram Business API scopes (as of 2024)
    val SCOPES = listOf("instagram_business_basic", "instagram_business_content_publish")
    
    fun getAuthorizationUrl(): String {
        return "${AUTHORIZATION_URL}?" +
                "client_id=${CLIENT_ID}&" +
                "redirect_uri=${REDIRECT_URI}&" +
                "scope=${SCOPES.joinToString(",")}&" +
                "response_type=code&" +
                "force_reauth=true"
    }
} 