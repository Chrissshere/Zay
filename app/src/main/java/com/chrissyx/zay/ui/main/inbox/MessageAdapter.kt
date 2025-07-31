package com.chrissyx.zay.ui.main.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chrissyx.zay.R
import com.chrissyx.zay.data.models.Message
import com.chrissyx.zay.utils.DeviceUtils
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val isPro: Boolean,
    private val onMessageClick: (Message) -> Unit,
    private val onSenderInfoClick: (Message) -> Unit,
    private val onRespondClick: (Message) -> Unit
) : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        private val senderInfoButton: MaterialButton = itemView.findViewById(R.id.senderInfoButton)
        private val anonymousLayout: LinearLayout = itemView.findViewById(R.id.anonymousLayout)
        private val respondButton: MaterialButton = itemView.findViewById(R.id.respondButton)

        fun bind(message: Message) {
            messageText.text = message.text
            
            // Format timestamp (only time, like iOS)
            val date = Date((message.timestamp * 1000).toLong())
            val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
            timestamp.text = timeFormatter.format(date)
            
            // Handle Pro vs Non-Pro user experience
            if (isPro && message.sender.isNotEmpty()) {
                // Show sender info button for Pro users
                senderInfoButton.visibility = View.VISIBLE
                anonymousLayout.visibility = View.GONE
                
                senderInfoButton.setOnClickListener {
                    onSenderInfoClick(message)
                }
            } else {
                // Show anonymous indicator for non-Pro users or when no sender
                senderInfoButton.visibility = View.GONE
                anonymousLayout.visibility = View.VISIBLE
            }
            
            // Handle message card click
            itemView.setOnClickListener {
                onMessageClick(message)
            }
            
            // Handle respond button click
            respondButton.setOnClickListener {
                onRespondClick(message)
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
} 