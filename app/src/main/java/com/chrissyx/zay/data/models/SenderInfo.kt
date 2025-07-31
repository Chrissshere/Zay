package com.chrissyx.zay.data.models

data class SenderInfo(
    val username: String = "",
    val device: String? = null,
    val location: Location? = null
) {
    data class Location(
        val latitude: Double,
        val longitude: Double
    )
} 