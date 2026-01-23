package com.utility.cam.ui.capturereview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import com.utility.cam.BuildConfig
import com.utility.cam.R
import com.utility.cam.data.BillingManager
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.PreferencesManager
import com.utility.cam.data.TTLDuration
import com.utility.cam.ui.common.CustomTTLDialog
import com.utility.cam.ui.common.ProLockedDialog
import com.utility.cam.ui.common.VideoPlayer
import com.utility.cam.ui.common.rememberProUserStateWithManagers
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureReviewScreen(
    capturedImagePath: String,
    onMediaSaved: () -> Unit,
    onRetake: () -> Unit,
    onNavigateToPro: () -> Unit = {}
) {
    val context = LocalContext.current
    val storageManager = remember { PhotoStorageManager(context) }
    val proUserState = rememberProUserStateWithManagers()
    val preferencesManager = proUserState.preferencesManager
    val actualIsProUser = proUserState.actualIsProUser
    val coroutineScope = rememberCoroutineScope()

    val defaultTTL by preferencesManager.getDefaultTTL().collectAsState(initial = TTLDuration.TWENTY_FOUR_HOURS)

    var selectedTTL by remember { mutableStateOf<TTLDuration?>(null) }
    var customTTLHours by remember { mutableStateOf<Int?>(null) }
    var description by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showCustomTTLDialog by remember { mutableStateOf(false) }
    var showProLockedDialog by remember { mutableStateOf(false) }

    val ttl = selectedTTL ?: defaultTTL
    val effectiveTTLMillis = customTTLHours?.let { it * 60 * 60 * 1000L } ?: ttl.toMilliseconds()

    // Detect if the file is a video
    val isVideo = capturedImagePath.endsWith(".mp4", ignoreCase = true)
    val titleRes = if (isVideo) R.string.capture_review_title_video else R.string.capture_review_title

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Media preview (Image or Video)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                if (isVideo) {
                    // Video player with controls
                    VideoPlayer(
                        videoUri = capturedImagePath,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Image preview
                    val painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(File(capturedImagePath))
                            .crossfade(true)
                            .build()
                    )

                    Image(
                        painter = painter,
                        contentDescription = stringResource(R.string.capture_review_title),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // TTL Selection
                Text(
                    stringResource(R.string.capture_review_delete_after),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TTLDuration.entries
                        .filter { !it.isDebugOnly || BuildConfig.DEBUG }
                        .forEach { duration ->
                            FilterChip(
                                selected = customTTLHours == null && ttl == duration,
                                onClick = {
                                    selectedTTL = duration
                                    customTTLHours = null // Clear custom when selecting preset
                                },
                                label = { Text(duration.getDisplayName(context)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom TTL option (Pro only) - separate line
                FilterChip(
                    selected = customTTLHours != null,
                    onClick = {
                        if (actualIsProUser) {
                            showCustomTTLDialog = true
                        } else {
                            showProLockedDialog = true
                        }
                    },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!actualIsProUser) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                if (customTTLHours != null) {
                                    "$customTTLHours" + stringResource(R.string.capture_review_custom_hours_suffix)
                                } else {
                                    stringResource(R.string.capture_review_custom)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.capture_review_description_label)) },
                    placeholder = { Text(stringResource(R.string.capture_review_description_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text(stringResource(R.string.capture_review_retake))
                    }

                    Button(
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                // Use custom TTL milliseconds if set, otherwise use enum TTL
                                storageManager.savePhoto(
                                    imageFile = File(capturedImagePath),
                                    ttlMilliseconds = effectiveTTLMillis,
                                    description = description.takeIf { it.isNotBlank() }
                                )
                                isSaving = false
                                onMediaSaved()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.capture_review_save))
                        }
                    }
                }
            }
        }
    }

    // Pro locked dialog
    if (showProLockedDialog) {
        ProLockedDialog(
            onDismiss = { showProLockedDialog = false },
            onUpgrade = onNavigateToPro,
            titleResId = R.string.capture_review_custom_pro_title,
            messageResId = R.string.capture_review_custom_pro_message,
            upgradeButtonResId = R.string.camera_video_pro_only_upgrade,
            cancelButtonResId = R.string.capture_review_custom_pro_ok
        )
    }

    // Custom TTL dialog (Pro only)
    if (showCustomTTLDialog) {
        CustomTTLDialog(
            onDismiss = { showCustomTTLDialog = false },
            onConfirm = { hours ->
                customTTLHours = hours
                selectedTTL = null // Clear enum selection when using custom
                showCustomTTLDialog = false
            }
        )
    }
}
