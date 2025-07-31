package com.chrissyx.zay.data.models

data class TrustedDevice(
    val deviceId: String = "",
    val deviceInfo: String = "",
    val trustedAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis()
)