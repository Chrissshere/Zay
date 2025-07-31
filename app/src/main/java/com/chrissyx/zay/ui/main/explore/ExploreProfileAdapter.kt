package com.chrissyx.zay.ui.main.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chrissyx.zay.R
import com.chrissyx.zay.data.models.ExploreProfile
import com.chrissyx.zay.databinding.ItemExploreProfileBinding
import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy

class ExploreProfileAdapter(
    private val onChatClick: (ExploreProfile) -> Unit,
    private val onSendMessageClick: (ExploreProfile) -> Unit
) : ListAdapter<ExploreProfile, ExploreProfileAdapter.ExploreProfileViewHolder>(ExploreProfileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreProfileViewHolder {
        val binding = ItemExploreProfileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExploreProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExploreProfileViewHolder, position: Int) {
        val profile = getItem(position)
        holder.bind(profile)
    }

    inner class ExploreProfileViewHolder(
        private val binding: ItemExploreProfileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: ExploreProfile) {
            binding.apply {
                // Set username
                usernameTextView.text = "@${profile.username}"
                
                // Show verification checkmark for verified users who want it displayed
                verificationCheckmark.visibility = if (profile.isVerified && profile.showVerificationInExplore) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                
                // Set prompt
                promptTextView.text = profile.prompt
                
                // Load banner image with proper cache management
                if (profile.bannerImageURL.isNotEmpty()) {
                    
                    Glide.with(itemView.context)
                        .load(profile.bannerImageURL)
                        .skipMemoryCache(true) // Skip memory cache for fresh images
                        .diskCacheStrategy(DiskCacheStrategy.DATA) // Cache processed image data
                        .placeholder(R.drawable.placeholder_banner)
                        .error(R.drawable.placeholder_banner)
                        .centerCrop()
                        .into(bannerImageView)
                } else {
                    // Show placeholder if no banner
                    bannerImageView.setImageResource(R.drawable.placeholder_banner)
                }
                
                chatButton.setOnClickListener {
                    onChatClick(profile)
                }
                
                sendMessageButton.setOnClickListener {
                    onSendMessageClick(profile)
                }
            }
        }
    }
}

class ExploreProfileDiffCallback : DiffUtil.ItemCallback<ExploreProfile>() {
    override fun areItemsTheSame(oldItem: ExploreProfile, newItem: ExploreProfile): Boolean {
        return oldItem.username == newItem.username
    }

    override fun areContentsTheSame(oldItem: ExploreProfile, newItem: ExploreProfile): Boolean {
        return oldItem == newItem
    }
} 