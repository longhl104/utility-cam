package com.utility.cam.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.utility.cam.R
import com.utility.cam.analytics.AnalyticsHelper
import com.utility.cam.data.BillingManager
import com.utility.cam.data.FeedbackManager
import com.utility.cam.data.PhotoEventBus
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.PreferencesManager
import com.utility.cam.data.UtilityPhoto
import com.utility.cam.ui.feedback.FeedbackDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.res.stringResource
import android.content.Intent
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPhotoDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val storageManager = remember { PhotoStorageManager(context) }
    val feedbackManager = remember { FeedbackManager(context) }
    val billingManager = remember { BillingManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }

    val isProUser by billingManager.isProUser.collectAsState()
    val debugProOverride by preferencesManager.getDebugProOverride().collectAsState(initial = false)
    val actualIsProUser = isProUser || (com.utility.cam.BuildConfig.DEBUG && debugProOverride)
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var photos by remember { mutableStateOf<List<UtilityPhoto>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showAnalyticsConsentDialog by remember { mutableStateOf(false) }

    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPhotoIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }

    // Function to load photos
    suspend fun loadPhotos() {
        photos = storageManager.getAllPhotos()
    }

    // Initial load and reload on trigger
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            isRefreshing = true
        }
        loadPhotos()
        isRefreshing = false
    }

    // Listen to PhotoEventBus for immediate refresh when photos are added/deleted
    LaunchedEffect(Unit) {
        PhotoEventBus.photoEvents.collect { _ ->
            // Refresh gallery when photos are added or deleted
            loadPhotos()
        }
    }

    // Periodic refresh every 60 seconds as a fallback (in case events are missed)
    // This interval is much longer now since we have event-driven updates
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(60_000) // 60 seconds
            loadPhotos()
        }
    }

    // Check and show feedback dialog when appropriate
    LaunchedEffect(Unit) {
        // Wait a bit for the screen to settle
        delay(3000)

        // Check if we should show feedback
        feedbackManager.shouldShowFeedbackPrompt().collect { shouldShow ->
            if (shouldShow && !showFeedbackDialog) {
                showFeedbackDialog = true
                feedbackManager.markPromptShown()
            }
        }
    }

    // Check if we should show analytics consent dialog (first launch)
    LaunchedEffect(Unit) {
        val hasShown = preferencesManager.hasShownAnalyticsConsent().first()
        if (!hasShown) {
            // Small delay to let the UI settle
            delay(500)
            showAnalyticsConsentDialog = true
        }
    }

    // Refresh when the screen resumes (comes back to foreground)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Selection mode top bar
                TopAppBar(
                    title = { Text(stringResource(R.string.gallery_selection_count, selectedPhotoIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedPhotoIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.gallery_exit_selection))
                        }
                    },
                    actions = {
                        // Share button
                        IconButton(
                            onClick = {
                                shareSelectedPhotos(context, photos.filter { it.id in selectedPhotoIds })
                                // Keep selection active after sharing
                            },
                            enabled = selectedPhotoIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.gallery_share_selected))
                        }

                        // Save permanently button
                        IconButton(
                            onClick = {
                                showSaveConfirmDialog = true
                            },
                            enabled = selectedPhotoIds.isNotEmpty()
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = stringResource(R.string.gallery_save_selected))
                        }

                        // Delete button
                        IconButton(
                            onClick = {
                                showDeleteConfirmDialog = true
                            },
                            enabled = selectedPhotoIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.gallery_delete_selected))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            } else {
                // Normal top bar
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(stringResource(R.string.gallery_title))
                            if (actualIsProUser) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "PRO",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.gallery_settings))
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = onNavigateToCamera,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.gallery_take_photo))
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                refreshTrigger++
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.gallery_no_photos),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.gallery_no_photos_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoGridItem(
                            photo = photo,
                            isSelected = selectedPhotoIds.contains(photo.id),
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    // Toggle selection
                                    selectedPhotoIds = if (photo.id in selectedPhotoIds) {
                                        selectedPhotoIds - photo.id
                                    } else {
                                        selectedPhotoIds + photo.id
                                    }
                                    // Exit selection mode if no photos selected
                                    if (selectedPhotoIds.isEmpty()) {
                                        isSelectionMode = false
                                    }
                                } else {
                                    onNavigateToPhotoDetail(photo.id)
                                }
                            },
                            onLongClick = {
                                // Enter selection mode and select this photo
                                isSelectionMode = true
                                selectedPhotoIds = selectedPhotoIds + photo.id
                            }
                        )
                    }
                }
            }
        }
    }

    // Show feedback dialog when conditions are met
    if (showFeedbackDialog) {
        FeedbackDialog(
            onRateNow = {
                coroutineScope.launch {
                    AnalyticsHelper.logFeedbackAction("rate_now")
                    feedbackManager.markUserRated()
                }
                showFeedbackDialog = false
            },
            onMaybeLater = {
                coroutineScope.launch {
                    AnalyticsHelper.logFeedbackAction("maybe_later")
                    feedbackManager.markPromptDismissed()
                }
                showFeedbackDialog = false
            },
            onNoThanks = {
                coroutineScope.launch {
                    AnalyticsHelper.logFeedbackAction("no_thanks")
                    feedbackManager.markUserRated() // Treat as if they rated to not show again
                }
                showFeedbackDialog = false
            }
        )
    }

    // Show analytics consent dialog on first launch
    if (showAnalyticsConsentDialog) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss without choosing */ },
            title = { Text(stringResource(R.string.settings_analytics_consent_dialog_title)) },
            text = { Text(stringResource(R.string.settings_analytics_consent_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Dismiss dialog first to prevent re-triggering
                        showAnalyticsConsentDialog = false

                        coroutineScope.launch {
                            // Save preferences
                            preferencesManager.setAnalyticsEnabled(true)
                            preferencesManager.setAnalyticsConsentShown()

                            // Initialize analytics now that user has consented
                            AnalyticsHelper.initialize(context)
                            AnalyticsHelper.setAnalyticsEnabled(true)
                            AnalyticsHelper.logAppLaunched()
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_analytics_consent_accept))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Dismiss dialog first to prevent re-triggering
                        showAnalyticsConsentDialog = false

                        coroutineScope.launch {
                            // Save preferences
                            preferencesManager.setAnalyticsEnabled(false)
                            preferencesManager.setAnalyticsConsentShown()

                            // Ensure analytics is disabled
                            AnalyticsHelper.setAnalyticsEnabled(false)
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_analytics_consent_decline))
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.gallery_delete_confirm_title, selectedPhotoIds.size)) },
            text = { Text(stringResource(R.string.gallery_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            selectedPhotoIds.forEach { photoId ->
                                storageManager.deletePhoto(photoId)
                            }
                            AnalyticsHelper.logBatchDelete(selectedPhotoIds.size)
                            isSelectionMode = false
                            selectedPhotoIds = emptySet()
                            showDeleteConfirmDialog = false
                            loadPhotos()
                        }
                    }
                ) {
                    Text(stringResource(R.string.gallery_delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.gallery_cancel_button))
                }
            }
        )
    }

    // Save confirmation dialog
    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            title = { Text(stringResource(R.string.gallery_save_confirm_title, selectedPhotoIds.size)) },
            text = { Text(stringResource(R.string.gallery_save_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            var savedCount = 0
                            selectedPhotoIds.forEach { photoId ->
                                if (storageManager.saveToGallery(photoId)) {
                                    savedCount++
                                }
                            }
                            AnalyticsHelper.logBatchSave(savedCount)
                            isSelectionMode = false
                            selectedPhotoIds = emptySet()
                            showSaveConfirmDialog = false
                            loadPhotos()
                        }
                    }
                ) {
                    Text(stringResource(R.string.gallery_save_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirmDialog = false }) {
                    Text(stringResource(R.string.gallery_cancel_button))
                }
            }
        )
    }
}

// Helper function to share multiple photos
private fun shareSelectedPhotos(context: android.content.Context, photos: List<UtilityPhoto>) {
    if (photos.isEmpty()) return

    try {
        val uris = photos.mapNotNull { photo ->
            val file = File(photo.filePath)
            if (file.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else null
        }

        if (uris.isEmpty()) return

        val shareIntent = Intent().apply {
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share photos"))
        AnalyticsHelper.logBatchShare(photos.size)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun PhotoGridItem(
    photo: UtilityPhoto,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        val imagePath = photo.filePath
        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(File(imagePath))
                .crossfade(true)
                .allowHardware(false)
                .size(800, 800) // Higher quality for grid items
                .build()
        )

        Image(
            painter = painter,
            contentDescription = photo.description ?: "Utility photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Selection overlay
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else
                            Color.Transparent
                    )
            )

            // Checkmark
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(Color.White, shape = RoundedCornerShape(16.dp))
                        .padding(4.dp)
                )
            }
        }

        // Timer overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = photo.getFormattedTimeRemaining(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }

        // Description overlay if present
        photo.description?.let { desc ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 2
                )
            }
        }
    }
}
