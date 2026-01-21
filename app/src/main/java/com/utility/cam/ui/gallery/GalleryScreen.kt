package com.utility.cam.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.utility.cam.R
import com.utility.cam.analytics.AnalyticsHelper
import com.utility.cam.data.FeedbackManager
import com.utility.cam.data.PhotoEventBus
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.UtilityPhoto
import com.utility.cam.ui.feedback.FeedbackDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.res.stringResource

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var photos by remember { mutableStateOf<List<UtilityPhoto>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

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
            TopAppBar(
                title = { Text(stringResource(R.string.gallery_title)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.gallery_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCamera,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.gallery_take_photo))
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
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoGridItem(
                            photo = photo,
                            onClick = { onNavigateToPhotoDetail(photo.id) }
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
}

@Composable
fun PhotoGridItem(
    photo: UtilityPhoto,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
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
