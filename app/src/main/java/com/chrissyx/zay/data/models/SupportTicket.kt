package com.chrissyx.zay.data.models

data class SupportTicket(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val subject: String = "",
    val description: String = "",
    val category: TicketCategory = TicketCategory.GENERAL,
    val priority: TicketPriority = TicketPriority.MEDIUM,
    val status: TicketStatus = TicketStatus.OPEN,
    val createdAt: Double = System.currentTimeMillis() / 1000.0,
    val updatedAt: Double = System.currentTimeMillis() / 1000.0,
    val assignedTo: String = "",
    val resolvedAt: Double = 0.0,
    val resolution: String = "",
    val deviceInfo: String = "",
    val appVersion: String = "",
    val attachments: List<String> = emptyList(),
    val internalNotes: String = "",

    val adminResponses: List<AdminResponse> = emptyList(),
    val progressUpdates: List<ProgressUpdate> = emptyList(),
    val platform: String = "UNKNOWN" // Track which platform user is from
)

// New data classes for admin features
data class AdminResponse(
    val id: String = "",
    val adminUsername: String = "",
    val message: String = "",
    val timestamp: Double = System.currentTimeMillis() / 1000.0,
    val type: ResponseType = ResponseType.TEXT
)

data class ProgressUpdate(
    val status: TicketStatus = TicketStatus.OPEN,
    val message: String = "",
    val timestamp: Double = System.currentTimeMillis() / 1000.0,
    val adminUsername: String = ""
)

// New data class for login links
data class LoginLink(
    val id: String = "",
    val ticketId: String = "",
    val targetUsername: String = "",
    val linkKey: String = "",
    val adminUsername: String = "",
    val createdAt: Double = System.currentTimeMillis() / 1000.0,
    val expiresAt: Double = 0.0,
    val isUsed: Boolean = false,
    val usedAt: Double = 0.0
)

enum class TicketCategory(val displayName: String) {
    GENERAL("General Support"),
    VERIFICATION("Account Verification"),
    TECHNICAL("Technical Issue"),
    ABUSE("Report Abuse"),
    FEATURE_REQUEST("Feature Request"),
    BUG_REPORT("Bug Report"),
    BILLING("Billing Support")
}

enum class TicketPriority(val displayName: String, val color: String) {
    LOW("Low", "#4CAF50"),
    MEDIUM("Medium", "#FF9800"),
    HIGH("High", "#FF5722"),
    URGENT("Urgent", "#F44336")
}

enum class TicketStatus(val displayName: String, val color: String) {
    OPEN("Open", "#2196F3"),
    IN_PROGRESS("In Progress", "#FF9800"),
    WAITING_FOR_USER("Waiting for User", "#9C27B0"),
    RESOLVED("Resolved", "#4CAF50"),
    CLOSED("Closed", "#607D8B")
}

enum class ResponseType(val displayName: String) {
    TEXT("Text Response"),
    LOGIN_LINK("Login Link Generated")
} 