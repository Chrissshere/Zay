package com.chrissyx.zay.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chrissyx.zay.data.models.AdminUser
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.databinding.FragmentAdminPanelBinding
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.launch
import android.graphics.Color

class AdminPanelFragment : Fragment() {

    private var _binding: FragmentAdminPanelBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var adminUserAdapter: AdminUserAdapter
    private lateinit var verificationRequestAdapter: VerificationRequestAdapter
    private lateinit var supportTicketAdapter: SupportTicketAdapter
    
    private var allUsers = listOf<AdminUser>()
    private var filteredUsers = listOf<AdminUser>()
    private var verificationRequests = listOf<com.chrissyx.zay.data.models.VerificationRequest>()
    private var supportTickets = listOf<com.chrissyx.zay.data.models.SupportTicket>()
    private var filteredTickets = listOf<com.chrissyx.zay.data.models.SupportTicket>()
    private var currentFilter = "all"
    private var currentTicketFilter = "all"
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            userPreferences = UserPreferences(requireContext())
            firebaseRepository = FirebaseRepository()
            
            setupUI()
            setupRecyclerView()
            setupVerificationRequests()
            setupSupportTickets()
            loadStatistics()
            loadUsers()
            loadVerificationRequests()
            loadSupportTickets()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error initializing admin panel", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        try {
            // Back button
            binding.backButton.setOnClickListener {
                try {
                    requireActivity().supportFragmentManager.popBackStack()
                } catch (e: Exception) {
                }
            }
            
            // Refresh button
            binding.refreshButton.setOnClickListener {
                refreshAllData()
            }
            
            // Global announcements
            binding.sendAnnouncementButton.setOnClickListener {
                sendGlobalAnnouncement()
            }
            
