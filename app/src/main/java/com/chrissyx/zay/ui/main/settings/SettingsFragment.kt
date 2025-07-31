package com.chrissyx.zay.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chrissyx.zay.MainActivity
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.databinding.FragmentSettingsBinding
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.launch
import com.chrissyx.zay.ui.admin.AdminPanelFragment
import android.content.Intent
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.chrissyx.zay.billing.BillingManager
import androidx.appcompat.app.AlertDialog

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var billingManager: BillingManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userPreferences = UserPreferences(requireContext())
        firebaseRepository = FirebaseRepository()
        
        setupUI()
        loadUserInfo()
    }

    private fun setupUI() {
        // Initialize billing manager
        billingManager = BillingManager(requireContext())
        billingManager.initialize { success ->
            if (success) {
            } else {
            }
        }
        
        // Subscription management moved to Admin Panel
        
        // Logout button
        binding.logoutButton.setOnClickListener {
            logout()
        }
        
        // Request Verification button
        binding.requestVerificationButton.setOnClickListener {
            requestVerification()
        }
        
        // Pro toggle (only for admins)
        binding.proToggle.setOnCheckedChangeListener { _, isChecked ->
            userPreferences.isPro = isChecked
        }
        
        // Verification toggle (only for verified users)
        binding.showVerificationToggle.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                try {
                    val success = firebaseRepository.updateUserVerificationDisplay(
                        userPreferences.username ?: "",
                        userPreferences.platform ?: "UNKNOWN",
                        isChecked
                    )
                    
                    if (success) {
                        Toast.makeText(requireContext(), 
                            if (isChecked) "Verification checkmark will show in explore" 
                            else "Verification checkmark hidden from explore", 
                            Toast.LENGTH_SHORT).show()
                            
                        // Refresh explore profiles to show the change immediately
                        try {
                            val mainTabFragment = parentFragmentManager.findFragmentByTag("MainTabFragment")
                            if (mainTabFragment is com.chrissyx.zay.ui.main.MainTabFragment) {
                                mainTabFragment.refreshExploreProfiles()
                            }
                        } catch (e: Exception) {
                        }
                    } else {
                        // Revert toggle if update failed
                        binding.showVerificationToggle.isChecked = !isChecked
                        Toast.makeText(requireContext(), "Failed to update verification display", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    binding.showVerificationToggle.isChecked = !isChecked
                    Toast.makeText(requireContext(), "Error updating setting", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Contact Support button
        binding.contactSupportButton.setOnClickListener {
            openContactSupport()
        }
        
        // My Support Tickets button
        binding.mySupportTicketsButton.setOnClickListener {
            openMySupportTickets()
        }
        
        // Manage Devices button
        binding.manageDevicesButton.setOnClickListener {
            openManageDevices()
        }
    }

    private fun loadUserInfo() {
        if (!isAdded || activity == null) return
        
        try {
            val username = userPreferences.username
            val role = userPreferences.role
            val isPro = userPreferences.isPro
            val platform = userPreferences.platform
            val isAdmin = role == "admin"
            
            
            // Set basic info immediately (no lag)
            binding.usernameText.text = "@$username"
            binding.roleText.text = if (isAdmin) "Admin" else "User"
            
            // Load verification status in background
            lifecycleScope.launch {
                try {
                    if (!isAdded || activity == null) return@launch
                    
                    // Show verification checkmark for Instagram-verified users
                    val userData = firebaseRepository.getUserByUsername(username ?: "")
                    val isVerified = userData?.isVerified == true
                    val isInstagramUser = platform == "INSTAGRAM" || userData?.platform?.equals("INSTAGRAM", true) == true
                    
                    // Admins get verification checkmark by default
                    val shouldShowCheckmark = (isVerified && isInstagramUser) || isAdmin
                    
                    if (!isAdded) return@launch
                    
                    binding.verificationCheckmark.visibility = if (shouldShowCheckmark) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    
                    // Show verification request button for non-verified, non-admin Instagram users
                    val shouldShowRequestButton = !isVerified && !isAdmin && isInstagramUser
                    binding.requestVerificationButton.visibility = if (shouldShowRequestButton) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    
                    // Auto-verify admin users if not already verified
                    if (isAdmin && !isVerified) {
                        try {
                            firebaseRepository.verifyUser(username ?: "")
                        } catch (e: Exception) {
                        }
                    }
                    
                    
                    // Show verification settings card for verified users (including admins)
                    if (shouldShowCheckmark) {
                        binding.verificationSettingsCard.visibility = View.VISIBLE
                        binding.showVerificationToggle.isChecked = userData?.showVerificationInExplore ?: true
                    } else {
                        binding.verificationSettingsCard.visibility = View.GONE
                    }
                    
                } catch (e: Exception) {
                    if (isAdded) {
                        binding.verificationCheckmark.visibility = View.GONE
                        binding.requestVerificationButton.visibility = View.GONE
                        binding.verificationSettingsCard.visibility = View.GONE
                    }
                }
            }
            
            // Show pro features for admins only (non-blocking)
            if (isAdmin || username == "_c_ssyx") {
                binding.proFeaturesCard.visibility = View.VISIBLE
                binding.proToggle.isChecked = isPro
                
                // Add admin panel button
                binding.adminPanelCard.visibility = View.VISIBLE
                binding.adminPanelButton.setOnClickListener {
                    openAdminPanel()
                }
                
                // If user is _c_ssyx but not admin role, force update role
                if (username == "_c_ssyx" && role != "admin") {
                    userPreferences.role = "admin"
                    userPreferences.isPro = true
                    // Reload to update UI
                    loadUserInfo()
                }
            } else {
                binding.proFeaturesCard.visibility = View.GONE
                binding.adminPanelCard.visibility = View.GONE
            }
            
            
            // Subscription management moved to Admin Panel
                
        } catch (e: Exception) {
            if (isAdded) {
                binding.roleText.text = "User"
                binding.proFeaturesCard.visibility = View.GONE
                binding.adminPanelCard.visibility = View.GONE
                binding.verificationCheckmark.visibility = View.GONE
                // Subscription management moved to Admin Panel
            }
        }
    }
    
    private fun openAdminPanel() {
        try {
            if (isAdded && activity != null && !requireActivity().isFinishing) {
                val adminPanelFragment = AdminPanelFragment()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, adminPanelFragment)
                    .addToBackStack("admin_panel")
                    .commitAllowingStateLoss() // Safer than commit()
            }
        } catch (e: Exception) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Error opening admin panel", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        try {
            
            lifecycleScope.launch {
                try {
                    userPreferences.logout()
                    
                    // Navigate back to login on main thread
                    if (isAdded && activity != null) {
                        (requireActivity() as MainActivity).showLogin()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    
                    // Fallback navigation
                    if (isAdded && activity != null) {
                        (requireActivity() as MainActivity).showLogin()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestVerification() {
        try {
            
            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Request Verification")
                .setMessage("To request verification, you need to re-authenticate with Instagram to confirm your account ownership. Do you want to continue?")
                .setPositiveButton("Continue") { _, _ ->
                    startInstagramReAuthentication()
                }
                .setNegativeButton("Cancel", null)
                .show()
                
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error starting verification request", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startInstagramReAuthentication() {
        try {
            // Launch Instagram authentication for verification
            val intent = Intent(requireContext(), com.chrissyx.zay.ui.auth.InstagramLoginActivity::class.java)
            intent.putExtra("is_verification_request", true)
            
            instagramVerificationLauncher.launch(intent)
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error launching Instagram authentication", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val instagramVerificationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val instagramUsername = result.data?.getStringExtra("instagram_username") ?: ""
                val accessToken = result.data?.getStringExtra("access_token") ?: ""
                
                if (instagramUsername.isNotEmpty() && accessToken.isNotEmpty()) {
                    submitVerificationRequest(instagramUsername, accessToken)
                } else {
                    Toast.makeText(requireContext(), "Instagram authentication failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Instagram authentication cancelled", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error processing Instagram authentication", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun submitVerificationRequest(instagramUsername: String, accessToken: String) {
        lifecycleScope.launch {
            try {
                val username = userPreferences.username ?: ""
                val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                
                val verificationRequest = com.chrissyx.zay.data.models.VerificationRequest(
                    username = username,
                    instagramUsername = instagramUsername,
                    instagramAccessToken = accessToken,
                    deviceInfo = deviceInfo,
                    userAgent = "Zay Android App"
                )
                
                val success = firebaseRepository.createVerificationRequest(verificationRequest)
                
                if (!isAdded) return@launch
                
                if (success) {
                    // Hide the request button since request is submitted
                    binding.requestVerificationButton.visibility = View.GONE
                    
                    Toast.makeText(
                        requireContext(), 
                        "Verification request submitted successfully! Admins will review your request.", 
                        Toast.LENGTH_LONG
                    ).show()
                    
                } else {
                    Toast.makeText(requireContext(), "Failed to submit verification request", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error submitting verification request", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleSubscription() {
        
        // Check if user is already subscribed
        if (userPreferences.isPro) {
            // User is already subscribed - show management options
            showManageSubscriptionDialog()
        } else {
            // User is not subscribed - show subscription options
            showSubscriptionConfirmDialog("$1.99") {
                simulateSubscriptionForTesting()
            }
        }
    }
    
    private fun showSubscriptionConfirmDialog(price: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("💎 Subscribe to Zay+")
            .setMessage("Get premium features for $price/month:\n\n" +
                    "✨ Priority placement in Explore\n" +
                    "🚀 Pro badge and features\n" +
                    "🎯 Enhanced visibility\n" +
                    "💬 Priority support\n\n" +
                    "Start your subscription now?")
            .setPositiveButton("Subscribe") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAlreadySubscribedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("💎 Already Subscribed")
            .setMessage("You're already a Zay+ subscriber! Enjoy your premium features.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun handleSubscriptionResult(success: Boolean, message: String?) {
        if (success) {
            // Update user to pro status
            userPreferences.isPro = true
            
            // Update in Firebase
            lifecycleScope.launch {
                try {
                    val username = userPreferences.username ?: return@launch
                    firebaseRepository.updateUserProStatus(username, true)
                    
                    Toast.makeText(requireContext(), "🎉 Welcome to Zay+! $message", Toast.LENGTH_LONG).show()
                    
                    // Refresh UI to show pro status
                    loadUserInfo()
                } catch (e: Exception) {
                }
            }
        } else {
            Toast.makeText(requireContext(), message ?: "Subscription failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun simulateSubscriptionForTesting() {
        
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Processing Subscription...")
            .setMessage("Simulating payment process...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        // Simulate processing delay
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000) // 2 second delay
            loadingDialog.dismiss()
            
            // Set subscription details
            userPreferences.subscriptionType = "Monthly"
            val renewalDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)) // 30 days from now
            userPreferences.subscriptionRenewalDate = renewalDate
            
            // Simulate successful subscription
            handleSubscriptionResult(true, "Development subscription activated!")
        }
    }

    private fun showManageSubscriptionDialog() {
        val renewalDate = userPreferences.subscriptionRenewalDate ?: "Unknown"
        val subscriptionType = userPreferences.subscriptionType ?: "Monthly"
        
        val benefits = """
            ✨ Priority placement in Explore
            🚀 Pro badge and features
            🎯 Enhanced visibility
            💬 Priority support
            📊 Advanced analytics
            🎨 Custom themes (coming soon)
        """.trimIndent()
        
        val message = """
            Current Plan: Zay+ $subscriptionType
            Renews on: $renewalDate
            
            Your Benefits:
            $benefits
            
            What would you like to do?
        """.trimIndent()
        
        AlertDialog.Builder(requireContext())
            .setTitle("💎 Manage Subscription")
            .setMessage(message)
            .setPositiveButton("Extend Subscription") { _, _ ->
                showExtendSubscriptionDialog()
            }
            .setNeutralButton("Cancel Subscription") { _, _ ->
                showCancelSubscriptionDialog()
            }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun showExtendSubscriptionDialog() {
        val currentPrice = 1.99
        val yearlyPrice = currentPrice * 12 * 0.8 // 20% off
        val twoYearPrice = currentPrice * 24 * 0.75 // 25% off
        
        val options = arrayOf(
            "1 Year - \$${String.format("%.2f", yearlyPrice)} (20% OFF - Save \$${String.format("%.2f", currentPrice * 12 - yearlyPrice)})",
            "2 Years - \$${String.format("%.2f", twoYearPrice)} (25% OFF - Save \$${String.format("%.2f", currentPrice * 24 - twoYearPrice)})"
        )
        
        AlertDialog.Builder(requireContext())
            .setTitle("🚀 Extend Your Subscription")
            .setMessage("Choose your new subscription plan:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> extendSubscription("yearly", yearlyPrice)
                    1 -> extendSubscription("2year", twoYearPrice)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun extendSubscription(type: String, price: Double) {
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Processing Extension...")
            .setMessage("Upgrading your subscription...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000) // Simulate processing
            loadingDialog.dismiss()
            
            // Update subscription details
            val renewalDate = when (type) {
                "yearly" -> {
                    userPreferences.subscriptionType = "Yearly"
                    java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000))
                }
                "2year" -> {
                    userPreferences.subscriptionType = "2-Year"
                    java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(System.currentTimeMillis() + 2L * 365 * 24 * 60 * 60 * 1000))
                }
                else -> userPreferences.subscriptionRenewalDate ?: "Unknown"
            }
            
            userPreferences.subscriptionRenewalDate = renewalDate
            
            Toast.makeText(
                requireContext(),
                "🎉 Subscription extended! Your new plan is active until $renewalDate",
                Toast.LENGTH_LONG
            ).show()
            
            // Subscription management moved to Admin Panel
        }
    }
    
    private fun showCancelSubscriptionDialog() {
        val renewalDate = userPreferences.subscriptionRenewalDate ?: "Unknown"
        
        AlertDialog.Builder(requireContext())
            .setTitle("❌ Cancel Subscription")
            .setMessage("Are you sure you want to cancel your Zay+ subscription?\n\n" +
                    "Your benefits will continue until: $renewalDate\n\n" +
                    "You can resubscribe anytime to restore your pro features.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelSubscription()
            }
            .setNegativeButton("Keep Subscription", null)
            .show()
    }
    
    private fun cancelSubscription() {
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Canceling Subscription...")
            .setMessage("Processing cancellation...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1500) // Simulate processing
            loadingDialog.dismiss()
            
            // Update subscription status
            userPreferences.isPro = false
            userPreferences.subscriptionType = null
            userPreferences.subscriptionRenewalDate = null
            
            // Update in Firebase
            val username = userPreferences.username ?: return@launch
            firebaseRepository.updateUserProStatus(username, false)
            
            Toast.makeText(
                requireContext(),
                "Subscription canceled. Your pro features will remain active until the renewal date.",
                Toast.LENGTH_LONG
            ).show()
            
            // Subscription management moved to Admin Panel
            loadUserInfo() // Refresh UI
        }
    }

    // Subscription button removed - management moved to Admin Panel
    
    private fun openContactSupport() {
        try {
            if (isAdded && activity != null) {
                val contactSupportFragment = com.chrissyx.zay.ui.support.ContactSupportFragment()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, contactSupportFragment)
                    .addToBackStack("contact_support")
                    .commit()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening contact support", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openMySupportTickets() {
        try {
            if (isAdded && activity != null) {
                val mySupportTicketsFragment = MySupportTicketsFragment()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, mySupportTicketsFragment)
                    .addToBackStack("my_support_tickets")
                    .commit()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening support tickets", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openManageDevices() {
        try {
            if (isAdded && activity != null && !requireActivity().isFinishing) {
                val manageDevicesFragment = com.chrissyx.zay.ui.settings.ManageDevicesFragment()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, manageDevicesFragment)
                    .addToBackStack("manage_devices")
                    .commitAllowingStateLoss()
            }
        } catch (e: Exception) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Error opening device management", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::billingManager.isInitialized) {
            billingManager.disconnect()
        }
        _binding = null
    }
} 