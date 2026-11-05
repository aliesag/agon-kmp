package com.agon.app.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Bridge events coming from the JS player (`window.AndroidNativePlayer.*`). */
data class VideoBridgeEvents(
    val onGoBack: () -> Unit = {},
    val onToggleFullScreen: () -> Unit = {},
    val onOpenEpisodesList: () -> Unit = {},
    val onPlayNextEpisode: () -> Unit = {},
    val onDirectPlayerReady: () -> Unit = {},
    val onServerError: (String) -> Unit = { _ -> },
    val onSelectServerIndex: (Int) -> Unit = { _ -> },
    val onVideoCaptured: (String, String) -> Unit = { _, _ -> }
)

/**
 * Renders a WebView that:
 *  - loads [url] (page) or [html] (self-contained player),
 *  - injects headers [headers] (Referer/Origin/UA),
 *  - exposes window.AndroidNativePlayer.* → [events] via the platform bridge.
 */
@Composable
expect fun VideoPlayerHost(
    url: String?,
    html: String?,
    baseUrl: String?,
    headers: Map<String, String>,
    userAgent: String?,
    modifier: Modifier,
    events: VideoBridgeEvents,
    onWebViewReady: (Any?) -> Unit = {},
)
