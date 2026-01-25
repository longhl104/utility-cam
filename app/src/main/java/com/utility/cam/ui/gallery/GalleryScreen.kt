package com.utility.cam.ui.gallery

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.utility.cam.R
import com.utility.cam.analytics.AnalyticsHelper
import com.utility.cam.data.FeedbackManager
import com.utility.cam.data.InAppReviewManager
import com.utility.cam.data.PhotoEventBus
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.PreferencesManager
import com.utility.cam.data.UtilityMedia
import com.utility.cam.ui.common.rememberProUserState
import com.utility.cam.ui.feedback.FeedbackDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

enum class GallerySortMode {
    BY_EXPIRATION,
    BY_CAPTURE_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMediaDetail: (String) -> Unit,
    onNavigateToBin: () -> Unit
) {
    val context = LocalContext.current
    val storageManager = remember { PhotoStorageManager(context) }
    val feedbackManager = remember { FeedbackManager(context) }
    val inAppReviewManager = remember { InAppReviewManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }
    val actualIsProUser = rememberProUserState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var photos by remember { mutableStateOf<List<UtilityMedia>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showAnalyticsConsentDialog by remember { mutableStateOf(false) }

    // Persist sort mode preference
    val sortModeString by preferencesManager.getGallerySortMode().collectAsState(initial = "BY_EXPIRATION")
    val sortMode = remember(sortModeString) {
        try {
            GallerySortMode.valueOf(sortModeString)
        } catch (_: Exception) {
            GallerySortMode.BY_EXPIRATION
        }
    }

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
                            verticalAlignment = Alignment.CenterVertically,
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
                        IconButton(onClick = onNavigateToBin) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.gallery_bin))
                        }
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
                    Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.gallery_take_photo))
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sort mode selector
                    if (!isSelectionMode) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            SegmentedButton(
                                selected = sortMode == GallerySortMode.BY_EXPIRATION,
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesManager.setGallerySortMode(GallerySortMode.BY_EXPIRATION.name)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text(stringResource(R.string.gallery_sort_by_expiration))
                            }
                            SegmentedButton(
                                selected = sortMode == GallerySortMode.BY_CAPTURE_TIME,
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesManager.setGallerySortMode(GallerySortMode.BY_CAPTURE_TIME.name)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text(stringResource(R.string.gallery_sort_by_capture))
                            }
                        }
                    }

                    // Grouped media display
                    val groupedMedia = when (sortMode) {
                        GallerySortMode.BY_EXPIRATION -> groupByExpiration(photos)
                        GallerySortMode.BY_CAPTURE_TIME -> groupByCapture(photos)
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    ) {
                        groupedMedia.forEach { (sectionTitle, sectionPhotos) ->
                            // Section header
                            item(key = "header_$sectionTitle", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = sectionTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            // Section items
                            items(sectionPhotos, key = { it.id }) { photo ->
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
                                            onNavigateToMediaDetail(photo.id)
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

                            // Track saved photos for review trigger
                            if (savedCount > 0) {
                                feedbackManager.incrementSavedPhotoCount(savedCount)

                                // Check if we should trigger in-app review
                                val shouldTriggerReview = feedbackManager.shouldTriggerReviewAfterSave()
                                if (shouldTriggerReview) {
                                    // Small delay to let the UI settle
                                    delay(1000)

                                    val activity = context as? Activity
                                    if (activity != null) {

                                        inAppReviewManager.launchReviewFlow(
                                            activity = activity,
                                            onComplete = {
                                                coroutineScope.launch {
                                                    feedbackManager.markUserRated()
                                                }
                                            },
                                            onFallback = {
                                                // Don't open Play Store automatically, just skip silently
                                            }
                                        )
                                    }
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

// Helper function to share multiple photos and videos
private fun shareSelectedPhotos(context: android.content.Context, photos: List<UtilityMedia>) {
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

        // Determine if we have mixed media types
        val hasVideos = photos.any { it.filePath.endsWith(".mp4", ignoreCase = true) }
        val hasImages = photos.any { !it.filePath.endsWith(".mp4", ignoreCase = true) }
        val mimeType = when {
            hasVideos && hasImages -> "*/*" // Mixed media
            hasVideos -> "video/*"
            else -> "image/*"
        }

        val shareIntent = Intent().apply {
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share media"))
        AnalyticsHelper.logBatchShare(photos.size)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun PhotoGridItem(
    photo: UtilityMedia,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Calculate urgency level based on time remaining
    val timeRemaining = photo.getTimeRemaining()
    val oneHour = TimeUnit.HOURS.toMillis(1)
    val oneDay = TimeUnit.DAYS.toMillis(1)

    val isUrgent = timeRemaining <= oneHour // Expiring within 1 hour
    val isWarning = timeRemaining in (oneHour + 1)..oneDay // Expiring within 1 day

    // Choose colors based on urgency
    val borderColor = when {
        isUrgent -> MaterialTheme.colorScheme.error
        isWarning -> Color(0xFFFFA726) // Orange for warning
        else -> Color.Transparent
    }

    val borderWidth = when {
        isUrgent -> 4.dp
        isWarning -> 3.dp
        else -> 0.dp
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        // Use full image for photos (Coil resizes efficiently), thumbnail for videos
        val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)
        val imagePath = if (isVideo) {
            // Use thumbnail for videos to avoid loading large video files
            photo.thumbnailPath ?: photo.filePath
        } else {
            // Use full image for photos to avoid grainy thumbnails
            photo.filePath
        }

        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(File(imagePath))
                .crossfade(true)
                .allowHardware(false)
                .size(800, 800) // Coil will resize efficiently
                .build()
        )

        Image(
            painter = painter,
            contentDescription = photo.description ?: "Utility photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Video indicator overlay
        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

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

        // Urgent/Warning badge
        if (isUrgent || isWarning) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        color = when {
                            isUrgent -> MaterialTheme.colorScheme.error
                            else -> Color(0xFFFFA726)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when {
                        isUrgent -> "⚠️ URGENT"
                        else -> "⏰ SOON"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }

        // Timer overlay with enhanced styling for urgent/warning items
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(
                    when {
                        isUrgent -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        isWarning -> Color(0xFFFFA726).copy(alpha = 0.9f)
                        else -> Color.Black.copy(alpha = 0.7f)
                    }
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = photo.getFormattedTimeRemaining(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = if (isUrgent || isWarning) {
                    androidx.compose.ui.text.font.FontWeight.Bold
                } else {
                    androidx.compose.ui.text.font.FontWeight.Normal
                }
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

/**
 * Group photos by expiration time sections
 */
private fun groupByExpiration(photos: List<UtilityMedia>): List<Pair<String, List<UtilityMedia>>> {
    val now = System.currentTimeMillis()
    val sections = mutableListOf<Pair<String, List<UtilityMedia>>>()

    // Define time ranges (in milliseconds)
    val oneHour = TimeUnit.HOURS.toMillis(1)
    val threeHours = TimeUnit.HOURS.toMillis(3)
    val sixHours = TimeUnit.HOURS.toMillis(6)
    val twelveHours = TimeUnit.HOURS.toMillis(12)
    val oneDay = TimeUnit.DAYS.toMillis(1)
    val threeDays = TimeUnit.DAYS.toMillis(3)
    val oneWeek = TimeUnit.DAYS.toMillis(7)

    // Group photos by time until expiration
    val expiringInOneHour = mutableListOf<UtilityMedia>()
    val expiringInThreeHours = mutableListOf<UtilityMedia>()
    val expiringInSixHours = mutableListOf<UtilityMedia>()
    val expiringInTwelveHours = mutableListOf<UtilityMedia>()
    val expiringInOneDay = mutableListOf<UtilityMedia>()
    val expiringInThreeDays = mutableListOf<UtilityMedia>()
    val expiringInOneWeek = mutableListOf<UtilityMedia>()
    val expiringLater = mutableListOf<UtilityMedia>()

    photos.forEach { photo ->
        val timeUntilExpiration = photo.expirationTimestamp - now

        when {
            timeUntilExpiration <= oneHour -> expiringInOneHour.add(photo)
            timeUntilExpiration <= threeHours -> expiringInThreeHours.add(photo)
            timeUntilExpiration <= sixHours -> expiringInSixHours.add(photo)
            timeUntilExpiration <= twelveHours -> expiringInTwelveHours.add(photo)
            timeUntilExpiration <= oneDay -> expiringInOneDay.add(photo)
            timeUntilExpiration <= threeDays -> expiringInThreeDays.add(photo)
            timeUntilExpiration <= oneWeek -> expiringInOneWeek.add(photo)
            else -> expiringLater.add(photo)
        }
    }

    // Add sections in order (most urgent first)
    if (expiringInOneHour.isNotEmpty()) {
        sections.add("Expiring within 1 hour" to expiringInOneHour.sortedBy { it.expirationTimestamp })
    }
    if (expiringInThreeHours.isNotEmpty()) {
        sections.add("Expiring within 3 hours" to expiringInThreeHours.sortedBy { it.expirationTimestamp })
    }
    if (expiringInSixHours.isNotEmpty()) {
        sections.add("Expiring within 6 hours" to expiringInSixHours.sortedBy { it.expirationTimestamp })
    }
    if (expiringInTwelveHours.isNotEmpty()) {
        sections.add("Expiring within 12 hours" to expiringInTwelveHours.sortedBy { it.expirationTimestamp })
    }
    if (expiringInOneDay.isNotEmpty()) {
        sections.add("Expiring within 1 day" to expiringInOneDay.sortedBy { it.expirationTimestamp })
    }
    if (expiringInThreeDays.isNotEmpty()) {
        sections.add("Expiring within 3 days" to expiringInThreeDays.sortedBy { it.expirationTimestamp })
    }
    if (expiringInOneWeek.isNotEmpty()) {
        sections.add("Expiring within 1 week" to expiringInOneWeek.sortedBy { it.expirationTimestamp })
    }
    if (expiringLater.isNotEmpty()) {
        sections.add("Expiring later" to expiringLater.sortedBy { it.expirationTimestamp })
    }

    return sections
}

/**
 * Group photos by capture time sections
 */
private fun groupByCapture(photos: List<UtilityMedia>): List<Pair<String, List<UtilityMedia>>> {
    val now = LocalDateTime.now()
    val sections = mutableListOf<Pair<String, List<UtilityMedia>>>()

    // Group photos by capture date
    val today = mutableListOf<UtilityMedia>()
    val yesterday = mutableListOf<UtilityMedia>()
    val thisWeek = mutableListOf<UtilityMedia>()
    val lastWeek = mutableListOf<UtilityMedia>()
    val thisMonth = mutableListOf<UtilityMedia>()
    val older = mutableListOf<UtilityMedia>()

    photos.forEach { photo ->
        val captureDate = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(photo.captureTimestamp),
            ZoneId.systemDefault()
        )

        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(captureDate.toLocalDate(), now.toLocalDate())

        when (daysDiff) {
            0L -> today.add(photo)
            1L -> yesterday.add(photo)
            in 2..6 -> thisWeek.add(photo)
            in 7..13 -> lastWeek.add(photo)
            in 14..30 -> thisMonth.add(photo)
            else -> older.add(photo)
        }
    }

    // Add sections in order (most recent first)
    if (today.isNotEmpty()) {
        sections.add("Today" to today.sortedByDescending { it.captureTimestamp })
    }
    if (yesterday.isNotEmpty()) {
        sections.add("Yesterday" to yesterday.sortedByDescending { it.captureTimestamp })
    }
    if (thisWeek.isNotEmpty()) {
        sections.add("This week" to thisWeek.sortedByDescending { it.captureTimestamp })
    }
    if (lastWeek.isNotEmpty()) {
        sections.add("Last week" to lastWeek.sortedByDescending { it.captureTimestamp })
    }
    if (thisMonth.isNotEmpty()) {
        sections.add("This month" to thisMonth.sortedByDescending { it.captureTimestamp })
    }
    if (older.isNotEmpty()) {
        sections.add("Older" to older.sortedByDescending { it.captureTimestamp })
    }

    return sections
}

