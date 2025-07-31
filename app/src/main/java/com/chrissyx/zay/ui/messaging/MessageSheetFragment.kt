package com.chrissyx.zay.ui.messaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.chrissyx.zay.R
import com.chrissyx.zay.databinding.FragmentMessageSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import android.content.Context
import android.view.inputmethod.InputMethodManager

class MessageSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentMessageSheetBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MessageViewModel
    private var targetUsername: String = ""

    companion object {
        private const val ARG_USERNAME = "username"
        
        fun newInstance(username: String): MessageSheetFragment {
            val fragment = MessageSheetFragment()
            val args = Bundle()
            args.putString(ARG_USERNAME, username)
            fragment.arguments = args
            return fragment
        }
    }

    override fun getTheme(): Int {
        return R.style.TransparentBottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onStart() {
        super.onStart()
        
        // Get the dialog and make it fully transparent
        dialog?.let { dialog ->
            // Make the dialog window background transparent
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            // Get the bottom sheet view and make it transparent
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
            
            // Set the dialog to be full width and remove any default styling
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            
            // Remove any default dialog styling
            dialog.window?.statusBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel with UserPreferences and Context
        val userPreferences = com.chrissyx.zay.utils.UserPreferences(requireContext())
        viewModel = MessageViewModel(userPreferences, requireContext())
        
        targetUsername = arguments?.getString(ARG_USERNAME) ?: ""
        
        if (targetUsername.isEmpty()) {
            Toast.makeText(requireContext(), "Error: No username provided", Toast.LENGTH_LONG).show()
            dismiss()
            return
        }
        
        setupUI()
        observeViewModel()
        
        // Load target user info
        viewModel.loadUserInfo(targetUsername)
    }

    private fun setupUI() {
        binding.usernameText.text = "@$targetUsername"
        
        // Set up keyboard dismiss when clicking outside EditText
        setupKeyboardDismiss()
        
        // Message input
        binding.messageEditText.addTextChangedListener { text ->
            val messageText = text.toString().trim()
            binding.sendButton.isEnabled = messageText.isNotEmpty()
            // Remove alpha changes - let the drawable handle the visual state
        }
        
        // Close button
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun setupKeyboardDismiss() {
        // Set up click listeners on views that should dismiss keyboard
        val viewsToDismissKeyboard = listOf(
            binding.root,
            binding.usernameText,
            binding.promptText
        )
        
        viewsToDismissKeyboard.forEach { view ->
            view.setOnClickListener {
                hideKeyboard()
            }
        }
        
        // Prevent EditText clicks from dismissing keyboard
        binding.messageEditText.setOnClickListener { 
            // Do nothing - let EditText handle its own clicks
        }
        
        // Set up send button with keyboard dismiss
        binding.sendButton.setOnClickListener { view ->
            val message = binding.messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                hideKeyboard() // Hide keyboard when sending
                viewModel.sendMessage(targetUsername, message)
            } else {
            }
        }
    }
    
    private fun hideKeyboard() {
        try {
            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val currentFocus = dialog?.currentFocus ?: binding.messageEditText
            inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            binding.messageEditText.clearFocus()
        } catch (e: Exception) {
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                
                // Update UI based on state
                state.userInfo?.let { user ->
                    binding.promptText.text = user.prompt
                }
                
                // Update send button state
                binding.sendButton.text = if (state.isSending) "Sending..." else "SEND MESSAGE"
                binding.sendButton.isEnabled = !state.isSending && binding.messageEditText.text.toString().trim().isNotEmpty()
                binding.progressBar.visibility = if (state.isSending) View.VISIBLE else View.GONE
                
                // Show success state
                if (state.messageSent) {
                    binding.sendButton.text = "Sent! ✓"
                    // Use a green drawable instead of setBackgroundColor to preserve rounded corners
                    binding.sendButton.setBackgroundResource(R.drawable.success_button_background)
                    binding.messageEditText.setText("")
                    
                    Toast.makeText(requireContext(), "Message sent successfully! 🎉", Toast.LENGTH_SHORT).show()
                    
                    // Auto-dismiss after success
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        dismiss()
                    }, 1500)
                } else {
                    // Reset to normal button background when not in success state
                    binding.sendButton.setBackgroundResource(R.drawable.send_button_background)
                }
                
                // Show error message
                state.errorMessage?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 