package com.agon.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.agon.app.data.ads.AdManager
import com.agon.app.data.matches.MatchItem
import com.agon.app.data.matches.MatchServer
import com.agon.app.data.matches.MatchStatus
import com.agon.app.ui.theme.Gold
import com.agon.app.ui.theme.NetflixBlack
import com.agon.app.ui.theme.NetflixRed
import com.agon.app.ui.theme.NetflixSurface
import com.agon.app.ui.theme.NetflixSurfaceVariant
import com.agon.app.viewmodel.MatchesViewModel

private fun toggleSystemBars(activity: Activity?, hide: Boolean) {
    val window = activity?.window ?: return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        val controller = window.insetsController
        if (hide) {
            controller?.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
            controller?.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller?.show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
        }
    } else {
        @Suppress("DEPRECATION")
        if (hide) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MatchPlayerScreen(
    matchId: String,
    vm: MatchesViewModel,
    onBack: () -> Unit,
    onOpenOtherMatch: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val allMatches by vm.matches.collectAsStateWithLifecycle()

    val match = remember(matchId, allMatches) {
        vm.getMatchById(matchId) ?: allMatches.firstOrNull { it.id == matchId }
    }

    var isLandscape by remember { mutableStateOf(false) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var currentServerIndex by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isStreamLoading by remember { mutableStateOf(true) }

    // Match player is completely ad-free as requested by user

    val exitFullscreenMode = {
        try {
            customViewCallback?.onCustomViewHidden()
        } catch (e: Exception) {}
        customFullscreenView = null
        customViewCallback = null
        isLandscape = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        toggleSystemBars(activity, false)
    }

    // Keep screen on during live match & restore portrait on dispose
    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            toggleSystemBars(activity, false)
            try {
                customViewCallback?.onCustomViewHidden()
            } catch (e: Exception) {}
            customFullscreenView = null
            customViewCallback = null
        }
    }

    val handleBack = {
        if (customFullscreenView != null || isLandscape) {
            exitFullscreenMode()
        } else {
            onBack()
        }
    }

    BackHandler {
        handleBack()
    }

    // If native WebChromeClient custom view is active, render it full screen
    if (customFullscreenView != null) {
        BackHandler {
            handleBack()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { _ ->
                    (customFullscreenView?.parent as? ViewGroup)?.removeView(customFullscreenView)
                    customFullscreenView!!
                }
            )
        }
        return
    }

    if (match == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NetflixBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = NetflixRed)
                Spacer(modifier = Modifier.height(12.dp))
                Text("جاري تحميل بيانات المباراة...", color = Color.White)
            }
        }
        return
    }

    val servers = remember(match) {
        val baseEmbed = match.embedUrl.ifEmpty { "https://mm.yallagoo.xyz/albaplayer/sport-2/" }
        val cleanBase = if (baseEmbed.contains("?")) baseEmbed.substringBefore("?") else baseEmbed
        val baseWithSlash = if (cleanBase.endsWith("/")) cleanBase else "$cleanBase/"

        listOf(
            MatchServer("s1", "سيرفر 1", "${baseWithSlash}?serv=1", "FHD • جودة عالية"),
            MatchServer("s2", "سيرفر 2", "${baseWithSlash}?serv=2", "HD • جودة سريعة"),
            MatchServer("s3", "سيرفر 3", "${baseWithSlash}?serv=3", "SD • بدون تقطيع"),
            MatchServer("s4", "سيرفر 4", "${baseWithSlash}?serv=4", "Multi • جودة متعددة")
        )
    }

    val activeUrl = remember(currentServerIndex, servers) {
        servers.getOrNull(currentServerIndex)?.embedUrl ?: "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // --- Live Stream Player Section ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isLandscape) Modifier.fillMaxSize()
                    else Modifier.aspectRatio(16f / 9f)
                )
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.setSupportMultipleWindows(false)
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                        // Native Javascript Interface to allow Web player fullscreen button to toggle landscape
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun requestFullscreen() {
                                activity?.runOnUiThread {
                                    isLandscape = true
                                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    toggleSystemBars(activity, true)
                                }
                            }

                            @JavascriptInterface
                            fun exitFullscreen() {
                                activity?.runOnUiThread {
                                    exitFullscreenMode()
                                }
                            }
                        }, "AndroidPlayer")

                        fun injectAdBlocker(view: WebView?) {
                            val adBlockJs = """
                                (function() {
                                    try {
                                        window.open = function() { return null; };

                                        // 1. Lock document visibility to prevent players from pausing on blur/focus loss
                                        try {
                                            Object.defineProperty(document, 'hidden', { get: function() { return false; } });
                                            Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; } });
                                        } catch(e) {}

                                        window.addEventListener('blur', function(e) { e.stopImmediatePropagation(); }, true);
                                        document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);

                                        // 2. Prevent video element from pausing automatically
                                        try {
                                            if (!window.__pausePatched) {
                                                window.__pausePatched = true;
                                                var origPause = HTMLVideoElement.prototype.pause;
                                                HTMLVideoElement.prototype.pause = function() {
                                                    if (window.__userExplicitPause) {
                                                        return origPause.apply(this, arguments);
                                                    }
                                                    return Promise.resolve();
                                                };
                                            }
                                        } catch(e) {}

                                        function sanitizePlayerAndOverlays() {
                                            // A. Destroy Telegram notification box specifically
                                            var aplrNotic = document.getElementById('aplr-notic');
                                            if (aplrNotic) {
                                                try { aplrNotic.remove(); } catch(e) {}
                                            }

                                            var refreshBtn = document.getElementById('refresh');
                                            if (refreshBtn) {
                                                try { refreshBtn.remove(); } catch(e) {}
                                            }

                                            var embedModal = document.getElementById('embedModal');
                                            if (embedModal) {
                                                try { embedModal.remove(); } catch(e) {}
                                            }

                                            // B. Remove in-player server tabs and bad classes
                                            var badClasses = [
                                                'aplr-menu', 'aplr-exbtns', 'notic-telegram', 'aplr-notic',
                                                'aplr-notic-inner', 'aplr-notic-icon', 'albaplayer_name',
                                                'embed-btn', 'embed-modal'
                                            ];
                                            for (var c = 0; c < badClasses.length; c++) {
                                                var els = document.getElementsByClassName(badClasses[c]);
                                                while (els.length > 0) {
                                                    try { els[0].parentNode.removeChild(els[0]); } catch(e) { break; }
                                                }
                                            }

                                            // C. Destroy Telegram & Betting popups
                                            var badPhrases = [
                                                'top betting channel', 'football betting predictions', 'betting predictions',
                                                'join over', 'free predictions', 'success rate', 'join now', 'earning daily',
                                                'تليجرام', 'تلجرام', 'telegram', 'مراهنات', 'قناة المراهنات',
                                                'اشترك في موقعنا', 'انضم الآن', 'حمل تطبيقنا', 'انقر للاشتراك', 'yalashotss'
                                            ];

                                            var allEls = document.querySelectorAll('div, section, article, p, span, a, button, dialog');
                                            for (var i = 0; i < allEls.length; i++) {
                                                var el = allEls[i];
                                                var txt = (el.innerText || el.textContent || '').toLowerCase().trim();
                                                for (var k = 0; k < badPhrases.length; k++) {
                                                    if (txt.indexOf(badPhrases[k]) !== -1 && txt.length < 500) {
                                                        var cur = el;
                                                        while (cur.parentElement && cur.parentElement !== document.body && cur.parentElement.tagName !== 'HTML') {
                                                            var st = window.getComputedStyle(cur.parentElement);
                                                            if (st.position === 'fixed' || st.position === 'absolute' || parseInt(st.zIndex || '0') > 15) {
                                                                cur = cur.parentElement;
                                                            } else {
                                                                break;
                                                            }
                                                        }
                                                        cur.style.setProperty('display', 'none', 'important');
                                                        cur.style.setProperty('visibility', 'hidden', 'important');
                                                        cur.style.setProperty('pointer-events', 'none', 'important');
                                                        try { cur.remove(); } catch(e) {}
                                                        break;
                                                    }
                                                }
                                            }

                                            // D. Remove in-player server tabs if text matches
                                            for (var j = 0; j < allEls.length; j++) {
                                                var sEl = allEls[j];
                                                var sTxt = (sEl.innerText || '').trim();
                                                if ((sTxt.includes('البث الرئيسي') && (sTxt.includes('بث 2') || sTxt.includes('تحديث'))) ||
                                                    (sTxt.includes('سيرفر') && sTxt.includes('جودة')) ||
                                                    (sTxt.startsWith('بث 1') && sTxt.includes('بث 2'))) {
                                                    sEl.style.setProperty('display', 'none', 'important');
                                                    sEl.style.setProperty('visibility', 'hidden', 'important');
                                                    sEl.style.setProperty('height', '0px', 'important');
                                                    try { sEl.remove(); } catch(e) {}
                                                }
                                            }

                                            // E. Remove Telegram links
                                            var links = document.querySelectorAll('a[href*="t.me"], a[href*="telegram"], a[href*="tg:"], img[src*="telegram"]');
                                            for (var l = 0; l < links.length; l++) {
                                                var lnk = links[l];
                                                var parentBox = lnk.closest('div') || lnk;
                                                parentBox.style.setProperty('display', 'none', 'important');
                                                try { parentBox.remove(); } catch(e) {}
                                            }

                                            // F. Hook web player fullscreen controls to trigger native landscape mode
                                            var fsButtons = document.querySelectorAll('button[class*="fullscreen"], [class*="vjs-fullscreen-control"], [aria-label*="fullscreen"], [title*="ملء الشاشة"], [title*="Full screen"], [title*="fullscreen"], .jw-icon-fullscreen, svg[class*="fullscreen"]');
                                            for (var f = 0; f < fsButtons.length; f++) {
                                                var fsBtn = fsButtons[f];
                                                if (!fsBtn.__hasFsListener) {
                                                    fsBtn.__hasFsListener = true;
                                                    fsBtn.addEventListener('click', function() {
                                                        try {
                                                            if (window.AndroidPlayer) {
                                                                if (document.fullscreenElement || document.webkitFullscreenElement) {
                                                                    window.AndroidPlayer.exitFullscreen();
                                                                } else {
                                                                    window.AndroidPlayer.requestFullscreen();
                                                                }
                                                            }
                                                        } catch(e) {}
                                                    });
                                                }
                                            }

                                            // G. Keep video playing and unpaused
                                            var vids = document.querySelectorAll('video');
                                            for (var v = 0; v < vids.length; v++) {
                                                var vd = vids[v];
                                                if (vd.paused && !window.__userExplicitPause && vd.readyState >= 2) {
                                                    vd.play().catch(function(){});
                                                }
                                            }
                                        }

                                        sanitizePlayerAndOverlays();

                                        if (!window.__cleanerActive) {
                                            window.__cleanerActive = true;
                                            setInterval(sanitizePlayerAndOverlays, 250);

                                            var obs = new MutationObserver(sanitizePlayerAndOverlays);
                                            obs.observe(document.documentElement, { childList: true, subtree: true });

                                            var css = document.createElement('style');
                                            css.id = 'cleaner-style';
                                            css.textContent = `
                                                #aplr-notic, .notic-telegram, .aplr-notic, .aplr-notic-inner, .aplr-notic-icon,
                                                .aplr-menu, .aplr-exbtns, #refresh, .albaplayer_name, .embed-btn, .embed-modal,
                                                .aplr-top, .aplr-head, .aplr-header, .alba-header, #channels, .channels, .aplr-channels,
                                                .aplr-nav, .aplr-menu-wrapper, .aplr-top-bar, .alba-top,
                                                [href*="t.me"], [href*="telegram"],
                                                [class*="telegram"], [id*="telegram"],
                                                [class*="betting"], [id*="betting"],
                                                [class*="prediction"], [id*="prediction"],
                                                .servers, #servers, .server-list, .server_btns, .ch-servers,
                                                .player-servers, .head-servers, .top-servers, .alba-servers,
                                                .page-title, .post-title, .entry-meta, header, nav, footer, .sidebar,
                                                div[style*="z-index: 2147483647"],
                                                div[style*="z-index:99999"],
                                                div[style*="z-index: 99999"] {
                                                    display: none !important;
                                                    visibility: hidden !important;
                                                    opacity: 0 !important;
                                                    height: 0px !important;
                                                    max-height: 0px !important;
                                                    width: 0px !important;
                                                    pointer-events: none !important;
                                                    position: absolute !important;
                                                    left: -9999px !important;
                                                    z-index: -9999 !important;
                                                }

                                                html, body {
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    width: 100vw !important;
                                                    height: 100vh !important;
                                                    overflow: hidden !important;
                                                    background: #000 !important;
                                                }

                                                .aplr-player-wrapper, .aplr-player-content, .video-con, .embed-responsive,
                                                .player-container, #rmpPlayer, .rmp-container, .rmp-video {
                                                    position: fixed !important;
                                                    top: 0 !important;
                                                    left: 0 !important;
                                                    right: 0 !important;
                                                    bottom: 0 !important;
                                                    width: 100% !important;
                                                    height: 100% !important;
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    background: #000 !important;
                                                    z-index: 10 !important;
                                                }

                                                .video-con iframe, .embed-responsive iframe {
                                                    position: absolute !important;
                                                    top: 0 !important;
                                                    left: 0 !important;
                                                    width: 100% !important;
                                                    height: 100% !important;
                                                    border: none !important;
                                                }
                                            `;
                                            document.documentElement.appendChild(css);
                                        }
                                    } catch(e) {}
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(adBlockJs, null)
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                injectAdBlocker(view)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isStreamLoading = false
                                injectAdBlocker(view)
                            }

                            override fun onLoadResource(view: WebView?, url: String?) {
                                super.onLoadResource(view, url)
                                injectAdBlocker(view)
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val reqUrl = request?.url?.toString()?.lowercase() ?: ""

                                // CRITICAL: NEVER block video stream manifests, playlists, fragments, or video players
                                val isMediaStream = reqUrl.contains(".m3u8") || reqUrl.contains(".ts") ||
                                        reqUrl.contains(".key") || reqUrl.contains(".mp4") ||
                                        reqUrl.contains("/hls/") || reqUrl.contains("stream") ||
                                        reqUrl.contains("albaplayer") || reqUrl.contains("yallagoo") ||
                                        reqUrl.contains("yalla.moda") || reqUrl.contains("yasirtv") ||
                                        reqUrl.contains("devcrafer") || reqUrl.contains("radiantmedia") ||
                                        reqUrl.contains("rmp") || reqUrl.contains("kooora")

                                if (isMediaStream) {
                                    return super.shouldInterceptRequest(view, request)
                                }

                                // Block only third-party popup ad and tracker networks
                                val adDomains = listOf(
                                    "adsterra.com", "popads.net", "monetag.com", "adtrue.com",
                                    "histats.com", "googletagmanager.com", "google-analytics.com",
                                    "doubleclick.net", "popcash.net", "propellerads.com", "hilltopads.com",
                                    "t.me", "telegram.org"
                                )

                                if (adDomains.any { reqUrl.contains(it) }) {
                                    return WebResourceResponse("application/javascript", "UTF-8", ByteArrayInputStream("".toByteArray()))
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: ""
                                val isMain = request?.isForMainFrame ?: true

                                // Completely block telegram and app store/deep link redirects
                                if (reqUrl.contains("t.me") || reqUrl.contains("telegram") || reqUrl.startsWith("tg:") || reqUrl.startsWith("intent:")) {
                                    return true
                                }

                                // Always allow iframes and subframes to load their video players
                                if (!isMain) {
                                    return false
                                }

                                // Only allow sports streaming domains on the main frame
                                val isAllowed = reqUrl.contains("yallagoo") ||
                                        reqUrl.contains("yalla.moda") ||
                                        reqUrl.contains("albaplayer") ||
                                        reqUrl.contains("yasirtv") ||
                                        reqUrl.contains("devcrafer")

                                return !isAllowed
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                if (newProgress >= 40) {
                                    injectAdBlocker(view)
                                }
                                if (newProgress >= 80) isStreamLoading = false
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                super.onShowCustomView(view, callback)
                                customFullscreenView = view
                                customViewCallback = callback
                                isLandscape = true
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                toggleSystemBars(activity, true)
                            }

                            override fun onHideCustomView() {
                                super.onHideCustomView()
                                exitFullscreenMode()
                            }
                        }

                        loadUrl(activeUrl)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    if (webView.url != activeUrl) {
                        isStreamLoading = true
                        webView.loadUrl(activeUrl)
                    }
                }
            )

            // Loading indicator overlay
            if (isStreamLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = NetflixRed,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "جاري تحميل البث المباشر...",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Top Player Overlay (Controls & Title)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "${match.team1Name} ضد ${match.team2Name}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NetflixRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "بث حي ومباشر",
                                color = NetflixRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Refresh stream button
                    IconButton(
                        onClick = {
                            isStreamLoading = true
                            webViewRef?.reload()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "إعادة تحميل",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // If fullscreen landscape, hide the details section
        if (isLandscape) return

        val otherMatches = remember(allMatches, matchId) {
            allMatches.filter { it.id != matchId }
        }

        // --- Details Section (Scoreboard, Servers, Match Info) ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Server Switcher Row (2x2 Grid for full visibility)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سيرفرات البث المباشر (${servers.size} سيرفرات):",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اختر السيرفر الأنسب لسرعتك",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val chunkedServers = remember(servers) { servers.chunked(2) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunkedServers.forEachIndexed { rowIndex, rowServers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowServers.forEachIndexed { colIndex, server ->
                                    val index = rowIndex * 2 + colIndex
                                    val isSelected = currentServerIndex == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) NetflixRed else NetflixSurfaceVariant)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) NetflixRed else Color(0xFF333340),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                if (currentServerIndex != index) {
                                                    currentServerIndex = index
                                                    isStreamLoading = true
                                                    webViewRef?.loadUrl(server.embedUrl)
                                                }
                                            }
                                            .padding(vertical = 10.dp, horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color.White else NetflixRed)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = server.name,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = server.qualityBadge,
                                                color = if (isSelected) Gold else Color(0xFFB0B3C0),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                if (rowServers.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Scoreboard Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NetflixSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = match.tournament,
                                color = Gold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val statusColor = Color(match.status.colorHex)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = match.status.label,
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Teams and Large Score
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Team 1
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = match.team1Logo,
                                    contentDescription = match.team1Name,
                                    modifier = Modifier.size(52.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = match.team1Name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Score / VS
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                if (match.status == MatchStatus.NOT_STARTED) {
                                    Text(
                                        text = match.matchTime,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "بتوقيت مكة",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = match.score1,
                                            color = Color.White,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = " - ",
                                            color = NetflixRed,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = match.score2,
                                            color = Color.White,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                if (match.liveMinute != null) {
                                    Text(
                                        text = match.liveMinute,
                                        color = NetflixRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Team 2
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = match.team2Logo,
                                    contentDescription = match.team2Name,
                                    modifier = Modifier.size(52.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = match.team2Name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFF2B2B36), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Match Metadata Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            InfoBadge(icon = "📺", title = "القناة", value = match.channel)
                            InfoBadge(icon = "🎤", title = "المعلق", value = match.commentator)
                            InfoBadge(icon = "⏰", title = "الموعد", value = match.matchTime)
                        }
                    }
                }
            }

            // Quick Actions Bar (Share)
            item {
                Button(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "شاهد مباراة ${match.team1Name} ضد ${match.team2Name} بث مباشر عبر تطبيق حكايتنا! ⚽")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة المباراة"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetflixSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة رابط المباراة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Other matches
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "مباريات أخرى قد تهمك ⚽",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(otherMatches.take(4), key = { it.id }) { other ->
                MatchCard(
                    match = other,
                    onClick = { onOpenOtherMatch(other.id) }
                )
            }
        }
    }
}

@Composable
private fun InfoBadge(icon: String, title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(title, color = Color.Gray, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
