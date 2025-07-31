package com.chrissyx.zay.ui.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chrissyx.zay.R
import com.chrissyx.zay.config.SnapchatConfig
import com.chrissyx.zay.databinding.ActivitySnapchatLoginBinding
import com.chrissyx.zay.network.SnapchatAuthService
import kotlinx.coroutines.launch

class SnapchatLoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySnapchatLoginBinding
    private lateinit var snapchatAuthService: SnapchatAuthService
    private var pkceParams: SnapchatAuthService.PKCEParams? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySnapchatLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        snapchatAuthService = SnapchatAuthService(this)
        setupWebView()
        startSnapchatAuth()
    }
    
    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
        
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { handleUrlRedirect(it) }
                return super.shouldOverrideUrlLoading(view, url)
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { handleUrlRedirect(it) }
            }
        }
        
        // Close button
        binding.closeButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
    
    private fun startSnapchatAuth() {
        try {
            pkceParams = snapchatAuthService.generatePKCEParams()
            val authUrl = snapchatAuthService.buildAuthorizationUrl(pkceParams!!)
            
            binding.webView.loadUrl(authUrl)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting Snapchat authentication", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
    
    private fun handleUrlRedirect(url: String) {
        
        if (url.startsWith(SnapchatConfig.REDIRECT_URI)) {
            val uri = Uri.parse(url)
            
            // Check for authorization code
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")
            val error = uri.getQueryParameter("error")
            
            when {
                error != null -> {
                    val errorDescription = uri.getQueryParameter("error_description") ?: "Unknown error"
                    Toast.makeText(this, "Snapchat authentication failed: $errorDescription", Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                code != null && state != null -> {
                    // Verify state parameter to prevent CSRF attacks
                    if (state == pkceParams?.state) {
                        exchangeCodeForToken(code)
                    } else {
                        Toast.makeText(this, "Security error - authentication cancelled", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                }
                else -> {
                    // Check for implicit grant flow (access_token in fragment)
                    val fragment = uri.fragment
                    if (fragment != null && fragment.contains("access_token=")) {
                        val accessToken = fragment.substringAfter("access_token=").substringBefore("&")
                        fetchUserProfileAndFinish(accessToken)
                    }
                }
            }
        }
    }
    
    private fun exchangeCodeForToken(authorizationCode: String) {
        lifecycleScope.launch {
            try {
                val codeVerifier = pkceParams?.codeVerifier ?: return@launch
                
                val accessToken = snapchatAuthService.exchangeCodeForToken(authorizationCode, codeVerifier)
                
                if (accessToken != null) {
                    fetchUserProfileAndFinish(accessToken)
                } else {
                    Toast.makeText(this@SnapchatLoginActivity, "Failed to authenticate with Snapchat", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@SnapchatLoginActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }
    
    private fun fetchUserProfileAndFinish(accessToken: String) {
        lifecycleScope.launch {
            try {
                val userProfile = snapchatAuthService.getUserProfile(accessToken)
                
                if (userProfile != null) {
                    
                    // Return user data to calling activity
                    val resultIntent = Intent().apply {
                        putExtra("snapchat_username", userProfile.displayName)
                        putExtra("snapchat_external_id", userProfile.externalId)
                        putExtra("access_token", accessToken)
                        putExtra("bitmoji_avatar", userProfile.bitmojiAvatar)
                        putExtra("platform", "SNAPCHAT")
                    }
                    
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                    
                } else {
                    Toast.makeText(this@SnapchatLoginActivity, "Failed to get Snapchat profile", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@SnapchatLoginActivity, "Error getting profile", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }
} 