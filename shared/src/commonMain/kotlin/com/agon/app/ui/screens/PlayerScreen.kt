package com.agon.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.agon.app.data.Catalog
import com.agon.app.data.Episode
import com.agon.app.data.Show
import com.agon.app.data.VideoServer
import com.agon.app.data.ads.AdManager
import com.agon.app.player.VideoBridgeEvents
import com.agon.app.player.VideoPlayerHost
import com.agon.app.ui.components.PosterArt
import com.agon.app.ui.theme.NetflixRed
import com.agon.app.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

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
    val deviceType by vm.deviceType.collectAsStateWithLifecycle()

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
            AdManager.showTransitionAd {
                currentEpisode = nextEpisode
                vm.loadEpisodeVideo(nextEpisode)
            }
        }
    }

    DisposableEffect(Unit) {
        AdManager.isInsidePlayer = true
        onDispose {
            AdManager.isInsidePlayer = false
            AdManager.isWatchingVideo = false
            AdManager.isPlayerInteracting = false
            vm.clearEpisodeVideo()
        }
    }

    var isAdChecked by remember(show.id) { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showEpisodesList by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val playerAutoHide by vm.playerAutoHide.collectAsStateWithLifecycle()
    val playerHideSeconds by vm.playerHideSeconds.collectAsStateWithLifecycle()

    LaunchedEffect(showControls) {
        AdManager.isPlayerInteracting = showControls
    }

    LaunchedEffect(showControls, showEpisodesList, lastInteractionTime, playerAutoHide, playerHideSeconds) {
        if (playerAutoHide && showControls && !showEpisodesList) {
            delay(playerHideSeconds * 1000L)
            showControls = false
        }
    }

    LaunchedEffect(show.id) {
        vm.loadEpisodes(show)
        AdManager.isInsidePlayer = true
        // Non-blocking: show ad in parallel, kick off playback immediately
        isAdChecked = true
        val ep = currentEpisode ?: episodes.firstOrNull()
        if (ep != null) {
            currentEpisode = ep
            vm.loadEpisodeVideo(ep)
        }
        AdManager.showBeforeEpisodeAd { }
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

    LaunchedEffect(showId) {
        vm.addToHistory(showId)
    }

    val handleBack = {
        if (isFullScreen) {
            isFullScreen = false
        } else {
            AdManager.isWatchingVideo = false
            AdManager.isInsidePlayer = false
            AdManager.isPlayerInteracting = false
            AdManager.showTransitionAd { onBack() }
        }
    }

    if (!isAdChecked) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0C0D14)),
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
                Spacer(Modifier.height(20.dp))
                Text(
                    "جاري تحضير المشغل والبث... 🎬",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(show.title, color = Color.LightGray.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
        return
    }

    val isTv = deviceType == "tv"

    if (isTv && !isFullScreen) {
        Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Column(modifier = Modifier.weight(0.65f).fillMaxHeight()) {
                Box(modifier = Modifier.weight(1f)) {
                    VideoPlayerSection(
                        show = show,
                        currentEpisode = currentEpisode,
                        onBack = handleBack,
                        isFullScreen = true,
                        onToggleFullScreen = {
                            isFullScreen = !isFullScreen
                            lastInteractionTime = Clock.System.now().toEpochMilliseconds()
                        },
                        vm = vm,
                        showControls = showControls,
                        onToggleControls = {
                            showControls = !showControls
                            lastInteractionTime = Clock.System.now().toEpochMilliseconds()
                        },
                        showEpisodesList = showEpisodesList,
                        onToggleEpisodesList = {
                            showEpisodesList = !showEpisodesList
                            lastInteractionTime = Clock.System.now().toEpochMilliseconds()
                        },
                        onSelectEpisode = { ep ->
                            if (ep.id != currentEpisode?.id) {
                                AdManager.isWatchingVideo = false
                                AdManager.showTransitionAd {
                                    currentEpisode = ep
                                    showEpisodesList = false
                                    lastInteractionTime = Clock.System.now().toEpochMilliseconds()
                                    vm.loadEpisodeVideo(ep)
                                }
                            } else {
                                showEpisodesList = false
                            }
                        },
                        onPlayNextEpisode = handlePlayNextEpisode
                    )
                }
                PlayerServersSection(vm = vm, currentEpisode = currentEpisode)
            }
            Column(modifier = Modifier.weight(0.35f).fillMaxHeight().background(Color(0xFF121212))) {
                PlayerBottomContent(
                    show = show,
                    episodes = episodes,
                    currentEpisode = currentEpisode,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onEpisodeSelected = { ep ->
                        if (ep.id != currentEpisode?.id) {
                            AdManager.isWatchingVideo = false
                            AdManager.showTransitionAd {
                                currentEpisode = ep
                                showControls = true
                                vm.saveProgress(show.id, ep.id)
                                lastInteractionTime = Clock.System.now().toEpochMilliseconds()
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
                .background(Color.Black),
            verticalArrangement = if (isFullScreen) Arrangement.Center else Arrangement.Top
        ) {
            VideoPlayerSection(
                show = show,
                currentEpisode = currentEpisode,
                onBack = handleBack,
                isFullScreen = isFullScreen,
                onToggleFullScreen = {
                    isFullScreen = !isFullScreen
                    lastInteractionTime = Clock.System.now().toEpochMilliseconds()
                },
                vm = vm,
                showControls = showControls,
                onToggleControls = {
                    showControls = !showControls
                    lastInteractionTime = Clock.System.now().toEpochMilliseconds()
                },
                showEpisodesList = showEpisodesList,
                onToggleEpisodesList = {
                    showEpisodesList = !showEpisodesList
                    lastInteractionTime = Clock.System.now().toEpochMilliseconds()
                },
                onSelectEpisode = { ep ->
                    if (ep.id != currentEpisode?.id) {
                        AdManager.isWatchingVideo = false
                        AdManager.showTransitionAd {
                            currentEpisode = ep
                            showEpisodesList = false
                            lastInteractionTime = Clock.System.now().toEpochMilliseconds()
                            isFullScreen = false
                            vm.loadEpisodeVideo(ep)
                        }
                    } else {
                        showEpisodesList = false
                    }
                },
                onPlayNextEpisode = handlePlayNextEpisode
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
                            AdManager.showTransitionAd {
                                currentEpisode = ep
                                showControls = true
                                vm.saveProgress(show.id, ep.id)
                                lastInteractionTime = Clock.System.now().toEpochMilliseconds()
                                isFullScreen = false
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
    onPlayNextEpisode: () -> Unit
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
        // Floating back button — always available
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رجوع",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        if (!isLoadingVideo && activeUrl != null) {
            // Native playback for direct streams (mp4/m3u8/webm). Embeds stay in WebView.
            val direct = activeUrl.lowercase().let {
                it.contains(".mp4") || it.contains(".m3u8") || it.contains(".webm")
            }
            if (direct) {
                com.agon.app.player.NativeStreamPlayer(
                    url = activeUrl,
                    referrer = currentEpisode?.link ?: show.link.ifEmpty { "https://ep.alooytv16.xyz/" },
                    modifier = Modifier.fillMaxSize()
                )
                return@Box
            }
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
                onPlayNextEpisode = onPlayNextEpisode
            )
        } else {
            val backdropOrPoster = show.backdrop.ifEmpty { show.poster }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (backdropOrPoster.isNotEmpty()) {
                    PosterArt(
                        showId = show.id,
                        posterUrl = backdropOrPoster,
                        title = "",
                        showTitle = false,
                        modifier = Modifier.fillMaxSize().alpha(0.20f)
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NetflixRed, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("جاري تحميل الحلقة...", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

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
    onPlayNextEpisode: (() -> Unit)? = null
) {
    var capturedStreamUrl by remember(url) { mutableStateOf<String?>(null) }
    var forceReady by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        capturedStreamUrl = null
        forceReady = false
        delay(4000)
        forceReady = true
    }

    val isAlooy = url.contains("alooytv", ignoreCase = true) ||
        url.contains("alooy", ignoreCase = true) ||
        url.contains("ep.alooytv16.xyz", ignoreCase = true)

    val targetUrl = capturedStreamUrl ?: url
    val targetLower = targetUrl.lowercase()

    val isDirectStream = targetLower.contains(".mp4") ||
        targetLower.contains(".m3u8") ||
        targetLower.contains(".webm") ||
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

    val html: String?
    val loadedUrl: String?
    val baseUrl: String?
    val headers = mutableMapOf<String, String>()

    if (isDirectStream) {
        html = buildProfessionalPlayerHtml(
            streamUrl = targetUrl,
            title = title,
            servers = servers,
            currentServerIndex = currentServerIndex
        )
        loadedUrl = null
        baseUrl = if (
            isAlooy ||
            targetLower.contains("alooytv") ||
            url.contains("alooytv") ||
            targetLower.contains("zvde") ||
            targetLower.contains("ylkqwsfghrt") ||
            targetLower.contains("ujeklsj") ||
            targetLower.contains("workers.dev") ||
            targetLower.contains(".click") ||
            targetLower.contains(".site") ||
            targetLower.contains("work789") ||
            targetLower.contains("work456")
        ) "https://ep.alooytv16.xyz/" else "https://w6.qrmzi.site/"
    } else {
        html = null
        loadedUrl = url
        baseUrl = null
        if (isAlooy || url.contains("alooytv") || url.contains("alooy")) {
            headers["Referer"] = "https://ep.alooytv16.xyz/"
            headers["Origin"] = "https://ep.alooytv16.xyz"
        } else if (url.contains("vidsp.net") || url.contains("vinovo")) {
            headers["Referer"] = "https://e.3cktv.cam/"
        } else if (!url.contains("3cktv") && referrer.isNotEmpty()) {
            headers["Referer"] = referrer
        }
    }

    VideoPlayerHost(
        url = loadedUrl,
        html = html,
        baseUrl = baseUrl,
        headers = headers,
        userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
        modifier = modifier,
        events = VideoBridgeEvents(
            onGoBack = { onBack?.invoke() },
            onToggleFullScreen = { onToggleFullScreen?.invoke() },
            onOpenEpisodesList = { onToggleEpisodesList?.invoke() },
            onPlayNextEpisode = { onPlayNextEpisode?.invoke() },
            onDirectPlayerReady = { forceReady = true },
            onServerError = { _ -> onServerError?.invoke() },
            onSelectServerIndex = { idx -> onSelectServer?.invoke(idx) },
            onVideoCaptured = { captured, _ -> if (captured.isNotBlank()) capturedStreamUrl = captured }
        )
    )
}

@Composable
private fun PlayerServersSection(vm: AppViewModel, currentEpisode: Episode?) {
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedServer by vm.selectedServer.collectAsStateWithLifecycle()
    if (servers.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF16161C)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text("السيرفرات المتاحة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(servers) { index, server ->
                val selected = server.url == selectedServer?.url
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) NetflixRed else Color(0xFF23232B))
                        .clickable {
                            currentEpisode?.let { ep -> vm.selectServer(server, ep.link) }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "سيرفر ${index + 1}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
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
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0F))) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF16161C),
            contentColor = Color.White,
            divider = {}
        ) {
            listOf("الحلقات", "المعلومات", "مشابه").forEachIndexed { i, label ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { onTabSelected(i) },
                    text = { Text(label, fontSize = 13.sp) }
                )
            }
        }
        when (selectedTab) {
            0 -> EpisodesTab(episodes = episodes, currentEpisode = currentEpisode, onEpisodeSelected = onEpisodeSelected)
            1 -> ShowInfoSection(show = show)
            2 -> SimilarShowsSection(show = show, onOpenShow = onOpenShow)
        }
    }
}

@Composable
private fun EpisodesTab(
    episodes: List<Episode>,
    currentEpisode: Episode?,
    onEpisodeSelected: (Episode) -> Unit
) {
    if (episodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد حلقات متاحة", color = Color(0xFF8C8C94), fontSize = 13.sp)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            Text(
                "قائمة الحلقات (${episodes.size})",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        items(episodes) { ep ->
            com.agon.app.ui.components.EpisodeItemRow(
                episode = ep,
                isSelected = ep.id == currentEpisode?.id,
                onClick = { onEpisodeSelected(ep) }
            )
        }
    }
}

@Composable
private fun ShowInfoSection(show: Show) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(show.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(show.year, color = Color(0xFF8C8C94), fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Text(show.rating, color = Color(0xFF8C8C94), fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF5C518), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(2.dp))
            Text(fmt(show.imdb.toFloat()), color = Color(0xFFF5C518), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        if (show.genres.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(show.genres) { g ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF23232B))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(g, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(show.description, color = Color(0xFFB3B3B3), fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun SimilarShowsSection(show: Show, onOpenShow: (String) -> Unit) {
    val similar = remember(show.id) {
        Catalog.all.filter { it.id != show.id && it.genres.any { g -> show.genres.contains(g) } }.take(12)
    }
    if (similar.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد أعمال مشابهة", color = Color(0xFF8C8C94), fontSize = 13.sp)
        }
        return
    }
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(similar) { s ->
            Column(
                modifier = Modifier.width(110.dp).clickable { onOpenShow(s.id) }
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp))) {
                    PosterArt(
                        showId = s.id,
                        posterUrl = s.poster,
                        title = s.title,
                        showTitle = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(s.title, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun fmt(pct: Float): String {
    val scaled = (pct * 10f + 0.5f).toInt()
    return "${scaled / 10}.${scaled % 10}"
}
