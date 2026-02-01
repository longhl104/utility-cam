package com.utility.cam.ui.camera

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.utility.cam.R
import com.utility.cam.analytics.AnalyticsHelper
import com.utility.cam.ui.ads.AdUnitIds
import com.utility.cam.ui.ads.BottomAdBanner
import com.utility.cam.ui.common.ProLockedDialog
import com.utility.cam.ui.common.rememberProUserState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    initialMode: String = "photo",
    onPhotoCapture: (File, String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPro: () -> Unit = {}
) {
    val actualIsProUser = rememberProUserState()

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
        // Audio permission will be requested only when switching to video mode
    }

    when {
        cameraPermissionState.status.isGranted -> {
            CameraPreviewScreen(
                initialMode = initialMode,
                onPhotoCapture = onPhotoCapture,
                onNavigateBack = onNavigateBack,
                onNavigateToPro = onNavigateToPro,
                isProUser = actualIsProUser
            )
        }

        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.camera_permission_required))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text(stringResource(R.string.camera_grant_permission))
                    }
                }
            }
        }
    }
}

@Suppress("unused")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPreviewScreen(
    initialMode: String = "photo",
    onPhotoCapture: (File, String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPro: () -> Unit,
    isProUser: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Audio permission state for requesting when switching to video
    val audioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    // Document scanner setup
    val documentScanner = remember {
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(false)
            .setPageLimit(1)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .build()
        GmsDocumentScanning.getClient(options)
    }

    // Document scanner result launcher
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pages?.let { pages ->
                // Get the first page's image URI and convert to File
                pages.firstOrNull()?.imageUri?.let { uri ->
                    try {
                        // Copy the scanned image to cache directory
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val scannedFile = File(
                            context.cacheDir,
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                                .format(System.currentTimeMillis()) + "_scan.jpg"
                        )
                        inputStream?.use { input ->
                            scannedFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        // Navigate to review screen with scanned document
                        onPhotoCapture(scannedFile, "photo")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    var captureMode by remember {
        mutableStateOf(
            if (initialMode == "video" && isProUser) CaptureMode.VIDEO else CaptureMode.PHOTO
        )
    }

    // Request audio permission when switching to video mode
    LaunchedEffect(captureMode) {
        if (captureMode == CaptureMode.VIDEO && !audioPermissionState.status.isGranted) {
            audioPermissionState.launchPermissionRequest()
        }
    }
    var showProLockedDialog by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableIntStateOf(0) }
    var activeRecording: Recording? by remember { mutableStateOf(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var isFlashOn by remember { mutableStateOf(false) }

    // Timer for recording duration
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0
            while (isRecording) {
                delay(1000)
                recordingTime++
            }
        } else {
            recordingTime = 0
        }
    }

    LaunchedEffect(lensFacing, captureMode) {
        val cameraProvider = context.getCameraProvider()
        val result = setupCamera(
            cameraProvider,
            previewView,
            lifecycleOwner,
            lensFacing,
            captureMode
        )
        imageCapture = result.first
        videoCapture = result.second
        camera = result.third
        zoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
    }

    // Control flash mode
    LaunchedEffect(isFlashOn, camera) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            onPreviewViewCreated = { previewView = it },
            onZoomChange = { scale ->
                camera?.let { cam ->
                    val zoomState = cam.cameraInfo.zoomState.value
                    val minZoom = zoomState?.minZoomRatio ?: 1f
                    val maxZoom = zoomState?.maxZoomRatio ?: 1f
                    val newZoom = (zoomRatio * scale).coerceIn(minZoom, maxZoom)
                    cam.cameraControl.setZoomRatio(newZoom)
                    zoomRatio = newZoom
                }
            },
            onFocus = { x, y ->
                previewView?.let { preview ->
                    camera?.let { cam ->
                        val factory = preview.meteringPointFactory
                        val point = factory.createPoint(x, y)
                        val action = FocusMeteringAction.Builder(point).build()
                        cam.cameraControl.startFocusAndMetering(action)
                    }
                }
            }
        )

        // Flash toggle button at top right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
        ) {
            IconButton(
                onClick = {
                    isFlashOn = !isFlashOn
                    AnalyticsHelper.logCameraFeatureUsed(if (isFlashOn) "flash_on" else "flash_off")
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = stringResource(
                        if (isFlashOn) R.string.camera_flash_on else R.string.camera_flash_off
                    ),
                    tint = if (isFlashOn) Color.Yellow else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Recording time indicator (visible only when recording)
        if (isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val minutes = recordingTime / 60
                val seconds = recordingTime % 60
                Text(
                    text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Zoom indicator (visible only when not recording)
        if (!isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            camera?.let { cam ->
                                cam.cameraControl.setZoomRatio(1f)
                                zoomRatio = 1f
                            }
                        }
                    }
            ) {
                Text(
                    text = String.format(Locale.US, "%.1fx", zoomRatio),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }


        // Camera controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                .padding(24.dp)
        ) {
            // Scan document button (Pro only, Photo mode only) - Above mode toggle
            if (captureMode == CaptureMode.PHOTO) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box {
                        IconButton(
                            onClick = {
                                if (isProUser) {
                                    AnalyticsHelper.logCameraFeatureUsed("document_scan")
                                    // Launch document scanner
                                    documentScanner.getStartScanIntent(context as Activity)
                                        .addOnSuccessListener { intentSender ->
                                            scannerLauncher.launch(
                                                IntentSenderRequest.Builder(intentSender).build()
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            e.printStackTrace()
                                        }
                                } else {
                                    AnalyticsHelper.logProFeatureAttempted("document_scan")
                                    showProLockedDialog = true
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            enabled = !isRecording && !isCapturing
                        ) {
                            Icon(
                                Icons.Default.DocumentScanner,
                                contentDescription = stringResource(R.string.camera_scan_document),
                                tint = Color.White
                            )
                        }

                        // PRO badge for normal users
                        if (!isProUser) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp, end = 2.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = { if (!isRecording) captureMode = CaptureMode.PHOTO },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = !isRecording,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (captureMode == CaptureMode.PHOTO)
                            Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = if (captureMode == CaptureMode.PHOTO)
                            Color.White.copy(alpha = 0.2f) else Color.Transparent,
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.camera_mode_photo))
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (!isRecording) {
                            if (isProUser) {
                                captureMode = CaptureMode.VIDEO
                            } else {
                                AnalyticsHelper.logProFeatureAttempted("video_capture")
                                showProLockedDialog = true
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = !isRecording,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (captureMode == CaptureMode.VIDEO)
                            Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = if (captureMode == CaptureMode.VIDEO)
                            Color.White.copy(alpha = 0.2f) else Color.Transparent,
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isProUser) Icons.Default.Videocam else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.camera_mode_video))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flip camera button
                IconButton(
                    onClick = {
                        if (!isRecording) {
                            AnalyticsHelper.logCameraFeatureUsed("camera_flip")
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    enabled = !isRecording
                ) {
                    Icon(
                        Icons.Default.FlipCameraAndroid,
                        contentDescription = stringResource(R.string.camera_flip),
                        tint = Color.White
                    )
                }

                // Capture button (Photo or Video)
                IconButton(
                    onClick = {
                        when (captureMode) {
                            CaptureMode.PHOTO -> {
                                if (!isCapturing) {
                                    isCapturing = true
                                    coroutineScope.launch {
                                        try {
                                            imageCapture?.let { capture ->
                                                val photoFile = takePicture(context, capture)
                                                if (photoFile != null) {
                                                    onPhotoCapture(photoFile, "photo")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            isCapturing = false
                                        }
                                    }
                                }
                            }

                            CaptureMode.VIDEO -> {
                                if (isRecording) {
                                    // Stop recording and track duration
                                    val duration = recordingTime * 1000L // Convert to milliseconds
                                    AnalyticsHelper.logVideoCaptured(duration)
                                    activeRecording?.stop()
                                    activeRecording = null
                                    isRecording = false
                                } else {
                                    // Start recording
                                    AnalyticsHelper.logCameraFeatureUsed("video_recording_started")
                                    coroutineScope.launch {
                                        try {
                                            videoCapture?.let { capture ->
                                                @Suppress("UnusedVariable") val videoFile =
                                                    startVideoRecording(
                                                        context,
                                                        capture,
                                                        audioPermissionState.status.isGranted,
                                                        onRecordingStarted = { recording ->
                                                            activeRecording = recording
                                                            isRecording = true
                                                        },
                                                        onRecordingStopped = { file ->
                                                            if (file != null) {
                                                                onPhotoCapture(file, "video")
                                                            }
                                                            isRecording = false
                                                        }
                                                    )
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            isRecording = false
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            if (isRecording) Color.Red else Color.White,
                            CircleShape
                        )
                        .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    enabled = !isCapturing
                ) {
                    Icon(
                        imageVector = when {
                            isRecording -> Icons.Default.FiberManualRecord
                            captureMode == CaptureMode.VIDEO -> Icons.Default.Videocam
                            else -> Icons.Default.CameraAlt
                        },
                        contentDescription = when {
                            isRecording -> stringResource(R.string.camera_stop_recording)
                            captureMode == CaptureMode.VIDEO -> stringResource(R.string.camera_start_recording)
                            else -> stringResource(R.string.camera_take_photo)
                        },
                        tint = if (isRecording) Color.White else Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Back button
                IconButton(
                    onClick = {
                        if (isRecording) {
                            activeRecording?.stop()
                            activeRecording = null
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.camera_close),
                        tint = Color.White
                    )
                }
            }

            // Ad banner at bottom (inside the controls column)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                BottomAdBanner(
                    isProUser = isProUser,
                    screenName = "Camera",
                    adUnitId = AdUnitIds.BANNER_CAMERA
                )
            }
        }
    }

    // Pro locked dialog
    if (showProLockedDialog) {
        ProLockedDialog(
            onDismiss = { showProLockedDialog = false },
            onUpgrade = onNavigateToPro
        )
    }
}


