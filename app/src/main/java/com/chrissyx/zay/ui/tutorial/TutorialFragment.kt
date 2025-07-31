package com.chrissyx.zay.ui.tutorial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.databinding.FragmentTutorialBinding
import com.chrissyx.zay.utils.UserPreferences
import com.chrissyx.zay.utils.InstagramStoryHelper
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.launch

class TutorialFragment : Fragment() {
    
    private var _binding: FragmentTutorialBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseRepository: FirebaseRepository
    
    private var currentStep = 0
    private val totalSteps = 3
    
    companion object {
        fun newInstance(profileLink: String): TutorialFragment {
            val fragment = TutorialFragment()
            val args = Bundle()
            args.putString("profile_link", profileLink)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userPreferences = UserPreferences(requireContext())
        firebaseRepository = FirebaseRepository()
        
        loadUserProfile()
        setupTutorialSteps()
        setupCancelButton()
    }
    
    private fun setupTutorialSteps() {
        // Step 1: Introduction
        binding.step1Layout.visibility = View.VISIBLE
        binding.step2Layout.visibility = View.GONE
        binding.step3Layout.visibility = View.GONE
        
        // Next button for step 1
        binding.nextButton.setOnClickListener {
            when (currentStep) {
                0 -> showStep2()
                1 -> showStep3()
                2 -> generateAndShareStory()
            }
        }
        
        // Back button
        binding.backButton.setOnClickListener {
            when (currentStep) {
                1 -> showStep1()
                2 -> showStep2()
                else -> {
                    // Close tutorial
                    try {
                        if (isAdded && activity != null && !requireActivity().isFinishing) {
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    } catch (e: Exception) {
                        activity?.onBackPressed()
                    }
                }
            }
        }
    }
    
    private fun showStep1() {
        currentStep = 0
        binding.step1Layout.visibility = View.VISIBLE
        binding.step2Layout.visibility = View.GONE
        binding.step3Layout.visibility = View.GONE
        
        binding.nextButton.text = "Next"
        binding.backButton.visibility = View.GONE
    }
    
    private fun showStep2() {
        currentStep = 1
        binding.step1Layout.visibility = View.GONE
        binding.step2Layout.visibility = View.VISIBLE
        binding.step3Layout.visibility = View.GONE
        
        binding.nextButton.text = "Next"
        binding.backButton.visibility = View.VISIBLE
        
        // Generate and show story preview
        generateStoryPreview()
    }
    
    private fun generateStoryPreview() {
        lifecycleScope.launch {
            try {
                val username = userPreferences.username ?: "user"
                val user = firebaseRepository.getUserByUsername(username)
                val prompt = user?.prompt ?: "send me anonymous messages!"
                val isVerified = user?.isVerified ?: false
                val profileImageUrl = user?.profilePictureURL
                
                if (!profileImageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .asBitmap()
                        .load(profileImageUrl)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                showPreviewWithProfileImage(username, prompt, resource, isVerified)
                            }
                            
                            override fun onLoadCleared(placeholder: Drawable?) {
                                showPreviewWithoutProfileImage(username, prompt, isVerified)
                            }
                        })
                } else {
                    showPreviewWithoutProfileImage(username, prompt, isVerified)
                }
                
            } catch (e: Exception) {
            }
        }
    }
    
    private fun showPreviewWithProfileImage(username: String, prompt: String, profileBitmap: Bitmap, isVerified: Boolean) {
        try {
            val previewBitmap = InstagramStoryHelper.generateProfileStoryImage(this, username, prompt, profileBitmap, isVerified)
            binding.storyPreviewImage.setImageBitmap(previewBitmap)
        } catch (e: Exception) {
            showPreviewWithoutProfileImage(username, prompt, isVerified)
        }
    }
    
    private fun showPreviewWithoutProfileImage(username: String, prompt: String, isVerified: Boolean) {
        try {
            val previewBitmap = InstagramStoryHelper.generateProfileStoryImage(this, username, prompt, null, isVerified)
            binding.storyPreviewImage.setImageBitmap(previewBitmap)
        } catch (e: Exception) {
        }
    }
    
    private fun showStep3() {
        currentStep = 2
        binding.step1Layout.visibility = View.GONE
        binding.step2Layout.visibility = View.GONE
        binding.step3Layout.visibility = View.VISIBLE
        
        binding.nextButton.text = "Share to Instagram Story"
        binding.backButton.visibility = View.VISIBLE
    }
    
    private fun generateAndShareStory() {
        val profileLink = arguments?.getString("profile_link") ?: ""
        
        lifecycleScope.launch {
            try {
                val username = userPreferences.username ?: "user"
                val user = firebaseRepository.getUserByUsername(username)
                val prompt = user?.prompt ?: "send me anonymous messages!"
                val isVerified = user?.isVerified ?: false
                val profileImageUrl = user?.profilePictureURL
                
                if (!profileImageUrl.isNullOrEmpty()) {
                    // Load profile picture and generate story
                    Glide.with(requireContext())
                        .asBitmap()
                        .load(profileImageUrl)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                InstagramStoryHelper.shareProfileStory(this@TutorialFragment, username, prompt, resource, isVerified, profileLink)
                            }
                            
                            override fun onLoadCleared(placeholder: Drawable?) {
                                InstagramStoryHelper.shareProfileStory(this@TutorialFragment, username, prompt, null, isVerified, profileLink)
                            }
                        })
                } else {
                    // Generate and share story without profile picture
                    InstagramStoryHelper.shareProfileStory(this@TutorialFragment, username, prompt, null, isVerified, profileLink)
                }
                
                Toast.makeText(requireContext(), "📋 Link copied to clipboard! Share the story and paste your link in Instagram.", Toast.LENGTH_LONG).show()
                
                // Auto-close tutorial after successful story sharing (delay to let user see the toast)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        if (isAdded && activity != null) {
                            closeTutorial()
                        }
                    } catch (e: Exception) {
                    }
                }, 2000) // 2 second delay
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error generating story: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupCancelButton() {
        binding.cancelButton.setOnClickListener {
            // Close tutorial completely - use the simplest and most reliable method
            closeTutorial()
        }
    }
    
    private fun closeTutorial() {
        try {
            // Navigate back to main app UI instead of closing the whole app
            val mainActivity = activity as? com.chrissyx.zay.MainActivity
            if (mainActivity != null && isAdded && !requireActivity().isFinishing) {
                mainActivity.showMainApp()
            } else {
                // Fallback: try to remove this fragment from the stack
                if (isAdded) {
                    parentFragmentManager.beginTransaction()
                        .remove(this)
                        .commitAllowingStateLoss()
                }
            }
        } catch (e: Exception) {
            // Last resort: try the old method but safer
            try {
                if (isAdded && activity != null) {
                    requireActivity().onBackPressed()
                }
            } catch (e2: Exception) {
            }
        }
    }
    
    private fun loadUserProfile() {
        val username = userPreferences.username ?: "user"
        
        // Set user initials as fallback
        val initials = username.take(2).uppercase()
        binding.tutorialProfileInitials.text = initials
        
        // Try to load profile picture
        lifecycleScope.launch {
            try {
                val user = firebaseRepository.getUserByUsername(username)
                val profilePictureURL = user?.profilePictureURL
                
                if (!profilePictureURL.isNullOrEmpty()) {
                    // Load profile picture using Glide
                    Glide.with(requireContext())
                        .load(profilePictureURL)
                        .circleCrop()
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                if (isAdded) {
                                    binding.tutorialProfileImage.setImageDrawable(resource)
                                    binding.tutorialProfileImage.visibility = View.VISIBLE
                                    binding.tutorialProfileInitials.visibility = View.GONE
                                }
                            }
                            
                            override fun onLoadCleared(placeholder: Drawable?) {
                                // Keep initials showing if image load fails
                            }
                        })
                }
            } catch (e: Exception) {
                // Keep showing initials if profile loading fails
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}





