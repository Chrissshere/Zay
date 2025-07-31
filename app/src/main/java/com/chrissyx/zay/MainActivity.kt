package com.chrissyx.zay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chrissyx.zay.databinding.ActivityMainBinding
import com.chrissyx.zay.ui.auth.LoginFragment
import com.chrissyx.zay.ui.main.MainTabFragment
import com.chrissyx.zay.ui.messaging.MessageSheetFragment
import com.chrissyx.zay.utils.UserPreferences
import com.chrissyx.zay.utils.LinkSecurityManager
import com.chrissyx.zay.utils.TicketUtils
import com.chrissyx.zay.data.repository.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userPreferences: UserPreferences
    private var pendingDeepLinkUsername: String? = null
    private lateinit var linkSecurityManager: LinkSecurityManager
    private lateinit var firebaseRepository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userPreferences = UserPreferences(this)
        linkSecurityManager = LinkSecurityManager(this)
        firebaseRepository = FirebaseRepository()
        
        // Clean up expired tokens on app start
        linkSecurityManager.cleanupExpiredTokens()
        
        // Handle deep links
        handleIntent(intent)
        
        if (userPreferences.isLoggedIn()) {
            binding.root.post {
                if (!isFinishing && !isDestroyed) {
                    showMainApp()
                }
            }
            pendingDeepLinkUsername?.let { username ->
                showMessageSheet(username)
                pendingDeepLinkUsername = null
            }
        } else {
            showLoginScreen()
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        val data = intent.data
        if (data != null && data.scheme == "zay") {
            when (data.host) {
                "profile" -> {
                    val username = data.pathSegments?.getOrNull(0)
                    val token = data.getQueryParameter("token")
                    
                    if (username != null && token != null) {
                        // Validate security token
                        val validatedUsername = linkSecurityManager.validateAndConsumeToken(token)
                        
                        if (validatedUsername == username) {
                            showMessageSheet(username)
                        } else {
                            showSecurityError()
                        }
                    } else if (username != null) {
                        // Direct link to user profile - open message sheet
                        showMessageSheet(username)
                    }
                }
                "instagram" -> {
                    // Handle Instagram OAuth callback
                    val code = data.getQueryParameter("code")
                    val error = data.getQueryParameter("error")
                    
                    if (code != null) {
                        // Handle Instagram verification success
                    } else if (error != null) {
                        // Handle Instagram verification error
                    }
                }
                "auth" -> {
                    // Handle OAuth callbacks: zay://auth/snapchat/callback or zay://auth/instagram/callback
                    val platform = data.pathSegments?.getOrNull(0) // "snapchat" or "instagram"
                    val action = data.pathSegments?.getOrNull(1)   // "callback"
                    
                    if (platform == "snapchat" && action == "callback") {
                        handleSnapchatCallback(data)
                    } else if (platform == "instagram" && action == "callback") {
                        handleInstagramCallback(data)
                    }
                }
                "zayapi" -> {
                    // Handle support ticket login: zay://zayapi/supportticket/id?=JH13BNK/key?=872977ndokn928ndo93bdbla
                    if (data.pathSegments?.getOrNull(0) == "supportticket") {
                        handleSupportTicketLogin(data)
                    }
                }
            }
        }
    }
    
    private fun handleSnapchatCallback(uri: Uri) {
        
        // Extract authorization code and state from callback
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        
        if (code != null && state != null) {
            // Handle successful Snapchat authorization
            // The SnapchatLoginActivity will handle the token exchange
        } else if (error != null) {
            // Handle Snapchat authorization error
        }
    }
    
    private fun handleInstagramCallback(uri: Uri) {
        
        // Extract authorization code from callback
        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")
        
        if (code != null) {
            // Handle successful Instagram authorization
        } else if (error != null) {
        }
    }
    
    private fun handleSupportTicketLogin(uri: Uri) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Parse ticket ID and login key from URL
                val ticketId = TicketUtils.parseTicketIdFromUrl(uri.toString())
                val loginKey = TicketUtils.parseLoginKeyFromUrl(uri.toString())
                
                if (ticketId == null || loginKey == null) {
                    showLoginError("Invalid login link format")
                    return@launch
                }
                
                
                // Get and validate login link from Firestore
                val loginLink = firebaseRepository.getLoginLink(loginKey)
                
                if (loginLink == null) {
                    showLoginError("Login link not found or has expired")
                    return@launch
                }
                
                // Check if already used
                if (loginLink.isUsed) {
                    showLoginError("This login link has already been used")
                    return@launch
                }
                
                // Check if expired
                val now = System.currentTimeMillis() / 1000.0
                if (now > loginLink.expiresAt) {
                    showLoginError("This login link has expired")
                    // Clean up expired link
                    firebaseRepository.deleteLoginLink(loginKey)
                    return@launch
                }
                
                // Check if ticket ID matches
                if (loginLink.ticketId != ticketId) {
                    showLoginError("Invalid login link")
                    return@launch
                }
                
                // Valid link - log in as target user
                val targetUsername = loginLink.targetUsername
                
                // Get user data for the target user
                val userData = firebaseRepository.getUserByUsernameWithFallback(targetUsername, "INSTAGRAM")
                    ?: firebaseRepository.getUserByUsernameWithFallback(targetUsername, "SNAPCHAT")
                    ?: firebaseRepository.getUserByUsernameWithFallback(targetUsername, "TIKTOK")
                
                if (userData != null) {
                    // Store login data
                    userPreferences.username = userData.username
                    userPreferences.platform = userData.platform
                    userPreferences.isPro = userData.isPro
                    userPreferences.role = userData.role
                    
                    // Mark link as used and delete from Firestore
                    firebaseRepository.useLoginLink(loginKey)
                    firebaseRepository.deleteLoginLink(loginKey)
                    
                    // Show success message
                    Toast.makeText(this@MainActivity, "✅ Logged in as @$targetUsername", Toast.LENGTH_LONG).show()
                    
                    // Navigate to main app
                    showMainApp()
                } else {
                    showLoginError("Target user account not found")
                }
                
            } catch (e: Exception) {
                showLoginError("Error processing login link")
            }
        }
    }
    
    private fun showLoginError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Login Link Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showMessageSheet(username: String) {
        val messageSheet = MessageSheetFragment.newInstance(username)
        messageSheet.show(supportFragmentManager, "MessageSheet")
    }
    
    private fun showLoginScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, LoginFragment())
            .commit()
    }
    
    fun showMainApp() {
        try {
            
            if (isFinishing || isDestroyed) {
                return
            }
            
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.container, MainTabFragment())
            
            // Use commitAllowingStateLoss to prevent crashes
            if (!supportFragmentManager.isStateSaved) {
                transaction.commit()
            } else {
                transaction.commitAllowingStateLoss()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun showLogin() {
        showLoginScreen()
    }

    private fun showSecurityError() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Security Error")
            .setMessage("This link has expired or has already been used for security reasons.\n\nPlease request a new link from the user.")
            .setPositiveButton("OK", null)
            .show()
    }
}