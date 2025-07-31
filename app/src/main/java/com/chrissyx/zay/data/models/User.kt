package com.chrissyx.zay.data.models

data class User(
    val username: String = "",
    val token: String = "",
    val prompt: String = "send me anonymous messages!",
    val deviceID: String = "",
    val device: String = "", // Device model like "iPhone12,3"
    val profilePictureURL: String? = null,
    val platform: String = "INSTAGRAM",
    val isPro: Boolean = false,
    val role: String = "user", // "admin" or "user"
    val isVerified: Boolean = false, // Instagram verification status
    val showVerificationInExplore: Boolean = true, // Toggle for showing checkmark in explore
    val lat: Double? = null, // Latitude
    val lon: Double? = null, // Longitude
    val createdAt: Long = System.currentTimeMillis(),
    val trustedDevices: List<String> = emptyList(), // List of trusted device IDs
    val trustedDeviceInfo: Map<String, String> = emptyMap() // Device ID -> Device Info mapping
) 