package com.chrissyx.zay.config

object SnapchatConfig {
    const val PUBLIC_CLIENT_ID = "YOUR_SNAPCHAT_PUBLIC_CLIENT_ID"
    const val CONFIDENTIAL_CLIENT_ID = "YOUR_SNAPCHAT_CONFIDENTIAL_CLIENT_ID"
    const val CONFIDENTIAL_CLIENT_SECRET = "YOUR_SNAPCHAT_CLIENT_SECRET"
    
    // Use Public Client with PKCE for mobile apps (recommended by Snapchat docs)
    const val CLIENT_ID = PUBLIC_CLIENT_ID
    // No client secret needed for PKCE flow
    
    // OAuth 2.0 Endpoints
    const val AUTHORIZATION_ENDPOINT = "https://accounts.snapchat.com/accounts/oauth2/auth"
    const val TOKEN_ENDPOINT = "https://accounts.snapchat.com/accounts/oauth2/token"
    
    // Scopes
    const val SCOPE_DISPLAY_NAME = "https://auth.snapchat.com/oauth2/api/user.display_name"
    const val SCOPE_EXTERNAL_ID = "https://auth.snapchat.com/oauth2/api/user.external_id"
    const val SCOPE_BITMOJI_AVATAR = "https://auth.snapchat.com/oauth2/api/user.bitmoji.avatar"
    
    // Default scopes for Zay app
    val DEFAULT_SCOPES = listOf(
        SCOPE_DISPLAY_NAME,
        SCOPE_EXTERNAL_ID,
        SCOPE_BITMOJI_AVATAR
    )
    
    // Professional redirect URI for OAuth callback
    const val REDIRECT_URI = "zay://auth/snapchat/callback"
    
    // Response types
    const val RESPONSE_TYPE_CODE = "code"
    const val RESPONSE_TYPE_TOKEN = "token"
    
    // Grant types
    const val GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code"
    const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
    
    // PKCE
    const val CODE_CHALLENGE_METHOD = "S256"
    
    // API endpoints for user data
    const val USER_API_BASE = "https://kit.snapchat.com/v1"
    const val USER_PROFILE_ENDPOINT = "$USER_API_BASE/me"
} 