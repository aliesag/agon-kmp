package com.agon.app

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun WebPlayer(html: String?, url: String?, modifier: Modifier) {
    AndroidView(modifier = modifier, factory = { c ->
        WebView(c).apply { settings.javaScriptEnabled = true; settings.mediaPlaybackRequiresUserGesture = false }
    }, update = { w ->
        if (html != null) w.loadDataWithBaseURL(url, html, "text/html", "utf-8", null) else if (url != null) w.loadUrl(url)
    })
}
