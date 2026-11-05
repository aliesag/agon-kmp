package com.agon.app.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VideoPlayerHost(
    url: String?,
    html: String?,
    baseUrl: String?,
    headers: Map<String, String>,
    userAgent: String?,
    modifier: Modifier,
    events: VideoBridgeEvents,
    onWebViewReady: (Any?) -> Unit
) { /* Android WebView bridge — wired in stage 6 */ }
