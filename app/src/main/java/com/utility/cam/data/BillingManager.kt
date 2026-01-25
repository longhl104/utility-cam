package com.utility.cam.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google Play Billing for in-app purchases
 */
class BillingManager(private val context: Context) {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_ID_LIFETIME_PRO = "lifetime_pro" // Replace with your actual product ID
    }

    private var billingClient: BillingClient? = null

    private val _isProUser = MutableStateFlow(false)
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _billingConnectionState =
        MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    val billingConnectionState: StateFlow<BillingConnectionState> =
        _billingConnectionState.asStateFlow()

    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Loading : PurchaseState()
        object Success : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    sealed class BillingConnectionState {
        object Disconnected : BillingConnectionState()
        object Connecting : BillingConnectionState()
        object Connected : BillingConnectionState()
        data class Failed(val message: String) : BillingConnectionState()
    }

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK if purchases != null -> {
                        for (purchase in purchases) {
                            handlePurchase(purchase)
                        }
                    }

                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        Log.d(TAG, "User canceled the purchase")
                        _purchaseState.value = PurchaseState.Idle
                    }

                    else -> {
                        Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
                        _purchaseState.value = PurchaseState.Error(billingResult.debugMessage)
                    }
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()

        startConnection()
    }

    private fun startConnection() {
        _billingConnectionState.value = BillingConnectionState.Connecting
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    _billingConnectionState.value = BillingConnectionState.Connected
                    // Query existing purchases
                    queryPurchases()
                    // Query product details
                    queryProductDetails()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _billingConnectionState.value =
                        BillingConnectionState.Failed(billingResult.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected")
                _billingConnectionState.value = BillingConnectionState.Disconnected
                // Try to restart the connection on the next request
            }
        })
    }

    private fun queryProductDetails() {
        Log.d(TAG, "queryProductDetails() called")
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_LIFETIME_PRO)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            Log.d(
                TAG,
                "queryProductDetailsAsync callback: responseCode=${billingResult.responseCode}"
            )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // productDetailsList is a List<ProductDetails>
                val productDetailsList = productDetailsResult.productDetailsList
                Log.d(TAG, "Product details list size: ${productDetailsList.size}")
                if (productDetailsList.isEmpty()) {
                    Log.w(
                        TAG,
                        "Product details list is empty - product may not be configured in Play Console"
                    )
                } else {
                    _productDetails.value = productDetailsList[0]
                    Log.d(TAG, "Product details loaded: ${productDetailsList[0]}")
                }
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
            }
        } ?: Log.e(TAG, "BillingClient is null when querying product details")
    }

    /**
     * Manually refresh product details. Useful when you want to ensure latest pricing is loaded.
     */
    fun refreshProductDetails() {
        Log.d(TAG, "Manual refresh requested. Connection state: ${_billingConnectionState.value}")
        if (_billingConnectionState.value is BillingConnectionState.Connected) {
            queryProductDetails()
        } else {
            Log.w(TAG, "Cannot refresh - billing not connected. Will retry connection.")
            startConnection()
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Check if user has purchased Lifetime Pro
                val hasLifetimePro = purchases.any {
                    it.products.contains(PRODUCT_ID_LIFETIME_PRO) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isProUser.value = hasLifetimePro
                Log.d(TAG, "User is pro: $hasLifetimePro")

                // Acknowledge unacknowledged purchases
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            } else {
                Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val productDetails = _productDetails.value
        if (productDetails == null) {
            Log.e(TAG, "Product details not available")
            _purchaseState.value = PurchaseState.Error("Product not available")
            return
        }

        _purchaseState.value = PurchaseState.Loading

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }

            // Check if this is the Lifetime Pro purchase
            if (purchase.products.contains(PRODUCT_ID_LIFETIME_PRO)) {
                _isProUser.value = true
                _purchaseState.value = PurchaseState.Success
                Log.d(TAG, "Lifetime Pro purchased!")
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }

    fun endConnection() {
        billingClient?.endConnection()
    }
}
