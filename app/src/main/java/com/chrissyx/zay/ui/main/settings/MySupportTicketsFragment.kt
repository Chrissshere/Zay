package com.chrissyx.zay.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chrissyx.zay.R
import com.chrissyx.zay.data.models.SupportTicket
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.databinding.FragmentMySupportTicketsBinding
import com.chrissyx.zay.ui.admin.SupportTicketAdapter
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.launch

class MySupportTicketsFragment : Fragment() {
    
    private var _binding: FragmentMySupportTicketsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var supportTicketAdapter: SupportTicketAdapter
    private var supportTickets = listOf<SupportTicket>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMySupportTicketsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            userPreferences = UserPreferences(requireContext())
            firebaseRepository = FirebaseRepository()
            
            setupUI()
            loadMyTickets()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading support tickets", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupUI() {
        // Back button
        binding.backButton.setOnClickListener {
            try {
                requireActivity().supportFragmentManager.popBackStack()
            } catch (e: Exception) {
            }
        }
        
        // Setup RecyclerView
        supportTicketAdapter = SupportTicketAdapter(
            onAssignClick = { _ -> }, // Users can't assign tickets - provide empty lambda
            onViewClick = { ticket -> viewTicketDetails(ticket) }
        )
        
        binding.ticketsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = supportTicketAdapter
        }
        
        // Refresh button
        binding.refreshButton.setOnClickListener {
            loadMyTickets()
        }
    }
    
    private fun loadMyTickets() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.ticketsRecyclerView.visibility = View.GONE
                binding.emptyStateText.visibility = View.GONE
                
                val username = userPreferences.username ?: ""
                supportTickets = firebaseRepository.getUserSupportTickets(username)
                
                if (!isAdded) return@launch
                
                binding.progressBar.visibility = View.GONE
                
                if (supportTickets.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.ticketsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                    binding.ticketsRecyclerView.visibility = View.VISIBLE
                    supportTicketAdapter.updateTickets(supportTickets)
                }
                
                // Update counter
                binding.ticketCountText.text = "${supportTickets.size} ticket${if (supportTickets.size != 1) "s" else ""}"
                
                
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.emptyStateText.text = "Error loading tickets"
                    Toast.makeText(requireContext(), "Error loading support tickets", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun viewTicketDetails(ticket: SupportTicket) {
        try {
            val message = buildString {
                append("Subject: ${ticket.subject}\n\n")
                append("Category: ${ticket.category.displayName}\n")
                append("Priority: ${ticket.priority.displayName}\n")
                append("Status: ${ticket.status.displayName}\n")
                if (ticket.assignedTo.isNotEmpty()) {
                    append("Assigned to: ${ticket.assignedTo}\n")
                }
                val createdDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date((ticket.createdAt * 1000).toLong()))
                append("Created: $createdDate\n\n")
                append("Description:\n${ticket.description}")
                
                if (ticket.resolution.isNotEmpty()) {
                    append("\n\nResolution:\n${ticket.resolution}")
                }
            }
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Support Ticket #${ticket.id.take(8)}")
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show()
                
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening ticket details", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 