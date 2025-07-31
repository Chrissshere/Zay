package com.chrissyx.zay.data.models

data class VerificationRequest(
    val username: String = "",
    val requestedAt: Double = System.currentTimeMillis() / 1000.0,
    val status: VerificationStatus = VerificationStatus.PENDING,
    val instagramUsername: String = "",
    val instagramAccessToken: String = "",
    val instagramVerificationData: Map<String, Any> = emptyMap(),
    val reviewedBy: String = "",
    val reviewedAt: Double = 0.0,
    val reviewNotes: String = "",
    val deviceInfo: String = "",
    val userAgent: String = ""
)

enum class VerificationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
} 