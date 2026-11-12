package com.mahmoudrateb.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun PlayerScreen(vm: MainViewModel, channel: Channel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var url by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val exo = remember { ExoPlayer.Builder(ctx).build() }

    DisposableEffect(Unit) {
        val l = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                err = error.errorCodeName
            }
        }
        exo.addListener(l)
        onDispose {
            exo.removeListener(l)
            exo.release()
        }
    }
    LaunchedEffect(channel) {
        try {
            url = vm.resolve(channel)
        } catch (e: Exception) {
            err = e.message
        }
    }
    LaunchedEffect(url) {
        url?.let {
            exo.setMediaItem(MediaItem.fromUri(it))
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { c -> PlayerView(c).apply { player = exo; useController = true } },
            modifier = Modifier.fillMaxSize()
        )
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
        }
        err?.let {
            Text(it, color = Color(0xFFFF8A80), modifier = Modifier.align(Alignment.Center))
        }
    }
}
