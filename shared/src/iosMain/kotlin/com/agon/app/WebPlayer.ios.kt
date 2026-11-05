package com.agon.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WebPlayer(html: String?, url: String?, modifier: Modifier) {
    UIKitView(
        factory = {
            val cfg = WKWebViewConfiguration().apply {
                allowsInlineMediaPlayback = true
                mediaTypesRequiringUserActionForPlayback = 0u
            }
            WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = cfg)
        },
        modifier = modifier,
        update = { web ->
            when {
                html != null -> web.loadHTMLString(html, baseURL = url?.let { NSURL.URLWithString(it) })
                url != null -> NSURL.URLWithString(url)?.let { web.loadRequest(NSURLRequest.requestWithURL(it)) }
            }
        }
    )
}
