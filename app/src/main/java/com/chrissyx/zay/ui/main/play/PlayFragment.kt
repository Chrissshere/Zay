package com.chrissyx.zay.ui.main.play

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.chrissyx.zay.R
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.databinding.FragmentPlayBinding
import com.chrissyx.zay.utils.LinkSecurityManager
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class PlayFragment : Fragment() {

    private var _binding: FragmentPlayBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PlayViewModel
    private lateinit var userPreferences: UserPreferences
    private lateinit var linkSecurityManager: LinkSecurityManager
    private lateinit var firebaseRepository: FirebaseRepository

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleImageSelected(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            // Safety check
            if (!isAdded || activity == null) {
                return
            }
            
            
            userPreferences = UserPreferences(requireContext())
            firebaseRepository = FirebaseRepository()
            
            viewModel = PlayViewModel(firebaseRepository, userPreferences)
            linkSecurityManager = LinkSecurityManager(requireContext())
            
            setupUI()
            observeViewModel()
            
            binding.root.post {
                if (isAdded && context != null && !requireActivity().isFinishing) {
                    try {
                        viewModel.loadUserData()
                        
                        // Generate TinyURL for the current user
                        generateInitialTinyUrl()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            if (isAdded) {
                Toast.makeText(requireContext(), "Error loading Play screen", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            if (::viewModel.isInitialized && isAdded && context != null && !requireActivity().isFinishing) {
                // Use post to avoid any timing issues
                binding.root.post {
                    if (isAdded && context != null && !requireActivity().isFinishing) {
                        try {
                            viewModel.forceReloadUserData()
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun debugProfilePicture() {
        val username = userPreferences.username ?: "unknown"
        
        // Check current state
        val currentState = viewModel.uiState.value
        
        // Test with the actual URL
        currentState.profilePictureURL?.let { url ->
            if (url.isNotEmpty()) {
                loadProfilePictureRobust(url)
                
                Toast.makeText(requireContext(), "Testing robust image loading - watch console", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Profile picture URL is empty", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "Profile picture URL is null - reloading data", Toast.LENGTH_LONG).show()
            viewModel.loadUserData()
        }
        
    }

    private fun generateInitialTinyUrl() {
        val username = userPreferences.username ?: return
        
        // Generate TinyURL in background without showing loading state
        viewModel.generateTinyUrl { tinyUrl ->
            // TinyURL is now stored in the ViewModel state and will be shown in the UI
            if (tinyUrl != null) {
                println("TinyURL generated successfully: $tinyUrl")
            } else {
                println("Failed to generate TinyURL, will use fallback")
            }
        }
    }

    private fun setupUI() {
        val username = userPreferences.username ?: "Unknown"
        
        // Set initial profile picture (first 2 letters of username)
        updateProfilePicture(null, username)
        
        // Upload photo button
        binding.uploadButton.setOnClickListener {
            openImagePicker()
        }
        
        // Edit prompt button
        binding.editPromptButton.setOnClickListener {
            viewModel.startEditingPrompt()
        }
        
        // Save prompt button
        binding.savePromptButton.setOnClickListener {
            val newPrompt = binding.promptEditText.text.toString()
            viewModel.savePrompt(newPrompt)
        }
        
        // Cancel edit button
        binding.cancelPromptButton.setOnClickListener {
            viewModel.cancelEditingPrompt()
        }
        
        // Copy link button
        binding.copyLinkButton.setOnClickListener {
            copyProfileLink()
        }
        
        // Share button
        binding.shareButton.setOnClickListener {
            openTutorial()
        }
        
        // Tutorial button
        binding.tutorialButton.setOnClickListener {
            openTutorial()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update username and custom link
                val username = state.username
                binding.usernameText.text = "@$username"
                
                // Show verification checkmark for verified users
                lifecycleScope.launch {
                    try {
                        val user = firebaseRepository.getUserByUsername(username)
                        val isVerified = user?.isVerified == true
                        val platform = user?.platform
                        val isAdmin = userPreferences.role == "admin"
                        
                        // Show checkmark for verified users or admins
                        binding.verificationCheckmark.visibility = if (isVerified || isAdmin) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    } catch (e: Exception) {
                        binding.verificationCheckmark.visibility = View.GONE
                    }
                }
                
                // Always show zay.me/username format, but handle errors
                when {
                    state.isGeneratingLink -> {
                        binding.customLinkText.text = "Generating TinyURL..."
                        binding.customLinkText.setTextColor(resources.getColor(android.R.color.black, null))
                    }
                    state.tinyUrlError -> {
                        binding.customLinkText.text = "Unable to generate link, please restart the app."
                        binding.customLinkText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    }
                    else -> {
                        binding.customLinkText.text = "zay.me/$username"
                        binding.customLinkText.setTextColor(resources.getColor(R.color.link_text_color, null))
                    }
                }
                
                // Update profile initials
                val initials = username.take(2).uppercase()
                binding.profileInitials.text = initials
                
                // Update prompt
                if (state.isEditingPrompt) {
                    binding.promptText.visibility = View.GONE
                    binding.promptEditLayout.visibility = View.VISIBLE
                    binding.editPromptButton.visibility = View.GONE
                    binding.promptEditText.setText(state.prompt)
                    binding.promptEditText.requestFocus()
                } else {
                    binding.promptText.visibility = View.VISIBLE
                    binding.promptEditLayout.visibility = View.GONE
                    binding.editPromptButton.visibility = View.VISIBLE
                    binding.promptText.text = state.prompt
                }
                
                // Update profile picture
                state.profilePictureURL?.let { url ->
                    if (url.isNotEmpty()) {
                        
                        // Make sure ImageView is visible and hide initials
                        binding.profileImage.visibility = View.VISIBLE
                        binding.profileInitials.visibility = View.GONE
                        
                        loadProfilePictureRobust(url)
                    } else {
                        updateProfilePicture(null, state.username)
                    }
                } ?: run {
                    updateProfilePicture(null, state.username)
                }
                
                // Show loading states
                binding.uploadProgressBar.visibility = if (state.isUploadingImage) View.VISIBLE else View.GONE
                binding.promptProgressBar.visibility = if (state.isSavingPrompt) View.VISIBLE else View.GONE
                
                // Handle link generation loading state for buttons only
                if (state.isGeneratingLink) {
                    binding.copyLinkButton.text = "🔗 Generating..."
                    binding.shareButton.text = "📤 Generating..."
                    binding.copyLinkButton.isEnabled = false
                    binding.shareButton.isEnabled = false
                } else {
                    binding.copyLinkButton.text = "🔗 Copy Link"
                    binding.shareButton.text = "📤 Share to Story!"
                    binding.copyLinkButton.isEnabled = true
                    binding.shareButton.isEnabled = true
                }
                
                // Show error messages
                state.errorMessage?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
                
                // Show success messages
                state.successMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                }
            }
        }
    }

    private fun updateProfilePicture(bitmap: Bitmap?, username: String) {
        if (bitmap != null) {
            binding.profileImage.setImageBitmap(bitmap)
            binding.profileInitials.visibility = View.GONE
        } else {
            // Show initials
            binding.profileImage.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.large_circle_gradient)
            )
            // Set initials text
            val initials = username.take(2).uppercase()
            binding.profileInitials.text = initials
            binding.profileInitials.visibility = View.VISIBLE
        }
    }

    private fun loadProfilePicture(url: String) {
        
        Glide.with(this)
            .asBitmap()
            .load(url)
            .skipMemoryCache(true) // Skip memory cache to always load fresh image
            .diskCacheStrategy(DiskCacheStrategy.NONE) // Skip disk cache for immediate updates
            .placeholder(R.drawable.large_circle_gradient) // Show placeholder while loading
            .error(R.drawable.large_circle_gradient) // Show placeholder on error
            .circleCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    
                    requireActivity().runOnUiThread {
                        binding.profileImage.setImageBitmap(resource)
                        binding.profileInitials.visibility = View.GONE
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
                
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    
                    requireActivity().runOnUiThread {
                        // Show initials if image fails to load
                        val username = userPreferences.username ?: ""
                        val initials = username.take(2).uppercase()
                        binding.profileImage.setImageDrawable(
                            ContextCompat.getDrawable(requireContext(), R.drawable.large_circle_gradient)
                        )
                        binding.profileInitials.text = initials
                        binding.profileInitials.visibility = View.VISIBLE
                    }
                }
            })
    }

    private fun loadProfilePictureSimple(url: String) {
        
        // Hide initials first
        binding.profileInitials.visibility = View.GONE
        
        Glide.with(this)
            .load(url)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .placeholder(R.drawable.large_circle_gradient)
            .error(R.drawable.large_circle_gradient)
            .circleCrop()
            .into(binding.profileImage)
            
    }

    private fun loadProfilePictureRobust(url: String) {
        
        if (!isAdded || context == null) {
            return
        }
        
        try {
            // First, hide initials
            binding.profileInitials.visibility = View.GONE
            
            Glide.with(this)
                .asBitmap()
                .load(url)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.large_circle_gradient)
                .error(R.drawable.large_circle_gradient)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        
                        if (isAdded && context != null) {
                            try {
                                binding.profileImage.setImageBitmap(resource)
                                binding.profileImage.visibility = View.VISIBLE // Make sure ImageView is visible!
                                binding.profileInitials.visibility = View.GONE
                                
                                // Success - image loaded and displayed
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                        }
                    }
                    
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                    
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        
                        if (isAdded && context != null) {
                            try {
                                val username = userPreferences.username ?: ""
                                val initials = username.take(2).uppercase()
                                
                                binding.profileImage.setImageDrawable(
                                    ContextCompat.getDrawable(requireContext(), R.drawable.large_circle_gradient)
                                )
                                binding.profileInitials.text = initials
                                binding.profileInitials.visibility = View.VISIBLE
                                
                                Toast.makeText(requireContext(), "Failed to load profile picture", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                            }
                        }
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun handleImageSelected(uri: Uri) {
        try {
            
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            
            // Resize and compress the image
            val resizedBitmap = resizeBitmap(bitmap, 512)
            
            // Update UI immediately with the processed image
            updateProfilePicture(resizedBitmap, userPreferences.username ?: "")
            
            // Convert to byte array with compression
            val byteArray = bitmapToByteArray(resizedBitmap)
            
            // Upload to Firebase
            viewModel.uploadProfilePicture(byteArray)
            
            // Clean up bitmaps
            if (bitmap != resizedBitmap) {
                bitmap.recycle()
            }
            resizedBitmap.recycle()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap // No need to resize
        }
        
        val scaleFactor = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }
        
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        // Use JPEG with 75% quality for good balance between size and quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        return stream.toByteArray()
    }

    private fun copyProfileLink() {
        val username = userPreferences.username ?: return
        val currentState = viewModel.uiState.value
        
        // If TinyURL is already generated, copy it directly
        if (currentState.generatedTinyUrl != null) {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Zay Profile Link", currentState.generatedTinyUrl)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "TinyURL copied to clipboard! 📋✨", Toast.LENGTH_SHORT).show()
            return
        }
        
        // If there's an error, copy zay:// as fallback
        if (currentState.tinyUrlError) {
            val fallbackLink = "zay://profile/$username"
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Zay Profile Link", fallbackLink)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Deep link copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Otherwise, generate TinyURL
        viewModel.generateTinyUrl { tinyUrl ->
            val linkToCopy = tinyUrl ?: "zay://profile/$username"
            
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Zay Profile Link", linkToCopy)
            clipboard.setPrimaryClip(clip)
            
            val message = if (tinyUrl != null) {
                "TinyURL copied to clipboard! 📋✨"
            } else {
                "Deep link copied to clipboard! 📋"
            }
            
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareProfileLink() {
        val username = userPreferences.username ?: return
        val currentState = viewModel.uiState.value
        
        // If TinyURL is already generated, share it directly
        if (currentState.generatedTinyUrl != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, currentState.generatedTinyUrl)
                putExtra(Intent.EXTRA_SUBJECT, "Check out my Zay profile!")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Zay Profile"))
            return
        }
        
        // If there's an error, share zay:// as fallback
        if (currentState.tinyUrlError) {
            val fallbackLink = "zay://profile/$username"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, fallbackLink)
                putExtra(Intent.EXTRA_SUBJECT, "Check out my Zay profile!")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Zay Profile"))
            return
        }
        
        // Otherwise, generate TinyURL first
        viewModel.generateTinyUrl { tinyUrl ->
            val linkToShare = tinyUrl ?: "zay://profile/$username"
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, linkToShare)
                putExtra(Intent.EXTRA_SUBJECT, "Check out my Zay profile!")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Zay Profile"))
        }
    }

    private fun openTutorial() {
        try {
            if (!isAdded || activity == null || requireActivity().isFinishing) {
                return
            }
            
            val currentState = viewModel.uiState.value
            val username = userPreferences.username ?: return
            
            // Use TinyURL if available, otherwise fallback to deep link
            val profileLink = currentState.generatedTinyUrl ?: "zay://profile/$username"
            
            val tutorialFragment = com.chrissyx.zay.ui.tutorial.TutorialFragment.newInstance(profileLink)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.container, tutorialFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss() // Safer commit
        } catch (e: Exception) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Error opening tutorial: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun shareProfile() {
        val username = userPreferences.username ?: return
        val prompt = userPreferences.prompt ?: "send me anonymous messages!"
        val currentState = viewModel.uiState.value
        
        // Use TinyURL if available, otherwise fallback to zay:// scheme
        val linkToShare = currentState.generatedTinyUrl ?: "zay://profile/$username"
        
        val shareText = "Send me anonymous messages on Zay! 💬\n\n\"$prompt\"\n\n$linkToShare"
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Send me anonymous messages on Zay!")
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share your Zay profile"))
    }

    private fun testImageLoading() {
        // Test with a known working image URL
        val testUrl = "https://firebasestorage.googleapis.com/v0/b/zayngl.appspot.com/o/profile_pictures%2Ftest.jpg?alt=media"
        
        Glide.with(this)
            .asBitmap()
            .load(testUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    binding.profileImage.setImageBitmap(resource)
                    binding.profileInitials.visibility = View.GONE
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
                
                override fun onLoadFailed(errorDrawable: Drawable?) {
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 