package com.chrissyx.zay.utils

import android.os.Build

object DeviceUtils {
    
    fun getDeviceModelName(deviceIdentifier: String?): String {
        if (deviceIdentifier.isNullOrEmpty()) {
            return "Unknown Device"
        }
        
        // iOS device name mapping (from iOS code)
        val iosDeviceMap = mapOf(
            // iPhone models
            "iPhone8,1" to "iPhone 6s", "iPhone8,2" to "iPhone 6s Plus",
            "iPhone8,4" to "iPhone SE", "iPhone9,1" to "iPhone 7", "iPhone9,3" to "iPhone 7",
            "iPhone9,2" to "iPhone 7 Plus", "iPhone9,4" to "iPhone 7 Plus",
            "iPhone10,1" to "iPhone 8", "iPhone10,4" to "iPhone 8",
            "iPhone10,2" to "iPhone 8 Plus", "iPhone10,5" to "iPhone 8 Plus",
            "iPhone10,3" to "iPhone X", "iPhone10,6" to "iPhone X",
            "iPhone11,2" to "iPhone XS", "iPhone11,4" to "iPhone XS Max", "iPhone11,6" to "iPhone XS Max",
            "iPhone11,8" to "iPhone XR", "iPhone12,1" to "iPhone 11", "iPhone12,3" to "iPhone 11 Pro",
            "iPhone12,5" to "iPhone 11 Pro Max", "iPhone12,8" to "iPhone SE 2nd Gen",
            "iPhone13,1" to "iPhone 12 mini", "iPhone13,2" to "iPhone 12",
            "iPhone13,3" to "iPhone 12 Pro", "iPhone13,4" to "iPhone 12 Pro Max",
            "iPhone14,4" to "iPhone 13 mini", "iPhone14,5" to "iPhone 13",
            "iPhone14,2" to "iPhone 13 Pro", "iPhone14,3" to "iPhone 13 Pro Max",
            "iPhone14,6" to "iPhone SE 3rd Gen", "iPhone14,7" to "iPhone 14",
            "iPhone14,8" to "iPhone 14 Plus", "iPhone15,2" to "iPhone 14 Pro",
            "iPhone15,3" to "iPhone 14 Pro Max", "iPhone15,4" to "iPhone 15",
            "iPhone15,5" to "iPhone 15 Plus", "iPhone16,1" to "iPhone 15 Pro",
            "iPhone16,2" to "iPhone 15 Pro Max", "iPhone17,3" to "iPhone 16",
            "iPhone17,4" to "iPhone 16 Plus", "iPhone17,1" to "iPhone 16 Pro",
            "iPhone17,2" to "iPhone 16 Pro Max",
            
            // iPad models  
            "iPad2,1" to "iPad 2", "iPad2,2" to "iPad 2", "iPad2,3" to "iPad 2", "iPad2,4" to "iPad 2",
            "iPad3,1" to "iPad 3rd Gen", "iPad3,2" to "iPad 3rd Gen", "iPad3,3" to "iPad 3rd Gen",
            "iPad3,4" to "iPad 4th Gen", "iPad3,5" to "iPad 4th Gen", "iPad3,6" to "iPad 4th Gen",
            "iPad6,11" to "iPad 5th Gen", "iPad6,12" to "iPad 5th Gen",
            "iPad7,5" to "iPad 6th Gen", "iPad7,6" to "iPad 6th Gen",
            "iPad7,11" to "iPad 7th Gen", "iPad7,12" to "iPad 7th Gen",
            "iPad11,6" to "iPad 8th Gen", "iPad11,7" to "iPad 8th Gen",
            "iPad12,1" to "iPad 9th Gen", "iPad12,2" to "iPad 9th Gen",
            "iPad13,18" to "iPad 10th Gen", "iPad13,19" to "iPad 10th Gen"
        )
        
        // Check if it's an iOS device
        val iosDeviceName = iosDeviceMap[deviceIdentifier]
        if (iosDeviceName != null) {
            return iosDeviceName
        }
        
        // If it's an Android device identifier, try to parse it
        // Android device identifiers are usually in format "manufacturer model"
        if (deviceIdentifier.contains(" ")) {
            val parts = deviceIdentifier.split(" ", limit = 2)
            if (parts.size >= 2) {
                val manufacturer = parts[0].replaceFirstChar { it.uppercase() }
                val model = parts[1].uppercase()
                return "$manufacturer $model"
            }
        }
        
        // For other cases, just return the identifier with some formatting
        return deviceIdentifier.replace("_", " ").split(" ").joinToString(" ") { 
            it.lowercase().replaceFirstChar { char -> char.uppercase() } 
        }
    }
    
    fun getCurrentDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    }
} 