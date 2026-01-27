package com.utility.cam.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(
    onPreviewViewCreated: (PreviewView) -> Unit,
    onZoomChange: (Float) -> Unit,
    onFocus: (Float, Float) -> Unit
) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also { previewView ->
                onPreviewViewCreated(previewView)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onFocus(offset.x, offset.y)
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    onZoomChange(zoom)
                }
            }
    )
}
