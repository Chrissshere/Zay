package com.chrissyx.zay.ui.main.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chrissyx.zay.data.models.Message
import com.chrissyx.zay.databinding.FragmentMessageDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.*

class MessageDetailFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentMessageDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var message: Message

    companion object {
        private const val ARG_MESSAGE_ID = "message_id"
        private const val ARG_MESSAGE_TEXT = "message_text"
        private const val ARG_MESSAGE_TIMESTAMP = "message_timestamp"
        private const val ARG_MESSAGE_SENDER = "message_sender"
        private const val ARG_MESSAGE_DEVICE = "message_device"

        fun newInstance(message: Message): MessageDetailFragment {
            val fragment = MessageDetailFragment()
            val args = Bundle().apply {
                putString(ARG_MESSAGE_ID, message.id)
                putString(ARG_MESSAGE_TEXT, message.text)
                putDouble(ARG_MESSAGE_TIMESTAMP, message.timestamp)
                putString(ARG_MESSAGE_SENDER, message.sender)
                putString(ARG_MESSAGE_DEVICE, message.device)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            message = Message(
                id = args.getString(ARG_MESSAGE_ID) ?: "",
                text = args.getString(ARG_MESSAGE_TEXT) ?: "",
                timestamp = args.getDouble(ARG_MESSAGE_TIMESTAMP),
                sender = args.getString(ARG_MESSAGE_SENDER) ?: "",
                device = args.getString(ARG_MESSAGE_DEVICE) ?: ""
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
    }

        private fun setupUI() {
        // Message text
        binding.messageText.text = message.text

        // Format date
        val date = Date((message.timestamp * 1000).toLong())
        val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.timestampText.text = dateFormatter.format(date)

        // Get actual Pro status
        val userPreferences = com.chrissyx.zay.utils.UserPreferences(requireContext())
        val isPro = userPreferences.isPro

        if (isPro && message.sender.isNotEmpty()) {
            // Show sender info for Pro users
            binding.senderInfoLayout.visibility = View.VISIBLE
            binding.anonymousLayout.visibility = View.GONE
            
            binding.senderUsernameText.text = "from: @${message.sender}"
            
            binding.showSenderInfoButton.setOnClickListener {
                val senderInfoFragment = SenderInfoFragment.newInstance(message.sender)
                senderInfoFragment.show(parentFragmentManager, "SenderInfo")
            }
        } else {
            // Show anonymous for non-Pro users
            binding.senderInfoLayout.visibility = View.GONE
            binding.anonymousLayout.visibility = View.VISIBLE
        }
        
        // Close button
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 