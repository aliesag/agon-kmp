package com.agon.app.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.view.TextureView
import android.graphics.SurfaceTexture
import android.view.Surface
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.agon.app.data.ShortComment
import com.agon.app.data.ShortItem
import com.agon.app.ui.theme.NetflixRed
import com.agon.app.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    vm: AppViewModel,
    onOpenShow: (String) -> Unit = {}
) {
    val shortsList by vm.shortsList.collectAsStateWithLifecycle()
    val userName by vm.profileName.collectAsStateWithLifecycle()
    val userEmail by vm.userEmail.collectAsStateWithLifecycle()
    val userAvatar by vm.profileAvatar.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedShortForComments by remember { mutableStateOf<ShortItem?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("لك") } // "المجتمع", "أتابعه", "لك"

    val pagerState = rememberPagerState(pageCount = { shortsList.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (shortsList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = null,
                    tint = NetflixRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "لا توجد مقاطع شورتس حالياً",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "كن أول من ينشر مقطع شورتس! اضغط زر + أعلاه لرفع فيديو من جهازك.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { vm.fetchShortsFromRemote() },
                    colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
                ) {
                    Text("تحديث الآن", color = Color.White)
                }
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val shortItem = shortsList.getOrNull(page)
                if (shortItem != null) {
                    val isCurrentPage = pagerState.currentPage == page
                    ShortItemView(
                        item = shortItem,
                        isCurrentPage = isCurrentPage,
                        onLikeClick = { vm.toggleLikeShort(shortItem.id) },
                        onCommentClick = { selectedShortForComments = shortItem },
                        onShareClick = {
                            vm.incrementShareShort(shortItem.id)
                            clipboardManager.setText(AnnotatedString(shortItem.videoUrl))
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "شاهد هذا المقطع القصير: ${shortItem.title}\n${shortItem.videoUrl}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة المقطع"))
                        },
                        onSaveClick = { vm.toggleSaveShort(shortItem.id) },
                        onNavigateToDetail = onOpenShow
                    )
                }
            }
        }

        // Top Overlay TikTok-Style Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live TV Icon (Top Right in TikTok)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LiveTv,
                    contentDescription = "بث مباشر",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Center Tabs: "المجتمع" | "أتابعه" | "لك"
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf("المجتمع", "أتابعه", "لك")
                tabs.forEach { tabTitle ->
                    val isSelected = selectedTab == tabTitle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTab = tabTitle }
                    ) {
                        Text(
                            text = tabTitle,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = if (isSelected) 17.sp else 15.sp
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(7.dp))
                        }
                    }
                }
            }

            // Top Left Action: Search & Upload (+) button
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upload Short Button (+)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NetflixRed)
                        .clickable { 
                            if (userName.isBlank() || userName == "ضيف" || userEmail.isBlank() || userAvatar.isBlank()) {
                                Toast.makeText(context, "يرجى إكمال بيانات حسابك (الاسم، البريد، والصورة) في قسم 'المزيد' أولاً لرفع الفيديوهات", Toast.LENGTH_LONG).show()
                            } else {
                                showUploadDialog = true 
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "رفع فيديو",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Search Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { showSearchScreen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "بحث",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    // Comments Modal Bottom Sheet
    if (selectedShortForComments != null) {
        val currentShort = selectedShortForComments!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedShortForComments = null },
            sheetState = sheetState,
            containerColor = Color(0xFF18181C),
            scrimColor = Color.Black.copy(alpha = 0.6f),
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            CommentsSheetContent(
                shortItem = currentShort,
                onClose = { selectedShortForComments = null },
                onAddComment = { text, linkedShowId ->
                    vm.addCommentToShort(currentShort.id, text, linkedShowId)
                }
            )
        }
    }

    // Upload Dialog
    if (showUploadDialog) {
        UploadShortDialog(
            onDismiss = { showUploadDialog = false },
            onUpload = { title, desc, url, showId ->
                vm.addShort(title, desc, url, showId)
                showUploadDialog = false
            }
        )
    }

    // TikTok Search Screen Overlay
    if (showSearchScreen) {
        TikTokSearchScreen(
            shortsList = shortsList,
            onClose = { showSearchScreen = false },
            onSelectShort = { selectedItem ->
                val index = shortsList.indexOfFirst { it.id == selectedItem.id }
                if (index >= 0) {
                    coroutineScope.launch {
                        pagerState.scrollToPage(index)
                    }
                }
                showSearchScreen = false
            }
        )
    }
}

