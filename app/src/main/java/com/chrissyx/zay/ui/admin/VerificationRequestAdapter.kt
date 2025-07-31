package com.chrissyx.zay.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chrissyx.zay.data.models.VerificationRequest
import com.chrissyx.zay.databinding.ItemVerificationRequestBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class VerificationRequestAdapter(
    private val onApproveClick: (VerificationRequest) -> Unit,
    private val onRejectClick: (VerificationRequest) -> Unit
) : RecyclerView.Adapter<VerificationRequestAdapter.RequestViewHolder>() {

    private var requests = listOf<VerificationRequest>()
    
    fun updateRequests(newRequests: List<VerificationRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
    
    override fun getItemCount(): Int = requests.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemVerificationRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    inner class RequestViewHolder(
        private val binding: ItemVerificationRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: VerificationRequest) {
            binding.usernameText.text = "@${request.username}"
            binding.instagramUsernameText.text = "Instagram: @${request.instagramUsername}"
            binding.deviceInfoText.text = request.deviceInfo.ifEmpty { "Unknown Device" }
            
            // Format request time
            val now = System.currentTimeMillis() / 1000.0
            val diff = now - request.requestedAt
            val timeText = when {
                diff < 60 -> "Just now"
                diff < 3600 -> "${(diff / 60).toInt()}m ago"
                diff < 86400 -> "${(diff / 3600).toInt()}h ago"
                else -> "${(diff / 86400).toInt()}d ago"
            }
            binding.requestTimeText.text = timeText
            
            // Set up action buttons
            binding.approveButton.setOnClickListener {
                onApproveClick(request)
            }
            
            binding.rejectButton.setOnClickListener {
                onRejectClick(request)
            }
        }
    }
} 