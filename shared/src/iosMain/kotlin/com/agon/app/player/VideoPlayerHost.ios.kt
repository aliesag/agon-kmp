package com.agon.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.setValue
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

private class MessageBridge(
    private val events: VideoBridgeEvents
) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        val body = didReceiveScriptMessage.body
        val name = didReceiveScriptMessage.name
        // JS sends: window.webkit.messageHandlers.bridge.postMessage({fn:"goBack", arg:...})
        val dict = body as? Map<*, *> ?: return
        val fn = dict["fn"] as? String ?: return
        val arg = dict["arg"]
        when (fn) {
            "goBack" -> events.onGoBack()
            "toggleFullScreen" -> events.onToggleFullScreen()
            "openEpisodesList" -> events.onOpenEpisodesList()
            "playNextEpisode" -> events.onPlayNextEpisode()
            "onDirectPlayerReady" -> events.onDirectPlayerReady()
            "onServerError" -> events.onServerError(arg as? String ?: "")
            "selectServerIndex" -> events.onSelectServerIndex((arg as? Number)?.toInt() ?: 0)
            "onVideoCaptured" -> {
                val pair = arg as? List<*> ?: return
                if (pair.size >= 2) events.onVideoCaptured(pair[0] as? String ?: "", pair[1] as? String ?: "")
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
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
) {
    val bridge = remember(events) { MessageBridge(events) }

    // Polyfill for Android's JavascriptInterface shape
    val injectScript = """
        (function(){
          if (window.AndroidNativePlayer) return;
          function send(fn, arg) {
            try { window.webkit.messageHandlers.bridge.postMessage({fn:fn, arg:arg}); } catch (e) {}
          }
          window.AndroidNativePlayer = {
            goBack: function(){ send('goBack'); },
            toggleFullScreen: function(){ send('toggleFullScreen'); },
            openEpisodesList: function(){ send('openEpisodesList'); },
            playNextEpisode: function(){ send('playNextEpisode'); },
            onDirectPlayerReady: function(){ send('onDirectPlayerReady'); },
            onServerError: function(msg){ send('onServerError', msg || ''); },
            selectServerIndex: function(i){ send('selectServerIndex', i); },
            onVideoCaptured: function(u,t){ send('onVideoCaptured', [u||'', t||'']); }
          };
        })();
    """.trimIndent()

    UIKitView(
        factory = {
            val cfg = WKWebViewConfiguration().apply {
                allowsInlineMediaPlayback = true
                mediaTypesRequiringUserActionForPlayback = 0u
                val controller = WKUserContentController()
                controller.addScriptMessageHandler(bridge, name = "bridge")
                controller.addUserScript(
                    WKUserScript(
                        source = injectScript,
                        injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                        forMainFrameOnly = true
                    )
                )
                userContentController = controller
            }
            val web = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = cfg)
            if (!userAgent.isNullOrBlank()) web.customUserAgent = userAgent

            when {
                html != null -> {
                    val base = baseUrl?.let { NSURL.URLWithString(it) }
                    web.loadHTMLString(html, baseURL = base)
                }
                url != null -> {
                    val req = NSMutableURLRequest.requestWithURL(NSURL.URLWithString(url)!!)
                    // Attach headers
                    headers.forEach { (k, v) -> req.setValue(v, forHTTPHeaderField = k) }
                    web.loadRequest(req)
                }
            }
            onWebViewReady(web)
            web
        },
        modifier = modifier,
        update = { _ -> }
    )

    DisposableEffect(bridge) {
        onDispose { }
    }
}
