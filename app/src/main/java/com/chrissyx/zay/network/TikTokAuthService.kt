package com.chrissyx.zay.network

import retrofit2.Response
import retrofit2.http.*

interface TikTokAuthService {
    
    @POST("oauth/access_token/")
    @FormUrlEncoded
    suspend fun getAccessToken(
        @Field("client_key") clientKey: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("redirect_uri") redirectUri: String
    ): Response<TikTokTokenResponse>
    
    @GET("user/info/")
    suspend fun getUserInfo(
        @Header("Authorization") authorization: String
    ): Response<TikTokUserResponse>
}

data class TikTokTokenResponse(
    val access_token: String?,
    val expires_in: Int?,
    val refresh_token: String?,
    val refresh_expires_in: Int?,
    val scope: String?,
    val token_type: String?,
    val error: String?,
    val error_description: String?
)

data class TikTokUserResponse(
    val data: TikTokUserData?,
    val error: TikTokError?
)

data class TikTokUserData(
    val user: TikTokUser?
)

data class TikTokUser(
    val open_id: String?,
    val union_id: String?,
    val avatar_url: String?,
    val avatar_url_100: String?,
    val avatar_large_url: String?,
    val display_name: String?,
    val bio_description: String?,
    val profile_deep_link: String?,
    val is_verified: Boolean?,
    val follower_count: Int?,
    val following_count: Int?,
    val likes_count: Int?,
    val video_count: Int?
)

data class TikTokError(
    val code: String?,
    val message: String?,
    val log_id: String?
) 