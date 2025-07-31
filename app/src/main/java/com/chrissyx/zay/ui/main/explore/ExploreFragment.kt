package com.chrissyx.zay.ui.main.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chrissyx.zay.R
import com.chrissyx.zay.data.models.ExploreProfile
import com.chrissyx.zay.databinding.FragmentExploreBinding
import com.chrissyx.zay.ui.messaging.MessageSheetFragment
import com.chrissyx.zay.utils.SmartCache
import kotlinx.coroutines.launch

class ExploreFragment : Fragment() {
    
    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ExploreViewModel by viewModels()
    private lateinit var exploreAdapter: ExploreProfileAdapter
    private lateinit var smartCache: SmartCache
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        smartCache = SmartCache(requireContext())
        setupUI()
        
        // Smart loading - only refresh if needed
        if (smartCache.shouldRefresh(SmartCache.Keys.EXPLORE_PROFILES)) {
            viewModel.refreshProfiles()
            smartCache.markAsLoaded(SmartCache.Keys.EXPLORE_PROFILES)
        } else {
            // Still observe to get latest cached data
            viewModel.refreshProfiles() // This should load from cache in ViewModel
        }
    }
    
    private fun setupUI() {
        setupRecyclerView()
        setupSearchView()
        setupFab()
        setupSwipeRefresh()
        
        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }
    
    private fun setupRecyclerView() {
        exploreAdapter = ExploreProfileAdapter(
            onChatClick = { profile ->
                handleChatClick(profile)
            },
            onSendMessageClick = { profile ->
                handleSendMessageClick(profile)
            }
        )
        
        binding.exploreProfilesRecyclerView.apply {
            adapter = exploreAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupSearchView() {
        binding.searchEditText.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            viewModel.searchProfiles(query)
        }
    }
    
    private fun updateUI(state: ExploreUiState) {
        
        // Update loading state
        binding.loadingProgressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        // Stop swipe refresh animation when loading is complete
        if (!state.isLoading) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
        
        // Update profiles list
        exploreAdapter.submitList(state.profiles)
        
        // Update empty state
        if (state.profiles.isEmpty() && !state.isLoading) {
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
        }
        
        // Update FAB based on existing profile status
        updateFab(state.hasExistingProfile)
        
        // Show error if any
        state.errorMessage?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    private fun updateFab(hasExistingProfile: Boolean) {
        if (hasExistingProfile) {
            // User has existing profile - show edit icon and text
            binding.addProfileFab.setImageResource(R.drawable.ic_edit_24)
            binding.addProfileFab.contentDescription = "Edit your explore profile"
        } else {
            // User doesn't have profile - show add icon
            binding.addProfileFab.setImageResource(R.drawable.ic_add_24)
            binding.addProfileFab.contentDescription = "Create explore profile"
        }
    }
    
    private fun setupFab() {
        binding.addProfileFab.setOnClickListener {
            val currentState = viewModel.uiState.value
            
            if (currentState.hasExistingProfile) {
                // Navigate to edit existing profile
                val existingProfile = currentState.existingProfile
                if (existingProfile != null) {
                    navigateToEditProfile(existingProfile)
                } else {
                    Toast.makeText(requireContext(), "Error loading your profile", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Navigate to create new profile
                navigateToCreateProfile()
            }
        }
    }
    
    private fun navigateToCreateProfile() {
        try {
            if (!isAdded || activity == null || requireActivity().isFinishing) {
                return
            }
            
            val fragment = CreateExploreProfileFragment.newInstance()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss() // Safer commit
        } catch (e: Exception) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Error opening profile creator", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun navigateToEditProfile(profile: ExploreProfile) {
        try {
            if (!isAdded || activity == null || requireActivity().isFinishing) {
                return
            }
            
            val fragment = CreateExploreProfileFragment.newInstance(profile)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss() // Safer commit
        } catch (e: Exception) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Error opening profile editor", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleChatClick(profile: ExploreProfile) {
        // Placeholder for future chat functionality
        Toast.makeText(
            requireContext(),
            "Chat feature coming soon!",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun handleSendMessageClick(profile: ExploreProfile) {
        // Open message UI directly in the app
        try {
            val messageSheet = com.chrissyx.zay.ui.messaging.MessageSheetFragment.newInstance(profile.username)
            messageSheet.show(parentFragmentManager, "MessageSheet")
        } catch (e: Exception) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Error opening message", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh profiles when fragment becomes visible
        viewModel.refreshProfiles()
    }
    
    // Method to force debug load all profiles (can be called for testing)
    private fun debugLoadAllProfiles() {
        viewModel.loadAllProfilesDebug()
    }
    
    fun refreshProfiles() {
        smartCache.forceRefresh(SmartCache.Keys.EXPLORE_PROFILES)
        viewModel.refreshProfiles()
        smartCache.markAsLoaded(SmartCache.Keys.EXPLORE_PROFILES)
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshProfiles()
        }
        
        // Customize refresh indicator colors
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.white,
            R.color.link_text_color
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 