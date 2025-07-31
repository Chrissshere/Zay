package com.chrissyx.zay.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chrissyx.zay.MainActivity
import com.chrissyx.zay.R
import com.chrissyx.zay.data.models.Platform
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.databinding.FragmentLoginBinding
import com.chrissyx.zay.network.InstagramService
import com.chrissyx.zay.network.NetworkModule
import com.chrissyx.zay.utils.DeviceHelper
import com.chrissyx.zay.utils.DeviceAuthManager
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: AuthViewModel
    
    // Instagram login launcher
    private val instagramLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleInstagramLoginResult(result.data)
    }
    
    // Snapchat login launcher
    private val snapchatLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSnapchatLoginResult(result.data)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel with dependencies
        val firebaseRepository = FirebaseRepository()
        val instagramService = InstagramService()
        val deviceHelper = DeviceHelper(requireContext())
        val userPreferences = UserPreferences(requireContext())
        val deviceAuthManager = DeviceAuthManager(requireContext(), firebaseRepository)
        
        viewModel = AuthViewModel(firebaseRepository, instagramService, deviceHelper, userPreferences, deviceAuthManager)
        
        // Set the Instagram login launcher in the ViewModel
        viewModel.instagramLoginLauncher = instagramLoginLauncher
        
        // Set the Snapchat login launcher in the ViewModel
        viewModel.snapchatLoginLauncher = snapchatLoginLauncher

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Fix cursor jumping issue
        var isUpdatingText = false
        
        binding.usernameEditText.addTextChangedListener { text ->
            if (!isUpdatingText) {
                isUpdatingText = true
                val currentText = text.toString()
                viewModel.updateUsername(currentText)
                isUpdatingText = false
            }
        }

        // Platform spinner
        val platforms = Platform.values()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            platforms.map { it.displayName }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.platformSpinner.adapter = adapter

        binding.platformSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedPlatform = platforms[position]
                if (selectedPlatform.isEnabled) {
                    viewModel.updatePlatform(selectedPlatform)
                } else {
                    // Revert to Instagram if user selects disabled platform
                    binding.platformSpinner.setSelection(0) // Instagram is at index 0
                    Toast.makeText(requireContext(), "${selectedPlatform.displayName.split(" - ")[0]} login coming soon!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.continueButton.setOnClickListener {
            viewModel.handleContinue()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update loading state
                binding.continueButton.isEnabled = !state.isLoading
                binding.continueButton.text = if (state.isLoading) "Loading..." else "Continue"

                // Show error message
                state.errorMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }

                // Handle login success
                if (state.loginSuccess) {
                    (activity as? MainActivity)?.showMainApp()
                }
                
                // Show account verification dialog
                if (state.showAccountVerification && state.existingAccountUsername != null) {
                    showAccountVerificationDialog(state.existingAccountUsername)
                }
                
                if (state.showManualVerification) {
                    showManualVerificationDialog(state.existingAccountUsername)
                }
                
                if (state.verificationRequestSent) {
                    showVerificationRequestSentDialog()
                }
                
                // Show trust device dialog
                if (state.showTrustDeviceDialog) {
                    showTrustDeviceDialog()
                }
            }
        }
    }

    private fun showAccountVerificationDialog(username: String) {
        val selectedPlatform = viewModel.uiState.value.selectedPlatform
        
        val platformName = selectedPlatform.displayName
        val verificationMessage = "We found an existing account for @$username.\n\nTo ensure account security, please verify your ownership:"
        
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setTitle("Account Verification")
            .setMessage(verificationMessage)
            .setNeutralButton("Contact Support") { _, _ ->
                openContactSupport()
            }
            .setNegativeButton("Create New Account") { _, _ ->
                viewModel.handleCreateNewAccount()
            }
            .setCancelable(false)
        
        // Add platform-specific verification button
        when (selectedPlatform) {
            Platform.INSTAGRAM -> {
                dialogBuilder.setPositiveButton("Verify with Instagram") { _, _ ->
                    viewModel.startInstagramVerification()
                }
            }
            Platform.SNAPCHAT -> {
                dialogBuilder.setPositiveButton("Verify with Snapchat") { _, _ ->
                    viewModel.startSnapchatVerification()
                }
            }
            Platform.TIKTOK -> {
                dialogBuilder.setPositiveButton("Verify with TikTok") { _, _ ->
                    // TikTok verification not implemented yet
                    Toast.makeText(requireContext(), "TikTok verification coming soon!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        dialogBuilder.show()
    }

    private fun showManualVerificationDialog(username: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Manual Verification")
            .setMessage("We found an existing account for @$username.\n\nSince Instagram Business verification requires a business account, you can request manual verification instead.\n\nHow would you like us to verify your Instagram account?")
            .setPositiveButton("Instagram DM") { _, _ ->
                viewModel.handleManualVerification("instagram_dm")
            }
            .setNeutralButton("Email Support") { _, _ ->
                viewModel.handleManualVerification("email_support")
            }
            .setNegativeButton("Create New Account") { _, _ ->
                viewModel.handleCreateNewAccount()
            }
            .setCancelable(false)
            .show()
    }

    private fun showVerificationRequestSentDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Verification Request Sent")
            .setMessage("We've received your verification request!\n\nOur team will manually verify your Instagram account within 24 hours. You'll receive a notification once verification is complete.\n\nFor now, you can create a new account or try again later.")
            .setPositiveButton("Create New Account") { _, _ ->
                viewModel.handleCreateNewAccount()
            }
            .setNegativeButton("Try Again Later") { _, _ ->
                viewModel.dismissManualVerification()
            }
            .setCancelable(false)
            .show()
    }

    private fun showMainApp() {
        (activity as? MainActivity)?.showMainApp()
    }
    
    private fun openContactSupport() {
        try {
            val contactSupportFragment = com.chrissyx.zay.ui.support.ContactSupportFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.container, contactSupportFragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening contact support", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showTrustDeviceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Trust Device?")
            .setMessage("By trusting this device you wouldn't have to verify your account ownership with support the next time you log in.")
            .setPositiveButton("Trust") { _, _ ->
                viewModel.handleTrustDeviceDecision(true)
            }
            .setNegativeButton("Don't Trust") { _, _ ->
                viewModel.handleTrustDeviceDecision(false)
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 