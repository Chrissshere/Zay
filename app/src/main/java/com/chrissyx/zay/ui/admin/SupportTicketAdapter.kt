package com.chrissyx.zay.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chrissyx.zay.data.models.SupportTicket
import com.chrissyx.zay.data.models.TicketPriority
import com.chrissyx.zay.data.models.TicketStatus
import com.chrissyx.zay.databinding.ItemSupportTicketBinding

class SupportTicketAdapter(
    private val onAssignClick: (SupportTicket) -> Unit,
    private val onViewClick: (SupportTicket) -> Unit
) : RecyclerView.Adapter<SupportTicketAdapter.TicketViewHolder>() {

    private var tickets = listOf<SupportTicket>()
    
    fun updateTickets(newTickets: List<SupportTicket>) {
        tickets = newTickets
        notifyDataSetChanged()
    }
    
    override fun getItemCount(): Int = tickets.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemSupportTicketBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(tickets[position])
    }

    inner class TicketViewHolder(
        private val binding: ItemSupportTicketBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: SupportTicket) {
            binding.subjectText.text = ticket.subject
            binding.usernameText.text = "@${ticket.username}"
            binding.categoryText.text = ticket.category.displayName
            binding.descriptionPreview.text = ticket.description
            
            // Priority badge
            binding.priorityBadge.text = ticket.priority.name
            binding.priorityBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor(ticket.priority.color)
            )
            
            // Status badge
            binding.statusBadge.text = ticket.status.name
            binding.statusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor(ticket.status.color)
            )
            
            // Format time
            val now = System.currentTimeMillis() / 1000.0
            val diff = now - ticket.createdAt
            val timeText = when {
                diff < 60 -> "Just now"
                diff < 3600 -> "${(diff / 60).toInt()}m ago"
                diff < 86400 -> "${(diff / 3600).toInt()}h ago"
                else -> "${(diff / 86400).toInt()}d ago"
            }
            binding.timeText.text = timeText
            
            // Action buttons
            binding.assignButton.setOnClickListener {
                onAssignClick(ticket)
            }
            
            binding.viewButton.setOnClickListener {
                onViewClick(ticket)
            }
            
            // Update assign button text based on assignment
            if (ticket.assignedTo.isNotEmpty()) {
                binding.assignButton.text = "Assigned"
                binding.assignButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#666666")
                )
            } else {
                binding.assignButton.text = "Assign"
                binding.assignButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#4CAF50")
                )
            }
        }
    }
} 