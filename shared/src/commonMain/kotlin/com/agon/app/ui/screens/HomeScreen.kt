package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import com.agon.app.ui.theme.NetflixRed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agon.app.data.Catalog
import com.agon.app.data.Show
import com.agon.app.ui.components.ContinueRow
import com.agon.app.ui.components.PosterArt
import com.agon.app.ui.components.ShowRow
import com.agon.app.ui.components.Top10Row
import com.agon.app.ui.tvClickable
import com.agon.app.viewmodel.AppViewModel

import com.agon.app.ui.components.ReportIssueDialog
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    matchesVm: com.agon.app.viewmodel.MatchesViewModel? = null,
    isGuest: Boolean = false,
    onNavigateLogin: (() -> Unit)? = null,
    onOpenMatches: (() -> Unit)? = null,
    onOpenShow: (String) -> Unit,
    onPlay: (String, String?) -> Unit,
) {
    val shows by vm.shows.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val deviceType by vm.deviceType.collectAsStateWithLifecycle()
    val isTv = deviceType == "tv"
    val heroHeight = if (isTv) 380.dp else 540.dp
    val featuredShows = (categories.firstOrNull()?.shows?.take(5) ?: emptyList()).ifEmpty { listOfNotNull(Catalog.featured) }
    val lastWatchedShow by vm.lastWatchedShow.collectAsStateWithLifecycle()
    val lastWatchedEpisode by vm.lastWatchedEpisode.collectAsStateWithLifecycle()
    val showContinueWatching by vm.showContinueWatching.collectAsStateWithLifecycle()
    val myList by vm.myList.collectAsStateWithLifecycle()
    var showReportDialog by remember { mutableStateOf(false) }

    // Permissions are requested lazily by the platform (iOS) on first use.

    val resolvedMatchesVm: com.agon.app.viewmodel.MatchesViewModel = matchesVm ?: androidx.lifecycle.viewmodel.compose.viewModel()
    val allMatches by resolvedMatchesVm.matches.collectAsStateWithLifecycle()
    val topLiveMatch = remember(allMatches) {
        allMatches.firstOrNull { it.status == com.agon.app.data.matches.MatchStatus.LIVE }
    }
    val topTodayMatch = remember(allMatches) {
        allMatches.firstOrNull { it.day == com.agon.app.data.matches.MatchDay.TODAY } ?: allMatches.firstOrNull()
    }

    if (showReportDialog) {
        ReportIssueDialog(
            showId = "home",
            onDismiss = { showReportDialog = false }
        )
    }

    if (shows.isEmpty() || categories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D12)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = Color(0xFFE50914),
                        strokeWidth = 3.5.dp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "جاري تحميل موارد تطبيق حكايتنا...",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "يرجى الانتظار لحين تجهيز صور وقائمة المسلسلات بالكامل",
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1200.dp)
            ) {
            // Top bar: logo + search + hakim + exit
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NetflixRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ح", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "حكايتنا",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (onSearchClick != null) {
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Filled.Search, contentDescription = "بحث", tint = Color.White)
                        }
                    }
                    if (onOpenHakim != null) {
                        IconButton(onClick = onOpenHakim) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "حكيم", tint = Color(0xFFF5C518))
                        }
                    }
                }
            }

            if (featuredShows.isNotEmpty()) {
                item {
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState { featuredShows.size }
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(heroHeight)
                    ) { page ->
                        val show = featuredShows[page]
                        HeroBanner(
                            show = show,
                            inList = show.id in myList,
                            isTv = isTv,
                            heroHeight = heroHeight,
                            onToggleList = { vm.toggleMyList(show.id) },
                            onPlay = { onPlay(show.id, null) },
                            onInfo = { onOpenShow(show.id) },
                        )
                    }
                }
            }
            if (showContinueWatching && lastWatchedShow != null) {
                item {
                    val currentShow = lastWatchedShow ?: return@item
                    val ep = lastWatchedEpisode
                    val label = if (ep != null) "متابعة الحلقة ${ep.episodeNum}" else "متابعة المشاهدة"
                    ContinueRow(
                        title = "متابعة المشاهدة", 
                        items = listOf(com.agon.app.viewmodel.Progress(currentShow.id, 0.5f, label))
                    ) { onOpenShow(currentShow.id) }
                }
            }

            // Live Matches Bar on HomeScreen
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF2C0B0E), Color(0xFF181922))
                            )
                        )
                        .border(1.dp, NetflixRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onOpenMatches?.invoke() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NetflixRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SportsSoccer,
                                    contentDescription = null,
                                    tint = NetflixRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "مباريات اليوم بث مباشر",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (topLiveMatch != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NetflixRed)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text("مباشر 🔴", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                val matchSubtitle = when {
                                    topLiveMatch != null -> "${topLiveMatch.team1Name} ضد ${topLiveMatch.team2Name} (${topLiveMatch.score1} - ${topLiveMatch.score2}) • ${topLiveMatch.tournament}"
                                    topTodayMatch != null -> "${topTodayMatch.team1Name} ضد ${topTodayMatch.team2Name} (${topTodayMatch.matchTime}) • ${topTodayMatch.tournament}"
                                    else -> "شاهد جدول ونتائج أهم مباريات اليوم بث مباشر"
                                }
                                Text(
                                    text = matchSubtitle,
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "عرض الكل",
                                color = NetflixRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = null,
                                tint = NetflixRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            items(categories.size, key = { idx -> categories[idx].title }) { idx ->
                val cat = categories[idx]
                if (cat.title == "أفضل 10 اليوم") {
                    Top10Row(cat.title, cat.shows) { onOpenShow(it.id) }
                } else {
                    ShowRow(cat.title, cat.shows) { onOpenShow(it.id) }
                }

                // Native Advanced Ad integrated between content categories
                if (idx == 1 && !isTv) {
                    Spacer(Modifier.height(8.dp))
                    com.agon.app.ui.components.AdMobNativeCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = if (isTv) 34.dp else 18.dp, bottom = if (isTv) 34.dp else 18.dp)
                    .size(if (isTv) 62.dp else 54.dp)
                    .clip(CircleShape)
                    .background(NetflixRed)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .clickable { showReportDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.BugReport,
                    contentDescription = "إبلاغ عن مشكلة",
                    tint = Color.White,
                    modifier = Modifier.size(if (isTv) 30.dp else 26.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroBanner(
    show: Show,
    inList: Boolean,
    isTv: Boolean,
    heroHeight: androidx.compose.ui.unit.Dp,
    onToggleList: () -> Unit,
    onPlay: () -> Unit,
    onInfo: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
    ) {
        PosterArt(
            showId = show.id,
            posterUrl = show.backdrop.ifEmpty { show.poster },
            title = "",
            showTitle = false,
            fitMode = "fill",
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.background,
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = if (isTv) 12.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = show.title,
                color = Color.White,
                fontSize = if (isTv) 22.sp else 20.sp,
                lineHeight = if (isTv) 28.sp else 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                show.genres.take(3).forEachIndexed { i, g ->
                    if (i > 0) Dot()
                    Text(g, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroAction(
                    icon = Icons.Filled.Add.takeIf { !inList } ?: Icons.Filled.Check,
                    label = "قائمتي",
                    onClick = onToggleList,
                )
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(46.dp)
                        .tvClickable(shape = RoundedCornerShape(8.dp)) { onPlay() }
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تشغيل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                HeroAction(icon = Icons.Filled.Info, label = "معلومات", onClick = onInfo)
            }
        }
    }
}

@Composable
private fun HeroAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .tvClickable(shape = RoundedCornerShape(8.dp)) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
    )
}
