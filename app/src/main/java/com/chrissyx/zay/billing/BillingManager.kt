package com.chrissyx.zay.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingManager(private val context: Context) : PurchasesUpdatedListener {
    
    private var billingClient: BillingClient? = null
    private var purchaseUpdateListener: ((Boolean, String?) -> Unit)? = null
    
    companion object {
        // Real subscription product ID for Google Play Console
        const val ZAY_PLUS_MONTHLY = "zay_plus_monthly"
        private const val TAG = "BillingManager"
    }
    
    fun initialize(callback: (Boolean) -> Unit) {
        
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
            
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
            
            override fun onBillingServiceDisconnected() {
            }
        })
    }
    
    suspend fun getSubscriptionDetails(): ProductDetails? {
        return suspendCancellableCoroutine { continuation ->
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(ZAY_PLUS_MONTHLY)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
                
            billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(productDetailsList.firstOrNull())
                } else {
                    continuation.resume(null)
                }
            }
        }
    }
    
    fun launchSubscriptionFlow(
        activity: Activity, 
        productDetails: ProductDetails,
        callback: (Boolean, String?) -> Unit
    ) {
        purchaseUpdateListener = callback
        
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            callback(false, "No subscription offer available")
            return
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
            
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            callback(false, "Failed to launch purchase flow")
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { purchasesList ->
                    for (purchase in purchasesList) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchaseUpdateListener?.invoke(false, "Purchase canceled")
            }
            else -> {
                purchaseUpdateListener?.invoke(false, "Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }
    
    private fun handlePurchase(purchase: Purchase) {
        
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge the purchase
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                    
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        purchaseUpdateListener?.invoke(true, "Subscription activated!")
                    } else {
                        purchaseUpdateListener?.invoke(false, "Failed to activate subscription")
                    }
                }
            } else {
                purchaseUpdateListener?.invoke(true, "Subscription activated!")
            }
        } else {
            purchaseUpdateListener?.invoke(false, "Purchase not completed")
        }
    }
    
    suspend fun checkActiveSubscriptions(): List<Purchase> {
        return suspendCancellableCoroutine { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
                
            billingClient?.queryPurchasesAsync(params) { billingResult, purchasesList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(purchasesList)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }
    }
    
    fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
        purchaseUpdateListener = null
    }
} 