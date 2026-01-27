package com.utility.cam.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Extension function to get CameraProvider
 */
suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener(
                { continuation.resume(future.get()) },
                ContextCompat.getMainExecutor(this)
            )
        }
    }

/**
 * Setup camera with given configuration
 */
fun setupCamera(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView?,
    lifecycleOwner: LifecycleOwner,
    lensFacing: Int,
    captureMode: CaptureMode
): Triple<ImageCapture?, VideoCapture<Recorder>?, Camera?> {
    return try {
        cameraProvider.unbindAll()

        val preview = Preview.Builder()
            .build().also {
                it.surfaceProvider = previewView?.surfaceProvider
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        val videoCapture = VideoCapture.withOutput(recorder)

        // Bind use cases based on capture mode
        val camera = if (captureMode == CaptureMode.PHOTO) {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } else {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
        }

        Triple(imageCapture, videoCapture, camera)
    } catch (e: Exception) {
        e.printStackTrace()
        Triple(null, null, null)
    }
}

/**
 * Take a picture using ImageCapture
 */
suspend fun takePicture(
    context: Context,
    imageCapture: ImageCapture
): File? = suspendCoroutine { continuation ->
    val photoFile = File(
        context.cacheDir,
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                continuation.resume(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                continuation.resume(null)
            }
        }
    )
}

/**
 * Start video recording
 */
@SuppressLint("MissingPermission")
suspend fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    hasAudioPermission: Boolean,
    onRecordingStarted: (Recording) -> Unit,
    onRecordingStopped: (File?) -> Unit
): Recording? = suspendCoroutine { continuation ->
    val videoFile = File(
        context.cacheDir,
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis()) + ".mp4"
    )

    val outputOptions = FileOutputOptions.Builder(videoFile).build()

    val pendingRecording = videoCapture.output
        .prepareRecording(context, outputOptions)

    // Enable audio only if permission is granted
    val recordingWithAudio = if (hasAudioPermission) {
        pendingRecording.withAudioEnabled()
    } else {
        pendingRecording
    }

    val recording = recordingWithAudio
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    // Recording started successfully
                }

                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) {
                        videoFile.delete()
                        onRecordingStopped(null)
                    } else {
                        onRecordingStopped(videoFile)
                    }
                }
            }
        }

    onRecordingStarted(recording)
    continuation.resume(recording)
}
