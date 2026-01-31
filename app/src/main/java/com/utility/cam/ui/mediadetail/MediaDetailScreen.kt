package com.utility.cam.ui.mediadetail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.content.FileProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.utility.cam.R
import com.utility.cam.analytics.AnalyticsHelper
import com.utility.cam.data.FeedbackManager
import com.utility.cam.data.InAppReviewManager
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.UtilityMedia
import com.utility.cam.ui.ads.BottomAdBanner
import com.utility.cam.ui.ads.AdUnitIds
import com.utility.cam.ui.common.VideoPlayer
import com.utility.cam.ui.common.rememberProUserState
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    mediaId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val storageManager = remember { PhotoStorageManager(context) }
    val feedbackManager = remember { FeedbackManager(context) }
    val inAppReviewManager = remember { InAppReviewManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val actualIsProUser = rememberProUserState()

    var media by remember { mutableStateOf<UtilityMedia?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mediaId) {
        media = storageManager.getPhoto(mediaId)
        description = media?.description ?: ""

        // Track media detail view
        media?.let { currentMedia ->
            val mediaType = if (currentMedia.filePath.endsWith(".mp4", ignoreCase = true)) "video" else "photo"
            AnalyticsHelper.logMediaDetailViewed(mediaId, mediaType)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    media?.let { currentMedia ->
        // Detect if it's a video
        val isVideo = currentMedia.filePath.endsWith(".mp4", ignoreCase = true)
        val titleRes = if (isVideo) R.string.photo_detail_title_video else R.string.photo_detail_title

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(titleRes)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.media_detail_back))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                // Track analytics
                                AnalyticsHelper.logPhotoShared(currentMedia.id)

                                // Share photo/video using Android's share sheet
                                val mediaFile = File(currentMedia.filePath)
                                val mediaUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    mediaFile
                                )

                                val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = mimeType
                                    putExtra(Intent.EXTRA_STREAM, mediaUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    currentMedia.description?.let {
                                        putExtra(Intent.EXTRA_TEXT, it)
                                    }
                                }

                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.media_detail_share_title)
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.media_detail_share))
                        }
                        IconButton(
                            onClick = { showSaveDialog = true }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = stringResource(R.string.media_detail_save))
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.media_detail_delete))
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                BottomAdBanner(
                    isProUser = actualIsProUser,
                    screenName = "MediaDetail",
                    adUnitId = AdUnitIds.BANNER_MEDIA_DETAIL
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Media preview (Image or Video)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                ) {
                    if (isVideo) {
                        // Video player
                        VideoPlayer(
                            videoUri = currentMedia.filePath,
                            modifier = Modifier.fillMaxSize(),
                            autoPlay = false // Don't auto-play in detail view
                        )
                    } else {
                        // Image preview
                        val painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(File(currentMedia.filePath))
                                .crossfade(true)
                                .build()
                        )

                        Image(
                            painter = painter,
                            contentDescription = currentMedia.description ?: "Utility media",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Details
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Expiration info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.media_detail_expires_in, currentMedia.getFormattedTimeRemaining()),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                                    .format(Date(currentMedia.expirationTimestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Captured time
                    Text(
                        stringResource(R.string.media_detail_captured),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                            .format(Date(currentMedia.captureTimestamp)),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.media_detail_description),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showDescriptionDialog = true }) {
                            Text(stringResource(if (description.isEmpty()) R.string.media_detail_add else R.string.media_detail_edit))
                        }
                    }

                    if (description.isNotEmpty()) {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            stringResource(R.string.media_detail_no_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actions
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.media_detail_keep_forever))
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.media_detail_delete_title)) },
                text = { Text(stringResource(R.string.media_detail_delete_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                storageManager.deletePhoto(mediaId)
                                showDeleteDialog = false

                                onNavigateBack()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.media_detail_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.media_detail_cancel))
                    }
                }
            )
        }

        // Save confirmation dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text(stringResource(R.string.media_detail_save_title)) },
                text = { Text(stringResource(R.string.media_detail_save_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val success = storageManager.saveToGallery(mediaId)
                                showSaveDialog = false
                                if (success) {
                                    AnalyticsHelper.logMediaKeptForever(mediaId)
                                    snackbarMessage = context.getString(R.string.media_detail_saved_success)

                                    // Track saved photo for review trigger
                                    feedbackManager.incrementSavedPhotoCount(1)

                                    // Check if we should trigger in-app review
                                    val shouldTriggerReview = feedbackManager.shouldTriggerReviewAfterSave()
                                    if (shouldTriggerReview) {
                                        // Small delay to let the UI settle
                                        kotlinx.coroutines.delay(1000)

                                        val activity = context as? Activity
                                        if (activity != null) {
                                            Log.d("MediaDetailScreen", "Triggering in-app review after saving photo")

                                            inAppReviewManager.launchReviewFlow(
                                                activity = activity,
                                                onComplete = {
                                                    Log.d("MediaDetailScreen", "Review flow completed after save")
                                                    coroutineScope.launch {
                                                        feedbackManager.markUserRated()
                                                    }
                                                },
                                                onFallback = {
                                                    Log.d("MediaDetailScreen", "Review flow not available, skipping")
                                                    // Don't open Play Store automatically, just skip silently
                                                }
                                            )
                                        }
                                    }

                                    kotlinx.coroutines.delay(1000)
                                    onNavigateBack()
                                } else {
                                    snackbarMessage = context.getString(R.string.media_detail_save_failed)
                                }
                            }
                        }
                    ) {
                        Text(context.getString(R.string.media_detail_save_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text(context.getString(R.string.media_detail_cancel))
                    }
                }
            )
        }

        // Description dialog
        if (showDescriptionDialog) {
            var tempDescription by remember { mutableStateOf(description) }

            AlertDialog(
                onDismissRequest = { showDescriptionDialog = false },
                title = { Text(context.getString(R.string.media_detail_edit_description_title)) },
                text = {
                    OutlinedTextField(
                        value = tempDescription,
                        onValueChange = { tempDescription = it },
                        label = { Text(context.getString(R.string.media_detail_description)) },
                        placeholder = { Text(context.getString(R.string.media_detail_edit_description_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                storageManager.updateDescription(mediaId, tempDescription)
                                description = tempDescription
                                media = storageManager.getPhoto(mediaId)
                                showDescriptionDialog = false
                            }
                        }
                    ) {
                        Text(context.getString(R.string.media_detail_save_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDescriptionDialog = false }) {
                        Text(context.getString(R.string.media_detail_cancel))
                    }
                }
            )
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
