package com.chrissyx.zay.data.models

enum class Platform(val displayName: String, val isEnabled: Boolean = true) {
    INSTAGRAM("Instagram", true),
    TIKTOK("TikTok - Coming Soon", false),
    SNAPCHAT("Snapchat - Coming Soon", false)
} 