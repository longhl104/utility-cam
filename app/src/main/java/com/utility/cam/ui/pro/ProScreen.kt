package com.utility.cam.ui.pro

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.utility.cam.BuildConfig
import com.utility.cam.R
import com.utility.cam.data.BillingManager
import com.utility.cam.data.PreferencesManager

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("COMPOSE_INVALID_RESOURCE_QUERY")
fun ProScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val billingManager = remember { BillingManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }

    val isProUser by billingManager.isProUser.collectAsState()
    val debugProOverride by preferencesManager.getDebugProOverride().collectAsState(initial = false)
    val actualIsProUser = isProUser || (BuildConfig.DEBUG && debugProOverride)
    val productDetails by billingManager.productDetails.collectAsState()
    val purchaseState by billingManager.purchaseState.collectAsState()
    val connectionState by billingManager.billingConnectionState.collectAsState()

    // Refresh product details when screen loads
    LaunchedEffect(Unit) {
        billingManager.refreshProductDetails()
    }

    // Handle purchase state changes
    LaunchedEffect(purchaseState) {
        when (purchaseState) {
            is BillingManager.PurchaseState.Success -> {
                Toast.makeText(context, context.getString(R.string.pro_purchase_success), Toast.LENGTH_LONG).show()
                billingManager.resetPurchaseState()
            }
            is BillingManager.PurchaseState.Error -> {
                val message = (purchaseState as BillingManager.PurchaseState.Error).message
                Toast.makeText(context, context.getString(R.string.pro_purchase_error, message), Toast.LENGTH_LONG).show()
                billingManager.resetPurchaseState()
            }
            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            billingManager.endConnection()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pro_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pro_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (actualIsProUser) {
                // User already has Pro
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.pro_already_purchased),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.pro_thank_you),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                // Show purchase option
                Text(
                    stringResource(R.string.pro_upgrade_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    stringResource(R.string.pro_upgrade_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pro Features
            Text(
                stringResource(R.string.pro_features_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Feature items
            ProFeatureItem(
                icon = Icons.Default.CloudOff,
                title = stringResource(R.string.pro_feature_1_title),
                description = stringResource(R.string.pro_feature_1_description)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProFeatureItem(
                icon = Icons.Default.Star,
                title = stringResource(R.string.pro_feature_2_title),
                description = stringResource(R.string.pro_feature_2_description)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProFeatureItem(
                icon = Icons.Default.Favorite,
                title = stringResource(R.string.pro_feature_3_title),
                description = stringResource(R.string.pro_feature_3_description)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProFeatureItem(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.pro_feature_4_title),
                description = stringResource(R.string.pro_feature_4_description)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!actualIsProUser) {
                // Purchase button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        productDetails?.let { details ->
                            Text(
                                stringResource(R.string.pro_one_time_payment),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                details.oneTimePurchaseOfferDetails?.formattedPrice ?: stringResource(R.string.pro_price_loading),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } ?: run {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.pro_loading_price),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                activity?.let {
                                    billingManager.launchPurchaseFlow(it)
                                } ?: run {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.pro_purchase_unavailable),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = productDetails != null && purchaseState !is BillingManager.PurchaseState.Loading
                        ) {
                            if (purchaseState is BillingManager.PurchaseState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.pro_purchase_button))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.pro_one_time_note),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Debug section - only in debug builds
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Debug Info",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Debug Pro Override: $debugProOverride",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "isProUser (Billing): $isProUser",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Actual Pro Status: $actualIsProUser",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Connection: ${connectionState::class.simpleName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Product Details: ${if (productDetails != null) "Loaded" else "Not loaded"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Product ID: ${BillingManager.PRODUCT_ID_LIFETIME_PRO}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        if (productDetails != null) {
                            Text(
                                "Price: ${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { billingManager.refreshProductDetails() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("Force Refresh Product Details")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
