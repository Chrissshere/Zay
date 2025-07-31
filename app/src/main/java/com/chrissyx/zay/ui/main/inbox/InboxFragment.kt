package com.chrissyx.zay.ui.main.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chrissyx.zay.data.models.Message
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.databinding.FragmentInboxBinding
import com.chrissyx.zay.utils.UserPreferences
import com.chrissyx.zay.utils.InstagramStoryHelper
import kotlinx.coroutines.launch

class InboxFragment : Fragment() {

    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: InboxViewModel
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            // Initialize ViewModel
            val userPreferences = UserPreferences(requireContext())
            val firebaseRepository = FirebaseRepository()
            viewModel = InboxViewModel(firebaseRepository, userPreferences)
            
            setupRecyclerView()
            setupUI()
            observeViewModel()
            
            // Load messages
            viewModel.loadMessages()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading inbox: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        try {
            messageAdapter = MessageAdapter(
                isPro = UserPreferences(requireContext()).isPro,
                onMessageClick = { message ->
                    showMessageDetail(message)
                },
                onSenderInfoClick = { message ->
                    showSenderInfo(message)
                },
                onRespondClick = { message ->
                    showRespondToMessage(message)
                }
            )
            binding.messagesRecyclerView.apply {
                adapter = messageAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }
        } catch (e: Exception) {
        }
    }

    private fun setupUI() {
        try {
            // Retry button
            binding.retryButton.setOnClickListener {
                viewModel.refreshMessages()
            }
        } catch (e: Exception) {
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            try {
                viewModel.uiState.collect { state ->
                    if (!isAdded || activity == null) return@collect
                    
                    
                    // Update loading state
                    binding.loadingLayout.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    // Update messages
                    if (!state.isLoading && state.errorMessage == null) {
                        if (state.messages.isEmpty()) {
                            // Show empty state
                            binding.messagesRecyclerView.visibility = View.GONE
                            binding.emptyLayout.visibility = View.VISIBLE
                            binding.errorLayout.visibility = View.GONE
                        } else {
                            // Show messages
                            binding.messagesRecyclerView.visibility = View.VISIBLE
                            binding.emptyLayout.visibility = View.GONE
                            binding.errorLayout.visibility = View.GONE
                            messageAdapter.submitList(state.messages)
                        }
                    }
                    
                    // Handle errors
                    state.errorMessage?.let { error ->
                        binding.messagesRecyclerView.visibility = View.GONE
                        binding.emptyLayout.visibility = View.GONE
                        binding.errorLayout.visibility = View.VISIBLE
                        binding.errorText.text = error
                        
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun showMessageDetail(message: Message) {
        // TODO: Show message detail bottom sheet (like iOS MessageDetailView)
        
        val messageDetailFragment = MessageDetailFragment.newInstance(message)
        messageDetailFragment.show(parentFragmentManager, "MessageDetail")
    }

    private fun showSenderInfo(message: Message) {
        // TODO: Show sender info bottom sheet
        
        val senderInfoFragment = SenderInfoFragment.newInstance(message.sender)
        senderInfoFragment.show(parentFragmentManager, "SenderInfo")
    }
    
    private fun showRespondToMessage(message: Message) {
        try {
            
            // Share the message response to Instagram story
            shareMessageResponseToStory(message)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error sharing response: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareMessageResponseToStory(message: Message) {
        lifecycleScope.launch {
            try {
                if (!isAdded || activity == null) return@launch
                
                // Get current user's prompt and profile info
                val userPreferences = UserPreferences(requireContext())
                val currentUsername = userPreferences.username ?: ""
                
                // Get user's prompt from Firebase
                val firebaseRepository = FirebaseRepository()
                val currentUser = firebaseRepository.getUserByUsername(currentUsername)
                val userPrompt = currentUser?.prompt ?: "send me anonymous messages!"
                val isVerified = currentUser?.isVerified ?: false
                val profileImageUrl = currentUser?.profilePictureURL
                
                // Load profile image if available, then share story
                if (!profileImageUrl.isNullOrEmpty()) {
                    com.bumptech.glide.Glide.with(requireContext())
                        .asBitmap()
                        .load(profileImageUrl)
                        .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                            override fun onResourceReady(resource: android.graphics.Bitmap, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                                InstagramStoryHelper.shareMessageResponseStory(
                                    this@InboxFragment,
                                    userPrompt,
                                    message.text,
                                    resource,
                                    isVerified,
                                    currentUsername
                                )
                            }
                            
                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                                InstagramStoryHelper.shareMessageResponseStory(
                                    this@InboxFragment,
                                    userPrompt,
                                    message.text,
                                    null,
                                    isVerified,
                                    currentUsername
                                )
                            }
                        })
                } else {
                    // Share story without profile picture
                    InstagramStoryHelper.shareMessageResponseStory(
                        this@InboxFragment,
                        userPrompt,
                        message.text,
                        null,
                        isVerified,
                        currentUsername
                    )
                }
                
            } catch (e: Exception) {
                if (isAdded && activity != null) {
                    Toast.makeText(requireContext(), "Error sharing to story: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}