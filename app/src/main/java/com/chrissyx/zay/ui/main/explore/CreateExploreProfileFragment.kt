package com.chrissyx.zay.ui.main.explore

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.chrissyx.zay.R
import com.chrissyx.zay.data.models.ExploreProfile
import com.chrissyx.zay.databinding.FragmentCreateExploreProfileBinding
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.launch
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.bumptech.glide.load.engine.DiskCacheStrategy
import androidx.navigation.fragment.findNavController

class CreateExploreProfileFragment : Fragment() {
    
    private var _binding: FragmentCreateExploreProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CreateExploreProfileViewModel by viewModels()
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.uploadBannerImage(uri.toString())
            }
        }
    }
    
    companion object {
        private const val ARG_PROFILE = "profile"
        
        fun newInstance(profile: ExploreProfile? = null): CreateExploreProfileFragment {
            return CreateExploreProfileFragment().apply {
                arguments = Bundle().apply {
                    profile?.let {
                        putSerializable(ARG_PROFILE, it)
                    }
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateExploreProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check if we're editing an existing profile
        arguments?.getSerializable(ARG_PROFILE)?.let { profile ->
            if (profile is ExploreProfile) {
                viewModel.initializeForEdit(profile)
            }
        }
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        setupToolbar()
        setupClickListeners()
        
        // Set the actual username
        val userPreferences = UserPreferences(requireContext())
        val username = userPreferences.username
        if (username.isNotEmpty()) {
            binding.usernameTextView.text = "@$username"
            
            // Show verification checkmark for verified Instagram users
            lifecycleScope.launch {
                try {
                    val firebaseRepository = FirebaseRepository()
                    val userData = firebaseRepository.getUserByUsername(username)
                    val isVerified = userData?.isVerified == true
                    val platform = userPreferences.platform
                    val isInstagramUser = platform == "INSTAGRAM" || userData?.platform?.equals("INSTAGRAM", true) == true
                    
                    if (isAdded) {
                        binding.verificationCheckmark.visibility = if (isVerified && isInstagramUser) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                } catch (e: Exception) {
                    if (isAdded) {
                        binding.verificationCheckmark.visibility = View.GONE
                    }
                }
            }
        }
    }
    
    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        binding.cancelButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
    
    private fun setupClickListeners() {
        binding.bannerImageView.setOnClickListener {
            openImagePicker()
        }
        
        binding.uploadBannerButton.setOnClickListener {
            openImagePicker()
        }
        
        binding.createButton.setOnClickListener {
            val prompt = binding.promptEditText.text.toString()
            viewModel.updatePrompt(prompt)
            viewModel.saveProfile()
        }
        
        // Update prompt in real-time
        binding.promptEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updatePrompt(s.toString())
                
                // Update character count
                val count = s?.length ?: 0
                binding.characterCountTextView.text = "$count/500"
            }
        })
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update banner image
                if (!state.bannerImageUri.isNullOrEmpty()) {
                    
                    Glide.with(this@CreateExploreProfileFragment)
                        .load(state.bannerImageUri)
                        .skipMemoryCache(true) // Skip cache to show updated image
                        .placeholder(R.drawable.placeholder_banner)
                        .error(R.drawable.placeholder_banner)
                        .centerCrop()
                        .into(binding.bannerImageView)
                } else {
                    binding.bannerImageView.setImageResource(R.drawable.placeholder_banner)
                }
                
                // Update prompt
                if (binding.promptEditText.text.toString() != state.prompt) {
                    binding.promptEditText.setText(state.prompt)
                    binding.promptEditText.setSelection(state.prompt.length)
                }
                
                // Update character count
                binding.characterCountTextView.text = "${state.prompt.length}/500"
                
                // Show loading states
                binding.createButton.isEnabled = !state.isLoading && !state.isUploadingBanner
                binding.createButton.text = when {
                    state.isLoading -> "Saving..."
                    state.isUploadingBanner -> "Uploading..."
                    state.isEditMode -> "Update Profile"
                    else -> "Create Profile"
                }
                
                // Handle errors
                state.errorMessage?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
                
                // Handle success
                if (state.isSuccess) {
                    val message = if (state.isEditMode) {
                        "Profile updated successfully!"
                    } else {
                        "Profile created successfully!"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.resetSuccess()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 