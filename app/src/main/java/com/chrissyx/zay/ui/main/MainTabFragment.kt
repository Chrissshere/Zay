package com.chrissyx.zay.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.chrissyx.zay.databinding.FragmentMainTabBinding
import com.chrissyx.zay.ui.main.explore.ExploreFragment
import com.chrissyx.zay.ui.main.inbox.InboxFragment
import com.chrissyx.zay.ui.main.play.PlayFragment
import com.chrissyx.zay.ui.main.settings.SettingsFragment

class MainTabFragment : Fragment() {

    private var _binding: FragmentMainTabBinding? = null
    private val binding get() = _binding!!
    
    private var currentTab = Tab.PLAY

    enum class Tab {
        PLAY, INBOX, EXPLORE, SETTINGS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupBottomNavigation()
        
        // Show Play tab after UI is ready
        binding.root.post {
            if (isAdded && !childFragmentManager.isDestroyed) {
                showTab(Tab.PLAY)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.playTab.setOnClickListener { showTab(Tab.PLAY) }
        binding.inboxTab.setOnClickListener { showTab(Tab.INBOX) }
        binding.exploreTab.setOnClickListener { showTab(Tab.EXPLORE) }
        binding.settingsTab.setOnClickListener { showTab(Tab.SETTINGS) }
    }

    private fun showTab(tab: Tab) {
        try {
            // Prevent navigation if fragment is not attached or activity is finishing
            if (!isAdded || activity == null || requireActivity().isFinishing) {
                return
            }
            
            // Allow initial load even if currentTab == tab
            if (currentTab == tab && childFragmentManager.findFragmentById(binding.contentContainer.id) != null) {
                return
            }
            
            currentTab = tab
            
            // Update tab selection visual state
            resetTabStates()
            when (tab) {
                Tab.PLAY -> {
                    binding.playTab.alpha = 1.0f
                    binding.playIcon.setColorFilter(android.graphics.Color.WHITE)
                    binding.playLabel.setTextColor(android.graphics.Color.WHITE)
                }
                Tab.INBOX -> {
                    binding.inboxTab.alpha = 1.0f
                    binding.inboxIcon.setColorFilter(android.graphics.Color.WHITE)
                    binding.inboxLabel.setTextColor(android.graphics.Color.WHITE)
                }
                Tab.EXPLORE -> {
                    binding.exploreTab.alpha = 1.0f
                    binding.exploreIcon.setColorFilter(android.graphics.Color.WHITE)
                    binding.exploreLabel.setTextColor(android.graphics.Color.WHITE)
                }
                Tab.SETTINGS -> {
                    binding.settingsTab.alpha = 1.0f
                    binding.settingsIcon.setColorFilter(android.graphics.Color.WHITE)
                    binding.settingsLabel.setTextColor(android.graphics.Color.WHITE)
                }
            }
            
            // Show corresponding fragment with error handling
            val fragment = when (tab) {
                Tab.PLAY -> PlayFragment()
                Tab.INBOX -> InboxFragment()
                Tab.EXPLORE -> ExploreFragment()
                Tab.SETTINGS -> SettingsFragment()
            }
            
            // Additional safety check before fragment transaction
            if (!isAdded || childFragmentManager.isDestroyed) {
                return
            }
            
            // Use safer fragment transaction
            val transaction = childFragmentManager.beginTransaction()
            transaction.replace(binding.contentContainer.id, fragment)
            
            // Add a small delay to prevent rapid switching issues
            if (!childFragmentManager.isStateSaved) {
                transaction.commitAllowingStateLoss() // Prevents IllegalStateException
            } else {
                // Try again after a short delay
                binding.root.post {
                    if (isAdded && !childFragmentManager.isDestroyed && !childFragmentManager.isStateSaved) {
                        childFragmentManager.beginTransaction()
                            .replace(binding.contentContainer.id, fragment)
                            .commitAllowingStateLoss()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Don't crash the app, just log the error
        }
    }
    
    private fun resetTabStates() {
        val inactiveColor = android.graphics.Color.parseColor("#80FFFFFF")
        
        binding.playTab.alpha = 0.6f
        binding.playIcon.setColorFilter(inactiveColor)
        binding.playLabel.setTextColor(inactiveColor)
        
        binding.inboxTab.alpha = 0.6f
        binding.inboxIcon.setColorFilter(inactiveColor)
        binding.inboxLabel.setTextColor(inactiveColor)
        
        binding.exploreTab.alpha = 0.6f
        binding.exploreIcon.setColorFilter(inactiveColor)
        binding.exploreLabel.setTextColor(inactiveColor)
        
        binding.settingsTab.alpha = 0.6f
        binding.settingsIcon.setColorFilter(inactiveColor)
        binding.settingsLabel.setTextColor(inactiveColor)
    }

    fun refreshExploreProfiles() {
        // Refresh explore profiles if explore tab is currently active or refresh the fragment
        val currentFragment = childFragmentManager.findFragmentById(binding.contentContainer.id)
        if (currentFragment is com.chrissyx.zay.ui.main.explore.ExploreFragment) {
            currentFragment.refreshProfiles()
        } else if (currentTab == Tab.EXPLORE) {
            // If explore tab is selected but fragment not found, recreate it
            showTab(Tab.EXPLORE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 