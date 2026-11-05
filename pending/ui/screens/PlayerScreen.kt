package com.agon.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import com.agon.app.data.ads.AdManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.agon.app.ui.tvClickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.agon.app.data.Catalog
import com.agon.app.data.Show
import com.agon.app.data.Episode
import com.agon.app.data.VideoServer
import com.agon.app.ui.components.PosterArt
import com.agon.app.ui.tvClickable
import coil.compose.AsyncImage
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import com.agon.app.ui.theme.NetflixRed
import com.agon.app.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
private var hasShownTvNoticeGlobally = false

@Composable
fun PlayerScreen(
    showId: String,
    episodeId: String? = null,
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenShow: (String) -> Unit
) {
    val shows by vm.shows.collectAsStateWithLifecycle()
    val show = shows.firstOrNull { it.id == showId } ?: Catalog.byId(showId) ?: return
    val context = LocalContext.current
    val activity = context as? Activity
    val deviceType by vm.deviceType.collectAsStateWithLifecycle()

    androidx.compose.runtime.DisposableEffect(deviceType) {
        onDispose {
            if (deviceType != "tv") {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
    
    val episodesMap by vm.showEpisodes.collectAsStateWithLifecycle()
    val episodes = episodesMap[show.id] ?: emptyList()

    var currentEpisode by remember(show.id, episodes) {
        mutableStateOf<Episode?>(episodes.find { it.id == episodeId } ?: episodes.firstOrNull())
    }

    val currentIndex = episodes.indexOfFirst { it.id == currentEpisode?.id }
    val nextEpisode = if (currentIndex in 0 until episodes.lastIndex) episodes[currentIndex + 1] else null

    val handlePlayNextEpisode = {
        if (nextEpisode != null) {
            AdManager.isWatchingVideo = false
            AdManager.showTransitionAd(activity) {
                currentEpisode = nextEpisode
                vm.loadEpisodeVideo(nextEpisode)
            }
        }
    }

    var activeCustomView by remember { mutableStateOf<android.view.View?>(null) }
    var activeCustomViewCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AdManager.isInsidePlayer = true
        onDispose {
            AdManager.isInsidePlayer = false
            AdManager.isWatchingVideo = false
            AdManager.isPlayerInteracting = false
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            try {
                val decorView = activity?.window?.decorView as? android.view.ViewGroup
                activeCustomView?.let { decorView?.removeView(it) }
                activeCustomViewCallback?.onCustomViewHidden()
            } catch (e: Exception) {}
            activeCustomView = null
            activeCustomViewCallback = null
            if (deviceType != "tv") {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                activity?.setImmersiveMode(false)
            }
            vm.clearEpisodeVideo()
        }
    }

    var isAdChecked by remember(show.id) { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showEpisodesList by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val playerAutoHide by vm.playerAutoHide.collectAsStateWithLifecycle()
    val playerHideSeconds by vm.playerHideSeconds.collectAsStateWithLifecycle()

    // Synchronize user interaction state with AdManager
    LaunchedEffect(showControls) {
        AdManager.isPlayerInteracting = showControls
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls, showEpisodesList, lastInteractionTime, playerAutoHide, playerHideSeconds) {
        if (playerAutoHide && showControls && !showEpisodesList) {
            delay(playerHideSeconds * 1000L)
            showControls = false
        }
    }

    LaunchedEffect(show.id) {
        vm.loadEpisodes(show)
        AdManager.isInsidePlayer = true
        AdManager.showBeforeEpisodeAd(activity) {
            isAdChecked = true
            val ep = currentEpisode ?: episodes.firstOrNull()
            if (ep != null) {
                currentEpisode = ep
                vm.loadEpisodeVideo(ep)
            }
        }
    }

    LaunchedEffect(currentEpisode, show.id, isAdChecked) {
        if (isAdChecked) {
            if (currentEpisode != null) {
                vm.loadEpisodeVideo(currentEpisode!!)
            } else if (show.link.isNotEmpty()) {
                val dummyEp = Episode(
                    id = "show_${show.id}",
                    title = show.title,
                    episodeNum = "1",
                    poster = show.poster,
                    link = show.link
                )
                vm.loadEpisodeVideo(dummyEp)
            }
        }
    }

    // Orientation control
    LaunchedEffect(isFullScreen, deviceType) {
        val activity = context.findActivity()
        if (deviceType == "tv") {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.setImmersiveMode(false)
        } else if (isFullScreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.setImmersiveMode(true)
        } else if (activeCustomView == null) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.setImmersiveMode(false)
        }
    }

    // Watch history
    LaunchedEffect(showId) {
        vm.addToHistory(showId)
    }

    // On Back handling to restore orientation
    val handleBack = {
        if (activeCustomView != null) {
            try {
                val act = activeCustomView?.context?.findActivity() ?: activity
                val decorView = act?.window?.decorView as? android.view.ViewGroup
                decorView?.removeView(activeCustomView)
                activeCustomViewCallback?.onCustomViewHidden()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            activeCustomView = null
            activeCustomViewCallback = null
            if (deviceType != "tv" && !isFullScreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                activity?.setImmersiveMode(false)
            }
        } else if (isFullScreen) {
            isFullScreen = false
            if (deviceType != "tv") {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                activity?.setImmersiveMode(false)
            }
        } else {
            AdManager.isWatchingVideo = false
            AdManager.isInsidePlayer = false
            AdManager.isPlayerInteracting = false
            if (deviceType != "tv") {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                activity?.setImmersiveMode(false)
            }
            AdManager.showTransitionAd(activity) {
                onBack()
            }
        }
    }

    androidx.activity.compose.BackHandler {
        handleBack()
    }

    if (!isAdChecked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0D14)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator(
                    color = NetflixRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "جاري تحضير المشغل والبث... 🎬",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = show.title,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    val isTv = deviceType == "tv"
    if (isTv && !isFullScreen) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    VideoPlayerSection(
                        show = show,
                        currentEpisode = currentEpisode,
                        onBack = handleBack,
                        isFullScreen = true,
                        onToggleFullScreen = { 
                            isFullScreen = !isFullScreen 
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        vm = vm,
                        showControls = showControls,
                        onToggleControls = { 
                            showControls = !showControls 
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        showEpisodesList = showEpisodesList,
                        onToggleEpisodesList = { 
                            showEpisodesList = !showEpisodesList 
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onSelectEpisode = { ep -> 
                            if (ep.id != currentEpisode?.id) {
                                AdManager.isWatchingVideo = false
                                AdManager.showTransitionAd(activity) {
                                    currentEpisode = ep 
                                    showEpisodesList = false
                                    lastInteractionTime = System.currentTimeMillis()
                                    vm.loadEpisodeVideo(ep)
                                }
                            } else {
                                showEpisodesList = false
                            }
                        },
                        onPlayNextEpisode = handlePlayNextEpisode,
                        onCustomViewAttached = { view, callback ->
                            activeCustomView = view
                            activeCustomViewCallback = callback
                        }
                    )
                }
                PlayerServersSection(vm = vm, currentEpisode = currentEpisode)
            }
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .background(Color(0xFF121212))
            ) {
                PlayerBottomContent(
                    show = show,
                    episodes = episodes,
                    currentEpisode = currentEpisode,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onEpisodeSelected = { ep ->
                        if (ep.id != currentEpisode?.id) {
                            AdManager.isWatchingVideo = false
                            AdManager.showTransitionAd(activity) {
                                currentEpisode = ep
                                showControls = true
                                vm.saveProgress(show.id, ep.id)
                                lastInteractionTime = System.currentTimeMillis()
                                vm.loadEpisodeVideo(ep)
                            }
                        }
                    },
                    vm = vm,
                    onOpenShow = onOpenShow
                )
            }

        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .then(if (!isFullScreen && deviceType != "tv") Modifier.statusBarsPadding() else Modifier),
            verticalArrangement = if (isFullScreen) Arrangement.Center else Arrangement.Top
        ) {
            VideoPlayerSection(
                show = show,
                currentEpisode = currentEpisode,
                onBack = handleBack,
                isFullScreen = isFullScreen,
                onToggleFullScreen = { 
                    val nextFs = !isFullScreen
                    isFullScreen = nextFs
                    lastInteractionTime = System.currentTimeMillis()
                    if (deviceType != "tv") {
                        if (nextFs) {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            activity?.setImmersiveMode(true)
                        } else {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            activity?.setImmersiveMode(false)
                        }
                    }
                },
                vm = vm,
                showControls = showControls,
                onToggleControls = { 
                    showControls = !showControls 
                    lastInteractionTime = System.currentTimeMillis()
                },
                showEpisodesList = showEpisodesList,
                onToggleEpisodesList = { 
                    showEpisodesList = !showEpisodesList 
                    lastInteractionTime = System.currentTimeMillis()
                },
                onSelectEpisode = { ep -> 
                    if (ep.id != currentEpisode?.id) {
                        AdManager.isWatchingVideo = false
                        AdManager.showTransitionAd(activity) {
                            currentEpisode = ep 
                            showEpisodesList = false
                            lastInteractionTime = System.currentTimeMillis()
                            if (deviceType != "tv") {
                                isFullScreen = false
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                activity?.setImmersiveMode(false)
                            }
                            vm.loadEpisodeVideo(ep)
                        }
                    } else {
                        showEpisodesList = false
                    }
                },
                onPlayNextEpisode = handlePlayNextEpisode,
                onCustomViewAttached = { view, callback ->
                    activeCustomView = view
                    activeCustomViewCallback = callback
                }
            )
            
            if (!isFullScreen) {
                PlayerServersSection(vm = vm, currentEpisode = currentEpisode)
                PlayerBottomContent(
                    show = show,
                    episodes = episodes,
                    currentEpisode = currentEpisode,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onEpisodeSelected = { ep ->
                        if (ep.id != currentEpisode?.id) {
                            AdManager.isWatchingVideo = false
                            AdManager.showTransitionAd(activity) {
                                currentEpisode = ep
                                showControls = true
                                vm.saveProgress(show.id, ep.id)
                                lastInteractionTime = System.currentTimeMillis()
                                if (deviceType != "tv") {
                                    isFullScreen = false
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    activity?.setImmersiveMode(false)
                                }
                                vm.loadEpisodeVideo(ep)
                            }
                        }
                    },
                    vm = vm,
                    onOpenShow = onOpenShow
                )
            }
        }
    }
}
@Composable
private fun VideoPlayerSection(
    show: Show,
    currentEpisode: Episode? = null,
    onBack: () -> Unit,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    vm: AppViewModel,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    showEpisodesList: Boolean,
    onToggleEpisodesList: () -> Unit,
    onSelectEpisode: (Episode) -> Unit,
    onPlayNextEpisode: () -> Unit,
    onCustomViewAttached: ((android.view.View?, android.webkit.WebChromeClient.CustomViewCallback?) -> Unit)? = null
) {
    val videoUrl by vm.videoUrl.collectAsStateWithLifecycle()
    val isLoadingVideo by vm.isLoadingVideo.collectAsStateWithLifecycle()
    val episodesMap by vm.showEpisodes.collectAsStateWithLifecycle()
    val episodes = episodesMap[show.id] ?: emptyList()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedServer by vm.selectedServer.collectAsStateWithLifecycle()
    val currentServerIndex = servers.indexOfFirst { it.url == selectedServer?.url }.coerceAtLeast(0)
    val deviceType by vm.deviceType.collectAsStateWithLifecycle()
    val isTv = deviceType == "tv"

    val activeUrl = videoUrl

    LaunchedEffect(isLoadingVideo, activeUrl) {
        AdManager.isWatchingVideo = (!isLoadingVideo && activeUrl != null)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullScreen) Modifier.fillMaxHeight() else Modifier.height(250.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!isLoadingVideo && activeUrl != null) {
            val playerTitle = if (currentEpisode != null) {
                "${show.title} - ${currentEpisode.title}"
            } else {
                show.title
            }
            WebVideoPlayer(
                url = activeUrl,
                referrer = currentEpisode?.link ?: show.link.ifEmpty { "https://ep.alooytv16.xyz/" },
                title = playerTitle,
                servers = servers,
                currentServerIndex = currentServerIndex,
                onSelectServer = { idx ->
                    if (idx >= 0 && idx < servers.size) {
                        currentEpisode?.let { ep ->
                            vm.selectServer(servers[idx], ep.link)
                        }
                    }
                },
                onServerError = {
                    currentEpisode?.let { ep ->
                        vm.selectNextServer(ep.link)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                isTv = isTv,
                onBack = onBack,
                onToggleFullScreen = onToggleFullScreen,
                onToggleEpisodesList = onToggleEpisodesList,
                onPlayNextEpisode = onPlayNextEpisode,
                onCustomViewAttached = onCustomViewAttached
            )
        } else {
            // Preparing Episode Loading Screen (Replaces intermediate website scraping page)
            val backdropOrPoster = show.backdrop.ifEmpty { show.poster }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (backdropOrPoster.isNotEmpty()) {
                    PosterArt(
                        showId = show.id, 
                        posterUrl = backdropOrPoster,
                        title = "",
                        showTitle = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.20f)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(46.dp),
                        color = NetflixRed,
                        strokeWidth = 3.5.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "جاري تجهيز الحلقة... 🎬",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يتم فحص وتجهيز أفضل سيرفر للبث المباشر ✨",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Top Bar (Back, Refresh, Fullscreen)
        val showFullscreenButton = isTv || (showControls && !isLoadingVideo)
        val showBackAndRefresh = showControls && !isLoadingVideo

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            AnimatedVisibility(
                visible = showBackAndRefresh,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .tvClickable(shape = CircleShape) { onBack() }
                        .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                if (!isTv) {
                    AnimatedVisibility(
                        visible = showBackAndRefresh,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .tvClickable(shape = CircleShape) { 
                                    val current = videoUrl
                                    vm.setVideoUrl(null)
                                    current?.let { vm.setVideoUrl(it) }
                                }
                                .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "تحديث", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                }

                AnimatedVisibility(
                    visible = showFullscreenButton,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .tvClickable(shape = CircleShape) { onToggleFullScreen() }
                            .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = "ملء الشاشة",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // TV Mode Friendly Notice Toast/Banner (Shows once, fades out automatically after 4s)
        if (isTv && !hasShownTvNoticeGlobally) {
            var tvNoticeVisible by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(4000)
                tvNoticeVisible = false
                hasShownTvNoticeGlobally = true
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = tvNoticeVisible,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 60.dp, start = 32.dp, end = 32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1420).copy(alpha = 0.92f))
                        .border(1.dp, Color(0xFFFFA000).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable {
                            tvNoticeVisible = false
                            hasShownTvNoticeGlobally = true
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DesktopWindows,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "وضع التلفزيون: قد تظهر بعض الأخطاء في بعض السيرفرات، نحن نعمل على حلها دائماً 📺✨",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

 

        // Episodes Drawer Overlay with Scrim Backdrop
        AnimatedVisibility(
            visible = showEpisodesList && !show.isMovie,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onToggleEpisodesList()
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color(0xFF141414).copy(alpha = 0.96f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            /* Consume clicks on the sheet content so it doesn't dismiss */
                        }
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Drag handle / Title header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Top drag bar indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f))
                            )

                            IconButton(
                                onClick = onToggleEpisodesList, 
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 12.dp, top = 8.dp)
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
                            }

                            Text(
                                "قائمة الحلقات (${episodes.size})", 
                                color = Color.White, 
                                fontWeight = FontWeight.Black, 
                                fontSize = 17.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(episodes) { ep ->
                                val isCurrent = ep.id == currentEpisode?.id
                                Column(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onSelectEpisode(ep)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                if (isCurrent) 2.dp else 0.dp, 
                                                if (isCurrent) Color.Red else Color.Transparent, 
                                                RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        AsyncImage(
                                            model = ep.poster.ifEmpty { show.poster },
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isCurrent) {
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.5f)), 
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Filled.PlayArrow, 
                                                    contentDescription = null, 
                                                    tint = Color.Red, 
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "الحلقة ${ep.episodeNum}", 
                                        color = if (isCurrent) Color.Red else Color.White, 
                                        fontSize = 13.sp, 
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1, 
                                        textAlign = TextAlign.Center, 
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        if (isCurrent) "جاري التشغيل 🎬" else "انقر للتشغيل", 
                                        color = if (isCurrent) Color.Red.copy(alpha = 0.8f) else Color.Gray, 
                                        fontSize = 10.sp, 
                                        textAlign = TextAlign.Center, 
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowInfoSection(show: Show) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = show.title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(show.year, color = Color.Gray, fontSize = 12.sp)
            Text(show.genres.joinToString(" • "), color = Color.Gray, fontSize = 12.sp)
            Text(show.duration, color = Color.Gray, fontSize = 12.sp)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", show.imdb),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun TabRowSection(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = title,
                    color = if (isSelected) NetflixRed else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(if (isSelected) NetflixRed else Color.Transparent)
                )
            }
        }
    }
}





@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimilarShowsSection(show: Show, onOpenShow: (String) -> Unit) {
    val related = Catalog.all.filter { it.id != show.id && it.genres.any { g -> g in show.genres } }.take(9)
    Column(modifier = Modifier.padding(16.dp)) {
        Text("أعمال مشابهة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        // Grid layout for similar works
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 3,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            related.forEach { r ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenShow(r.id) }
                ) {
                    PosterArt(showId = r.id, posterUrl = r.poster, title = r.title, showTitle = true)
                }
            }
        }
    }
}

private fun fmt(pct: Float): String {
    val totalSeconds = (pct / 100f * 42 * 60 + 15).toInt()
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

// Utility to find activity from context
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Utility to set immersive mode
private fun Activity.setImmersiveMode(enable: Boolean) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    if (enable) {
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    } else {
        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
    }
}

fun buildHlsPlayerHtml(masterUrl: String, variantUrl: String, title: String = ""): String = ""
/*

        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>Netflix Shahid Ultra Player</title>
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
            <style>
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                    user-select: none;
                    -webkit-user-select: none;
                    -webkit-touch-callout: none;
                }
                html, body {
                    width: 100%;
                    height: 100%;
                    background: #000;
                    color: #fff;
                    font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                    overflow: hidden;
                }
                #player-container {
                    position: relative;
                    width: 100%;
                    height: 100%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: #000;
                }
                video {
                    width: 100%;
                    height: 100%;
                    object-fit: contain;
                    background: #000;
                    transition: object-fit 0.2s ease, filter 0.1s ease;
                }
                .controls-overlay {
                    position: absolute;
                    inset: 0;
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    background: linear-gradient(180deg, rgba(0,0,0,0.88) 0%, rgba(0,0,0,0) 25%, rgba(0,0,0,0) 70%, rgba(0,0,0,0.92) 100%);
                    opacity: 1;
                    transition: opacity 0.3s ease;
                    z-index: 10;
                    padding: 14px 18px;
                }
                .controls-overlay.hidden {
                    opacity: 0;
                    pointer-events: none;
                }
                .top-bar {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    width: 100%;
                    gap: 12px;
                }
                .player-title-box {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    flex: 1;
                    overflow: hidden;
                }
                .brand-badge {
                    background: linear-gradient(135deg, #E50914, #B20710);
                    color: #fff;
                    font-size: 11px;
                    font-weight: 900;
                    padding: 4px 10px;
                    border-radius: 6px;
                    letter-spacing: 0.5px;
                    box-shadow: 0 2px 8px rgba(229, 9, 20, 0.5);
                    flex-shrink: 0;
                }
                .player-title {
                    font-size: 14px;
                    font-weight: 800;
                    color: #fff;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    text-shadow: 0 2px 4px rgba(0,0,0,0.8);
                }
                .top-actions {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    flex-shrink: 0;
                }
                .icon-btn {
                    background: rgba(255,255,255,0.18);
                    backdrop-filter: blur(12px);
                    border: 1px solid rgba(255,255,255,0.15);
                    color: #fff;
                    width: 38px;
                    height: 38px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 15px;
                    cursor: pointer;
                    transition: all 0.2s;
                }
                .icon-btn:active {
                    transform: scale(0.9);
                    background: #E50914;
                }
                .action-pill {
                    background: rgba(255,255,255,0.18);
                    border: 1px solid rgba(255,255,255,0.25);
                    color: #fff;
                    padding: 6px 14px;
                    border-radius: 20px;
                    font-size: 12px;
                    font-weight: bold;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    backdrop-filter: blur(10px);
                    transition: all 0.2s;
                }
                .action-pill:active {
                    background: #E50914;
                    border-color: #E50914;
                }
                
                .center-gestures {
                    position: absolute;
                    inset: 50px 0 70px 0;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    z-index: 5;
                }
                .touch-zone {
                    flex: 1;
                    height: 100%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    position: relative;
                }
                .seek-badge {
                    position: absolute;
                    background: rgba(0,0,0,0.85);
                    border: 1.5px solid rgba(229,9,20,0.8);
                    color: #fff;
                    padding: 12px 22px;
                    border-radius: 30px;
                    font-size: 13px;
                    font-weight: bold;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    opacity: 0;
                    transform: scale(0.7);
                    transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
                    pointer-events: none;
                    box-shadow: 0 4px 15px rgba(0,0,0,0.8);
                }
                .seek-badge.active {
                    opacity: 1;
                    transform: scale(1);
                }
                .play-center-btn {
                    background: linear-gradient(135deg, #E50914, #B20710);
                    color: #fff;
                    width: 68px;
                    height: 68px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 26px;
                    border: none;
                    box-shadow: 0 0 30px rgba(229, 9, 20, 0.7);
                    cursor: pointer;
                    transition: transform 0.2s;
                    z-index: 12;
                }
                .play-center-btn:active {
                    transform: scale(0.85);
                }

                .hud-indicator {
                    position: absolute;
                    top: 50%;
                    transform: translateY(-50%);
                    background: rgba(0,0,0,0.85);
                    border: 1px solid rgba(255,255,255,0.2);
                    padding: 14px 18px;
                    border-radius: 16px;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    gap: 10px;
                    opacity: 0;
                    pointer-events: none;
                    transition: opacity 0.2s ease;
                    z-index: 40;
                    box-shadow: 0 8px 25px rgba(0,0,0,0.8);
                }
                .hud-indicator.active {
                    opacity: 1;
                }
                #hud-volume { right: 24px; }
                #hud-brightness { left: 24px; }
                .hud-bar-container {
                    width: 6px;
                    height: 90px;
                    background: rgba(255,255,255,0.25);
                    border-radius: 3px;
                    position: relative;
                    overflow: hidden;
                }
                .hud-bar-fill {
                    position: absolute;
                    bottom: 0;
                    width: 100%;
                    background: #E50914;
                    border-radius: 3px;
                    transition: height 0.1s linear;
                }

                .skip-intro-btn {
                    position: absolute;
                    bottom: 85px;
                    left: 20px;
                    background: rgba(20,20,20,0.9);
                    border: 1.5px solid rgba(255,255,255,0.4);
                    color: #fff;
                    padding: 8px 18px;
                    border-radius: 25px;
                    font-size: 13px;
                    font-weight: bold;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    cursor: pointer;
                    z-index: 15;
                    backdrop-filter: blur(10px);
                    box-shadow: 0 4px 15px rgba(0,0,0,0.6);
                    transition: all 0.2s;
                }
                .skip-intro-btn:active {
                    background: #E50914;
                    border-color: #E50914;
                }

                .next-ep-card {
                    position: absolute;
                    bottom: 80px;
                    right: 20px;
                    background: rgba(20,20,20,0.95);
                    border: 1.5px solid #E50914;
                    padding: 12px 18px;
                    border-radius: 12px;
                    display: none;
                    align-items: center;
                    gap: 12px;
                    z-index: 30;
                    backdrop-filter: blur(12px);
                    box-shadow: 0 8px 25px rgba(229,9,20,0.4);
                    animation: slideIn 0.3s ease;
                }
                @keyframes slideIn {
                    from { transform: translateX(50px); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
                .next-ep-btn {
                    background: #E50914;
                    color: #fff;
                    border: none;
                    padding: 8px 14px;
                    border-radius: 8px;
                    font-size: 12px;
                    font-weight: bold;
                    cursor: pointer;
                }

                .bottom-bar {
                    display: flex;
                    flex-direction: column;
                    gap: 8px;
                    width: 100%;
                }
                .progress-row {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    width: 100%;
                }
                .time-text {
                    font-size: 12px;
                    font-weight: 700;
                    color: #ddd;
                    min-width: 48px;
                    text-align: center;
                    font-variant-numeric: tabular-nums;
                }
                .progress-bar-container {
                    flex: 1;
                    height: 24px;
                    display: flex;
                    align-items: center;
                    position: relative;
                    cursor: pointer;
                }
                .progress-track {
                    position: absolute;
                    width: 100%;
                    height: 4px;
                    background: rgba(255,255,255,0.3);
                    border-radius: 2px;
                }
                .progress-buffer {
                    position: absolute;
                    height: 4px;
                    background: rgba(255,255,255,0.5);
                    border-radius: 2px;
                    width: 0%;
                }
                .progress-fill {
                    position: absolute;
                    height: 4px;
                    background: #E50914;
                    border-radius: 2px;
                    width: 0%;
                }
                .progress-thumb {
                    position: absolute;
                    width: 14px;
                    height: 14px;
                    background: #E50914;
                    border: 2.5px solid #fff;
                    border-radius: 50%;
                    top: 50%;
                    transform: translate(-50%, -50%);
                    left: 0%;
                    box-shadow: 0 0 10px rgba(0,0,0,0.9);
                }
                .controls-row {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    width: 100%;
                }
                .left-controls, .right-controls {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }

                .toast-banner {
                    position: absolute;
                    top: 25%;
                    left: 50%;
                    transform: translate(-50%, -50%) scale(0.8);
                    background: rgba(0,0,0,0.85);
                    border: 1.5px solid rgba(229, 9, 20, 0.8);
                    color: #fff;
                    padding: 10px 22px;
                    border-radius: 30px;
                    font-size: 13px;
                    font-weight: bold;
                    box-shadow: 0 4px 20px rgba(0,0,0,0.7);
                    opacity: 0;
                    pointer-events: none;
                    transition: all 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
                    z-index: 50;
                    white-space: nowrap;
                }
                .toast-banner.show {
                    opacity: 1;
                    transform: translate(-50%, -50%) scale(1);
                }

                .lock-indicator {
                    position: absolute;
                    top: 16px;
                    right: 16px;
                    z-index: 60;
                    background: rgba(0,0,0,0.85);
                    border: 1.5px solid rgba(229,9,20,0.8);
                    padding: 8px 18px;
                    border-radius: 25px;
                    font-size: 13px;
                    font-weight: bold;
                    display: none;
                    align-items: center;
                    gap: 8px;
                    cursor: pointer;
                    box-shadow: 0 4px 15px rgba(0,0,0,0.8);
                }
                .lock-indicator.active {
                    display: flex;
                }

                .settings-modal {
                    position: absolute;
                    bottom: 0;
                    left: 0;
                    right: 0;
                    background: rgba(18,18,18,0.96);
                    backdrop-filter: blur(20px);
                    border-top-left-radius: 20px;
                    border-top-right-radius: 20px;
                    padding: 20px;
                    z-index: 100;
                    transform: translateY(100%);
                    transition: transform 0.3s cubic-bezier(0.1, 0.9, 0.2, 1);
                    border-top: 1px solid rgba(255,255,255,0.15);
                }
                .settings-modal.active {
                    transform: translateY(0);
                }
                .modal-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 16px;
                    padding-bottom: 10px;
                    border-bottom: 1px solid rgba(255,255,255,0.1);
                }
                .modal-title {
                    font-size: 15px;
                    font-weight: bold;
                    color: #fff;
                }
                .options-grid {
                    display: grid;
                    grid-template-columns: repeat(3, 1fr);
                    gap: 10px;
                }
                .option-btn {
                    background: rgba(255,255,255,0.08);
                    border: 1px solid rgba(255,255,255,0.15);
                    color: #fff;
                    padding: 10px;
                    border-radius: 10px;
                    font-size: 12px;
                    font-weight: 600;
                    text-align: center;
                    cursor: pointer;
                }
                .option-btn.selected {
                    background: #E50914;
                    border-color: #E50914;
                }
            </style>
        </head>
        <body>
            <div id="player-container">
                <video id="video" playsinline webkit-playsinline></video>
                <div class="toast-banner" id="toast-banner"></div>

                <div class="hud-indicator" id="hud-volume">
                    <i class="fa-solid fa-volume-high" style="color:#E50914; font-size:18px;"></i>
                    <div class="hud-bar-container"><div class="hud-bar-fill" id="hud-vol-fill" style="height:100%;"></div></div>
                    <span id="hud-vol-text" style="font-size:11px; font-weight:bold;">100%</span>
                </div>

                <div class="hud-indicator" id="hud-brightness">
                    <i class="fa-solid fa-sun" style="color:#E50914; font-size:18px;"></i>
                    <div class="hud-bar-container"><div class="hud-bar-fill" id="hud-bright-fill" style="height:70%;"></div></div>
                    <span id="hud-bright-text" style="font-size:11px; font-weight:bold;">70%</span>
                </div>

                <button class="skip-intro-btn" id="skip-intro-btn"><i class="fa-solid fa-forward-step"></i> تخطي المقدمة (+85 ثانية)</button>

                <div class="next-ep-card" id="next-ep-card">
                    <div>
                        <div style="font-size: 11px; color: #aaa;">الحلقة التالية</div>
                        <div style="font-size: 13px; font-weight: bold;" id="next-ep-timer-text">جاهز للتشغيل</div>
                    </div>
                    <button class="next-ep-btn" id="next-ep-action-btn">تشغيل الآن ⏭</button>
                </div>

                <div id="controls-overlay" class="controls-overlay">
                    <div class="top-bar">
                        <div class="player-title-box">
                            <button class="icon-btn" id="btn-back" title="رجوع"><i class="fa-solid fa-arrow-right"></i></button>
                            <span class="brand-badge">NETFLIX</span>
                            <span class="player-title" id="title-text">$safeTitle</span>
                        </div>
                        <div class="top-actions">
                            <button class="action-pill" id="aspect-btn" title="أبعاد الشاشة"><i class="fa-solid fa-maximize"></i> <span id="aspect-text">أبعاد الشاشة</span></button>
                            <button class="icon-btn" id="speed-btn" title="السرعة"><i class="fa-solid fa-gauge-high"></i></button>
                            <button class="icon-btn" id="lock-btn" title="قفل التحكم"><i class="fa-solid fa-lock"></i></button>
                        </div>
                    </div>

                    <div class="center-gestures">
                        <div class="touch-zone" id="zone-left">
                            <div class="seek-badge" id="badge-left"><i class="fa-solid fa-rotate-left"></i> 10- ثواني</div>
                        </div>
                        <button class="play-center-btn" id="center-play-btn"><i class="fa-solid fa-play" id="center-play-icon"></i></button>
                        <div class="touch-zone" id="zone-right">
                            <div class="seek-badge" id="badge-right">10+ ثواني <i class="fa-solid fa-rotate-right"></i></div>
                        </div>
                    </div>

                    <div class="bottom-bar">
                        <div class="progress-row">
                            <span class="time-text" id="time-current">00:00</span>
                            <div class="progress-bar-container" id="progress-container">
                                <div class="progress-track"></div>
                                <div class="progress-buffer" id="progress-buffer"></div>
                                <div class="progress-fill" id="progress-fill"></div>
                                <div class="progress-thumb" id="progress-thumb"></div>
                            </div>
                            <span class="time-text" id="time-remaining">-00:00</span>
                        </div>

                        <div class="controls-row">
                            <div class="left-controls">
                                <button class="icon-btn" id="btn-rewind"><i class="fa-solid fa-rotate-left"></i></button>
                                <button class="icon-btn" id="btn-forward"><i class="fa-solid fa-rotate-right"></i></button>
                            </div>

                            <div class="right-controls">
                                <button class="action-pill" id="btn-episodes"><i class="fa-solid fa-layer-group"></i> الحلقات</button>
                                <button class="icon-btn" id="rotate-btn" title="تدوير / ملء الشاشة"><i class="fa-solid fa-up-right-and-down-left-from-center"></i></button>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="lock-indicator" id="lock-indicator">
                    <i class="fa-solid fa-lock"></i> اضغط لفك قفل التحكم 🔓
                </div>

                <div class="settings-modal" id="settings-modal">
                    <div class="modal-header">
                        <span class="modal-title" id="modal-title-text">إعدادات المشغل</span>
                        <button class="icon-btn" id="close-modal-btn"><i class="fa-solid fa-xmark"></i></button>
                    </div>
                    <div id="modal-content" class="options-grid"></div>
                </div>
            </div>

            <script src="https://cdn.jsdelivr.net/npm/hls.js@1.4.12/dist/hls.min.js"></script>
            <script>
                const video = document.getElementById('video');
                const overlay = document.getElementById('controls-overlay');
                const centerPlayBtn = document.getElementById('center-play-btn');
                const centerPlayIcon = document.getElementById('center-play-icon');
                const progressContainer = document.getElementById('progress-container');
                const progressFill = document.getElementById('progress-fill');
                const progressBuffer = document.getElementById('progress-buffer');
                const progressThumb = document.getElementById('progress-thumb');
                const timeCurrent = document.getElementById('time-current');
                const timeRemaining = document.getElementById('time-remaining');
                const btnRewind = document.getElementById('btn-rewind');
                const btnForward = document.getElementById('btn-forward');
                const btnEpisodes = document.getElementById('btn-episodes');
                const rotateBtn = document.getElementById('rotate-btn');
                const aspectBtn = document.getElementById('aspect-btn');
                const aspectText = document.getElementById('aspect-text');
                const speedBtn = document.getElementById('speed-btn');
                const lockBtn = document.getElementById('lock-btn');
                const lockIndicator = document.getElementById('lock-indicator');
                const settingsModal = document.getElementById('settings-modal');
                const modalTitleText = document.getElementById('modal-title-text');
                const modalContent = document.getElementById('modal-content');
                const closeModalBtn = document.getElementById('close-modal-btn');
                const zoneLeft = document.getElementById('zone-left');
                const zoneRight = document.getElementById('zone-right');
                const badgeLeft = document.getElementById('badge-left');
                const badgeRight = document.getElementById('badge-right');
                const toastBanner = document.getElementById('toast-banner');
                const skipIntroBtn = document.getElementById('skip-intro-btn');
                const nextEpCard = document.getElementById('next-ep-card');
                const nextEpActionBtn = document.getElementById('next-ep-action-btn');

                let hls = null;
                let isLocked = false;
                let hideTimer = null;
                let toastTimer = null;
                let currentAspectIndex = 0;
                const aspectModes = ['contain', 'cover', 'fill'];
                const aspectLabels = ['أصلية (Fit)', 'تعبئة زوم (Zoom)', 'تمديد كامل (Stretch)'];
                const btnBack = document.getElementById('btn-back');
                if (btnBack) {
                    btnBack.addEventListener('click', (e) => {
                        e.stopPropagation();
                        if (window.AndroidNativePlayer && window.AndroidNativePlayer.goBack) {
                            window.AndroidNativePlayer.goBack();
                        }
                    });
                }
                const masterSource = "$masterUrl";
                const fallbackSource = "$variantUrl";

                function showToast(msg) {
                    if (!toastBanner) return;
                    toastBanner.innerText = msg;
                    toastBanner.classList.add('show');
                    clearTimeout(toastTimer);
                    toastTimer = setTimeout(() => {
                        toastBanner.classList.remove('show');
                    }, 2200);
                }

                function resetHideTimer() {
                    if (isLocked) return;
                    overlay.classList.remove('hidden');
                    clearTimeout(hideTimer);
                    hideTimer = setTimeout(() => {
                        if (!video.paused) {
                            overlay.classList.add('hidden');
                        }
                    }, 3500);
                }

                document.getElementById('player-container').addEventListener('click', (e) => {
                    if (settingsModal.classList.contains('active')) {
                        settingsModal.classList.remove('active');
                        return;
                    }
                    if (isLocked) return;
                    if (e.target.closest('.icon-btn, .action-pill, .play-center-btn, .progress-bar-container, .settings-modal, .skip-intro-btn, .next-ep-card')) return;
                    
                    if (overlay.classList.contains('hidden')) {
                        resetHideTimer();
                    } else {
                        overlay.classList.add('hidden');
                    }
                });

                // Gestures: Volume and Brightness vertical drag
                let touchStartY = 0, touchStartX = 0;
                let isDraggingVol = false, isDraggingBright = false;
                let startVol = 1, startBright = 1, currentBrightness = 1.0;
                const hudVolume = document.getElementById('hud-volume');
                const hudVolFill = document.getElementById('hud-vol-fill');
                const hudVolText = document.getElementById('hud-vol-text');
                const hudBrightness = document.getElementById('hud-brightness');
                const hudBrightFill = document.getElementById('hud-bright-fill');
                const hudBrightText = document.getElementById('hud-bright-text');

                document.getElementById('player-container').addEventListener('touchstart', (e) => {
                    if (isLocked || e.touches.length > 1) return;
                    const touch = e.touches[0];
                    const w = window.innerWidth;
                    touchStartX = touch.clientX;
                    touchStartY = touch.clientY;

                    if (e.target.closest('.icon-btn, .action-pill, .play-center-btn, .progress-bar-container, .settings-modal, .skip-intro-btn, .next-ep-card')) return;

                    if (touchStartX > w * 0.65) {
                        isDraggingVol = true;
                        startVol = video.volume;
                    } else if (touchStartX < w * 0.35) {
                        isDraggingBright = true;
                        startBright = currentBrightness;
                    }
                });

                document.getElementById('player-container').addEventListener('touchmove', (e) => {
                    if (e.touches.length > 1) return;
                    const touch = e.touches[0];
                    const deltaY = touchStartY - touch.clientY;
                    const h = window.innerHeight;
                    const deltaPct = deltaY / (h * 0.5);

                    if (isDraggingVol) {
                        e.preventDefault();
                        let nv = Math.max(0, Math.min(1, startVol + deltaPct));
                        video.volume = nv;
                        let pct = Math.round(nv * 100);
                        hudVolFill.style.height = pct + '%';
                        hudVolText.innerText = pct + '%';
                        hudVolume.classList.add('active');
                    } else if (isDraggingBright) {
                        e.preventDefault();
                        let nb = Math.max(0.2, Math.min(1.5, startBright + deltaPct));
                        currentBrightness = nb;
                        video.style.filter = 'brightness(' + nb + ')';
                        let pct = Math.round((nb / 1.5) * 100);
                        hudBrightFill.style.height = pct + '%';
                        hudBrightText.innerText = pct + '%';
                        hudBrightness.classList.add('active');
                    }
                });

                document.getElementById('player-container').addEventListener('touchend', () => {
                    if (isDraggingVol) {
                        isDraggingVol = false;
                        setTimeout(() => hudVolume.classList.remove('active'), 800);
                    }
                    if (isDraggingBright) {
                        isDraggingBright = false;
                        setTimeout(() => hudBrightness.classList.remove('active'), 800);
                    }
                });

                // Double tap seeking
                let lastTapLeft = 0;
                zoneLeft.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const now = Date.now();
                    if (now - lastTapLeft < 300) {
                        video.currentTime = Math.max(0, video.currentTime - 10);
                        flashBadge(badgeLeft);
                    }
                    lastTapLeft = now;
                    resetHideTimer();
                });

                let lastTapRight = 0;
                zoneRight.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const now = Date.now();
                    if (now - lastTapRight < 300) {
                        video.currentTime = Math.min(video.duration || 0, video.currentTime + 10);
                        flashBadge(badgeRight);
                    }
                    lastTapRight = now;
                    resetHideTimer();
                });

                function flashBadge(badge) {
                    badge.classList.add('active');
                    setTimeout(() => badge.classList.remove('active'), 600);
                }

                function togglePlay() {
                    if (video.paused) {
                        video.play().catch(()=>{});
                    } else {
                        video.pause();
                    }
                    resetHideTimer();
                }

                centerPlayBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    togglePlay();
                });

                video.addEventListener('play', () => {
                    centerPlayIcon.className = 'fa-solid fa-pause';
                    resetHideTimer();
                });

                video.addEventListener('pause', () => {
                    centerPlayIcon.className = 'fa-solid fa-play';
                    overlay.classList.remove('hidden');
                });

                function formatTime(s) {
                    if (isNaN(s) || s < 0) return '00:00';
                    const mins = Math.floor(s / 60);
                    const secs = Math.floor(s % 60);
                    return (mins < 10 ? '0' : '') + mins + ':' + (secs < 10 ? '0' : '') + secs;
                }

                let autoNextTriggered = false;
                video.addEventListener('timeupdate', () => {
                    if (!video.duration) return;
                    const pct = (video.currentTime / video.duration) * 100;
                    progressFill.style.width = pct + '%';
                    progressThumb.style.left = pct + '%';
                    timeCurrent.innerText = formatTime(video.currentTime);
                    timeRemaining.innerText = '-' + formatTime(video.duration - video.currentTime);

                    if (video.buffered.length > 0) {
                        const bufEnd = video.buffered.end(video.buffered.length - 1);
                        progressBuffer.style.width = ((bufEnd / video.duration) * 100) + '%';
                    }

                    const remaining = video.duration - video.currentTime;
                    if (remaining <= 15 && remaining > 0) {
                        nextEpCard.style.display = 'flex';
                    } else if (remaining > 15) {
                        nextEpCard.style.display = 'none';
                    }
                });

                video.addEventListener('ended', () => {
                    if (!autoNextTriggered) {
                        autoNextTriggered = true;
                        if (window.AndroidNativePlayer && window.AndroidNativePlayer.playNextEpisode) {
                            window.AndroidNativePlayer.playNextEpisode();
                        }
                    }
                });

                if (nextEpActionBtn) {
                    nextEpActionBtn.addEventListener('click', (e) => {
                        e.stopPropagation();
                        autoNextTriggered = true;
                        if (window.AndroidNativePlayer && window.AndroidNativePlayer.playNextEpisode) {
                            window.AndroidNativePlayer.playNextEpisode();
                        }
                    });
                }

                if (skipIntroBtn) {
                    skipIntroBtn.addEventListener('click', (e) => {
                        e.stopPropagation();
                        video.currentTime = Math.min(video.duration || 0, video.currentTime + 85);
                        showToast('تم تخطي المقدمة ⏭ (+85 ثانية)');
                        resetHideTimer();
                    });
                }

                function seek(e) {
                    const rect = progressContainer.getBoundingClientRect();
                    const pos = (e.clientX - rect.left) / rect.width;
                    const targetTime = Math.max(0, Math.min(1, pos)) * video.duration;
                    video.currentTime = targetTime;
                }

                let isSeeking = false;
                progressContainer.addEventListener('pointerdown', (e) => {
                    isSeeking = true;
                    seek(e);
                });
                document.addEventListener('pointermove', (e) => {
                    if (isSeeking) seek(e);
                });
                document.addEventListener('pointerup', () => {
                    isSeeking = false;
                });

                btnRewind.addEventListener('click', (e) => {
                    e.stopPropagation();
                    video.currentTime = Math.max(0, video.currentTime - 10);
                    resetHideTimer();
                });

                btnForward.addEventListener('click', (e) => {
                    e.stopPropagation();
                    video.currentTime = Math.min(video.duration || 0, video.currentTime + 10);
                    resetHideTimer();
                });

                if (rotateBtn) {
                    rotateBtn.addEventListener('click', (e) => {
                        e.stopPropagation();
                        if (window.AndroidNativePlayer && window.AndroidNativePlayer.toggleFullScreen) {
                            window.AndroidNativePlayer.toggleFullScreen();
                        }
                        showToast('تدوير وتعديل اتجاه الشاشة 🔄');
                        resetHideTimer();
                    });
                }

                if (btnEpisodes) {
                    btnEpisodes.addEventListener('click', (e) => {
                        e.stopPropagation();
                        if (window.AndroidNativePlayer && window.AndroidNativePlayer.openEpisodesList) {
                            window.AndroidNativePlayer.openEpisodesList();
                        }
                        resetHideTimer();
                    });
                }

                aspectBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    currentAspectIndex = (currentAspectIndex + 1) % aspectModes.length;
                    video.style.objectFit = aspectModes[currentAspectIndex];
                    const label = aspectLabels[currentAspectIndex];
                    if (aspectText) aspectText.innerText = 'أبعاد الشاشة';
                    showToast('أبعاد الشاشة: ' + label);
                    resetHideTimer();
                });

                speedBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    modalTitleText.innerText = 'سرعة التشغيل';
                    modalContent.innerHTML = '';
                    const speeds = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0];
                    speeds.forEach(spd => {
                        const btn = document.createElement('div');
                        btn.className = 'option-btn' + (video.playbackRate === spd ? ' selected' : '');
                        btn.innerText = spd === 1.0 ? '1.0x (عادي)' : spd + 'x';
                        btn.onclick = () => {
                            video.playbackRate = spd;
                            settingsModal.classList.remove('active');
                            showToast('السرعة: ' + spd + 'x');
                        };
                        modalContent.appendChild(btn);
                    });
                    settingsModal.classList.add('active');
                });

                closeModalBtn.addEventListener('click', () => {
                    settingsModal.classList.remove('active');
                });

                lockBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    isLocked = true;
                    overlay.classList.add('hidden');
                    lockIndicator.classList.add('active');
                    showToast('تم قفل الشاشة 🔒');
                });

                lockIndicator.addEventListener('click', () => {
                    isLocked = false;
                    lockIndicator.classList.remove('active');
                    overlay.classList.remove('hidden');
                    resetHideTimer();
                });

                function initHls(sourceUrl) {
                    if (hls) {
                        try { hls.destroy(); } catch(e) {}
                        hls = null;
                    }

                    var isM3u8 = sourceUrl.indexOf('.m3u8') !== -1;

                    if (isM3u8 && window.Hls && Hls.isSupported()) {
                        hls = new Hls({
                            maxMaxBufferLength: 60,
                            enableWorker: true,
                            lowLatencyMode: true,
                            xhrSetup: function(xhr, url) {
                                xhr.withCredentials = false;
                            }
                        });
                        hls.loadSource(sourceUrl);
                        hls.attachMedia(video);
                        hls.on(Hls.Events.MANIFEST_PARSED, function() {
                            video.play().catch(function(e){});
                        });
                        hls.on(Hls.Events.ERROR, function(evt, data) {
                            if (data.fatal) {
                                switch (data.type) {
                                    case Hls.ErrorTypes.NETWORK_ERROR:
                                        hls.startLoad();
                                        break;
                                    case Hls.ErrorTypes.MEDIA_ERROR:
                                        hls.recoverMediaError();
                                        break;
                                    default:
                                        if (sourceUrl !== fallbackSource && fallbackSource && fallbackSource.length > 0) {
                                            initHls(fallbackSource);
                                        } else {
                                            try { hls.destroy(); } catch(e){}
                                            hls = null;
                                            video.src = sourceUrl;
                                            video.load();
                                            video.play().catch(function(e){});
                                        }
                                        break;
                                }
                            }
                        });
                    } else {
                        video.src = sourceUrl;
                        video.load();
                        video.play().catch(function(e){});
                    }
                }

                video.addEventListener('error', function(e) {
                    if (video.src !== fallbackSource && fallbackSource && fallbackSource.length > 0) {
                        initHls(fallbackSource);
                    }
                });

                initHls(masterSource);
            </script>
        </body>
        </html>
    """.trimIndent()
}
*/

@Composable
fun WebVideoPlayer(
    url: String, 
    referrer: String,
    title: String = "",
    servers: List<VideoServer> = emptyList(),
    currentServerIndex: Int = 0,
    onSelectServer: ((Int) -> Unit)? = null,
    onServerError: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isTv: Boolean = false,
    onBack: (() -> Unit)? = null,
    onToggleFullScreen: (() -> Unit)? = null,
    onToggleEpisodesList: (() -> Unit)? = null,
    onPlayNextEpisode: (() -> Unit)? = null,
    onCustomViewAttached: ((android.view.View?, android.webkit.WebChromeClient.CustomViewCallback?) -> Unit)? = null
) {
    var capturedStreamUrl by remember(url) { mutableStateOf<String?>(null) }
    var isWebLoaded by remember(url) { mutableStateOf(false) }
    var isDirectPlayerReady by remember(url) { mutableStateOf(false) }
    var forceReady by remember(url) { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val playerFocusRequester = remember { FocusRequester() }

    // TV Remote Virtual Mouse State
    var mousePosition by remember { mutableStateOf(Offset(600f, 350f)) }
    var isClicking by remember { mutableStateOf(false) }
    var isMouseVisible by remember { mutableStateOf(true) }
    var showMouseHint by remember { mutableStateOf(true) }
    var playerWidth by remember { mutableIntStateOf(1920) }
    var playerHeight by remember { mutableIntStateOf(1080) }

    LaunchedEffect(Unit) {
        delay(4500)
        showMouseHint = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    try {
                        webViewRef?.evaluateJavascript("try { var v = document.querySelectorAll('video, audio'); for(var i=0; i<v.length; i++){ v[i].pause(); } } catch(e){}", null)
                        webViewRef?.onPause()
                        webViewRef?.pauseTimers()
                    } catch (_: Exception) {}
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    try {
                        webViewRef?.onResume()
                        webViewRef?.resumeTimers()
                    } catch (_: Exception) {}
                }
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                    try {
                        webViewRef?.evaluateJavascript("try { var v = document.querySelectorAll('video, audio'); for(var i=0; i<v.length; i++){ v[i].pause(); v[i].src=''; v[i].load(); } } catch(e){}", null)
                        webViewRef?.stopLoading()
                        webViewRef?.loadUrl("about:blank")
                        webViewRef?.onPause()
                        webViewRef?.pauseTimers()
                        webViewRef?.destroy()
                        webViewRef = null
                    } catch (_: Exception) {}
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                webViewRef?.let { wv ->
                    wv.evaluateJavascript("try { var v = document.querySelectorAll('video, audio'); for(var i=0; i<v.length; i++){ v[i].pause(); v[i].src=''; v[i].load(); } } catch(e){}", null)
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.onPause()
                    wv.pauseTimers()
                    wv.destroy()
                }
                webViewRef = null
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isTv) {
        if (isTv) {
            delay(300)
            try {
                playerFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(url) {
        isWebLoaded = false
        isDirectPlayerReady = false
        forceReady = false
        delay(4000)
        forceReady = true
    }

    fun triggerWebViewTouchAtCenter(wv: WebView?) {
        wv?.let { view ->
            val width = view.width
            val height = view.height
            if (width > 0 && height > 0) {
                val x = width / 2f
                val y = height / 2f
                val downTime = android.os.SystemClock.uptimeMillis()
                val eventTime = android.os.SystemClock.uptimeMillis()
                val downEvent = android.view.MotionEvent.obtain(downTime, eventTime, android.view.MotionEvent.ACTION_DOWN, x, y, 0)
                val upEvent = android.view.MotionEvent.obtain(downTime, eventTime + 100, android.view.MotionEvent.ACTION_UP, x, y, 0)
                try {
                    view.dispatchTouchEvent(downEvent)
                    view.dispatchTouchEvent(upEvent)
                } catch(e: Exception){}
                downEvent.recycle()
                upEvent.recycle()
            }
        }
    }

    fun clickAtMouse(x: Float, y: Float) {
        webViewRef?.let { view ->
            val downTime = android.os.SystemClock.uptimeMillis()
            val eventTime = android.os.SystemClock.uptimeMillis()
            val downEvent = android.view.MotionEvent.obtain(downTime, eventTime, android.view.MotionEvent.ACTION_DOWN, x, y, 0)
            val upEvent = android.view.MotionEvent.obtain(downTime, eventTime + 50, android.view.MotionEvent.ACTION_UP, x, y, 0)
            try {
                view.dispatchTouchEvent(downEvent)
                view.dispatchTouchEvent(upEvent)
            } catch (e: Exception) {}
            downEvent.recycle()
            upEvent.recycle()

            val jsClick = """
                (function() {
                    try {
                        var x = $x;
                        var y = $y;
                        var el = document.elementFromPoint(x, y);
                        if (el) {
                            el.click();
                            var ev = new MouseEvent('click', { clientX: x, clientY: y, bubbles: true, cancelable: true });
                            el.dispatchEvent(ev);
                        }
                    } catch(e) {}
                })();
            """.trimIndent()
            view.evaluateJavascript(jsClick, null)
        }
    }

val agonTvNavJs = """
    (function() {
        if (!window.agonTvNav) {
            window.agonTvNav = {
                clickCurrent: function() {
                    try {
                        var f1 = document.forms['F1'] || document.getElementById('F1');
                        var vp = document.getElementById('vid_play');
                        if (f1) {
                            var fcInput = f1.file_code || document.querySelector("input[name='file_code']");
                            if (fcInput) {
                                if (!fcInput.value || fcInput.value.trim().length < 2) {
                                    var path = window.location.pathname || '';
                                    var code = path.replace('.html','').split("/").pop().split("-").pop();
                                    if (code) { fcInput.value = code; }
                                }
                                if (vp) { try { vp.click(); } catch(e){} }
                                if (typeof f1.submit === 'function') { try { f1.submit(); } catch(e){} }
                            }
                        } else if (vp) {
                            try { vp.click(); } catch(e){}
                        }
                    } catch(e) {}

                    try {
                        if (typeof jwplayer === 'function') {
                            var jp = jwplayer();
                            if (jp && typeof jp.play === 'function') {
                                var st = jp.getState();
                                if (st === 'playing') { jp.pause(); } else { jp.play(); }
                            }
                        }
                    } catch(e) {}

                    try {
                        var v = document.querySelector('video');
                        if (v) {
                            v.muted = false;
                            if (v.paused) {
                                var p = v.play();
                                if (p !== undefined) {
                                    p.catch(function() {
                                        v.muted = true;
                                        v.play().catch(function(){});
                                    });
                                }
                            } else {
                                v.pause();
                            }
                        }
                    } catch(e){}

                    try {
                        var el = document.elementFromPoint(window.innerWidth / 2, window.innerHeight / 2);
                        if (el) {
                            el.click();
                            try {
                                var ev = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
                                el.dispatchEvent(ev);
                            } catch(e) {}
                        }
                    } catch(e) {}

                    try {
                        var iframes = document.querySelectorAll('iframe');
                        for (var i = 0; i < iframes.length; i++) {
                            try {
                                var idoc = iframes[i].contentDocument || iframes[i].contentWindow.document;
                                if (idoc) {
                                    var iel = idoc.elementFromPoint(idoc.defaultView.innerWidth / 2, idoc.defaultView.innerHeight / 2);
                                    if (iel) { iel.click(); }
                                    var ivp = idoc.getElementById('vid_play');
                                    if (ivp) { try { ivp.click(); } catch(e){} }
                                    var ivids = idoc.querySelectorAll('video');
                                    for (var j = 0; j < ivids.length; j++) {
                                        if (ivids[j].paused) ivids[j].play().catch(function(){}); else ivids[j].pause();
                                    }
                                }
                            } catch(e) {}
                        }
                    } catch(e) {}

                    var selectors = [
                        '#vid_play', '#desk', '.jw-display-icon', '.vjs-big-play-button', '.play-btn', '.play-icon',
                        '#play', '.playbutton', '.btn-play', '.play_btn', '.plyr__control--overlaid',
                        '.click-to-play', '#center-play-btn', '.fp-play', '.vjs-play-control',
                        '.play-overlay', '#play-btn', '.click-play', '.poster', '.vjs-poster'
                    ];
                    for (var s = 0; s < selectors.length; s++) {
                        var els = document.querySelectorAll(selectors[s]);
                        for (var j = 0; j < els.length; j++) {
                            try { els[j].click(); } catch(err){}
                        }
                    }
                },
                seekForward: function(sec) {
                    var s = sec || 10;
                    try {
                        if (typeof jwplayer === 'function') {
                            var jp = jwplayer();
                            if (jp && typeof jp.seek === 'function' && typeof jp.getPosition === 'function') {
                                jp.seek(jp.getPosition() + s);
                                return;
                            }
                        }
                    } catch(e) {}
                    try {
                        var v = document.querySelector('video');
                        if (v) { v.currentTime += s; return; }
                    } catch(e) {}
                    try {
                        var iframes = document.querySelectorAll('iframe');
                        for (var i = 0; i < iframes.length; i++) {
                            try {
                                var idoc = iframes[i].contentDocument || iframes[i].contentWindow.document;
                                if (idoc) {
                                    var ivids = idoc.querySelectorAll('video');
                                    for (var j = 0; j < ivids.length; j++) { ivids[j].currentTime += s; }
                                }
                            } catch(e) {}
                        }
                    } catch(e) {}
                },
                seekBackward: function(sec) {
                    var s = sec || 10;
                    try {
                        if (typeof jwplayer === 'function') {
                            var jp = jwplayer();
                            if (jp && typeof jp.seek === 'function' && typeof jp.getPosition === 'function') {
                                var pos = Math.max(0, jp.getPosition() - s);
                                jp.seek(pos);
                                return;
                            }
                        }
                    } catch(e) {}
                    try {
                        var v = document.querySelector('video');
                        if (v) { v.currentTime = Math.max(0, v.currentTime - s); return; }
                    } catch(e) {}
                    try {
                        var iframes = document.querySelectorAll('iframe');
                        for (var i = 0; i < iframes.length; i++) {
                            try {
                                var idoc = iframes[i].contentDocument || iframes[i].contentWindow.document;
                                if (idoc) {
                                    var ivids = idoc.querySelectorAll('video');
                                    for (var j = 0; j < ivids.length; j++) { ivids[j].currentTime = Math.max(0, ivids[j].currentTime - s); }
                                }
                            } catch(e) {}
                        }
                    } catch(e) {}
                }
            };
        }
    })();
""".trimIndent()


    val isAlooy = referrer.contains("alooy", ignoreCase = true) ||
            url.contains("alooy", ignoreCase = true) ||
            url.contains("ylkqwsfghrt", ignoreCase = true) ||
            url.contains("zvde", ignoreCase = true) ||
            url.contains("ujeklsj", ignoreCase = true) ||
            url.contains("workers.dev", ignoreCase = true) ||
            url.contains("work789", ignoreCase = true) ||
            url.contains("work456", ignoreCase = true) ||
            url.contains(".click", ignoreCase = true) ||
            url.contains(".site", ignoreCase = true)

    val lowerUrl = url.lowercase()
    val isDirectInitially = lowerUrl.contains(".m3u8") || lowerUrl.contains(".mp4") || lowerUrl.contains(".webm") ||
            lowerUrl.contains("127.0.0.1") || lowerUrl.contains("/stream?url=") ||
            lowerUrl.contains("workers.dev") || lowerUrl.contains("zvde") || lowerUrl.contains("ylkqwsfghrt") || lowerUrl.contains("ujeklsj") ||
            lowerUrl.contains(".click") || lowerUrl.contains(".site") || lowerUrl.contains(".online") ||
            (lowerUrl.contains(".jpg") && (lowerUrl.contains("file/") || lowerUrl.contains("upload") || lowerUrl.contains("alooy"))) ||
            Regex("""https?://(?:vid\d+|w\d+)\.""", RegexOption.IGNORE_CASE).containsMatchIn(url)
    val isReady = isDirectPlayerReady || (isWebLoaded && capturedStreamUrl == null) || forceReady

    LaunchedEffect(url) {
        try {
            delay(5000)
            forceReady = true
        } catch (e: Exception) {}
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .focusRequester(playerFocusRequester)
            .focusable()
            .onSizeChanged { size ->
                playerWidth = size.width
                playerHeight = size.height
                if (mousePosition.x == 600f && mousePosition.y == 350f && size.width > 0 && size.height > 0) {
                    mousePosition = Offset(size.width / 2f, size.height / 2f)
                }
            }
            .onPreviewKeyEvent { event ->
                if (isTv) {
                    val moveStep = 42f
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                isMouseVisible = true
                                mousePosition = Offset(
                                    mousePosition.x,
                                    (mousePosition.y - moveStep).coerceIn(10f, (playerHeight - 10).coerceAtLeast(100).toFloat())
                                )
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                isMouseVisible = true
                                mousePosition = Offset(
                                    mousePosition.x,
                                    (mousePosition.y + moveStep).coerceIn(10f, (playerHeight - 10).coerceAtLeast(100).toFloat())
                                )
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                isMouseVisible = true
                                mousePosition = Offset(
                                    (mousePosition.x - moveStep).coerceIn(10f, (playerWidth - 10).coerceAtLeast(100).toFloat()),
                                    mousePosition.y
                                )
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                isMouseVisible = true
                                mousePosition = Offset(
                                    (mousePosition.x + moveStep).coerceIn(10f, (playerWidth - 10).coerceAtLeast(100).toFloat()),
                                    mousePosition.y
                                )
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            isMouseVisible = true
                            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                isClicking = true
                            } else if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyUp) {
                                isClicking = false
                                clickAtMouse(mousePosition.x, mousePosition.y)
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
                        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyUp) {
                                clickAtMouse(mousePosition.x, mousePosition.y)
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyUp) {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                            android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                webViewRef?.evaluateJavascript("$agonTvNavJs; window.agonTvNav.seekForward(10);", null)
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                            android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                webViewRef?.evaluateJavascript("$agonTvNavJs; window.agonTvNav.seekBackward(10);", null)
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER,
                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
                            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                triggerWebViewTouchAtCenter(webViewRef)
                                webViewRef?.evaluateJavascript("$agonTvNavJs; window.agonTvNav.clickCurrent();", null)
                                true
                            }
                            else -> false
                        }
                    } else {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                            android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                            android.view.KeyEvent.KEYCODE_MEDIA_REWIND,
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER,
                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
                            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> true
                            else -> false
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    isFocusable = false
                    isFocusableInTouchMode = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    val webViewInstance = this
                    android.webkit.CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(webViewInstance, true)
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false
                        val defaultUserAgent = settings.userAgentString
                        userAgentString = defaultUserAgent.replace("; wv", "")
                    }

                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun toggleFullScreen() {
                            post {
                                onToggleFullScreen?.invoke()
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun openEpisodesList() {
                            post {
                                onToggleEpisodesList?.invoke()
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun playNextEpisode() {
                            post {
                                onPlayNextEpisode?.invoke()
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun goBack() {
                            post {
                                onBack?.invoke()
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun onDirectPlayerReady() {
                            post {
                                isDirectPlayerReady = true
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun selectServerIndex(index: Int) {
                            post {
                                onSelectServer?.invoke(index)
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun onServerError() {
                            post {
                                onServerError?.invoke()
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun onVideoCaptured(streamUrl: String) {
                            if (isAlooy || url.contains("alooy", ignoreCase = true) || streamUrl.contains("alooy", ignoreCase = true) || streamUrl.contains("ylkqwsfghrt", ignoreCase = true)) {
                                post {
                                    val safeUrl = streamUrl
                                    capturedStreamUrl = safeUrl
                                }
                            }
                        }
                    }, "AndroidNativePlayer")

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        private var customView: android.view.View? = null
                        private var customViewCallback: CustomViewCallback? = null

                        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                            return false
                        }

                        override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                            super.onShowCustomView(view, callback)
                            if (customView != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            customView = view
                            customViewCallback = callback
                            onCustomViewAttached?.invoke(view, callback)
                            
                            val activity = view?.context?.findActivity() ?: context.findActivity()
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            activity?.setImmersiveMode(true)
                            
                            val decorView = activity?.window?.decorView as? android.view.ViewGroup
                            decorView?.addView(view, android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            ))
                        }

                        override fun onHideCustomView() {
                            super.onHideCustomView()
                            val act = customView?.context?.findActivity() ?: context.findActivity()
                            val decorView = act?.window?.decorView as? android.view.ViewGroup
                            if (customView != null) {
                                decorView?.removeView(customView)
                            }
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                            onCustomViewAttached?.invoke(null, null)
                            
                            val isTv = act?.resources?.configuration?.uiMode?.and(android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
                            if (!isTv) {
                                act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                act?.setImmersiveMode(false)
                            }
                        }
                    }
                    
                    webViewClient = object : WebViewClient() {
                        private val adKeywords = listOf(
                            "adsystem", "adsterra", "exoclick", "popads", "propellerads", "onclickads",
                            "doubleclick", "google-analytics", "histats", "amung.us", "coinhive", 
                            "crypto-loot", "bitly", "shorturl", "adskeeper", "mgid", "outbrain", 
                            "taboola", "revcontent", "popunder", "juicyads", "ero-advertising", "a-doubleclick",
                            "yandex.ru", "mail.ru", "adfox", "adsafeprotected", "adnxs", "smartadserver", "openx"
                        )

                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                            val requestUrl = request?.url?.toString() ?: return false
                            if (!requestUrl.startsWith("http://") && !requestUrl.startsWith("https://")) {
                                return true
                            }
                            
                            val requestUrlLower = requestUrl.lowercase()
                            // Block anti-leech video redirect from archive.org
                            if (requestUrlLower.contains("archive.org") || requestUrlLower.contains("alooytv.com.mp4")) {
                                return true
                            }

                            for (keyword in adKeywords) {
                                if (requestUrlLower.contains(keyword)) {
                                    return true
                                }
                            }

                            // Block external ad redirections completely to keep the user in the video player screen
                            val initialUri = try { java.net.URI(url) } catch(e: Exception) { null }
                            val requestUri = try { java.net.URI(requestUrl) } catch(e: Exception) { null }
                            if (initialUri != null && requestUri != null) {
                                val initialHost = initialUri.host ?: ""
                                val requestHost = requestUri.host ?: ""
                                val isAllowedHost = initialHost.isEmpty() || requestHost.isEmpty() ||
                                        requestHost.contains("127.0.0.1") ||
                                        requestHost.contains("localhost") ||
                                        requestHost.contains(initialHost) ||
                                        requestHost.contains("alooytv") ||
                                        requestHost.contains("alooy") ||
                                        requestHost.contains("ylkqwsfghrt") ||
                                        requestHost.contains("zvde") ||
                                        requestHost.contains("ujeklsj") ||
                                        requestHost.contains("workers.dev") ||
                                        requestHost.contains("work456") ||
                                        requestHost.contains("work789") ||
                                        requestHost.contains("site") ||
                                        requestHost.contains("click") ||
                                        requestHost.contains("online") ||
                                        requestHost.contains("fitnur.com") ||
                                        requestHost.contains("3cktv") ||
                                        requestHost.contains("vidsp") ||
                                        requestHost.contains("vinovo") ||
                                        requestHost.contains("vids") ||
                                        requestHost.contains("vidaraa") ||
                                        requestHost.contains("cloudflare") ||
                                        requestHost.contains("challenges") ||
                                        requestHost.contains("jsdelivr") ||
                                        requestHost.contains("googleapis") ||
                                        requestHost.contains("gstatic") ||
                                        requestHost.contains("googlevideo") ||
                                        requestHost.contains("playmate") ||
                                        requestHost.contains("luluvdo") ||
                                        requestHost.contains("savefiles") ||
                                        requestHost.contains("bysejikuar") ||
                                        requestHost.contains("firestream") ||
                                        requestHost.contains("dhtpre") ||
                                        requestHost.contains("voe") ||
                                        requestHost.contains("upzur") ||
                                        requestHost.contains("streamtape") ||
                                        requestHost.contains("ok.ru") ||
                                        requestHost.contains("uqload") ||
                                        requestHost.contains("dood") ||
                                        requestHost.contains("mp4upload")
                                
                                if (!isAllowedHost) {
                                    return true // Block ad redirect
                                }
                            }
                            return false 
                        }

                        override fun shouldInterceptRequest(view: WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                            val requestUrl = request?.url?.toString() ?: return null
                            val requestUrlLower = requestUrl.lowercase()

                            // Block anti-leech redirect video from archive.org
                            if (requestUrlLower.contains("archive.org") || requestUrlLower.contains("alooytv.com.mp4")) {
                                return android.webkit.WebResourceResponse("video/mp4", null, 404, "Not Found", null, null)
                            }

                            // Cloudflare Workers streams run directly without interception
                            if (requestUrlLower.contains("workers.dev")) {
                                return super.shouldInterceptRequest(view, request)
                            }

                            for (keyword in adKeywords) {
                                if (requestUrlLower.contains(keyword)) {
                                    return android.webkit.WebResourceResponse("text/plain", "UTF-8", null)
                                }
                            }

                            // Intercept Alooytv CDN video requests to inject official Referer, preventing anti-leech redirect
                            val isAlooyCdn = isAlooy ||
                                    requestUrlLower.contains("ylkqwsfghrt") ||
                                    requestUrlLower.contains("ujeklsj") ||
                                    requestUrlLower.contains("zvde") ||
                                    requestUrlLower.contains("alooy") ||
                                    requestUrlLower.contains(".click/") ||
                                    requestUrlLower.contains(".site/") ||
                                    requestUrlLower.contains(".online/") ||
                                    requestUrlLower.contains("/wp-alooytv/") ||
                                    Regex("""https?://(?:vid\d+|w\d+)\.""", RegexOption.IGNORE_CASE).containsMatchIn(requestUrl)

                            if (isAlooyCdn && !requestUrlLower.startsWith("http://127.0.0.1") && !requestUrlLower.contains("/stream?url=")) {
                                try {
                                    val urlObj = java.net.URL(requestUrl)
                                    val conn = urlObj.openConnection() as java.net.HttpURLConnection
                                    conn.instanceFollowRedirects = false
                                    conn.connectTimeout = 15000
                                    conn.readTimeout = 30000
                                    conn.requestMethod = request?.method ?: "GET"

                                    request?.requestHeaders?.forEach { (key, value) ->
                                        if (!key.equals("Referer", ignoreCase = true) && !key.equals("Origin", ignoreCase = true)) {
                                            conn.setRequestProperty(key, value)
                                        }
                                    }

                                    val rangeHeader = request?.requestHeaders?.entries?.firstOrNull { it.key.equals("range", ignoreCase = true) }?.value
                                    if (!rangeHeader.isNullOrBlank()) {
                                        conn.setRequestProperty("Range", rangeHeader)
                                    }

                                    // Inject official Alooytv Referer and Origin
                                    conn.setRequestProperty("Referer", "https://ep.alooytv16.xyz/")
                                    conn.setRequestProperty("Origin", "https://ep.alooytv16.xyz")
                                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                                    conn.setRequestProperty("Accept", "*/*")

                                    val code = conn.responseCode
                                    if (code in 300..399) {
                                        val location = conn.getHeaderField("Location")
                                        if (location != null && (location.contains("archive.org") || location.contains("alooytv.com.mp4"))) {
                                            return android.webkit.WebResourceResponse("video/mp4", null, 404, "Not Found", null, null)
                                        }
                                    }

                                    if (code == 200 || code == 206) {
                                        val respHeaders = HashMap<String, String>()
                                        conn.headerFields?.forEach { (k, vList) ->
                                            if (k != null && vList != null && vList.isNotEmpty()) {
                                                respHeaders[k] = vList.first()
                                            }
                                        }
                                        respHeaders["Access-Control-Allow-Origin"] = "*"
                                        respHeaders["Access-Control-Allow-Methods"] = "GET, HEAD, OPTIONS"
                                        respHeaders["Access-Control-Allow-Headers"] = "Range, Origin, Accept"
                                        respHeaders["Content-Type"] = "video/mp4"

                                        val mime = "video/mp4"
                                        val msg = if (code == 206) "Partial Content" else "OK"
                                        return android.webkit.WebResourceResponse(
                                            mime,
                                            null,
                                            code,
                                            msg,
                                            respHeaders,
                                            conn.inputStream
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            view?.evaluateJavascript("if (typeof autoPassEmbed === 'function') { autoPassEmbed(); }", null)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            val curUrl = (url ?: "").lowercase()
                            val isAlooy = curUrl.contains("alooytv")
                            val is3ck = curUrl.contains("3cktv")

                            val earlyCss = if (isAlooy) {
                                """
                                    header, footer, nav, sidebar, .header, .footer, .navbar, .sidebar, 
                                    .related-posts, .comments, #comments, .ads, .advertisement,
                                    .social-share, .breadcrumb, #disqus_thread, h1, h2, h3,
                                    .download-meta, .download-rating, .fulltext, .block-tabs, .footer-main, .footer-bottom {
                                        display: none !important;
                                    }
                                    html, body {
                                        background-color: #000 !important;
                                        background: #000 !important;
                                        overflow: hidden !important;
                                        display: flex !important;
                                        align-items: center !important;
                                        justify-content: center !important;
                                        margin: 0 !important;
                                        padding: 0 !important;
                                        width: 100% !important;
                                        height: 100% !important;
                                    }
                                    #alooy-player, .video-container, .player-container, #player-wrapper {
                                        width: 100% !important;
                                        height: 100% !important;
                                        display: flex !important;
                                        align-items: center !important;
                                        justify-content: center !important;
                                        margin: auto !important;
                                    }
                                    video {
                                        width: 100% !important;
                                        height: 100% !important;
                                        max-width: 100% !important;
                                        max-height: 100% !important;
                                        object-fit: contain !important;
                                        object-position: center center !important;
                                        margin: auto !important;
                                    }
                                """.trimIndent()
                            } else if (is3ck) {
                                """
                                    header, footer, nav, #header, #footer, #nav, #sidebar, .header, .footer, .navbar,
                                    .singleـwrapper, .secTitle:not(.secWatch), .related-posts, .comments, #comments, .ads, .advertisement,
                                    .breadcrumb, #disqus_thread, .logo, .menu, .search-box, .top-bar, .bottom-bar {
                                        display: none !important;
                                    }
                                    html, body {
                                        background-color: #000 !important;
                                        background: #000 !important;
                                        margin: 0 !important;
                                        padding: 0 !important;
                                        width: 100% !important;
                                        min-height: 100% !important;
                                        color: #fff !important;
                                    }
                                    .sec-line, .servContent, .singleInfo, .postEmbed {
                                        display: block !important;
                                        width: 100% !important;
                                        background: #000 !important;
                                        padding: 0 !important;
                                        margin: 0 !important;
                                    }
                                    .postEmbed iframe, .singleInfo iframe, iframe {
                                        display: block !important;
                                        width: 100% !important;
                                        height: 100% !important;
                                        min-height: 280px !important;
                                        border: none !important;
                                    }
                                """.trimIndent()
                            } else {
                                """
                                    header, footer, nav, #header, #footer, #nav, #sidebar, .header, .footer, .navbar,
                                    .ads, .advertisement, .related-posts, .comments, #comments, .social-share, .breadcrumb {
                                        display: none !important;
                                    }
                                    html, body {
                                        background-color: #000 !important;
                                        background: #000 !important;
                                        margin: 0 !important;
                                        padding: 0 !important;
                                        width: 100% !important;
                                        height: 100% !important;
                                    }
                                    video, iframe, embed, object {
                                        width: 100% !important;
                                        height: 100% !important;
                                        border: none !important;
                                    }
                                """.trimIndent()
                            }

                            val earlyJs = """
                                (function() {
                                    try {
                                        var style = document.createElement('style');
                                        style.id = 'agon-hide-style';
                                        style.innerHTML = `$earlyCss`;
                                        if (document.head) document.head.appendChild(style);
                                    } catch(e) {}
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(earlyJs, null)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (capturedStreamUrl != null) {
                                view?.postDelayed({
                                    isDirectPlayerReady = true
                                }, 300)
                            } else {
                                val curUrl = (url ?: "").lowercase()
                                val isAlooy = curUrl.contains("alooytv")
                                val is3ck = curUrl.contains("3cktv")

                                val js = if (isAlooy) {
                                    """
                                    (function() {
                                        if (window.isInternalAgonPlayer || (document.getElementById('playerContainer') && document.querySelector('.btn-play-pause'))) {
                                            return;
                                        }
                                        window.open = function() { return null; };
                                        try {
                                            var style = document.createElement('style');
                                            style.innerHTML = `
                                                header, footer, nav, sidebar, .header, .footer, .navbar, .sidebar, 
                                                .related-posts, .comments, #comments, .ads, .advertisement,
                                                .social-share, .breadcrumb, #disqus_thread, h1, h2, h3,
                                                .download-meta, .download-rating, .fulltext, .block-tabs, .footer-main, .footer-bottom,
                                                .logo, .menu, .search-box, .top-bar, .bottom-bar, .side-menu,
                                                #header, #footer, #nav, #sidebar {
                                                    display: none !important;
                                                    opacity: 0 !important;
                                                    visibility: hidden !important;
                                                    height: 0 !important;
                                                    overflow: hidden !important;
                                                }
                                                html, body {
                                                    background-color: #000 !important;
                                                    background: #000 !important;
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    width: 100% !important;
                                                    height: 100% !important;
                                                    display: flex !important;
                                                    align-items: center !important;
                                                    justify-content: center !important;
                                                    overflow: hidden !important;
                                                }
                                                .video-container, .player-container, #player-wrapper, .video-player-wrapper {
                                                    position: relative !important;
                                                    width: 100% !important;
                                                    height: 100% !important;
                                                    display: flex !important;
                                                    align-items: center !important;
                                                    justify-content: center !important;
                                                    z-index: 999999 !important;
                                                    background: #000 !important;
                                                    margin: auto !important;
                                                }
                                                video, iframe, embed, object, #player, .video-player, #iframe, .jwplayer, #vplayer, .preview, #alooy-player {
                                                    width: 100% !important;
                                                    height: 100% !important;
                                                    max-width: 100% !important;
                                                    max-height: 100% !important;
                                                    object-fit: contain !important;
                                                    object-position: center center !important;
                                                    background: #000 !important;
                                                    border: none !important;
                                                    margin: auto !important;
                                                }
                                                #vid_play, button#vid_play, .jw-display-icon, .vjs-big-play-button, .play-btn, .play-icon, #play, .playbutton, .btn-play, .play_btn, .plyr__control--overlaid, .click-to-play, #center-play-btn, .fp-play, .vjs-play-control, .play-overlay, #play-btn, .click-play {
                                                    border: 4px solid #E50914 !important;
                                                    border-radius: 50% !important;
                                                    box-shadow: 0 0 30px #E50914, 0 0 12px #FF3333 !important;
                                                    outline: 2.5px solid #FFFFFF !important;
                                                    outline-offset: 3px !important;
                                                    background-color: rgba(229, 9, 20, 0.4) !important;
                                                }
                                            `;
                                            if (document.head) document.head.appendChild(style);
                                        } catch(e) {}

                                        function passEmbedInDoc(doc) {
                                            if (!doc) return;
                                            try {
                                                var vp = doc.getElementById('vid_play');
                                                var desk = doc.getElementById('desk');
                                                var f1 = doc.forms['F1'] || doc.getElementById('F1');

                                                if (f1 && !f1._agonSubmitted) {
                                                    f1._agonSubmitted = true;
                                                    var fcInput = f1.file_code || doc.querySelector("input[name='file_code']");
                                                    if (fcInput && (!fcInput.value || fcInput.value.trim().length < 2)) {
                                                        var path = (doc.location && doc.location.pathname) || window.location.pathname || '';
                                                        var code = path.replace('.html','').split("/").pop().split("-").pop();
                                                        if (code) { fcInput.value = code; }
                                                    }
                                                    var refInput = f1.referer || doc.querySelector("input[name='referer']");
                                                    if (refInput && !refInput.value) {
                                                        refInput.value = doc.referrer || (doc.location && doc.location.href) || window.location.href;
                                                    }
                                                    if (vp) { try { vp.click(); } catch(e){} }
                                                    if (desk) { try { desk.click(); } catch(e){} }
                                                    if (typeof f1.submit === 'function') {
                                                        try { f1.submit(); } catch(e){}
                                                    }
                                                } else if (vp && !vp._agonClicked && !f1) {
                                                    vp._agonClicked = true;
                                                    try { vp.click(); } catch(e){}
                                                }
                                            } catch(e){}
                                        }

                                        function autoPassEmbed() {
                                            passEmbedInDoc(document);
                                            try {
                                                var iframes = document.querySelectorAll('iframe');
                                                for (var i = 0; i < iframes.length; i++) {
                                                    try {
                                                        var idoc = iframes[i].contentDocument || iframes[i].contentWindow.document;
                                                        if (idoc) { passEmbedInDoc(idoc); }
                                                    } catch(e) {}
                                                }
                                            } catch(e) {}
                                        }

                                        function autoPlayAll() {
                                            autoPassEmbed();
                                            try {
                                                var vids = document.querySelectorAll('video');
                                                for (var i = 0; i < vids.length; i++) {
                                                    var v = vids[i];
                                                    v.muted = false;
                                                    if (v.paused) {
                                                        var p = v.play();
                                                        if (p !== undefined) {
                                                            p.catch(function() {
                                                                v.muted = true;
                                                                v.play().catch(function(){});
                                                            });
                                                        }
                                                    }
                                                    var s = v.currentSrc || v.src;
                                                    if (s && !s.startsWith("blob:") && (s.indexOf('.m3u8') !== -1 || s.indexOf('.mp4') !== -1 || s.indexOf('.webm') !== -1 || s.indexOf('zvde') !== -1 || s.indexOf('workers.dev') !== -1 || (s.indexOf('.jpg') !== -1 && s.indexOf('file/') !== -1))) {
                                                        if (window.AndroidNativePlayer && window.AndroidNativePlayer.onVideoCaptured) {
                                                            window.AndroidNativePlayer.onVideoCaptured(s);
                                                        }
                                                    }
                                                }

                                                var selectors = [
                                                    '.jw-display-icon', '.vjs-big-play-button', '.play-btn', '.play-icon',
                                                    '#play', '.playbutton', '.btn-play', '.play_btn', '.fp-play',
                                                    '.vjs-play-control', '.play-trigger', '.plyr__control--overlaid',
                                                    '.play-overlay', '#play-btn', '.click-play'
                                                ];
                                                for (var s = 0; s < selectors.length; s++) {
                                                    var els = document.querySelectorAll(selectors[s]);
                                                    for (var e = 0; e < els.length; e++) {
                                                        try { els[e].click(); } catch(err){}
                                                    }
                                                }
                                            } catch(err) {}
                                        }

                                        autoPlayAll();
                                        setInterval(autoPlayAll, 500);
                                    })();
                                    """.trimIndent()
                                } else if (is3ck) {
                                    """
                                    (function() {
                                        window.open = function() { return null; };
                                        try {
                                            var style = document.createElement('style');
                                            style.innerHTML = `
                                                header, footer, nav, #header, #footer, #nav, #sidebar, .header, .footer, .navbar,
                                                .singleـwrapper, .secTitle:not(.secWatch), .related-posts, .comments, #comments, .ads, .advertisement,
                                                .breadcrumb, #disqus_thread, .logo, .menu, .search-box, .top-bar, .bottom-bar {
                                                    display: none !important;
                                                    opacity: 0 !important;
                                                    visibility: hidden !important;
                                                    height: 0 !important;
                                                    overflow: hidden !important;
                                                }
                                                html, body {
                                                    background-color: #000 !important;
                                                    background: #000 !important;
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    width: 100% !important;
                                                    min-height: 100% !important;
                                                    color: #fff !important;
                                                }
                                                .sec-line, .servContent, .singleInfo, .postEmbed {
                                                    display: block !important;
                                                    width: 100% !important;
                                                    background: #000 !important;
                                                    padding: 0 !important;
                                                    margin: 0 !important;
                                                }
                                                .postEmbed iframe, .singleInfo iframe, iframe {
                                                    display: block !important;
                                                    width: 100% !important;
                                                    height: 100% !important;
                                                    min-height: 280px !important;
                                                    border: none !important;
                                                }
                                                .streamNames, .servList {
                                                    display: flex !important;
                                                    flex-wrap: wrap !important;
                                                    gap: 8px !important;
                                                    padding: 10px !important;
                                                    background: #141414 !important;
                                                    justify-content: center !important;
                                                }
                                                .servList li {
                                                    display: inline-flex !important;
                                                    align-items: center !important;
                                                    background: #262626 !important;
                                                    color: #fff !important;
                                                    padding: 6px 14px !important;
                                                    border-radius: 8px !important;
                                                    cursor: pointer !important;
                                                    font-size: 13px !important;
                                                    border: 1px solid #333 !important;
                                                }
                                                .servList li.active {
                                                    background: #E50914 !important;
                                                    border-color: #E50914 !important;
                                                    color: #fff !important;
                                                    font-weight: bold !important;
                                                }
                                            `;
                                            if (document.head) document.head.appendChild(style);
                                        } catch(e) {}

                                        document.addEventListener('click', function() {
                                            var vids = document.querySelectorAll('video');
                                            for (var i = 0; i < vids.length; i++) {
                                                if (vids[i].muted) {
                                                    vids[i].muted = false;
                                                }
                                            }
                                        }, { once: true });

                                        function autoPlay3ck() {
                                            try {
                                                var vids = document.querySelectorAll('video');
                                                for (var i = 0; i < vids.length; i++) {
                                                    var v = vids[i];
                                                    if (v.paused && !v.hasAutoPlayed) {
                                                        v.hasAutoPlayed = true;
                                                        var p = v.play();
                                                        if (p !== undefined) {
                                                            p.catch(function() {
                                                                v.muted = true;
                                                                v.play().catch(function(){});
                                                            });
                                                        }
                                                    }
                                                    var s = v.currentSrc || v.src;
                                                    if (s && !s.startsWith("blob:") && (s.indexOf('.m3u8') !== -1 || s.indexOf('.mp4') !== -1)) {
                                                        if (window.AndroidNativePlayer && window.AndroidNativePlayer.onVideoCaptured) {
                                                            window.AndroidNativePlayer.onVideoCaptured(s);
                                                        }
                                                    }
                                                }
                                            } catch(e) {}
                                        }

                                        autoPlay3ck();
                                        setInterval(autoPlay3ck, 1000);
                                    })();
                                    """.trimIndent()
                                } else {
                                    """
                                    (function() {
                                        window.open = function() { return null; };
                                        try {
                                            var style = document.createElement('style');
                                            style.innerHTML = `
                                                header, footer, nav, #header, #footer, #nav, #sidebar, .header, .footer, .navbar,
                                                .ads, .advertisement, .related-posts, .comments, #comments, .social-share, .breadcrumb {
                                                    display: none !important;
                                                    opacity: 0 !important;
                                                    visibility: hidden !important;
                                                    height: 0 !important;
                                                }
                                                html, body {
                                                    background-color: #000 !important;
                                                    background: #000 !important;
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    width: 100% !important;
                                                    height: 100% !important;
                                                }
                                                video, iframe, embed, object, #player, .video-player, #iframe, .jwplayer, #vplayer {
                                                    width: 100% !important;
                                                    height: 100% !important;
                                                    border: none !important;
                                                }
                                            `;
                                            if (document.head) document.head.appendChild(style);
                                        } catch(e) {}

                                        document.addEventListener('click', function() {
                                            var vids = document.querySelectorAll('video');
                                            for (var i = 0; i < vids.length; i++) {
                                                if (vids[i].muted) {
                                                    vids[i].muted = false;
                                                }
                                            }
                                        }, { once: true });

                                        function autoPlayDirect() {
                                            try {
                                                var vids = document.querySelectorAll('video');
                                                for (var i = 0; i < vids.length; i++) {
                                                    var v = vids[i];
                                                    if (v.paused && !v.hasAutoPlayed) {
                                                        v.hasAutoPlayed = true;
                                                        var p = v.play();
                                                        if (p !== undefined) {
                                                            p.catch(function() {
                                                                v.muted = true;
                                                                v.play().catch(function(){});
                                                            });
                                                        }
                                                    }
                                                    var s = v.currentSrc || v.src;
                                                    if (s && !s.startsWith("blob:") && (s.indexOf('.m3u8') !== -1 || s.indexOf('.mp4') !== -1)) {
                                                        if (window.AndroidNativePlayer && window.AndroidNativePlayer.onVideoCaptured) {
                                                            window.AndroidNativePlayer.onVideoCaptured(s);
                                                        }
                                                    }
                                                }
                                                if (!window.hasClickedPlayBtn) {
                                                    var selectors = [
                                                        '.jw-display-icon', '.vjs-big-play-button', '.play-btn', '.play-icon',
                                                        '#play', '.playbutton', '.btn-play', '.play_btn', '.fp-play',
                                                        '.vjs-play-control', '.play-trigger', '.plyr__control--overlaid'
                                                    ];
                                                    for (var s = 0; s < selectors.length; s++) {
                                                        var els = document.querySelectorAll(selectors[s]);
                                                        for (var e = 0; e < els.length; e++) {
                                                            try { els[e].click(); window.hasClickedPlayBtn = true; } catch(err){}
                                                        }
                                                    }
                                                }
                                            } catch(e) {}
                                        }

                                        autoPlayDirect();
                                        setInterval(autoPlayDirect, 1000);
                                    })();
                                    """.trimIndent()
                                }

                                view?.evaluateJavascript(js, null)

                                view?.postDelayed({
                                    isWebLoaded = true
                                }, 500)
                            }
                        }
                    }
                }
            },
            update = { webView ->
                val targetUrl = capturedStreamUrl ?: url
                if (webView.tag != targetUrl) {
                    webView.tag = targetUrl
                    val targetLower = targetUrl.lowercase()
                    val isDirectStream = targetLower.contains(".m3u8") || 
                        targetLower.contains(".mp4") || 
                        targetLower.contains(".webm") ||
                        targetLower.contains("127.0.0.1") ||
                        targetLower.contains("stream?url=") ||
                        targetLower.contains("workers.dev") ||
                        targetLower.contains("work789") ||
                        targetLower.contains("work456") ||
                        targetLower.contains("zvde") ||
                        targetLower.contains("ylkqwsfghrt") ||
                        targetLower.contains("ujeklsj") ||
                        targetLower.contains(".click") ||
                        targetLower.contains(".site") ||
                        targetLower.contains(".online") ||
                        (targetLower.contains(".jpg") && (targetLower.contains("file/") || targetLower.contains("upload") || targetLower.contains("alooy"))) ||
                        Regex("""https?://(?:vid\d+|w\d+)\.""", RegexOption.IGNORE_CASE).containsMatchIn(targetUrl) ||
                        targetUrl == capturedStreamUrl
                    if (isDirectStream) {
                        val playerHtml = buildProfessionalPlayerHtml(
                            streamUrl = targetUrl,
                            title = title,
                            servers = servers,
                            currentServerIndex = currentServerIndex
                        )
                        val baseUrl = if (isAlooy || targetLower.contains("alooytv") || url.contains("alooytv") || targetLower.contains("zvde") || targetLower.contains("ylkqwsfghrt") || targetLower.contains("ujeklsj") || targetLower.contains("workers.dev") || targetLower.contains(".click") || targetLower.contains(".site") || targetLower.contains("work789") || targetLower.contains("work456")) {
                            "https://ep.alooytv16.xyz/"
                        } else {
                            "https://w6.qrmzi.site/"
                        }
                        webView.loadDataWithBaseURL(baseUrl, playerHtml, "text/html", "UTF-8", baseUrl)
                    } else {
                        val headers = HashMap<String, String>()
                        if (isAlooy || url.contains("alooytv") || url.contains("alooy")) {
                            headers["Referer"] = "https://ep.alooytv16.xyz/"
                            headers["Origin"] = "https://ep.alooytv16.xyz"
                        } else if (url.contains("vidsp.net") || url.contains("vinovo")) {
                            headers["Referer"] = "https://e.3cktv.cam/"
                        } else if (!url.contains("3cktv") && referrer.isNotEmpty()) {
                            headers["Referer"] = referrer
                        }
                        webView.loadUrl(url, headers)
                    }
                }
                webViewRef = webView
            },
            onRelease = { webView ->
                try {
                    webView.evaluateJavascript("try { var v = document.querySelectorAll('video, audio'); for(var i=0; i<v.length; i++){ v[i].pause(); v[i].src=''; v[i].load(); } } catch(e){}", null)
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.onPause()
                    webView.pauseTimers()
                    webView.destroy()
                } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading Overlay while WebView initializes stream (Prevents showing source website content)
        androidx.compose.animation.AnimatedVisibility(
            visible = !isWebLoaded && !isDirectPlayerReady && !forceReady,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        color = NetflixRed,
                        strokeWidth = 3.5.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "جاري تجهيز الحلقة... 🎬",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "لحظات ويبدأ التشغيل تلقائياً ✨",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // TV Remote Virtual Mouse Pointer (Visible & Controllable ONLY on TV)
        if (isTv && isMouseVisible) {
            // Click Ripple Feedback
            if (isClicking) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((mousePosition.x - 22).toInt(), (mousePosition.y - 22).toInt()) }
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE50914).copy(alpha = 0.5f))
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            // High-Contrast Glowing Mouse Arrow Pointer
            Box(
                modifier = Modifier
                    .offset { IntOffset(mousePosition.x.toInt(), mousePosition.y.toInt()) }
                    .size(38.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(0f, 28f)
                        lineTo(7.5f, 20.5f)
                        lineTo(16f, 31f)
                        lineTo(20f, 28f)
                        lineTo(12.5f, 17.5f)
                        lineTo(23f, 17.5f)
                        close()
                    }
                    // Outer dark drop-shadow for maximum contrast on both bright and dark content
                    drawPath(path, color = Color.Black.copy(alpha = 0.7f), style = Stroke(width = 6.5f, join = StrokeJoin.Round))
                    // Crisp outline
                    drawPath(path, color = Color.Black, style = Stroke(width = 3.5f, join = StrokeJoin.Round))
                    // Fill: Red accent on click, pure white on idle
                    drawPath(path, color = if (isClicking) Color(0xFFE50914) else Color.White)
                }
            }

            // TV Remote Mouse Navigation Indicator Badge (Fades out automatically after 4.5 seconds)
            androidx.compose.animation.AnimatedVisibility(
                visible = showMouseHint,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141414).copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🖱️ مؤشر التحكم: الأسهم للتحريك | OK للنقر",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}@Composable
private fun PlayerServersSection(vm: AppViewModel, currentEpisode: Episode?) {
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedServer by vm.selectedServer.collectAsStateWithLifecycle()

    if (servers.size > 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212))
                .padding(vertical = 10.dp)
        ) {
            Text(
                text = "سيرفرات المشاهدة:",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(servers) { index, server ->
                    val isSelected = selectedServer?.url == server.url
                    val serverDisplayName = "سيرفر ${index + 1}"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NetflixRed else Color(0xFF222226))
                            .tvClickable(shape = RoundedCornerShape(6.dp)) {
                                currentEpisode?.let { ep ->
                                    vm.selectServer(server, ep.link)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = serverDisplayName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerBottomContent(
    show: Show,
    episodes: List<Episode>,
    currentEpisode: Episode?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onEpisodeSelected: (Episode) -> Unit,
    vm: AppViewModel,
    onOpenShow: (String) -> Unit
) {
    val tabs = if (!show.isMovie && episodes.isNotEmpty()) listOf("الحلقات", "معلومات", "أعمال مشابهة") else listOf("معلومات", "أعمال مشابهة")
    val currentTab = tabs.getOrNull(selectedTab.coerceIn(0, tabs.lastIndex)) ?: "معلومات"

    val availableSeasons = remember(episodes) {
        episodes.map { it.seasonNumber }.distinct().sorted()
    }
    var selectedSeasonNumber by remember(episodes, currentEpisode) {
        mutableIntStateOf(
            currentEpisode?.seasonNumber ?: availableSeasons.firstOrNull() ?: 1
        )
    }
    val filteredEpisodes = remember(episodes, selectedSeasonNumber) {
        if (availableSeasons.size > 1) {
            episodes.filter { it.seasonNumber == selectedSeasonNumber }
        } else {
            episodes
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            ShowInfoSection(show = show)
        }
        
        item {
            TabRowSection(
                tabs = tabs,
                selectedTab = selectedTab.coerceIn(0, tabs.lastIndex),
                onTabSelected = onTabSelected
            )
        }

        when (currentTab) {
            "الحلقات" -> {
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val isAlooy = show.link.contains("alooytv", ignoreCase = true) ||
                            show.id.contains("alooy", ignoreCase = true) ||
                            show.genres.any { it.contains("alooy", ignoreCase = true) }

                    val activeEp = currentEpisode ?: episodes.firstOrNull()
                    val activeEpId = activeEp?.id ?: show.id
                    val isDownloaded = com.agon.app.data.EpisodeDownloaderManager.isEpisodeDownloaded(activeEpId, show.id)
                    val progresses by com.agon.app.data.EpisodeDownloaderManager.downloadProgresses.collectAsState()
                    val progressPct = progresses[activeEpId]

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Only show in-app download button for AlooyTV content
                        if (isAlooy) {
                            if (progressPct != null && progressPct in 0..99) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E1E24))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "جاري التحميل للمشاهدة بدون إنترنت...",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$progressPct%",
                                            color = NetflixRed,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = (progressPct ?: 0) / 100f,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = NetflixRed,
                                        trackColor = Color(0xFF383D42)
                                    )
                                }
                            } else if (isDownloaded) {
                                Button(
                                    onClick = {
                                        android.widget.Toast.makeText(context, "الحلقة محملة وتعمل بدون إنترنت", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "تم التحميل",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تم التحميل - مشاهدة بدون نت",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val epToDownload = activeEp
                                        val videoToDownload = vm.videoUrl.value ?: epToDownload?.link ?: show.link
                                        if (epToDownload != null && videoToDownload.isNotEmpty()) {
                                            com.agon.app.data.EpisodeDownloaderManager.startDownload(
                                                context = context,
                                                showId = show.id,
                                                showTitle = show.title,
                                                episodeId = epToDownload.id,
                                                episodeTitle = epToDownload.title,
                                                episodeNum = epToDownload.episodeNum,
                                                poster = epToDownload.poster.ifEmpty { show.poster },
                                                videoUrl = videoToDownload
                                            )
                                            android.widget.Toast.makeText(context, "بدأ التنزيل! لمشاهدتها اذهب إلى قائمة 'حلقات محملة' في الشريط السفلي.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE50914),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = "تنزيل الحلقة",
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تنزيل الحلقة للمشاهدة دون إنترنت",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (availableSeasons.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "اختر الموسم:",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2A2A2E))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${availableSeasons.size} مواسم",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(end = 16.dp)
                            ) {
                                items(availableSeasons) { seasonNum ->
                                    val isSelected = seasonNum == selectedSeasonNumber
                                    Surface(
                                        onClick = { selectedSeasonNumber = seasonNum },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) NetflixRed else Color(0xFF1E1E24),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) NetflixRed else Color.White.copy(alpha = 0.1f)
                                        ),
                                        tonalElevation = if (isSelected) 8.dp else 0.dp,
                                        modifier = Modifier.tvClickable(shape = RoundedCornerShape(10.dp)) {
                                            selectedSeasonNumber = seasonNum
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "الموسم $seasonNum",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                            Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 18.dp))
                        }

                        Text(
                            text = if (availableSeasons.size > 1) "حلقات الموسم $selectedSeasonNumber (${filteredEpisodes.size})" else "قائمة الحلقات (${filteredEpisodes.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(filteredEpisodes, key = { it.id }) { ep ->
                    val isCurrent = currentEpisode?.id == ep.id
                    val posterUrl = ep.poster.ifEmpty { show.poster }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .tvClickable(shape = RoundedCornerShape(8.dp)) { 
                                onEpisodeSelected(ep)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(76.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E24))
                                .border(
                                    width = if (isCurrent) 1.5.dp else 0.dp,
                                    color = if (isCurrent) NetflixRed else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            if (posterUrl.isNotEmpty()) {
                                coil.compose.AsyncImage(
                                    model = posterUrl,
                                    contentDescription = ep.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isCurrent) Color.Red.copy(alpha = 0.4f)
                                        else Color.Black.copy(alpha = 0.35f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(if (isCurrent) NetflixRed else Color.Black.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "الحلقة ${ep.episodeNum}",
                                    color = if (isCurrent) NetflixRed else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (ep.ribbon != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE50914).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .border(0.5.dp, Color(0xFFE50914), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = ep.ribbon,
                                            color = Color(0xFFE50914),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isCurrent) "جاري التشغيل 🎬" else "انقر للتشغيل",
                                color = if (isCurrent) NetflixRed else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            "معلومات" -> {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "قصة العمل",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = show.description,
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        
                        val currentShow = vm.shows.collectAsStateWithLifecycle().value.find { it.id == show.id } ?: show
                        
                        if (currentShow.castMembers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "طاقم العمل",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(currentShow.castMembers) { member ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(80.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1A1A1A))
                                        ) {
                                            if (member.profilePath != null) {
                                                coil.compose.AsyncImage(
                                                    model = member.profilePath,
                                                    contentDescription = member.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = Color.Gray)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = member.name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else if (show.cast.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "طاقم العمل",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = show.cast.joinToString("، "),
                                color = Color.Gray,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                        
                        LaunchedEffect(show.id) {
                            vm.loadShowDetails(show.id)
                        }
                    }
                }
            }
            "أعمال مشابهة" -> {
                item {
                    SimilarShowsSection(show = show, onOpenShow = onOpenShow)
                }
            }
        }
    }
}
