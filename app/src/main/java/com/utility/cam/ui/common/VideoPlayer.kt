package com.utility.cam.ui.common

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi

/**
 * Reusable video player component using ExoPlayer
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUri: String,
    modifier: Modifier = Modifier,
    repeatMode: Int = Player.REPEAT_MODE_ALL,
    autoPlay: Boolean = true,
    showControls: Boolean = true,
    keepScreenOn: Boolean = true
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // Create and configure ExoPlayer
    DisposableEffect(videoUri) {
        val player = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            this.repeatMode = repeatMode
            prepare()
            playWhenReady = autoPlay
        }
        exoPlayer = player

        onDispose {
            player.release()
            exoPlayer = null
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = showControls
                this.keepScreenOn = keepScreenOn
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
        },
        modifier = modifier
    )
}
