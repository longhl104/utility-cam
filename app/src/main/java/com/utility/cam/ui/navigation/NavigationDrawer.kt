package com.utility.cam.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.utility.cam.R
import com.utility.cam.ui.common.rememberProUserStateWithManagers

@Composable
fun AppNavigationDrawer(
    currentRoute: String?,
    onNavigateToGallery: () -> Unit,
    onNavigateToBin: () -> Unit,
    onNavigateToPdfGenerator: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPro: () -> Unit,
    onDismiss: () -> Unit
) {
    val proUserState = rememberProUserStateWithManagers()
    val actualIsProUser = proUserState.actualIsProUser

    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // App Title
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Pro Badge (if user is Pro)
            if (actualIsProUser) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.pro_already_purchased),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Navigation Items
            NavigationDrawerItem(
                icon = Icons.Default.Photo,
                label = stringResource(R.string.gallery_title),
                selected = currentRoute == Screen.Gallery.route,
                onClick = {
                    onNavigateToGallery()
                    onDismiss()
                }
            )

            NavigationDrawerItem(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.bin_title),
                selected = currentRoute == Screen.Bin.route,
                onClick = {
                    onNavigateToBin()
                    onDismiss()
                }
            )

            // PDF Generator (Pro only)
            NavigationDrawerItem(
                icon = Icons.Default.PictureAsPdf,
                label = stringResource(R.string.pdf_generator_title),
                selected = currentRoute == Screen.PdfGenerator.route,
                onClick = {
                    if (actualIsProUser) {
                        onNavigateToPdfGenerator()
                        onDismiss()
                    } else {
                        onNavigateToPro()
                        onDismiss()
                    }
                },
                badge = if (!actualIsProUser) {
                    { ProBadge() }
                } else null
            )

            NavigationDrawerItem(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.settings_title),
                selected = currentRoute == Screen.Settings.route,
                onClick = {
                    onNavigateToSettings()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Upgrade to Pro button (if not Pro user)
            if (!actualIsProUser) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Button(
                    onClick = {
                        onNavigateToPro()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_go_pro),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationDrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    badge: (@Composable () -> Unit)? = null
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
        },
        badge = badge,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ProBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "PRO",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