@Composable
private fun ShortItemView(
    item: ShortItem,
    isCurrentPage: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    var isPaused by remember { mutableStateOf(false) }
    var currentTimeSec by remember { mutableStateOf(0f) }
    var durationSec by remember { mutableStateOf(0f) }
    var userSeekPos by remember { mutableStateOf<Float?>(null) }
    var isSeeking by remember { mutableStateOf(false) }
    var isLandscapeVideo by remember { mutableStateOf(false) }
    var showLandscapeFullscreen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var showLongPressMenu by remember { mutableStateOf(false) }

        var isVideoReady by remember(item.id, isCurrentPage) { mutableStateOf(true) }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fullscreen Video Player
            if (isCurrentPage && !showLandscapeFullscreen) {
                ShortVideoPlayer(
                    videoUrl = item.videoUrl,
                    isPaused = isPaused,
                    playbackSpeed = playbackSpeed,
                    seekToPositionSeconds = userSeekPos,
                    onVideoReady = { isVideoReady = true },
                    onTimeUpdate = { current, total ->
                        if (!isSeeking) {
                            currentTimeSec = current
                            durationSec = total
                        }
                    },
                    onVideoFormat = { isLandscape ->
                        isLandscapeVideo = isLandscape
                    }
                )
            } else if (isCurrentPage && showLandscapeFullscreen) {
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

        // Show poster and loader until video is ready
        if (!isVideoReady || !isCurrentPage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF181820), Color(0xFF0C0C10))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.posterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isCurrentPage && !isVideoReady) {
                    CircularProgressIndicator(
                        color = NetflixRed,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        // Gesture Overlay to intercept touches before they reach the WebView
        if (isCurrentPage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { isPaused = !isPaused },
                            onLongPress = { showLongPressMenu = true }
                        )
                    }
            )
        }

        // Active Speed Indicator Pill Badge if not 1.0x
        if (playbackSpeed != 1.0f) {
            Surface(
                color = Color(0xFFFE2C55),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .clickable { playbackSpeed = 1.0f }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "سرعة ${playbackSpeed}x (إلغاء)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Pause / Play Indicator Overlay
        AnimatedVisibility(
            visible = isPaused,
            enter = fadeIn() + androidx.compose.animation.scaleIn(initialScale = 1.5f),
            exit = fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Fullscreen Badge Button in center-bottom — ONLY shown if video is landscape/horizontal!
        if (isLandscapeVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable { showLandscapeFullscreen = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CropFree,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "شاشة كاملة",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Timer Display Overlay (00:03 / 00:55 format as requested in Screenshot 1)
        if (isSeeking || isPaused) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 170.dp)
                    .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${formatTime(if (isSeeking && userSeekPos != null) userSeekPos!! else currentTimeSec)} / ${formatTime(durationSec)}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Gradient overlay for bottom UI text contrast
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Right Action Bar (Avatar with follow +, Heart, Comment, Save, Share, Rotating Disc)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // User Profile Avatar + Follow (+) Badge
            Box(
                modifier = Modifier.padding(bottom = 6.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (item.uploaderAvatar.isNotEmpty()) {
                    AsyncImage(
                        model = item.uploaderAvatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NetflixRed)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.uploaderName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                // Red + Follow Badge Icon
                var isFollowed by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .offset(y = 8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isFollowed) Color.White else NetflixRed)
                        .clickable { isFollowed = !isFollowed },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFollowed) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = "متابعة",
                        tint = if (isFollowed) NetflixRed else Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Real Like Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { onLikeClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isLiked) Icons.Filled.Favorite else Icons.Filled.Favorite,
                        contentDescription = "إعجاب",
                        tint = if (item.isLiked) NetflixRed else Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.likesCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Real Comment Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { onCommentClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubble,
                        contentDescription = "تعليقات",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.commentsCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bookmark / Save Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { onSaveClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isSaved) Icons.Filled.Bookmark else Icons.Filled.Bookmark,
                        contentDescription = "حفظ",
                        tint = if (item.isSaved) Color(0xFFFFD700) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Real Share Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { onShareClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Reply,
                        contentDescription = "مشاركة",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.sharesCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Rotating Music Disc
            val infiniteTransition = rememberInfiniteTransition(label = "disc")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .rotate(if (isPaused) 0f else angle)
                    .clip(CircleShape)
                    .border(2.dp, Color.DarkGray, CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (item.uploaderAvatar.isNotEmpty()) {
                    AsyncImage(
                        model = item.uploaderAvatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Info Panel (Uploader, Caption, Hashtags, Audio)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Username @handle
            Text(
                        text = item.uploaderName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            // Caption / Title with hashtags
            Text(
                text = item.title + if (item.description.isNotEmpty()) " ${item.description}" else "",
                color = Color.White,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Audio track name row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = item.audioTitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Linked Show Chip
            if (item.linkedShowId != null) {
                val linkedShow = com.agon.app.data.Catalog.all.find { it.id == item.linkedShowId }
                if (linkedShow != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            onNavigateToDetail(linkedShow.id)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.PlayCircleFilled, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(14.dp))
                            Text(
                                text = "مشاهدة: ${linkedShow.title}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Bottom Seeker Progress Bar (TikTok style)
        if (durationSec > 0f) {
            Slider(
                value = if (isSeeking && userSeekPos != null) userSeekPos!! else currentTimeSec,
                valueRange = 0f..durationSec,
                onValueChange = { pos ->
                    isSeeking = true
                    userSeekPos = pos
                },
                onValueChangeFinished = {
                    isSeeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 4.dp)
            )
        }
    }

    // Fullscreen Landscape Video Dialog
    if (showLandscapeFullscreen) {
        Dialog(
            onDismissRequest = { showLandscapeFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                ShortVideoPlayer(
                    videoUrl = item.videoUrl,
                    isPaused = isPaused,
                    playbackSpeed = playbackSpeed,
                    seekToPositionSeconds = userSeekPos,
                    onVideoReady = {},
                    onTimeUpdate = { current, total ->
                        currentTimeSec = current
                        durationSec = total
                    },
                    onVideoFormat = { _ -> }
                )

                // Top Close Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showLandscapeFullscreen = false },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showLongPressMenu) {
        TikTokLongPressBottomSheet(
            item = item,
            currentSpeed = playbackSpeed,
            onSelectSpeed = { sp -> playbackSpeed = sp },
            onDismiss = { showLongPressMenu = false }
        )
    }
}

fun extractDirectMediaUrl(raw: String): String {
    val trimmed = raw.trim()
    var url = trimmed
    if (trimmed.contains("<")) {
        val srcRegex = Regex("""(?:src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val match = srcRegex.find(trimmed)
        if (match != null) {
            val src = match.groupValues[1]
            if (src.startsWith("http://") || src.startsWith("https://")) {
                url = src
            }
        } else {
            val urlRegex = Regex("""https?://[^\s"'<>]+\.(?:mp4|webm|m3u8|mov|mkv)(?:\?[^\s"'<>]*)?""", RegexOption.IGNORE_CASE)
            val urlMatch = urlRegex.find(trimmed)
            if (urlMatch != null) {
                url = urlMatch.value
            } else {
                val genericHttpRegex = Regex("""https?://[^\s"'<>]+""")
                val genericMatch = genericHttpRegex.find(trimmed)
                if (genericMatch != null) {
                    url = genericMatch.value
                }
            }
        }
    }

    if (url.contains("tmpfiles.org/")) {
        url = url.replace("tmpfiles.org/dl/dl/", "tmpfiles.org/dl/")
        if (!url.contains("tmpfiles.org/dl/")) {
            url = url.replace("tmpfiles.org/", "tmpfiles.org/dl/")
        }
    }

    return url
}

@Composable
private fun ShortVideoPlayer(
    videoUrl: String,
    isPaused: Boolean,
    playbackSpeed: Float = 1.0f,
    seekToPositionSeconds: Float?,
    onVideoReady: () -> Unit,
    onTimeUpdate: (currentTime: Float, duration: Float) -> Unit,
    onVideoFormat: (isLandscape: Boolean) -> Unit = {}
) {
    val cleanUrl = remember(videoUrl) { extractDirectMediaUrl(videoUrl) }
    val isDirectMedia = remember(cleanUrl) {
        cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")
    }

    if (isDirectMedia) {
        NativeTextureVideoPlayer(
            videoUrl = cleanUrl,
            isPaused = isPaused,
            playbackSpeed = playbackSpeed,
            seekToPositionSeconds = seekToPositionSeconds,
            onVideoReady = onVideoReady,
            onTimeUpdate = onTimeUpdate,
            onVideoFormat = onVideoFormat
        )
    } else {
        FallbackWebVideoPlayer(
            videoUrl = videoUrl,
            isPaused = isPaused,
            playbackSpeed = playbackSpeed,
            seekToPositionSeconds = seekToPositionSeconds,
            onVideoReady = onVideoReady,
            onTimeUpdate = onTimeUpdate,
            onVideoFormat = onVideoFormat
        )
    }
}

@Composable
private fun NativeTextureVideoPlayer(
    videoUrl: String,
    isPaused: Boolean,
    playbackSpeed: Float,
    seekToPositionSeconds: Float?,
    onVideoReady: () -> Unit,
    onTimeUpdate: (currentTime: Float, duration: Float) -> Unit,
    onVideoFormat: (isLandscape: Boolean) -> Unit
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var surfaceTextureRef by remember { mutableStateOf<SurfaceTexture?>(null) }
    var surfaceRef by remember { mutableStateOf<Surface?>(null) }
    var isPrepared by remember { mutableStateOf(false) }

    // Periodically send playback progress
    LaunchedEffect(isPrepared, isPaused, mediaPlayer) {
        if (isPrepared && mediaPlayer != null) {
            while (true) {
                try {
                    val mp = mediaPlayer
                    if (mp != null) {
                        val current = mp.currentPosition / 1000f
                        val dur = mp.duration / 1000f
                        if (dur > 0) {
                            onTimeUpdate(current, dur)
                        }
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(250)
            }
        }
    }

    // Play/Pause control
    LaunchedEffect(isPaused, isPrepared) {
        try {
            val mp = mediaPlayer ?: return@LaunchedEffect
            if (isPrepared) {
                if (isPaused) {
                    if (mp.isPlaying) mp.pause()
                } else {
                    if (!mp.isPlaying) mp.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Playback speed control
    LaunchedEffect(playbackSpeed, isPrepared) {
        try {
            val mp = mediaPlayer ?: return@LaunchedEffect
            if (isPrepared && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = mp.playbackParams
                params.speed = playbackSpeed
                mp.playbackParams = params
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Seeking control
    LaunchedEffect(seekToPositionSeconds) {
        seekToPositionSeconds?.let { posSec ->
            try {
                val mp = mediaPlayer ?: return@LaunchedEffect
                if (isPrepared) {
                    val targetMs = (posSec * 1000).toInt().coerceAtLeast(0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mp.seekTo(targetMs.toLong(), MediaPlayer.SEEK_CLOSEST)
                    } else {
                        mp.seekTo(targetMs)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(videoUrl) {
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            isLooping = true
            setOnPreparedListener { preparedMp ->
                isPrepared = true
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playbackSpeed != 1.0f) {
                        val params = preparedMp.playbackParams
                        params.speed = playbackSpeed
                        preparedMp.playbackParams = params
                    }
                    if (!isPaused) {
                        preparedMp.start()
                    }
                    onVideoReady()
                    val w = preparedMp.videoWidth
                    val h = preparedMp.videoHeight
                    if (w > 0 && h > 0) {
                        onVideoFormat(w > h)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            setOnVideoSizeChangedListener { _, w, h ->
                if (w > 0 && h > 0) {
                    onVideoFormat(w > h)
                }
            }
            setOnErrorListener { _, what, extra ->
                android.util.Log.e("ShortsScreen", "MediaPlayer error: what=$what extra=$extra")
                true
            }
        }

        try {
            val headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            mp.setDataSource(context, Uri.parse(videoUrl), headers)
            mp.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                mp.setDataSource(videoUrl)
                mp.prepareAsync()
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        mediaPlayer = mp

        onDispose {
            isPrepared = false
            try {
                mp.stop()
            } catch (_: Exception) {}
            try {
                mp.reset()
            } catch (_: Exception) {}
            try {
                mp.release()
            } catch (_: Exception) {}
            mediaPlayer = null
            surfaceRef?.release()
            surfaceRef = null
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        surfaceTextureRef = st
                        val surface = Surface(st)
                        surfaceRef = surface
                        try {
                            mediaPlayer?.setSurface(surface)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        try {
                            mediaPlayer?.setSurface(null)
                        } catch (_: Exception) {}
                        surfaceRef?.release()
                        surfaceRef = null
                        surfaceTextureRef = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        update = {
            surfaceTextureRef?.let { st ->
                if (surfaceRef == null) {
                    val s = Surface(st)
                    surfaceRef = s
                    try {
                        mediaPlayer?.setSurface(s)
                    } catch (_: Exception) {}
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FallbackWebVideoPlayer(
    videoUrl: String,
    isPaused: Boolean,
    playbackSpeed: Float = 1.0f,
    seekToPositionSeconds: Float?,
    onVideoReady: () -> Unit,
    onTimeUpdate: (currentTime: Float, duration: Float) -> Unit,
    onVideoFormat: (isLandscape: Boolean) -> Unit = {}
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.let { wv ->
                    wv.evaluateJavascript("try { var v = document.getElementById('vPlayer'); if(v) { v.pause(); v.src=''; } } catch(e){}", null)
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

    LaunchedEffect(seekToPositionSeconds) {
        seekToPositionSeconds?.let { pos ->
            webViewRef?.evaluateJavascript("if (document.getElementById('vPlayer')) document.getElementById('vPlayer').currentTime = $pos;", null)
        }
    }

    LaunchedEffect(playbackSpeed) {
        webViewRef?.evaluateJavascript("if (document.getElementById('vPlayer')) document.getElementById('vPlayer').playbackRate = $playbackSpeed;", null)
    }

    LaunchedEffect(isPaused) {
        val js = if (isPaused) {
            "if (document.getElementById('vPlayer')) document.getElementById('vPlayer').pause();"
        } else {
            "if (document.getElementById('vPlayer')) document.getElementById('vPlayer').play();"
        }
        webViewRef?.evaluateJavascript(js, null)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    mediaPlaybackRequiresUserGesture = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                webViewClient = object : WebViewClient() {}
                webChromeClient = WebChromeClient()

                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onTimeUpdate(current: Float, total: Float) {
                        if (total > 0f) {
                            onTimeUpdate(current, total)
                        }
                    }

                    @android.webkit.JavascriptInterface
                    fun onVideoFormat(isLandscape: Boolean) {
                        onVideoFormat(isLandscape)
                    }

                    @android.webkit.JavascriptInterface
                    fun onVideoReady() {
                        onVideoReady()
                    }
                }, "AndroidBridge")

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            body, html {
                                margin: 0; padding: 0; width: 100%; height: 100%;
                                background-color: #000; overflow: hidden;
                                display: flex; justify-content: center; align-items: center;
                            }
                            video {
                                width: 100vw; height: 100vh; object-fit: contain; background-color: #000;
                            }
                        </style>
                    </head>
                    <body>
                        <video id="vPlayer" autoplay loop playsinline webkit-playsinline x5-playsinline preload="auto">
                            <source src="$videoUrl" type="video/mp4">
                            <source src="$videoUrl" type="video/webm">
                        </video>
                        <script>
                            var v = document.getElementById('vPlayer');
                            function notifyReady() {
                                if (window.AndroidBridge && window.AndroidBridge.onVideoReady) {
                                    window.AndroidBridge.onVideoReady();
                                }
                            }
                            v.addEventListener('canplay', notifyReady);
                            v.addEventListener('playing', notifyReady);
                            v.addEventListener('loadeddata', notifyReady);
                            var playPromise = v.play();
                            if (playPromise !== undefined) {
                                playPromise.then(function() {
                                    notifyReady();
                                }).catch(function() {
                                    v.muted = true;
                                    v.play().then(notifyReady);
                                });
                            }
                            setTimeout(notifyReady, 1000);
                        </script>
                    </body>
                    </html>
                """.trimIndent()

                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                webViewRef = this
            }
        },
        update = { webView ->
            webViewRef = webView
            webView.evaluateJavascript("""
                var v = document.getElementById('vPlayer');
                if (v) {
                    v.playbackRate = $playbackSpeed;
                    if ($isPaused) { v.pause(); } else { v.play(); }
                }
            """.trimIndent(), null)
        },
        onRelease = { webView ->
            try {
                webView.evaluateJavascript("try { var v = document.getElementById('vPlayer'); if(v) { v.pause(); v.src=''; } } catch(e){}", null)
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.onPause()
                webView.pauseTimers()
                webView.destroy()
            } catch (_: Exception) {}
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TikTokLongPressBottomSheet(
    item: ShortItem,
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181C),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Gray.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "خيارات المقطع",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = Color(0xFFFE2C55),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "سرعة التشغيل",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (currentSpeed != 1.0f) {
                    Text(
                        text = "الحالية: ${currentSpeed}x",
                        color = Color(0xFFFE2C55),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val speeds = listOf(1.0f, 1.5f, 2.0f, 3.0f, 4.0f, 5.0f)
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                speeds.forEach { sp ->
                    val isSel = (currentSpeed == sp)
                    val label = if (sp == 1.0f) "1.0x" else "${sp}x"
                    Surface(
                        color = if (isSel) Color(0xFFFE2C55) else Color(0xFF2A2A32),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable {
                            onSelectSpeed(sp)
                            Toast.makeText(context, "تم ضبط السرعة إلى ${sp}x", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 20.dp),
                color = Color(0xFF2A2A32)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onDismiss()
                        downloadVideo(context, item.videoUrl, item.title)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A32)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "تحميل",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("تحميل", color = Color.White, fontSize = 12.sp)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onDismiss()
                        clipboardManager.setText(AnnotatedString(item.videoUrl))
                        Toast.makeText(context, "تم نسخ رابط الفيديو", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A32)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "نسخ الرابط",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("نسخ الرابط", color = Color.White, fontSize = 12.sp)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onDismiss()
                        Toast.makeText(context, "سنقلل من عرض هذا المحتوى", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A32)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = "غير مهتم",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("غير مهتم", color = Color.White, fontSize = 12.sp)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onDismiss()
                        Toast.makeText(context, "شكراً لإبلاغك، سنراجع هذا الفيديو", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A32)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "إبلاغ",
                            tint = Color(0xFFFE2C55)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("إبلاغ", color = Color(0xFFFE2C55), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun downloadVideo(context: Context, videoUrl: String, title: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(videoUrl))
            .setTitle(title.ifEmpty { "شورتس" })
            .setDescription("جاري تحميل مقطع الشورتس...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "Short_${System.currentTimeMillis()}.mp4")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "بدأ تحميل الفيديو...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر بدء التحميل: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun formatTime(seconds: Float): String {
    if (seconds <= 0 || seconds.isNaN() || seconds.isInfinite()) return "00:00"
    val totalSecs = seconds.toInt()
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

@Composable
private fun CommentsSheetContent(
    shortItem: ShortItem,
    onClose: () -> Unit,
    onAddComment: (String, String?) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    var showShowPicker by remember { mutableStateOf(false) }
    var selectedLinkedShowId by remember { mutableStateOf<String?>(null) }

    if (showShowPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showShowPicker = false },
            title = { Text("اختر مسلسلاً للإرفاق", color = Color.White) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(com.agon.app.data.Catalog.all) { show ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLinkedShowId = show.id
                                    showShowPicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            coil.compose.AsyncImage(
                                model = show.poster,
                                contentDescription = null,
                                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Text(show.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showShowPicker = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF222222)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.72f)
            .background(Color(0xFF18181C))
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Drag indicator handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
        }

        // Header Title & Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = "${shortItem.commentsCount} تعليق",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "إغلاق",
                    tint = Color.Gray
                )
            }
        }

        HorizontalDivider(color = Color(0xFF2C2C32))

        // Comments List
        if (shortItem.comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد تعليقات بعد. كُن أول من يعلّق!",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(shortItem.comments) { comment ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (comment.avatar.isNotEmpty()) {
                            AsyncImage(
                                model = comment.avatar,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NetflixRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = comment.user.take(1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = comment.user,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = comment.timeAgo,
                                    color = Color.DarkGray,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = comment.text,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            if (!comment.linkedShowId.isNullOrEmpty()) {
                                val show = com.agon.app.data.Catalog.all.find { it.id == comment.linkedShowId }
                                if (show != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        coil.compose.AsyncImage(
                                            model = show.poster,
                                            contentDescription = null,
                                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        Text(show.title, color = NetflixRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2C2C32))

        // Input Field Container pinned above keyboard
        if (selectedLinkedShowId != null) {
            val show = com.agon.app.data.Catalog.all.find { it.id == selectedLinkedShowId }
            if (show != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2A32)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مرفق: ${show.title}", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { selectedLinkedShowId = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "إزالة", tint = Color.Gray)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F1F24))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { showShowPicker = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Attach Show", tint = Color.Gray)
            }
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                placeholder = { Text("إضافة تعليق...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NetflixRed,
                    unfocusedBorderColor = Color(0xFF33333A),
                    focusedContainerColor = Color(0xFF2A2A32),
                    unfocusedContainerColor = Color(0xFF2A2A32),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (newCommentText.isNotBlank() || selectedLinkedShowId != null) NetflixRed else Color(0xFF33333A))
                    .clickable {
                        if (newCommentText.isNotBlank() || selectedLinkedShowId != null) {
                            onAddComment(newCommentText, selectedLinkedShowId)
                            newCommentText = ""
                            selectedLinkedShowId = null
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "إرسال",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private data class LocalVideoItem(
    val id: Long,
    val uri: Uri,
    val durationFormatted: String,
    val thumbnail: Bitmap?
)

private fun fetchLocalDeviceVideos(context: android.content.Context): List<LocalVideoItem> {
    val list = mutableListOf<LocalVideoItem>()
    val projection = arrayOf(
        android.provider.MediaStore.Video.Media._ID,
        android.provider.MediaStore.Video.Media.DURATION
    )
    val sortOrder = "${android.provider.MediaStore.Video.Media.DATE_ADDED} DESC"
    try {
        val cursor = context.contentResolver.query(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )
        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
            val durCol = c.getColumnIndex(android.provider.MediaStore.Video.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val durationMs = if (durCol != -1) c.getLong(durCol) else 0L
                val contentUri = android.content.ContentUris.withAppendedId(
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val secs = (durationMs / 1000) % 60
                val mins = (durationMs / (1000 * 60)) % 60
                val formatted = String.format("%02d:%02d", mins, secs)
                val thumbnail = runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, contentUri)
                        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } finally {
                        retriever.release()
                    }
                }.getOrNull()
                list.add(LocalVideoItem(id, contentUri, formatted, thumbnail))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadShortDialog(
    onDismiss: () -> Unit,
    onUpload: (title: String, desc: String, url: String, linkedShowId: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(0) } // 0 = TikTok Video Gallery, 1 = Metadata & Publish
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedShowId by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }

    // Load device videos from MediaStore
    var localVideos by remember { mutableStateOf<List<LocalVideoItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.Dispatchers.IO.let { dispatcher ->
            kotlinx.coroutines.withContext(dispatcher) {
                localVideos = fetchLocalDeviceVideos(context)
            }
        }
    }

    // System Picker Fallback
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            selectedVideoThumbnail = runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } finally {
                    retriever.release()
                }
            }.getOrNull()
            step = 1 // Move to metadata input
        }
    }

    Dialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            color = Color(0xFF101014)
        ) {
            if (step == 0) {
                // STEP 0: TikTok Style Gallery Picker
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar: Close Icon + Filter Dropdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close Icon (X)
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF22222A), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White
                            )
                        }

                        // Filter Title e.g. "الأحدث ∨"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF22222A))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { videoPickerLauncher.launch("video/*") }
                        ) {
                            Text(
                                text = "الأحدث",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Spacer to balance header layout
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    // Category Tabs Bar: "الفيديوهات" (Red Accent)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "الفيديوهات",
                                color = NetflixRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(3.dp)
                                    .background(NetflixRed, RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3-Column TikTok Grid
                    Box(modifier = Modifier.weight(1f)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // First Tile: System File Selector
                            item {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(Color(0xFF1E1E28))
                                        .clickable { videoPickerLauncher.launch("video/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Folder,
                                            contentDescription = null,
                                            tint = NetflixRed,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "فتح المعرض",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Device Videos from MediaStore
                            items(localVideos) { video ->
                                val isSelected = selectedVideoUri == video.uri
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(Color.Black)
                                        .clickable {
                                            selectedVideoUri = if (isSelected) null else video.uri
                                            selectedVideoThumbnail = if (isSelected) null else video.thumbnail
                                        }
                                ) {
                                    AsyncImage(
                                        model = video.thumbnail ?: video.uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Selection Radio Circle (Top-Right)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .background(
                                                if (isSelected) NetflixRed else Color.Black.copy(alpha = 0.5f),
                                                CircleShape
                                            )
                                            .border(
                                                width = if (isSelected) 0.dp else 1.5.dp,
                                                color = if (isSelected) Color.Transparent else Color.White,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Video Duration Badge (Bottom-Right)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = video.durationFormatted,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Empty State if MediaStore has no items
                        if (localVideos.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "اختر فيديو من المعرض للنشر مباشرة",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { videoPickerLauncher.launch("video/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
                                ) {
                                    Text("تصفح الفيديوهات", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Bottom Action Bar: "التالي" Button (Red TikTok Style)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF16161C))
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { if (selectedVideoUri != null) step = 1 },
                            enabled = selectedVideoUri != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetflixRed,
                                disabledContainerColor = Color(0xFF2C2C35)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (selectedVideoUri != null) "التالي" else "حدد فيديو أولاً",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // STEP 1: Metadata Input & Upload Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (!isUploading) step = 0 },
                            enabled = !isUploading
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نشر الشورت",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Video Thumbnail Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        selectedVideoUri?.let { uri ->
                            AsyncImage(
                                model = selectedVideoThumbnail ?: uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                imageVector = Icons.Filled.PlayCircleFilled,
                                contentDescription = null,
                                tint = NetflixRed,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Input Fields
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان الشورت") },
                        placeholder = { Text("مثال: مشهد حماسي من المسلسل") },
                        singleLine = true,
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NetflixRed,
                            unfocusedBorderColor = Color(0xFF383842),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("الوصف / هاشتاق") },
                        placeholder = { Text("#حكاياتنا #مسلسلات #شورتس") },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NetflixRed,
                            unfocusedBorderColor = Color(0xFF383842),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Select Series Row (Horizontal Scroll)
                    Text("المسلسل المرتبط (إجباري قبل النشر):", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(com.agon.app.data.Catalog.all) { show ->
                            val isSel = show.id == selectedShowId
                            Column(
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable { selectedShowId = show.id },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) NetflixRed else Color.Transparent)
                                        .border(2.dp, if (isSel) NetflixRed else Color.Gray, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = show.poster,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                    if (isSel) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(NetflixRed.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = show.title,
                                    color = if (isSel) NetflixRed else Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isUploading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = NetflixRed,
                            trackColor = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    uploadStatusMessage?.let { status ->
                        if (status.contains("فشل")) {
                            Text(
                                text = status,
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Publish Button
                    Button(
                        onClick = {
                            val uri = selectedVideoUri ?: return@Button
                            isUploading = true
                            uploadStatusMessage = null
                            coroutineScope.launch {
                                val uploadedUrl = com.agon.app.utils.ImageUploader.uploadVideoToTop4ToPAndFallbacks(context, uri)
                                if (!uploadedUrl.isNullOrEmpty()) {
                                    onUpload(title.ifBlank { "شورت جديد" }, desc, uploadedUrl, selectedShowId)
                                    isUploading = false
                                } else {
                                    uploadStatusMessage = "فشل رفع الفيديو، يرجى المحاولة مرة أخرى."
                                    isUploading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                        enabled = selectedVideoUri != null && title.isNotBlank() && selectedShowId != null && !isUploading,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = if (isUploading) "جاري النشر..." else "نشر الآن",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TikTokSearchScreen(
    shortsList: List<ShortItem>,
    onClose: () -> Unit,
    onSelectShort: (ShortItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf("الأفضل") }
    var activeFilterChip by remember { mutableStateOf("الكل") }
    var isExpandedHistory by remember { mutableStateOf(false) }

    val recentSearches = remember {
        mutableStateListOf<String>()
    }

    val trendingSearches = remember(shortsList) {
        val titles = shortsList.map { it.title }.distinct().take(6)
        if (titles.isNotEmpty()) titles else listOf("مسلسلات حكاياتنا", "شورتس حصرية", "مقاطع ممتعة")
    }

    val filteredShorts = remember(searchQuery, shortsList, activeFilterChip) {
        var list = if (searchQuery.isBlank()) {
            shortsList
        } else {
            shortsList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.uploaderName.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        when (activeFilterChip) {
            "تم التحميل مؤخراً" -> list.reversed()
            else -> list
        }
    }

    val realUsers = remember(searchQuery, shortsList) {
        shortsList
            .groupBy { it.uploaderName }
            .map { (uploaderName, items) ->
                Triple(
                    uploaderName,
                    items.firstOrNull { it.uploaderAvatar.isNotEmpty() }?.uploaderAvatar ?: "",
                    items
                )
            }
            .filter { (name, _, _) ->
                if (searchQuery.isBlank()) true
                else name.contains(searchQuery, ignoreCase = true)
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    color = Color(0xFF222228),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )

                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = trendingSearches.firstOrNull() ?: "ابحث عن فيديوهات أو مستخدمين...",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Cancel,
                                contentDescription = "مسح",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }

                Text(
                    text = "بحث",
                    color = Color(0xFFFE2C55),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable {
                            if (searchQuery.isNotBlank() && !recentSearches.contains(searchQuery)) {
                                recentSearches.add(0, searchQuery)
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }

            if (searchQuery.isEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        val displayedHistory = if (isExpandedHistory) recentSearches else recentSearches.take(3)
                        items(displayedHistory.size) { idx ->
                            val item = displayedHistory[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { searchQuery = item }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = item,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "حذف",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { recentSearches.remove(item) }
                                )
                            }
                        }

                        if (recentSearches.size > 3) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.clickable { isExpandedHistory = !isExpandedHistory },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (isExpandedHistory) "عرض أقل" else "عرض المزيد",
                                            color = Color.Gray,
                                            fontSize = 13.sp
                                        )
                                        Icon(
                                            imageVector = if (isExpandedHistory) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color(0xFF222228)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "قد يعجبك",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    items(trendingSearches.size) { index ->
                        val term = trendingSearches[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { searchQuery = term }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (index < 2) Color(0xFFFE2C55) else Color.Gray)
                            )
                            Text(
                                text = term,
                                color = if (index < 2) Color(0xFFFE2C55) else Color.White,
                                fontWeight = if (index < 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    val categories = listOf("الأفضل", "الفيديوهات", "المستخدمون")
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(activeCategory).coerceAtLeast(0),
                        edgePadding = 12.dp,
                        containerColor = Color(0xFF121212),
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            val index = categories.indexOf(activeCategory).coerceAtLeast(0)
                            if (index < tabPositions.size) {
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[index])
                                        .height(2.dp)
                                        .background(Color(0xFFFE2C55))
                                )
                            }
                        },
                        divider = {}
                    ) {
                        categories.forEach { cat ->
                            val isSelected = activeCategory == cat
                            Tab(
                                selected = isSelected,
                                onClick = { activeCategory = cat },
                                text = {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filterChips = listOf("الكل", "تم التحميل مؤخراً")
                        filterChips.forEach { chip ->
                            val isSelected = activeFilterChip == chip
                            Surface(
                                color = if (isSelected) Color(0xFFFE2C55) else Color(0xFF222228),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.clickable { activeFilterChip = chip }
                            ) {
                                Text(
                                    text = chip,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (activeCategory == "الأفضل" || activeCategory == "المستخدمون") {
                            if (realUsers.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "المستخدمون",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                items(realUsers.size) { idx ->
                                    val (userName, userAvatar, userShorts) = realUsers[idx]
                                    val totalLikes = userShorts.sumOf { it.likesCount }

                                    Surface(
                                        color = Color(0xFF1E1E24),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFF2A2A32)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                userShorts.firstOrNull()?.let { onSelectShort(it) }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (userAvatar.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = userAvatar,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(52.dp)
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(52.dp)
                                                            .clip(CircleShape)
                                                            .background(NetflixRed),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = userName.take(1),
                                                            color = Color.White,
                                                            fontSize = 20.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = userName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp,
                                                            color = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Filled.CheckCircle,
                                                            contentDescription = "موثق",
                                                            tint = Color(0xFF20D5EC),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = "${userShorts.size} فيديو • $totalLikes إعجاب",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            var isUserFollowed by remember { mutableStateOf(false) }
                                            Button(
                                                onClick = { isUserFollowed = !isUserFollowed },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isUserFollowed) Color(0xFF2A2A32) else Color(0xFFFE2C55),
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                                modifier = Modifier.height(34.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isUserFollowed) "تمت المتابعة" else "متابعة",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (activeCategory == "الأفضل" || activeCategory == "الفيديوهات") {
                            item {
                                Text(
                                    text = "الفيديوهات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            val chunks = filteredShorts.chunked(2)
                            items(chunks.size) { chunkIdx ->
                                val pair = chunks[chunkIdx]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { item ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            SearchVideoResultCard(
                                                item = item,
                                                onClick = { onSelectShort(item) }
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
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
private fun SearchVideoResultCard(
    item: ShortItem,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1E24),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, Color(0xFF2A2A32)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(Color.Black)
            ) {
                if (item.posterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 120f
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${item.likesCount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(NetflixRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.uploaderName.take(1),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = item.uploaderName,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
