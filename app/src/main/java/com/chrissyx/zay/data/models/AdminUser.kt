package com.chrissyx.zay.data.models

data class AdminUser(
    val username: String = "",
    val role: String = "user",
    val isPro: Boolean = false,
    val isVerified: Boolean = false,
    val isBanned: Boolean = false,
    val banExpiry: Long = 0,
    val banReason: String = "",
    val createdAt: Double = 0.0,
    val lastActive: Double = 0.0,
    val messagesSent: Int = 0,
    val messagesReceived: Int = 0,
    val exploreProfile: Boolean = false,
    val device: String = "",
    val platform: Platform = Platform.INSTAGRAM,
    val banInfo: BanInfo? = null
) {
    val deviceModel: String get() = device
}

data class BanInfo(
    val type: String = "", // "full", "explore", "inbox", "sharing"
    val reason: String = "",
    val duration: Long = 0, // Duration in milliseconds
    val expiry: Long = 0, // Expiry timestamp
    val bannedBy: String = "",
    val bannedAt: Long = System.currentTimeMillis()
) 