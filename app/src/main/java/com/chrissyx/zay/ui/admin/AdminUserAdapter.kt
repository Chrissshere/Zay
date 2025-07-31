package com.chrissyx.zay.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chrissyx.zay.R
import com.chrissyx.zay.data.models.AdminUser
import com.chrissyx.zay.data.models.Platform
import com.chrissyx.zay.databinding.ItemAdminUserBinding
import java.text.SimpleDateFormat
import java.util.*

class AdminUserAdapter(
    private val onBanClick: (AdminUser) -> Unit,
    private val onUnbanClick: (AdminUser) -> Unit,
    private val onVerifyClick: (AdminUser) -> Unit,
    private val onDeleteClick: (AdminUser) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>() {

    private var users = listOf<AdminUser>()
    
    fun updateUsers(newUsers: List<AdminUser>) {
        users = newUsers
        notifyDataSetChanged()
    }
    
    override fun getItemCount(): Int = users.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemAdminUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    inner class UserViewHolder(
        private val binding: ItemAdminUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: AdminUser) {
            binding.usernameText.text = "@${user.username}"
            binding.roleText.text = user.role.uppercase()
            
            // Show verification checkmark for Instagram-verified users
            binding.verificationCheckmark.visibility = if (user.isVerified && user.platform == Platform.INSTAGRAM) {
                View.VISIBLE
            } else {
                View.GONE
            }
            
            // Format creation date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val createdDate = Date((user.createdAt * 1000).toLong())
            binding.createdDateText.text = "Joined ${dateFormat.format(createdDate)}"
            
            // Show status badges
            binding.verifiedBadge.visibility = if (user.isVerified) View.VISIBLE else View.GONE
            binding.proBadge.visibility = if (user.isPro) View.VISIBLE else View.GONE
            binding.bannedBadge.visibility = if (user.isBanned) View.VISIBLE else View.GONE
            
            // Show device info
            binding.deviceText.text = if (user.device.isNotEmpty()) user.device else "Unknown Device"
            
            // Show ban info if user is banned
            if (user.isBanned && user.banInfo != null) {
                binding.banInfoText.visibility = View.VISIBLE
                binding.banInfoText.text = "Banned: ${user.banInfo!!.reason}"
            } else {
                binding.banInfoText.visibility = View.GONE
            }
            
            // Set up action buttons based on user status
            setupActionButtons(user)
        }
        
        private fun setupActionButtons(user: AdminUser) {
            // Ban/Unban button
            if (user.isBanned) {
                binding.banButton.text = "Unban"
                binding.banButton.setBackgroundColor(binding.root.context.getColor(R.color.link_text_color))
                binding.banButton.setOnClickListener { onUnbanClick(user) }
            } else {
                binding.banButton.text = "Ban"
                binding.banButton.setBackgroundColor(binding.root.context.getColor(android.R.color.holo_red_dark))
                binding.banButton.setOnClickListener { onBanClick(user) }
            }
            
            // Verify button
            if (user.isVerified) {
                binding.verifyButton.text = "Verified"
                binding.verifyButton.isEnabled = false
                binding.verifyButton.alpha = 0.5f
            } else {
                binding.verifyButton.text = "Verify"
                binding.verifyButton.isEnabled = true
                binding.verifyButton.alpha = 1.0f
                binding.verifyButton.setOnClickListener { onVerifyClick(user) }
            }
            
            // Delete button
            binding.deleteButton.setOnClickListener { onDeleteClick(user) }
        }
    }
} 