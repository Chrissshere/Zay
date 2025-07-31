package com.chrissyx.zay.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chrissyx.zay.config.InstagramConfig
import com.chrissyx.zay.databinding.ActivityInstagramLoginBinding
import com.chrissyx.zay.network.InstagramAuthService
import kotlinx.coroutines.launch

class InstagramLoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityInstagramLoginBinding
    private val instagramService = InstagramAuthService()
    
    companion object {
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_INSTAGRAM_ID = "instagram_id"
        const val EXTRA_ERROR = "error"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstagramLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupWebView()
        loadInstagramAuth()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Instagram Login"
        }
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                
                if (url?.startsWith(InstagramConfig.REDIRECT_URI) == true) {
                    handleRedirect(url)
                    return true
                }
                return false
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }
    }
    
    private fun loadInstagramAuth() {
        val authUrl = instagramService.getAuthorizationUrl()
        binding.webView.loadUrl(authUrl)
    }
    
    private fun handleRedirect(url: String) {
        
        val uri = Uri.parse(url)
        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")
        val errorReason = uri.getQueryParameter("error_reason")
        
        when {
            code != null -> {
                exchangeCodeForUserInfo(code)
            }
            error != null -> {
                val fullError = buildString {
                    append(error)
                    if (errorReason != null) append(" ($errorReason)")
                    if (errorDescription != null) append(": $errorDescription")
                }
                
                // Handle specific Instagram API errors
                when (error) {
                    "access_denied" -> {
                        if (errorReason == "user_denied") {
                            returnError("Instagram login was cancelled by user")
                        } else {
                            returnError("Access denied by Instagram")
                        }
                    }
                    "invalid_request" -> {
                        returnError("Invalid Instagram app configuration. Please check your app settings.")
                    }
                    "unauthorized_client" -> {
                        returnError("Instagram app not authorized. Please check your app credentials.")
                    }
                    "invalid_scope" -> {
                        returnError("Invalid Instagram permissions requested. Please update your app configuration.")
                    }
                    else -> {
                        returnError("Instagram authorization failed: $fullError")
                    }
                }
            }
            else -> {
                returnError("Unknown error occurred during Instagram login")
            }
        }
    }
    
    private fun exchangeCodeForUserInfo(code: String) {
        lifecycleScope.launch {
            try {
                // Show loading
                Toast.makeText(this@InstagramLoginActivity, "Getting Instagram profile...", Toast.LENGTH_SHORT).show()
                
                // Exchange code for access token
                val accessToken = instagramService.exchangeCodeForToken(code)
                if (accessToken == null) {
                    returnError("Failed to get Instagram access token")
                    return@launch
                }
                
                // Get user profile
                val userProfile = instagramService.getUserProfile(accessToken)
                if (userProfile == null) {
                    returnError("Failed to get Instagram profile")
                    return@launch
                }
                
                
                // Return success result
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_SUCCESS, true)
                    putExtra(EXTRA_USERNAME, userProfile.username)
                    putExtra(EXTRA_INSTAGRAM_ID, userProfile.id)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
                
            } catch (e: Exception) {
                e.printStackTrace()
                returnError("An error occurred: ${e.message}")
            }
        }
    }
    
    private fun returnError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        val resultIntent = Intent().apply {
            putExtra(EXTRA_SUCCESS, false)
            putExtra(EXTRA_ERROR, message)
        }
        setResult(RESULT_CANCELED, resultIntent)
        finish()
    }
    
    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
} 