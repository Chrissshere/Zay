package com.chrissyx.zay.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chrissyx.zay.databinding.ItemTrustedDeviceBinding

class TrustedDeviceAdapter(
    private val onRemoveClick: (deviceId: String, deviceInfo: String) -> Unit
) : ListAdapter<TrustedDeviceItem, TrustedDeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemTrustedDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(
        private val binding: ItemTrustedDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TrustedDeviceItem) {
            binding.deviceInfoText.text = item.deviceInfo
            binding.deviceIdText.text = "ID: ${item.deviceId.take(8)}..."
            
            if (item.isCurrentDevice) {
                binding.currentDeviceBadge.visibility = View.VISIBLE
                binding.removeButton.visibility = View.GONE
            } else {
                binding.currentDeviceBadge.visibility = View.GONE
                binding.removeButton.visibility = View.VISIBLE
                binding.removeButton.setOnClickListener {
                    onRemoveClick(item.deviceId, item.deviceInfo)
                }
            }
        }
    }

    private class DeviceDiffCallback : DiffUtil.ItemCallback<TrustedDeviceItem>() {
        override fun areItemsTheSame(oldItem: TrustedDeviceItem, newItem: TrustedDeviceItem): Boolean {
            return oldItem.deviceId == newItem.deviceId
        }

        override fun areContentsTheSame(oldItem: TrustedDeviceItem, newItem: TrustedDeviceItem): Boolean {
            return oldItem == newItem
        }
    }
}