            // Search functionality
            try {
                binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = false
                    
                    override fun onQueryTextChange(newText: String?): Boolean {
                        try {
                            searchUsers(newText ?: "")
                        } catch (e: Exception) {
                        }
                        return true
                    }
                })
                binding.searchView.visibility = View.VISIBLE
            } catch (e: Exception) {
            }
            
            // Filter buttons
            try {
                binding.filterAllButton.setOnClickListener { 
                }
                binding.filterVerifiedButton.setOnClickListener { 
                }
                binding.filterBannedButton.setOnClickListener { 
                }
                
                // Make filter buttons visible
                binding.filterAllButton.visibility = View.VISIBLE
                binding.filterVerifiedButton.visibility = View.VISIBLE
                binding.filterBannedButton.visibility = View.VISIBLE
                
                // Set initial filter
                applyFilter("all")
            } catch (e: Exception) {
            }
            
            // Device Management
            try {
                binding.viewAllDevicesButton.setOnClickListener {
                    viewAllDevices()
                }
                binding.clearUserDevicesButton.setOnClickListener {
                    clearUserDevices()
                }
                
                // Device search
                binding.deviceSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        searchUserDevices(query ?: "")
                        return true
                    }
                    
                    override fun onQueryTextChange(newText: String?): Boolean {
                        if (!newText.isNullOrBlank() && newText.length > 2) {
                            searchUserDevices(newText)
                        }
                        return true
                    }
                })
            } catch (e: Exception) {
            }
            
            // Subscription Management
            try {
                binding.manageSubscriptionsButton.setOnClickListener {
                    showSubscriptionManagement()
                }
            } catch (e: Exception) {
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupRecyclerView() {
        try {
            adminUserAdapter = AdminUserAdapter(
                onBanClick = { user -> 
                    try { showBanDialog(user) } catch (e: Exception) { 
                        Toast.makeText(requireContext(), "Error opening ban dialog", Toast.LENGTH_SHORT).show()
                    }
                },
                onUnbanClick = { user -> 
                    try { unbanUser(user) } catch (e: Exception) { 
                        Toast.makeText(requireContext(), "Error unbanning user", Toast.LENGTH_SHORT).show()
                    }
                },
                onVerifyClick = { user -> 
                    try { verifyUser(user) } catch (e: Exception) { 
                        Toast.makeText(requireContext(), "Error verifying user", Toast.LENGTH_SHORT).show()
                    }
                },
                onDeleteClick = { user -> 
                    try { showDeleteDialog(user) } catch (e: Exception) { 
                        Toast.makeText(requireContext(), "Error opening delete dialog", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            
            binding.usersRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = adminUserAdapter
                visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupVerificationRequests() {
        try {
            verificationRequestAdapter = VerificationRequestAdapter(
                onApproveClick = { request -> 
                    try { approveVerificationRequest(request) } catch (e: Exception) { 
                        Toast.makeText(requireContext(), "Error approving request", Toast.LENGTH_SHORT).show()
                    }
                },
                onRejectClick = { request -> 
                    try { showRejectDialog(request) } catch (e: Exception) { 
                        Toast.makeText(requireContext(), "Error opening reject dialog", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            
            binding.verificationRequestsRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = verificationRequestAdapter
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupSupportTickets() {
        try {
            supportTicketAdapter = SupportTicketAdapter(
                onAssignClick = { ticket -> 
                    try { assignTicket(ticket) } catch (e: Exception) { 
                        Toast.makeText(requireContext(), "Error assigning ticket", Toast.LENGTH_SHORT).show()
                    }
                },
                onViewClick = { ticket -> 
                    try { viewTicketDetails(ticket) } catch (e: Exception) { 
                        Toast.makeText(requireContext(), "Error opening ticket details", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            
            binding.supportTicketsRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = supportTicketAdapter
            }
            
            // Setup filter buttons
            binding.ticketsAllButton.setOnClickListener { applyTicketFilter("all") }
            binding.ticketsOpenButton.setOnClickListener { applyTicketFilter("open") }
            binding.ticketsUrgentButton.setOnClickListener { applyTicketFilter("urgent") }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val stats = firebaseRepository.getAdminStatistics()
                
                if (!isAdded) return@launch
                
                binding.totalUsersText.text = stats["totalUsers"]?.toString() ?: "0"
                binding.activeUsersText.text = stats["activeUsers"]?.toString() ?: "0"
                binding.verifiedUsersText.text = stats["verifiedUsers"]?.toString() ?: "0"
                binding.bannedUsersText.text = stats["bannedUsers"]?.toString() ?: "0"
                
                
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    binding.totalUsersText.text = "Error"
                    binding.activeUsersText.text = "Error"
                    binding.verifiedUsersText.text = "Error"
                    binding.bannedUsersText.text = "Error"
                    Toast.makeText(requireContext(), "Error loading statistics", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                
                allUsers = firebaseRepository.getAllUsersForAdmin()
                filteredUsers = allUsers
                
                if (!isAdded) return@launch
                
                binding.progressBar.visibility = View.GONE
                adminUserAdapter.updateUsers(filteredUsers)
                
                
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error loading users", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun applyFilter(filter: String) {
        try {
            currentFilter = filter
            
            try {
                binding.filterAllButton.isSelected = filter == "all"
                binding.filterVerifiedButton.isSelected = filter == "verified"
                binding.filterBannedButton.isSelected = filter == "banned"
            } catch (e: Exception) {
            }
            
            // Filter users
            filteredUsers = when (filter) {
                "verified" -> allUsers.filter { it.isVerified }
                "banned" -> allUsers.filter { it.isBanned }
                else -> allUsers
            }
            
            try {
                adminUserAdapter.updateUsers(filteredUsers)
            } catch (e: Exception) {
            }
            
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun searchUsers(query: String) {
        try {
            val searchQuery = query.lowercase().trim()
            
            val baseUsers = when (currentFilter) {
                "verified" -> allUsers.filter { it.isVerified }
                "banned" -> allUsers.filter { it.isBanned }
                else -> allUsers
            }
            
            filteredUsers = if (searchQuery.isEmpty()) {
                baseUsers
            } else {
                baseUsers.filter { user ->
                    try {
                        user.username.lowercase().contains(searchQuery) ||
                        user.device.lowercase().contains(searchQuery) ||
                        user.role.lowercase().contains(searchQuery)
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            
            try {
                adminUserAdapter.updateUsers(filteredUsers)
            } catch (e: Exception) {
            }
            
        } catch (e: Exception) {
        }
    }
    
    private fun showBanDialog(user: AdminUser) {
        try {
            val dialog = BanUserDialog.newInstance(user.username) { reason, duration ->
                banUser(user, reason, duration)
            }
            dialog.show(childFragmentManager, "ban_dialog")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error opening ban dialog", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun banUser(user: AdminUser, reason: String, duration: Long) {
        lifecycleScope.launch {
            try {
                val banInfo = com.chrissyx.zay.data.models.BanInfo(
                    type = "full",
                    reason = reason,
                    duration = duration,
                    expiry = if (duration == -1L) -1L else System.currentTimeMillis() + (duration * 1000),
                    bannedBy = userPreferences.username ?: "admin",
                    bannedAt = System.currentTimeMillis()
                )
                
                val success = firebaseRepository.banUser(user.username, banInfo)
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "User ${user.username} banned successfully", Toast.LENGTH_SHORT).show()
                    loadUsers() // Reload to update UI
                    loadStatistics() // Update stats
                } else {
                    Toast.makeText(requireContext(), "Failed to ban user ${user.username}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error banning user", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun unbanUser(user: AdminUser) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.unbanUser(user.username)
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "User ${user.username} unbanned successfully", Toast.LENGTH_SHORT).show()
                    loadUsers() // Reload to update UI
                    loadStatistics() // Update stats
                } else {
                    Toast.makeText(requireContext(), "Failed to unban user ${user.username}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error unbanning user", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun verifyUser(user: AdminUser) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.verifyUser(user.username)
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "User ${user.username} verified successfully", Toast.LENGTH_SHORT).show()
                    loadUsers() // Reload to update UI
                    loadStatistics() // Update stats
                } else {
                    Toast.makeText(requireContext(), "Failed to verify user ${user.username}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error verifying user", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showDeleteDialog(user: AdminUser) {
        try {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete User")
                .setMessage("Are you sure you want to permanently delete user @${user.username}?\n\nThis will:\n• Delete their account\n• Delete all their messages\n• Delete their explore profile\n• This action cannot be undone")
                .setPositiveButton("Delete") { _, _ ->
                    deleteUser(user)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening delete dialog", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteUser(user: AdminUser) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.deleteUser(user.username)
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "User ${user.username} deleted successfully", Toast.LENGTH_SHORT).show()
                    loadUsers() // Reload to update UI
                    loadStatistics() // Update stats
                } else {
                    Toast.makeText(requireContext(), "Failed to delete user ${user.username}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error deleting user", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendGlobalAnnouncement() {
        try {
            val title = binding.announcementTitleEditText.text.toString().trim()
            val message = binding.announcementMessageEditText.text.toString().trim()
            
            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in both title and message", Toast.LENGTH_SHORT).show()
                return
            }
            
            lifecycleScope.launch {
                try {
                    val success = firebaseRepository.sendGlobalAnnouncement(title, message)
                    
                    if (!isAdded) return@launch
                    
                    if (success) {
                        // Clear fields
                        binding.announcementTitleEditText.text.clear()
                        binding.announcementMessageEditText.text.clear()
                        
                        Toast.makeText(requireContext(), "Announcement sent successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to send announcement", Toast.LENGTH_SHORT).show()
                    }
                        
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error sending announcement", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadVerificationRequests() {
        lifecycleScope.launch {
            try {
                verificationRequests = firebaseRepository.getAllVerificationRequests()
                
                if (!isAdded) return@launch
                
                verificationRequestAdapter.updateRequests(verificationRequests)
                binding.verificationRequestsCountText.text = verificationRequests.size.toString()
                
                // Show/hide empty state
                if (verificationRequests.isEmpty()) {
                    binding.verificationRequestsEmptyText.visibility = View.VISIBLE
                    binding.verificationRequestsRecyclerView.visibility = View.GONE
                } else {
                    binding.verificationRequestsEmptyText.visibility = View.GONE
                    binding.verificationRequestsRecyclerView.visibility = View.VISIBLE
                }
                
                
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    binding.verificationRequestsCountText.text = "Error"
                    Toast.makeText(requireContext(), "Error loading verification requests", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun approveVerificationRequest(request: com.chrissyx.zay.data.models.VerificationRequest) {
        lifecycleScope.launch {
            try {
                val reviewerUsername = userPreferences.username ?: "admin"
                val success = firebaseRepository.approveVerificationRequest(request.username, reviewerUsername)
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "Verification request approved for @${request.username}", Toast.LENGTH_SHORT).show()
                    loadVerificationRequests() // Refresh list
                    loadStatistics() // Update stats
                } else {
                    Toast.makeText(requireContext(), "Failed to approve verification request", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error approving verification request", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showRejectDialog(request: com.chrissyx.zay.data.models.VerificationRequest) {
        try {
            val inputEditText = android.widget.EditText(requireContext())
            inputEditText.hint = "Reason for rejection (optional)"
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Reject Verification Request")
                .setMessage("Are you sure you want to reject the verification request for @${request.username}?")
                .setView(inputEditText)
                .setPositiveButton("Reject") { _, _ ->
                    val notes = inputEditText.text.toString().trim()
                    rejectVerificationRequest(request, notes)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening reject dialog", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun rejectVerificationRequest(request: com.chrissyx.zay.data.models.VerificationRequest, notes: String) {
        lifecycleScope.launch {
            try {
                val reviewerUsername = userPreferences.username ?: "admin"
                val success = firebaseRepository.rejectVerificationRequest(request.username, reviewerUsername, notes)
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "Verification request rejected for @${request.username}", Toast.LENGTH_SHORT).show()
                    loadVerificationRequests() // Refresh list
                } else {
                    Toast.makeText(requireContext(), "Failed to reject verification request", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error rejecting verification request", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadSupportTickets() {
        lifecycleScope.launch {
            try {
                supportTickets = firebaseRepository.getAllSupportTickets()
                
                if (!isAdded) return@launch
                
                applyTicketFilter(currentTicketFilter)
                
                // Update counters
                val openTickets = supportTickets.count { 
                    it.status == com.chrissyx.zay.data.models.TicketStatus.OPEN || 
                    it.status == com.chrissyx.zay.data.models.TicketStatus.IN_PROGRESS 
                }
                binding.openTicketsCountText.text = openTickets.toString()
                
                // Show/hide empty state
                if (supportTickets.isEmpty()) {
                    binding.supportTicketsEmptyText.visibility = View.VISIBLE
                    binding.supportTicketsRecyclerView.visibility = View.GONE
                } else {
                    binding.supportTicketsEmptyText.visibility = View.GONE
                    binding.supportTicketsRecyclerView.visibility = View.VISIBLE
                }
                
                
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    binding.openTicketsCountText.text = "!"
                    Toast.makeText(requireContext(), "Error loading support tickets", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showReplyDialog(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        try {
            val inputEditText = android.widget.EditText(requireContext())
            inputEditText.hint = "Reply to ticket (required)"
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Reply to Ticket")
                .setMessage("Reply to ticket #${ticket.id} for @${ticket.username}?")
                .setView(inputEditText)
                .setPositiveButton("Reply") { _, _ ->
                    val reply = inputEditText.text.toString().trim()
                    if (reply.isNotEmpty()) {
                        replyToTicket(ticket, reply)
                    } else {
                        Toast.makeText(requireContext(), "Reply cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening reply dialog", Toast.LENGTH_SHORT).show()
        }
    }

    private fun replyToTicket(ticket: com.chrissyx.zay.data.models.SupportTicket, reply: String) {
        lifecycleScope.launch {
            try {
                val adminUsername = userPreferences.username ?: "admin"
                val response = com.chrissyx.zay.data.models.AdminResponse(
                    id = java.util.UUID.randomUUID().toString(),
                    adminUsername = adminUsername,
                    message = reply,
                    timestamp = System.currentTimeMillis() / 1000.0,
                    type = com.chrissyx.zay.data.models.ResponseType.TEXT
                )
                
                val success = firebaseRepository.addAdminResponseToTicket(ticket.id, response)
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "Reply sent successfully", Toast.LENGTH_SHORT).show()
                    loadSupportTickets() // Refresh the list
                } else {
                    Toast.makeText(requireContext(), "Failed to send reply", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error sending reply", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun closeTicket(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.updateSupportTicketStatus(ticket.id, com.chrissyx.zay.data.models.TicketStatus.CLOSED)
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "Ticket closed successfully", Toast.LENGTH_SHORT).show()
                    loadSupportTickets() // Refresh list
                } else {
                    Toast.makeText(requireContext(), "Failed to close ticket", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error closing ticket", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun assignTicket(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        try {
            val adminUsername = userPreferences.username ?: "admin"
            
            lifecycleScope.launch {
                try {
                    val success = firebaseRepository.updateSupportTicketStatus(
                        ticket.id, 
                        com.chrissyx.zay.data.models.TicketStatus.IN_PROGRESS, 
                        adminUsername
                    )
                    
                    if (!isAdded) return@launch
                    
                    if (success) {
                        Toast.makeText(requireContext(), "Ticket assigned to you", Toast.LENGTH_SHORT).show()
                        loadSupportTickets() // Refresh list
                    } else {
                        Toast.makeText(requireContext(), "Failed to assign ticket", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error assigning ticket", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error assigning ticket", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun viewTicketDetails(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        try {
            val message = buildString {
                append("Ticket ID: ${ticket.id}\n")
                append("Subject: ${ticket.subject}\n\n")
                append("From: @${ticket.username}")
                if (ticket.platform != "UNKNOWN") {
                    append(" (${ticket.platform})")
                }
                append("\n")
                if (ticket.email.isNotEmpty()) {
                    append("Email: ${ticket.email}\n")
                }
                append("Category: ${ticket.category.displayName}\n")
                append("Priority: ${ticket.priority.displayName}\n")
                append("Status: ${ticket.status.displayName}\n")
                if (ticket.assignedTo.isNotEmpty()) {
                    append("Assigned to: ${ticket.assignedTo}\n")
                }
                append("Device: ${ticket.deviceInfo}\n")
                append("App Version: ${ticket.appVersion}\n")
                if (ticket.attachments.isNotEmpty()) {
                    append("📷 Attachments: ${ticket.attachments.size} image(s)\n")
                }
                append("\nDescription:\n${ticket.description}")
                
                if (ticket.internalNotes.isNotEmpty()) {
                    append("\n\nInternal Notes:\n${ticket.internalNotes}")
                }
                
                // Show admin responses if any
                if (ticket.adminResponses.isNotEmpty()) {
                    append("\n\nAdmin Responses:\n")
                    ticket.adminResponses.forEach { response ->
                        val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date((response.timestamp * 1000).toLong()))
                        append("[$date] ${response.adminUsername}: ${response.message}\n")
                    }
                }
            }
            
            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Support Ticket #${ticket.id}")
                .setMessage(message)
                .setPositiveButton("Close", null)
                .create()
            
            dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL, "More Actions") { _, _ ->
                showTicketActionsDialog(ticket)
            }
            
            // Add button to view images if attachments exist
            if (ticket.attachments.isNotEmpty()) {
                dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE, "View Images (${ticket.attachments.size})") { _, _ ->
                    showTicketImages(ticket)
                }
            }
            
            dialog.show()
                
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening ticket details", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showTicketImages(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        try {
            if (ticket.attachments.isEmpty()) {
                Toast.makeText(requireContext(), "No images attached to this ticket", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Create a scrollable view with all images
            val scrollView = android.widget.ScrollView(requireContext())
            val linearLayout = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }
            
            ticket.attachments.forEachIndexed { index, imageUrl ->
                // Add image title
                val titleText = android.widget.TextView(requireContext()).apply {
                    text = "Image ${index + 1}:"
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, if (index > 0) 24 else 0, 0, 8)
                }
                linearLayout.addView(titleText)
                
                // Add image view
                val imageView = android.widget.ImageView(requireContext()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 16
                    }
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    adjustViewBounds = true
                    maxHeight = 800 // Limit height to keep dialog manageable
                }
                
                // Load image with Glide
                com.bumptech.glide.Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(imageView)
                
                linearLayout.addView(imageView)
                
                // Add clickable URL text for copying/opening
                val urlText = android.widget.TextView(requireContext()).apply {
                    text = "📎 $imageUrl"
                    textSize = 12f
                    setTextColor(android.graphics.Color.BLUE)
                    setPadding(0, 0, 0, 16)
                    setOnClickListener {
                        // Copy URL to clipboard
                        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Image URL", imageUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireContext(), "Image URL copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
                linearLayout.addView(urlText)
            }
            
            scrollView.addView(linearLayout)
            
            // Show dialog with images
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("📷 Ticket Images (${ticket.attachments.size})")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Copy All URLs") { _, _ ->
                    val allUrls = ticket.attachments.joinToString("\n")
                    val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("All Image URLs", allUrls)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(requireContext(), "All image URLs copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                .show()
                
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading images: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showTicketActionsDialog(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        val actions = arrayOf(
            "📝 Respond by Text",
            "🔗 Generate Login Link",
            "📋 Add Internal Note", 
            "✅ Resolve Ticket",
            "🔄 Change Status"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Ticket Actions")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showRespondByTextDialog(ticket)
                    1 -> showGenerateLoginLinkDialog(ticket)
                    2 -> showAddNoteDialog(ticket)
                    3 -> resolveTicket(ticket)
                    4 -> showChangeStatusDialog(ticket)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showRespondByTextDialog(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        try {
            val inputEditText = android.widget.EditText(requireContext())
            inputEditText.hint = "Reply to ticket (required)"
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Reply to Ticket #${ticket.id}")
                .setMessage("Reply to ticket #${ticket.id} for @${ticket.username}?")
                .setView(inputEditText)
                .setPositiveButton("Reply") { _, _ ->
                    val reply = inputEditText.text.toString().trim()
                    if (reply.isNotEmpty()) {
                        replyToTicket(ticket, reply)
                    } else {
                        Toast.makeText(requireContext(), "Reply cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening respond by text dialog", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showGenerateLoginLinkDialog(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        try {
            val inputEditText = android.widget.EditText(requireContext())
            inputEditText.hint = "Enter target username (e.g., _c_ssyx)"
            inputEditText.setText("@${ticket.username}") // Pre-fill with ticket username
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Generate Login Link")
                .setMessage("Enter the username you want to generate a login link for:")
                .setView(inputEditText)
                .setPositiveButton("Generate TinyURL") { _, _ ->
                    val targetUsername = inputEditText.text.toString().trim().removePrefix("@")
                    if (targetUsername.isNotEmpty()) {
                        generateLoginLinkForUser(ticket, targetUsername)
                    } else {
                        Toast.makeText(requireContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening login link dialog", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun generateLoginLinkForUser(ticket: com.chrissyx.zay.data.models.SupportTicket, targetUsername: String) {
        lifecycleScope.launch {
            try {
                // Generate unique login components exactly as specified
                val ticketId = ticket.id
                val loginKey = com.chrissyx.zay.utils.TicketUtils.generateLoginKey()
                
                // Create the exact deep link format: zay://zayapi/supportticket/id?=JH13BNK/key?=872977ndokn928ndo93bdbla
                val deepLink = "zay://zayapi/supportticket/id?=${ticketId}/key?=${loginKey}"
                
                // Generate TinyURL using the actual API
                val tinyUrl = try {
                    val result = com.chrissyx.zay.utils.TicketUtils.generateTinyUrl(deepLink)
                    result
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                
                // Store login link in Firestore for tracking and expiration
                val loginLink = com.chrissyx.zay.data.models.LoginLink(
                    id = loginKey, // Use the login key as document ID
                    ticketId = ticketId,
                    targetUsername = targetUsername,
                    linkKey = loginKey,
                    adminUsername = userPreferences.username ?: "admin",
                    createdAt = System.currentTimeMillis() / 1000.0,
                    expiresAt = (System.currentTimeMillis() / 1000.0) + (24 * 60 * 60), // 24 hours expiration
                    isUsed = false,
                    usedAt = 0.0
                )
                
                val success = firebaseRepository.createLoginLink(loginLink)
                
                if (!isAdded) return@launch
                
                if (success) {
                    // Show the generated links to admin
                    val dialogMessage = if (tinyUrl != null) {
                        "✅ TinyURL created successfully!\n\n🔗 TinyURL:\n$tinyUrl\n\n📋 Original Deep Link:\n$deepLink\n\n⏰ This link will log the user directly into @$targetUsername's account.\n\n🕒 Link expires in 24 hours or after first use.\n📱 When clicked, the link opens the app and auto-deletes from Firestore."
                    } else {
                        "⚠️ TinyURL generation failed, but deep link is ready!\n\n📋 Deep Link:\n$deepLink\n\n⏰ This link will log the user directly into @$targetUsername's account.\n\n🕒 Link expires in 24 hours or after first use.\n📱 When clicked, the link opens the app and auto-deletes from Firestore."
                    }
                    
                    val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Login Link Generated for @$targetUsername")
                        .setMessage(dialogMessage)
                        .setNeutralButton("Copy Deep Link") { _, _ ->
                            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Deep Link", deepLink)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(requireContext(), "Deep link copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    
                    // Only show TinyURL options if generation was successful
                    if (tinyUrl != null) {
                        builder.setPositiveButton("Copy TinyURL") { _, _ ->
                            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Login TinyURL", tinyUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(requireContext(), "TinyURL copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Share TinyURL") { _, _ ->
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "Login link for @$targetUsername: $tinyUrl")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Zay Login Link")
                            }
                            startActivity(android.content.Intent.createChooser(shareIntent, "Share Login Link"))
                        }
                    } else {
                        builder.setPositiveButton("Share Deep Link") { _, _ ->
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "Login link for @$targetUsername: $deepLink")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Zay Login Link")
                            }
                            startActivity(android.content.Intent.createChooser(shareIntent, "Share Login Link"))
                        }
                    }
                    
                    builder.show()
                } else {
                    Toast.makeText(requireContext(), "Failed to create login link", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error generating login link", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddNoteDialog(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        try {
            val inputEditText = android.widget.EditText(requireContext())
            inputEditText.hint = "Internal note (visible to admins only)"
            inputEditText.setText(ticket.internalNotes)
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add Internal Note")
                .setView(inputEditText)
                .setPositiveButton("Save") { _, _ ->
                    val note = inputEditText.text.toString().trim()
                    saveTicketNote(ticket.id, note)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
        }
    }
    
    private fun saveTicketNote(ticketId: String, note: String) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.addSupportTicketNote(ticketId, note)
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "Note saved", Toast.LENGTH_SHORT).show()
                    loadSupportTickets() // Refresh list
                } else {
                    Toast.makeText(requireContext(), "Failed to save note", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error saving note", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun resolveTicket(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.updateSupportTicketStatus(
                    ticket.id, 
                    com.chrissyx.zay.data.models.TicketStatus.RESOLVED
                )
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "Ticket marked as resolved", Toast.LENGTH_SHORT).show()
                    loadSupportTickets() // Refresh list
                } else {
                    Toast.makeText(requireContext(), "Failed to resolve ticket", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error resolving ticket", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyTicketFilter(filter: String) {
        try {
            currentTicketFilter = filter
            
            filteredTickets = when (filter) {
                "open" -> supportTickets.filter { 
                    it.status == com.chrissyx.zay.data.models.TicketStatus.OPEN ||
                    it.status == com.chrissyx.zay.data.models.TicketStatus.IN_PROGRESS
                }
                "urgent" -> supportTickets.filter { 
                    it.priority == com.chrissyx.zay.data.models.TicketPriority.HIGH ||
                    it.priority == com.chrissyx.zay.data.models.TicketPriority.URGENT
                }
                else -> supportTickets
            }
            
            supportTicketAdapter.updateTickets(filteredTickets)
            
            // Update button states
            val buttons = listOf(
                binding.ticketsAllButton to "all",
                binding.ticketsOpenButton to "open", 
                binding.ticketsUrgentButton to "urgent"
            )
            
            buttons.forEach { (button, buttonFilter) ->
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (buttonFilter == filter) Color.parseColor("#2196F3") else Color.parseColor("#444444")
                )
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun refreshAllData() {
        try {
            
            // Update last updated time
            val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            binding.lastUpdateText.text = "Last updated: $currentTime"
            
            // Show loading state
            Toast.makeText(requireContext(), "🔄 Refreshing admin data...", Toast.LENGTH_SHORT).show()
            
            // Reload all data
            loadStatistics()
            loadUsers()
            loadVerificationRequests()
            loadSupportTickets()
            
            // Success feedback
            lifecycleScope.launch {
                kotlinx.coroutines.delay(1000)
                if (isAdded) {
                    Toast.makeText(requireContext(), "✅ Data refreshed successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "❌ Failed to refresh data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showChangeStatusDialog(ticket: com.chrissyx.zay.data.models.SupportTicket) {
        try {
            val statuses = com.chrissyx.zay.data.models.TicketStatus.values()
            val statusNames = statuses.map { it.displayName }.toTypedArray()
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Change Ticket Status")
                .setItems(statusNames) { _, which ->
                    val newStatus = statuses[which]
                    changeTicketStatus(ticket, newStatus)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
        }
    }
    
    private fun changeTicketStatus(ticket: com.chrissyx.zay.data.models.SupportTicket, newStatus: com.chrissyx.zay.data.models.TicketStatus) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.updateSupportTicketStatus(ticket.id, newStatus, userPreferences.username ?: "admin")
                
                if (!isAdded) return@launch
                
                if (success) {
                    Toast.makeText(requireContext(), "Status updated to ${newStatus.displayName}", Toast.LENGTH_SHORT).show()
                    loadSupportTickets() // Refresh the list
                } else {
                    Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error updating status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Device Management Methods
    private fun viewAllDevices() {
        lifecycleScope.launch {
            try {
                // Use existing allUsers data instead of calling non-existent method
                var totalDevices = 0
                val deviceInfo = StringBuilder()
                
                deviceInfo.append("📱 TRUSTED DEVICES OVERVIEW\n\n")
                
                for (adminUser in allUsers) {
                    // Convert AdminUser to User to get device info
                    val user = firebaseRepository.getUserByUsername(adminUser.username)
                    if (user != null) {
                        val deviceCount = user.trustedDevices.size
                        if (deviceCount > 0) {
                            totalDevices += deviceCount
                            deviceInfo.append("👤 ${user.username} (${user.platform})\n")
                            deviceInfo.append("   Devices: $deviceCount\n\n")
                            
                            user.trustedDeviceInfo.forEach { (deviceId: String, info: String) ->
                                deviceInfo.append("   • $info\n")
                                deviceInfo.append("     ID: ${deviceId.take(8)}...\n")
                            }
                            deviceInfo.append("\n")
                        }
                    }
                }
                
                if (totalDevices == 0) {
                    deviceInfo.append("No trusted devices found across all users.")
                } else {
                    deviceInfo.insert(0, "Total Devices: $totalDevices\n\n")
                }
                
                if (isAdded) {
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("All Trusted Devices")
                        .setMessage(deviceInfo.toString())
                        .setPositiveButton("OK", null)
                        .show()
                }
                
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error loading device information", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun clearUserDevices() {
        val username = binding.deviceSearchView.query.toString().trim()
        
        if (username.isBlank()) {
            Toast.makeText(requireContext(), "Enter a username to clear devices", Toast.LENGTH_SHORT).show()
            return
        }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear User Devices")
            .setMessage("Remove ALL trusted devices for user: $username?\n\nThis will force them to verify their account on all devices.")
            .setPositiveButton("Clear All") { _, _ ->
                performClearUserDevices(username)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performClearUserDevices(username: String) {
        lifecycleScope.launch {
            try {
                val user = firebaseRepository.getUserByUsername(username)
                if (user == null) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "User not found: $username", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                val deviceCount = user.trustedDevices.size
                
                // Clear all trusted devices for this user
                val success = firebaseRepository.clearAllTrustedDevices(username)
                
                if (success) {
                    if (isAdded) {
                        binding.deviceCountInfoText.text = "Cleared $deviceCount devices for $username"
                        Toast.makeText(requireContext(), "Successfully cleared $deviceCount devices for $username", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to clear devices for $username", Toast.LENGTH_SHORT).show()
                    }
                }
                
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error clearing devices: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun searchUserDevices(username: String) {
        if (username.isBlank()) {
            binding.deviceCountInfoText.text = "Select a user to view their trusted devices"
            return
        }
        
        lifecycleScope.launch {
            try {
                val user = firebaseRepository.getUserByUsername(username)
                if (user == null) {
                    binding.deviceCountInfoText.text = "User not found: $username"
                    return@launch
                }
                
                val deviceCount = user.trustedDevices.size
                if (deviceCount == 0) {
                    binding.deviceCountInfoText.text = "$username has no trusted devices"
                } else {
                    binding.deviceCountInfoText.text = "$username has $deviceCount trusted device(s) - Click to manage"
                    binding.deviceCountInfoText.setOnClickListener {
                        showUserDeviceManagement(username, user.trustedDevices, user.trustedDeviceInfo)
                    }
                }
                
            } catch (e: Exception) {
                binding.deviceCountInfoText.text = "Error loading device info for $username"
            }
        }
    }
    
    private fun showUserDeviceManagement(username: String, trustedDevices: List<String>, deviceInfo: Map<String, String>) {
        val deviceList = trustedDevices.mapIndexed { index, deviceId ->
            val info = deviceInfo[deviceId] ?: "Unknown Device"
            "Device ${index + 1}: $info"
        }
        
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("📱 Manage Devices for @$username")
            .setMessage("${trustedDevices.size} trusted device(s)")
        
        if (trustedDevices.isNotEmpty()) {
            // Show devices with individual remove options
            val deviceOptions = deviceList.toMutableList()
            deviceOptions.add("🗑️ Clear All Devices")
            
            dialogBuilder.setItems(deviceOptions.toTypedArray()) { _, which ->
                if (which == deviceOptions.size - 1) {
                    // Clear all devices option
                    showAdminClearAllDevicesDialog(username)
                } else {
                    // Individual device removal
                    val selectedDeviceId = trustedDevices[which]
                    val selectedDeviceInfo = deviceInfo[selectedDeviceId] ?: "Unknown Device"
                    showAdminRemoveDeviceDialog(username, selectedDeviceId, selectedDeviceInfo)
                }
            }
        }
        
        dialogBuilder
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun showAdminRemoveDeviceDialog(username: String, deviceId: String, deviceInfo: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Remove Device")
            .setMessage("Remove trusted device for @$username?\n\nDevice: $deviceInfo\n\nThis user will need to verify their account if they log in from this device again.")
            .setPositiveButton("Remove Device") { _, _ ->
                adminRemoveDevice(username, deviceId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAdminClearAllDevicesDialog(username: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🗑️ Clear All Devices")
            .setMessage("Remove ALL trusted devices for @$username?\n\nThis will force them to verify their account on ALL devices.\n\nThis action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                adminClearAllDevices(username)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun adminRemoveDevice(username: String, deviceId: String) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.removeTrustedDevice(username, deviceId)
                if (success) {
                    Toast.makeText(requireContext(), "✅ Device removed for @$username", Toast.LENGTH_SHORT).show()
                    // Refresh the search to update device count
                    searchUserDevices(username)
                } else {
                    Toast.makeText(requireContext(), "❌ Failed to remove device", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun adminClearAllDevices(username: String) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.clearAllTrustedDevices(username)
                if (success) {
                    Toast.makeText(requireContext(), "✅ All devices cleared for @$username", Toast.LENGTH_SHORT).show()
                    // Refresh the search to update device count
                    searchUserDevices(username)
                } else {
                    Toast.makeText(requireContext(), "❌ Failed to clear devices", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showSubscriptionManagement() {
        val options = arrayOf(
            "View All Pro Users",
            "Grant Pro to User",
            "Revoke Pro from User",
            "View Subscription Statistics"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("💎 Manage User Subscriptions")
            .setMessage("Select an action to manage user subscriptions:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAllProUsers()
                    1 -> showGrantProDialog()
                    2 -> showRevokeProDialog()
                    3 -> showSubscriptionStats()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAllProUsers() {
        lifecycleScope.launch {
            try {
                val proUsers = firebaseRepository.getAllProUsers()
                val userList = proUsers.joinToString("\n") { "• @${it.username} (${it.platform})" }
                
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("💎 Pro Users (${proUsers.size} total)")
                    .setMessage(if (proUsers.isNotEmpty()) userList else "No Pro users found")
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading Pro users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showGrantProDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "Enter username"
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Grant Pro Subscription")
            .setMessage("Enter the username to grant Pro access:")
            .setView(input)
            .setPositiveButton("Grant Pro") { _, _ ->
                val username = input.text.toString().trim()
                if (username.isNotEmpty()) {
                    grantProToUser(username)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showRevokeProDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "Enter username"
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Revoke Pro Subscription")
            .setMessage("Enter the username to revoke Pro access:")
            .setView(input)
            .setPositiveButton("Revoke Pro") { _, _ ->
                val username = input.text.toString().trim()
                if (username.isNotEmpty()) {
                    revokeProFromUser(username)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun grantProToUser(username: String) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.updateUserProStatus(username, true)
                if (success) {
                    Toast.makeText(requireContext(), "✅ Granted Pro to @$username", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "❌ Failed to grant Pro to @$username", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun revokeProFromUser(username: String) {
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.updateUserProStatus(username, false)
                if (success) {
                    Toast.makeText(requireContext(), "✅ Revoked Pro from @$username", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "❌ Failed to revoke Pro from @$username", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showSubscriptionStats() {
        lifecycleScope.launch {
            try {
                val totalUsers = allUsers.size
                val proUsers = allUsers.count { it.isPro }
                val freeUsers = totalUsers - proUsers
                val proPercentage = if (totalUsers > 0) (proUsers * 100.0 / totalUsers) else 0.0
                
                val statsMessage = """
                    📊 Subscription Statistics
                    
                    Total Users: $totalUsers
                    Pro Users: $proUsers (${String.format("%.1f", proPercentage)}%)
                    Free Users: $freeUsers
                    
                    Revenue (Est.): $${String.format("%.2f", proUsers * 1.99)}/month
                """.trimIndent()
                
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("📊 Subscription Statistics")
                    .setMessage(statsMessage)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading stats: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}