package com.chrissyx.zay.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.chrissyx.zay.R
import java.net.URLEncoder

class TikTokLoginActivity : AppCompatActivity() {
    
    companion object {
        const val CLIENT_KEY = "YOUR_TIKTOK_CLIENT_KEY"
        const val CLIENT_SECRET = "YOUR_TIKTOK_CLIENT_SECRET"
        const val REDIRECT_URI = "https://zayngl.web.app/tiktok-callback"
        const val SCOPE = "user.info.basic"
        
        const val EXTRA_AUTH_CODE = "auth_code"
        const val EXTRA_ERROR = "error"
    }
    
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tiktok_login)
        
        webView = findViewById(R.id.webView)
        setupWebView()
        loadTikTokAuth()
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { handleUrl(it) }
                return false
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { handleUrl(it) }
            }
        }
    }
    
    private fun loadTikTokAuth() {
        val authUrl = buildAuthUrl()
        webView.loadUrl(authUrl)
    }
    
    private fun buildAuthUrl(): String {
        val baseUrl = "https://www.tiktok.com/auth/authorize/"
        val params = mapOf(
            "client_key" to CLIENT_KEY,
            "scope" to SCOPE,
            "response_type" to "code",
            "redirect_uri" to REDIRECT_URI,
            "state" to "tiktok_auth_${System.currentTimeMillis()}"
        )
        
        val queryString = params.map { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }.joinToString("&")
        
        return "$baseUrl?$queryString"
    }
    
    private fun handleUrl(url: String) {
        
        if (url.startsWith(REDIRECT_URI)) {
            val uri = Uri.parse(url)
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            
            when {
                code != null -> {
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_AUTH_CODE, code)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
                error != null -> {
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_ERROR, error)
                    }
                    setResult(RESULT_CANCELED, resultIntent)
                    finish()
                }
            }
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            setResult(RESULT_CANCELED)
            super.onBackPressed()
        }
    }
} 