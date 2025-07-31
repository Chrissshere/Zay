package com.chrissyx.zay.data.models

import java.io.Serializable

data class ExploreProfile(
    val username: String = "",
    val prompt: String = "",
    val bannerImageURL: String = "",
    val isActive: Boolean = true,
    val createdAt: Double = 0.0,
    val lastUpdated: Double = 0.0,
    val tags: List<String> = emptyList(),
    val location: String? = null,
    val isVerified: Boolean = false,
    val isPro: Boolean = false,
    val showVerificationInExplore: Boolean = true
) : Serializable 