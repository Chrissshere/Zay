package com.chrissyx.zay.data.repository

import com.chrissyx.zay.data.models.User
import com.chrissyx.zay.data.models.Message
import com.chrissyx.zay.data.models.SenderInfo
import com.chrissyx.zay.data.models.ExploreProfile
import com.chrissyx.zay.data.models.Platform
import com.chrissyx.zay.data.models.AdminUser
import com.chrissyx.zay.data.models.BanInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.chrissyx.zay.utils.TicketUtils
import android.net.Uri
import java.io.InputStream

class FirebaseRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    // User operations
    suspend fun getUserByUsername(username: String): User? {
        return try {
            val document = firestore.collection("users").document(username).get().await()
            
            if (document.exists()) {
                val user = document.toObject(User::class.java)?.copy(username = username)
                user
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // New method for platform-specific user lookup
    suspend fun getUserByUsernameAndPlatform(username: String, platform: String): User? {
        return try {
            val documentId = "${username}@${platform.uppercase()}"
            val document = firestore.collection("users").document(documentId).get().await()
            
            if (document.exists()) {
                val user = document.toObject(User::class.java)?.copy(username = username)
                user
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Check if username exists on any platform
    suspend fun getUsersWithUsername(username: String): List<User> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
            
            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(User::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            
            users
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun createOrUpdateUser(user: User): Boolean {
        return try {
            firestore.collection("users").document(user.username).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // New platform-specific user creation
    suspend fun createOrUpdateUserWithPlatform(user: User): Boolean {
        return try {
            val documentId = "${user.username}@${user.platform.uppercase()}"
            firestore.collection("users").document(documentId).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateUserPrompt(username: String, prompt: String): Boolean {
        return try {
            firestore.collection("users").document(username)
                .update("prompt", prompt).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateUserProfilePicture(username: String, profilePictureURL: String): Boolean {
        return try {
            
            firestore.collection("users").document(username)
                .update("profilePictureURL", profilePictureURL).await()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Message operations
    suspend fun sendMessage(recipientUsername: String, message: Message): Boolean {
        return try {
            
            // First, check if the recipient user exists
            val userDoc = firestore.collection("users").document(recipientUsername).get().await()
            if (!userDoc.exists()) {
                return false
            }
            
    
            val messageData = mapOf(
                "text" to message.text,
                "timestamp" to message.timestamp,
                "sender" to message.sender,
                "device" to message.device
            )
            
            
            val docRef = firestore.collection("users").document(recipientUsername)
                .collection("messages").add(messageData).await()
            
            true
        } catch (e: Exception) {
            if (e is com.google.firebase.firestore.FirebaseFirestoreException) {
            }
            e.printStackTrace()
            false
        }
    }
    
    fun getMessagesForUser(username: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("users").document(username)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(messages)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Explore users
    suspend fun getRandomUsers(limit: Int = 20): List<User> {
        return try {
            val snapshot = firestore.collection("users")
                .limit(limit.toLong())
                .get().await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(username = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Profile picture upload with compression
    suspend fun uploadProfilePicture(username: String, imageBytes: ByteArray): String? {
        return try {
            
            // Compress image if it's too large
            val compressedBytes = if (imageBytes.size > 500 * 1024) { // 500KB
                compressImage(imageBytes, 70) // 70% quality
            } else {
                imageBytes
            }
            
            
            val storageRef = storage.reference.child("profile_pictures/$username.jpg")
            
            // Set metadata
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("username", username)
                .setCustomMetadata("uploadTime", System.currentTimeMillis().toString())
                .build()
            
            val uploadTask = storageRef.putBytes(compressedBytes, metadata).await()
            
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            downloadUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Banner image upload for explore profiles with compression
    suspend fun uploadBannerImage(username: String, imageBytes: ByteArray): String? {
        return try {
            
            // Compress banner image if it's too large
            val compressedBytes = if (imageBytes.size > 1024 * 1024) { // 1MB
                compressImage(imageBytes, 80) // 80% quality for banners
            } else {
                imageBytes
            }
            
            
            val storageRef = storage.reference.child("explore_banners/$username.jpg")
            
            // Set metadata
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("username", username)
                .setCustomMetadata("uploadTime", System.currentTimeMillis().toString())
                .build()
            
            val uploadTask = storageRef.putBytes(compressedBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            downloadUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Helper method to compress images
    private fun compressImage(imageBytes: ByteArray, quality: Int): ByteArray {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            val compressedBytes = outputStream.toByteArray()
            bitmap.recycle() // Free up memory
            compressedBytes
        } catch (e: Exception) {
            imageBytes // Return original if compression fails
        }
    }
    
    // Explore Profile methods
    suspend fun createExploreProfile(profile: ExploreProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if user already has an explore profile
            val existingProfile = getExploreProfileByUsername(profile.username)
            if (existingProfile != null) {
                return@withContext false
            }
            
            // Create profile in global explore_profiles collection (visible to all users)
            firestore.collection("explore_profiles")
                .document(profile.username)
                .set(profile)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateExploreProfile(profile: ExploreProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            // Update profile in global explore_profiles collection
            firestore.collection("explore_profiles")
                .document(profile.username)
                .set(profile)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getExploreProfileByUsername(username: String): ExploreProfile? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("explore_profiles")
                .document(username)
                .get()
                .await()
            
            if (doc.exists()) {
                doc.toObject(ExploreProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getAllExploreProfiles(): List<ExploreProfile> = withContext(Dispatchers.IO) {
        try {
            
            // First, try to get all profiles without any filter to see what's in the database
            val allSnapshot = firestore.collection("explore_profiles")
                .get()
                .await()
            
            
            // Log all documents for debugging
            allSnapshot.documents.forEach { doc ->
            }
            
            // Now get only active profiles
            val snapshot = firestore.collection("explore_profiles")
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            
            val profiles = snapshot.documents.mapNotNull { doc ->
                try {
                    val profile = doc.toObject(ExploreProfile::class.java)
                    profile
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            
            
            // Enrich profiles with current user verification data
            val enrichedProfiles = enrichExploreProfilesWithVerification(profiles)
            
            // If no active profiles found, try getting all profiles as fallback
            if (enrichedProfiles.isEmpty() && allSnapshot.documents.isNotEmpty()) {
                val fallbackProfiles = allSnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ExploreProfile::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                return@withContext enrichExploreProfilesWithVerification(fallbackProfiles)
            }
            
            enrichedProfiles
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Enrich explore profiles with current user verification data
    private suspend fun enrichExploreProfilesWithVerification(profiles: List<ExploreProfile>): List<ExploreProfile> {
        return profiles.map { profile ->
            try {
                // Get current user data to check verification status and display preference
                val userData = getUserByUsernameWithFallback(profile.username, "INSTAGRAM") // Try Instagram first as primary
                    ?: getUserByUsernameWithFallback(profile.username, "SNAPCHAT")
                    ?: getUserByUsernameWithFallback(profile.username, "TIKTOK")
                
                if (userData != null) {
                    // Update profile with current verification data
                    profile.copy(
                        isVerified = userData.isVerified || userData.role == "admin", // Admins are auto-verified
                        showVerificationInExplore = userData.showVerificationInExplore,
                        isPro = userData.isPro
                    )
                } else {
                    profile
                }
            } catch (e: Exception) {
                profile
            }
        }
    }
    
    // Method to get ALL profiles for debugging (including inactive ones)
    suspend fun getAllExploreProfilesDebug(): List<ExploreProfile> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("explore_profiles")
                .get()
                .await()
            
            
            val profiles = snapshot.documents.mapNotNull { doc ->
                try {
                    val profile = doc.toObject(ExploreProfile::class.java)
                    profile
                } catch (e: Exception) {
                    null
                }
            }
            
            profiles
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun searchExploreProfiles(query: String): List<ExploreProfile> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("explore_profiles")
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val allProfiles = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ExploreProfile::class.java)
            }
            
            
            // Filter profiles based on search query
            val filteredProfiles = allProfiles.filter { profile ->
                profile.username.contains(query, ignoreCase = true) ||
                profile.prompt.contains(query, ignoreCase = true) ||
                profile.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
            
            filteredProfiles
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun deleteExploreProfile(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("explore_profiles")
                .document(username)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSenderInfo(username: String): SenderInfo? {
        return try {
            val doc = firestore.collection("users").document(username).get().await()
            
            if (!doc.exists()) {
                return SenderInfo(
                    username = username,
                    device = null,
                    location = null
                )
            }
            
            // Extract data from Firestore document using DocumentSnapshot methods
            
            val device = try {
                doc.getString("device")
            } catch (e: Exception) {
                null
            }
            
            val lat = try {
                doc.getDouble("lat")
            } catch (e: Exception) {
                null
            }
            
            val lon = try {
                doc.getDouble("lon")
            } catch (e: Exception) {
                null
            }
            
            
            val location = if (lat != null && lon != null) {
                SenderInfo.Location(latitude = lat, longitude = lon)
            } else {
                null
            }
            
            val senderInfo = SenderInfo(
                username = username,
                device = device,
                location = location
            )
            
            senderInfo
        } catch (e: Exception) {
            e.printStackTrace()
            SenderInfo(
                username = username,
                device = null,
                location = null
            )
        }
    }

    suspend fun updateUserLocation(username: String, latitude: Double, longitude: Double, device: String): Boolean {
        return try {
            
            val updates = mapOf(
                "lat" to latitude,
                "lon" to longitude,
                "device" to device
            )
            
            firestore.collection("users").document(username)
                .update(updates).await()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun updateUserDevice(username: String, device: String): Boolean {
        return try {
            
            firestore.collection("users").document(username)
                .update("device", device).await()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateUserProStatus(username: String, isPro: Boolean): Boolean {
        return try {
            firestore.collection("users")
                .document(username)
                .update("isPro", isPro).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getAllProUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            val proUsers = mutableListOf<User>()
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("isPro", true)
                .get()
                .await()
            
            for (document in querySnapshot.documents) {
                try {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        proUsers.add(user)
                    }
                } catch (e: Exception) {
                }
            }
            
            proUsers
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Manual verification request for Instagram accounts
    suspend fun createVerificationRequest(username: String, request: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("verification_requests")
                .document(username)
                .set(request)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getVerificationRequest(username: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("verification_requests")
                .document(username)
                .get()
                .await()
            
            if (doc.exists()) {
                doc.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Admin Panel Methods
    suspend fun getAdminStatistics(): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            val usersSnapshot = firestore.collection("users").get().await()
            val totalUsers = usersSnapshot.size()
            
            var activeUsers = 0
            var verifiedUsers = 0
            var bannedUsers = 0
            
            usersSnapshot.documents.forEach { doc ->
                val lastActive = doc.getDouble("lastActive") ?: 0.0
                val isVerified = doc.getBoolean("isVerified") ?: false
                val isBanned = doc.getBoolean("isBanned") ?: false
                
                // Consider active if logged in within last 30 days
                val thirtyDaysAgo = (System.currentTimeMillis() / 1000.0) - (30 * 24 * 60 * 60)
                if (lastActive > thirtyDaysAgo) activeUsers++
                
                if (isVerified) verifiedUsers++
                if (isBanned) bannedUsers++
            }
            
            mapOf(
                "totalUsers" to totalUsers,
                "activeUsers" to activeUsers,
                "verifiedUsers" to verifiedUsers,
                "bannedUsers" to bannedUsers
            )
        } catch (e: Exception) {
            mapOf(
                "totalUsers" to 0,
                "activeUsers" to 0,
                "verifiedUsers" to 0,
                "bannedUsers" to 0
            )
        }
    }
    
    suspend fun getAllUsersForAdmin(): List<AdminUser> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    AdminUser(
                        username = doc.id, // Document ID is the username
                        role = doc.getString("role") ?: "user",
                        isPro = doc.getBoolean("isPro") ?: false,
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        isBanned = doc.getBoolean("isBanned") ?: false,
                        banExpiry = doc.getLong("banExpiry") ?: 0,
                        banReason = doc.getString("banReason") ?: "",
                        createdAt = doc.getDouble("createdAt") ?: 0.0,
                        lastActive = doc.getDouble("lastActive") ?: 0.0,
                        device = doc.getString("device") ?: "",
                        platform = try { Platform.valueOf(doc.getString("platform") ?: "INSTAGRAM") } catch (e: Exception) { Platform.INSTAGRAM }
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getVerifiedUsers(): List<AdminUser> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("isVerified", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    AdminUser(
                        username = doc.id,
                        role = doc.getString("role") ?: "user",
                        isPro = doc.getBoolean("isPro") ?: false,
                        isVerified = true,
                        isBanned = doc.getBoolean("isBanned") ?: false,
                        banExpiry = doc.getLong("banExpiry") ?: 0,
                        banReason = doc.getString("banReason") ?: "",
                        createdAt = doc.getDouble("createdAt") ?: 0.0,
                        lastActive = doc.getDouble("lastActive") ?: 0.0,
                        device = doc.getString("device") ?: "",
                        platform = try { Platform.valueOf(doc.getString("platform") ?: "INSTAGRAM") } catch (e: Exception) { Platform.INSTAGRAM }
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getBannedUsers(): List<AdminUser> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("isBanned", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    AdminUser(
                        username = doc.id,
                        role = doc.getString("role") ?: "user",
                        isPro = doc.getBoolean("isPro") ?: false,
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        isBanned = true,
                        banExpiry = doc.getLong("banExpiry") ?: 0,
                        banReason = doc.getString("banReason") ?: "",
                        createdAt = doc.getDouble("createdAt") ?: 0.0,
                        lastActive = doc.getDouble("lastActive") ?: 0.0,
                        device = doc.getString("device") ?: "",
                        platform = try { Platform.valueOf(doc.getString("platform") ?: "INSTAGRAM") } catch (e: Exception) { Platform.INSTAGRAM }
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun searchUsersForAdmin(query: String): List<AdminUser> = withContext(Dispatchers.IO) {
        try {
            // Firestore doesn't support case-insensitive search, so we'll get all users and filter
            val allUsers = getAllUsersForAdmin()
            allUsers.filter { user ->
                user.username.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun banUser(username: String, banInfo: BanInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "isBanned" to true,
                "banExpiry" to banInfo.expiry,
                "banReason" to banInfo.reason,
                "banType" to banInfo.type,
                "bannedAt" to banInfo.bannedAt,
                "bannedBy" to banInfo.bannedBy
            )
            
            firestore.collection("users")
                .document(username)
                .update(updates)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun unbanUser(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "isBanned" to false,
                "banExpiry" to 0,
                "banReason" to "",
                "banType" to "",
                "bannedAt" to 0,
                "bannedBy" to ""
            )
            
            firestore.collection("users")
                .document(username)
                .update(updates)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun verifyUser(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users")
                .document(username)
                .update("isVerified", true)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deleteUser(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete user document
            firestore.collection("users")
                .document(username)
                .delete()
                .await()
            
            // Delete user's messages
            val messagesSnapshot = firestore.collection("users")
                .document(username)
                .collection("messages")
                .get()
                .await()
            
            messagesSnapshot.documents.forEach { doc ->
                doc.reference.delete()
            }
            
            // Delete explore profile if exists
            firestore.collection("explore_profiles")
                .document(username)
                .delete()
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun sendGlobalAnnouncement(title: String, message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val announcement = mapOf(
                "title" to title,
                "message" to message,
                "timestamp" to System.currentTimeMillis() / 1000.0,
                "type" to "global"
            )
            
            firestore.collection("announcements")
                .add(announcement)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Verification Request Methods
    suspend fun createVerificationRequest(request: com.chrissyx.zay.data.models.VerificationRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("verification_requests")
                .document(request.username)
                .set(request)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getAllVerificationRequests(): List<com.chrissyx.zay.data.models.VerificationRequest> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("verification_requests")
                .whereEqualTo("status", "PENDING")
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(com.chrissyx.zay.data.models.VerificationRequest::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun approveVerificationRequest(username: String, reviewedBy: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Update verification request status
            firestore.collection("verification_requests")
                .document(username)
                .update(
                    mapOf(
                        "status" to "APPROVED",
                        "reviewedBy" to reviewedBy,
                        "reviewedAt" to System.currentTimeMillis() / 1000.0
                    )
                )
                .await()
            
            // Verify the user
            verifyUser(username)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun rejectVerificationRequest(username: String, reviewedBy: String, notes: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("verification_requests")
                .document(username)
                .update(
                    mapOf(
                        "status" to "REJECTED",
                        "reviewedBy" to reviewedBy,
                        "reviewedAt" to System.currentTimeMillis() / 1000.0,
                        "reviewNotes" to notes
                    )
                )
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Support Ticket Methods
    suspend fun createSupportTicket(ticket: com.chrissyx.zay.data.models.SupportTicket): Boolean = withContext(Dispatchers.IO) {
        try {
            // Use the provided ticket ID or generate one if empty
            val ticketId = if (ticket.id.isNotEmpty()) ticket.id else TicketUtils.generateTicketId()
            val ticketWithId = ticket.copy(id = ticketId)
            
            firestore.collection("support_tickets")
                .document(ticketId)
                .set(ticketWithId)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getAllSupportTickets(): List<com.chrissyx.zay.data.models.SupportTicket> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("support_tickets")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(com.chrissyx.zay.data.models.SupportTicket::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun updateSupportTicketStatus(ticketId: String, status: com.chrissyx.zay.data.models.TicketStatus, assignedTo: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to System.currentTimeMillis() / 1000.0
            )
            
            if (assignedTo.isNotEmpty()) {
                updates["assignedTo"] = assignedTo
            }
            
            if (status == com.chrissyx.zay.data.models.TicketStatus.RESOLVED || status == com.chrissyx.zay.data.models.TicketStatus.CLOSED) {
                updates["resolvedAt"] = System.currentTimeMillis() / 1000.0
            }
            
            firestore.collection("support_tickets")
                .document(ticketId)
                .update(updates)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun addSupportTicketNote(ticketId: String, note: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("support_tickets")
                .document(ticketId)
                .update(
                    mapOf(
                        "internalNotes" to note,
                        "updatedAt" to System.currentTimeMillis() / 1000.0
                    )
                )
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getUserSupportTickets(username: String): List<com.chrissyx.zay.data.models.SupportTicket> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("support_tickets")
                .whereEqualTo("username", username)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(com.chrissyx.zay.data.models.SupportTicket::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Admin response methods
    suspend fun addAdminResponseToTicket(ticketId: String, response: com.chrissyx.zay.data.models.AdminResponse): Boolean = withContext(Dispatchers.IO) {
        try {
            val ticketRef = firestore.collection("support_tickets").document(ticketId)
            
            firestore.runTransaction { transaction ->
                val ticket = transaction.get(ticketRef).toObject(com.chrissyx.zay.data.models.SupportTicket::class.java)
                if (ticket != null) {
                    val updatedResponses = ticket.adminResponses + response
                    transaction.update(ticketRef, mapOf(
                        "adminResponses" to updatedResponses,
                        "updatedAt" to System.currentTimeMillis() / 1000.0,
                        "status" to com.chrissyx.zay.data.models.TicketStatus.IN_PROGRESS.name
                    ))
                }
            }.await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Login link methods
    suspend fun createLoginLink(loginLink: com.chrissyx.zay.data.models.LoginLink): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("login_links")
                .document(loginLink.id)
                .set(loginLink)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getLoginLink(linkId: String): com.chrissyx.zay.data.models.LoginLink? = withContext(Dispatchers.IO) {
        try {
            val document = firestore.collection("login_links").document(linkId).get().await()
            document.toObject(com.chrissyx.zay.data.models.LoginLink::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun useLoginLink(linkId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("login_links")
                .document(linkId)
                .update(mapOf(
                    "isUsed" to true,
                    "usedAt" to System.currentTimeMillis() / 1000.0
                ))
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deleteLoginLink(linkId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("login_links")
                .document(linkId)
                .delete()
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Update user verification display preference
    suspend fun updateUserVerificationDisplay(username: String, platform: String, showInExplore: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            // Try platform-specific user first
            val platformUser = getUserByUsernameAndPlatform(username, platform)
            if (platformUser != null) {
                val documentId = "${username}@${platform.uppercase()}"
                firestore.collection("users")
                    .document(documentId)
                    .update("showVerificationInExplore", showInExplore)
                    .await()
                
                return@withContext true
            }
            
            // Fallback to old format user
            val oldUser = getUserByUsername(username)
            if (oldUser != null) {
                firestore.collection("users")
                    .document(username)
                    .update("showVerificationInExplore", showInExplore)
                    .await()
                
                return@withContext true
            }
            
            false
        } catch (e: Exception) {
            false
        }
    }
    
    // Upload image to Firebase Storage and return download URL
    suspend fun uploadImageToStorageWithContext(context: android.content.Context, imageUri: Uri, filename: String): String? = withContext(Dispatchers.IO) {
        try {
            
            // Get input stream from URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                return@withContext null
            }
            
            // Read bytes and compress if needed
            val byteArray = inputStream.readBytes()
            inputStream.close()
            
            if (byteArray.isEmpty()) {
                return@withContext null
            }
            
            
            val compressedBytes = if (byteArray.size > 500 * 1024) { // If larger than 500KB
                val compressed = compressImage(byteArray, 70) // 70% quality
                compressed
            } else {
                byteArray
            }
            
            // Upload to Firebase Storage
            val storageRef = storage.reference.child(filename)
            
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("uploadTime", System.currentTimeMillis().toString())
                .setCustomMetadata("originalSize", byteArray.size.toString())
                .build()
            
            val uploadTask = storageRef.putBytes(compressedBytes, metadata).await()
            
            if (uploadTask.task.isSuccessful) {
                // Get download URL
                val downloadUrl = storageRef.downloadUrl.await()
                return@withContext downloadUrl.toString()
            } else {
                val exception = uploadTask.task.exception
                exception?.printStackTrace()
                return@withContext null
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun verifyUserPlatform(username: String, platform: String): Boolean {
        return try {
            firestore.collection("users")
                .document(username)
                .update(
                    mapOf(
                        "isVerified" to true,
                        "platform" to platform
                    )
                ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Migration method for existing users
    suspend fun migrateUserToPlatformSpecific(username: String, platform: String): Boolean {
        return try {
            
            // Get user from old format (username only)
            val oldUser = getUserByUsername(username)
            if (oldUser != null) {
                // Create new platform-specific document
                val success = createOrUpdateUserWithPlatform(oldUser.copy(platform = platform))
                if (success) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
    

    suspend fun getUserByUsernameWithFallback(username: String, platform: String): User? {
        return try {
            // First try platform-specific lookup
            var user = getUserByUsernameAndPlatform(username, platform)
            
            if (user == null) {
                // Fallback to old format lookup
                user = getUserByUsername(username)
                if (user != null) {
                    // Optionally migrate here or handle in the calling code
                }
            }
            
            user
        } catch (e: Exception) {
            null
        }
    }
    
    // Device Authentication Methods
    suspend fun addTrustedDevice(username: String, deviceId: String, deviceInfo: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userRef = firestore.collection("users").document(username)
            val user = userRef.get().await().toObject(User::class.java)
            
            if (user != null) {
                val updatedTrustedDevices = user.trustedDevices.toMutableList()
                val updatedDeviceInfo = user.trustedDeviceInfo.toMutableMap()
                
                if (!updatedTrustedDevices.contains(deviceId)) {
                    updatedTrustedDevices.add(deviceId)
                    updatedDeviceInfo[deviceId] = deviceInfo
                    
                    userRef.update(
                        mapOf(
                            "trustedDevices" to updatedTrustedDevices,
                            "trustedDeviceInfo" to updatedDeviceInfo
                        )
                    ).await()
                    
                    true
                } else {
                    true
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun removeTrustedDevice(username: String, deviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userRef = firestore.collection("users").document(username)
            val user = userRef.get().await().toObject(User::class.java)
            
            if (user != null) {
                val updatedTrustedDevices = user.trustedDevices.toMutableList()
                val updatedDeviceInfo = user.trustedDeviceInfo.toMutableMap()
                
                updatedTrustedDevices.remove(deviceId)
                updatedDeviceInfo.remove(deviceId)
                
                userRef.update(
                    mapOf(
                        "trustedDevices" to updatedTrustedDevices,
                        "trustedDeviceInfo" to updatedDeviceInfo
                    )
                ).await()
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun clearAllTrustedDevices(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = getUserByUsername(username)
            if (user != null) {
                firestore.collection("users").document(username).update(
                    mapOf(
                        "trustedDevices" to emptyList<String>(),
                        "trustedDeviceInfo" to emptyMap<String, String>()
                    )
                ).await()
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
} 