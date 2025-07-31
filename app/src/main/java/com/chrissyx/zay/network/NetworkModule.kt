package com.chrissyx.zay.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    
    private const val INSTAGRAM_BASE_URL = "https://graph.facebook.com/v22.0/"
    private const val TIKTOK_BASE_URL = "https://open-api.tiktok.com/platform/oauth/v2/"
    
    private val instagramRetrofit = Retrofit.Builder()
        .baseUrl(INSTAGRAM_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val tiktokRetrofit = Retrofit.Builder()
        .baseUrl(TIKTOK_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val instagramService: InstagramService = instagramRetrofit.create(InstagramService::class.java)
    val tiktokService: TikTokAuthService = tiktokRetrofit.create(TikTokAuthService::class.java)
} 