package com.agon.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Download
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agon.app.data.Catalog
import com.agon.app.data.Episode
import com.agon.app.ui.components.PosterArt
import com.agon.app.ui.tvClickable
import com.agon.app.viewmodel.AppViewModel

@Composable
fun DetailScreen(
    showId: String,
    vm: AppViewModel,
    onBack: () -> Unit,
    onPlay: (String, String?) -> Unit,
    onOpenShow: (String) -> Unit,
    onNavigateToDownloads: () -> Unit,
) {
    val shows by vm.shows.collectAsStateWithLifecycle()
    val show = shows.firstOrNull { it.id == showId } ?: Catalog.byId(showId) ?: return
    val myList by vm.myList.collectAsStateWithLifecycle()
    val inList = show.id in myList

    val episodesMap by vm.showEpisodes.collectAsStateWithLifecycle()
    val isLoadingEpisodes by vm.isLoadingEpisodes.collectAsStateWithLifecycle()
    val episodes = episodesMap[show.id] ?: emptyList()
    val isComingSoon = episodes.firstOrNull()?.id == "coming_soon"

    val seasons = remember(episodes) { episodes.map { it.seasonNumber }.distinct().sorted() }
    var selectedSeason by remember(show.id) { mutableIntStateOf(1) }
    
    LaunchedEffect(seasons) {
        if (seasons.isNotEmpty() && !seasons.contains(selectedSeason)) {
            selectedSeason = seasons.first()
        }
    }
    val filteredEpisodes = episodes.filter { it.seasonNumber == selectedSeason }

    LaunchedEffect(show.id) {
        vm.loadEpisodes(show)
        vm.loadShowDetails(show.id)
    }

    val related = shows.filter { it.id != show.id && it.genres.any { g -> g in show.genres } }.take(6)

    val deviceType by vm.deviceType.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()

    val isAlooy = show.link.contains("alooytv", ignoreCase = true) ||
            show.id.contains("alooy", ignoreCase = true) ||
            show.genres.any { it.contains("alooy", ignoreCase = true) }
            
    var selectedEpisodeId by remember { mutableStateOf<String?>(null) }
    var showDownloadSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(showDownloadSnackbar) {
        if (showDownloadSnackbar) {
            kotlinx.coroutines.delay(6000)
            showDownloadSnackbar = false
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                val headerArt = show.backdrop.ifEmpty { show.poster }
                PosterArt(showId = show.id, posterUrl = headerArt, title = "", showTitle = false, modifier = Modifier.fillMaxWidth().height(420.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(420.dp).background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(0.4f), Color.Transparent, MaterialTheme.colorScheme.background)
                        )
                    )
                )
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(40.dp)
                        .tvClickable(shape = CircleShape) { onBack() }
                        .background(Color.Black.copy(0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "رجوع", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = show.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(show.year, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    RatingChip(if (show.isMovie) "فيلم" else show.rating)
                    Text(show.duration, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.secondary).padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("IMDb ${vm.formatRating(show.imdb)}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .tvClickable(shape = RoundedCornerShape(6.dp), enabled = !isComingSoon) {
                            if (!isComingSoon) {
                                onPlay(show.id, episodes.firstOrNull()?.id)
                            }
                        }
                        .background(if (isComingSoon) Color.Gray.copy(alpha = 0.25f) else Color.White, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isComingSoon) Icons.Filled.Info else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (isComingSoon) Color.White.copy(alpha = 0.5f) else Color.Black
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isComingSoon) "يعرض قريباً على تطبيقنا" else if (show.isMovie) "شاهد الفيلم الآن 🎬" else "شاهد الان",
                            color = if (isComingSoon) Color.White.copy(alpha = 0.5f) else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                
                if (isAlooy) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .tvClickable(shape = RoundedCornerShape(10.dp)) {
                                // Find first available episode to download
                                episodes.firstOrNull()?.let { ep ->
                                    com.agon.app.data.EpisodeDownloaderManager.startDownload(
                                        context = context,
                                        showId = show.id,
                                        showTitle = show.title,
                                        episodeId = ep.id,
                                        episodeTitle = ep.title,
                                        episodeNum = ep.episodeNum,
                                        poster = ep.poster.ifEmpty { show.poster },
                                        videoUrl = ep.link.ifEmpty { show.link }
                                    )
                                    snackbarMessage = "بدأ تحميل الحلقة! اضغط للذهاب للحلقات المحملة"
                                    showDownloadSnackbar = true
                                }
                            }
                            .background(Color(0xFFE50914), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "تنزيل الحلقة للمشاهدة دون إنترنت",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                val isDownloaded = downloads.contains(show.id)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .tvClickable(shape = RoundedCornerShape(6.dp)) { vm.toggleDownload(show.id) }
                        .background(
                            if (isDownloaded) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Filled.Check else Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isDownloaded) "تمت الإضافة" else "إضافة إلى المشاهدة لاحقاً",
                            color = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                
                Text(
                    text = "قصة العمل",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(show.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 21.sp)

                Spacer(Modifier.height(16.dp))

                if (show.castMembers.isNotEmpty()) {
                    Text(
                        text = "طاقم العمل",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(show.castMembers) { member ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(80.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (member.profilePath != null) {
                                        coil3.compose.AsyncImage(
                                            model = member.profilePath,
                                            contentDescription = member.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = member.name,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else if (show.cast.isNotEmpty()) {
                    Text(
                        text = "طاقم العمل",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = show.cast.joinToString("، "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ActionItem(
                        icon = if (inList) Icons.Filled.Check else Icons.Filled.Add,
                        label = "قائمتي",
                    ) { vm.toggleMyList(show.id) }
                    ActionItem(Icons.Filled.ThumbUp, "تقييم") {}
                    ActionItem(Icons.Filled.Share, "مشاركة") {}
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        if (isLoadingEpisodes) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (isComingSoon) {
            item {
                ComingSoonBanner()
            }
        } else if (!show.isMovie && episodes.isNotEmpty()) {
            if (seasons.size > 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "اختر الموسم",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(seasons) { sNum ->
                                val isSelected = sNum == selectedSeason
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFE50914) else Color(0xFF1E1E1E))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.4f) else Color(0xFF333333),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .tvClickable(shape = RoundedCornerShape(8.dp)) { selectedSeason = sNum }
                                        .padding(horizontal = 22.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "الموسم $sNum",
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (seasons.size > 1) "حلقات الموسم $selectedSeason (${filteredEpisodes.size})" else "قائمة الحلقات (${episodes.size})",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            items(filteredEpisodes) { ep ->
                val isDownloaded = com.agon.app.data.EpisodeDownloaderManager.isEpisodeDownloaded(ep.id, show.id)
                val progresses by com.agon.app.data.EpisodeDownloaderManager.downloadProgresses.collectAsState()
                val progressPct = progresses[ep.id]

                EpisodeItemRow(
                    episode = ep,
                    isAlooy = isAlooy,
                    isSelected = selectedEpisodeId == ep.id,
                    isDownloaded = isDownloaded,
                    progressPct = progressPct,
                    onDownload = {
                        val videoToDownload = ep.link.ifEmpty { show.link }
                        if (videoToDownload.isNotEmpty()) {
                            com.agon.app.data.EpisodeDownloaderManager.startDownload(
                                context = context,
                                showId = show.id,
                                showTitle = show.title,
                                episodeId = ep.id,
                                episodeTitle = ep.title,
                                episodeNum = ep.episodeNum,
                                poster = ep.poster.ifEmpty { show.poster },
                                videoUrl = videoToDownload
                            )
                            snackbarMessage = "بدأ تحميل الحلقة ${ep.episodeNum}! اضغط للذهاب للحلقات المحملة"
                            showDownloadSnackbar = true
                        }
                    },
                    onClick = { 
                        selectedEpisodeId = ep.id
                        onPlay(show.id, ep.id) 
                    }
                )
            }
        }

        if (related.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "مشابه لهذا",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = if (deviceType == "tv") 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(related, key = { it.id }) { r ->
                        com.agon.app.ui.components.PosterCard(
                            show = r,
                            width = if (deviceType == "tv") 115 else 135,
                            onClick = { onOpenShow(r.id) }
                        )
                    }
                }
            }
        }
    }
        
        // Beautiful floating snackbar for downloading feedback
        androidx.compose.animation.AnimatedVisibility(
            visible = showDownloadSnackbar,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        showDownloadSnackbar = false
                        onNavigateToDownloads() 
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = snackbarMessage,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "اذهب",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingChip(rating: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(rating, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .tvClickable(shape = RoundedCornerShape(8.dp)) { onClick() }
            .padding(8.dp),
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
fun EpisodeItemRow(
    episode: Episode,
    isAlooy: Boolean = false,
    isSelected: Boolean = false,
    isDownloaded: Boolean = false,
    progressPct: Int? = null,
    onDownload: () -> Unit = {},
    onClick: () -> Unit
) {
    val isComingSoon = episode.id == "coming_soon_next"
    val isDownloading = progressPct != null && progressPct in 0..99

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .tvClickable(shape = RoundedCornerShape(8.dp), enabled = !isComingSoon) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
                if (episode.poster.isNotEmpty()) {
                    val cleanedEpPoster = remember(episode.poster) { com.agon.app.data.TmdbHelper.cleanPosterUrl(episode.poster) }
                    coil3.compose.AsyncImage(
                        model = cleanedEpPoster,
                        contentDescription = episode.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isSelected) Color(0xFFE50914).copy(alpha = 0.6f) 
                            else Color.Black.copy(alpha = if (isComingSoon) 0.6f else 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isComingSoon) Icons.Filled.Info else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (isComingSoon) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                if (episode.episodeNum.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(
                                color = if (isComingSoon) Color.Gray else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(bottomEnd = 8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = episode.episodeNum,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isComingSoon) "الحلقة القادمة" else "الحلقة ${episode.episodeNum}",
                    color = if (isSelected) Color(0xFFE50914) 
                           else if (isComingSoon) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) 
                           else MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.title,
                    color = if (isComingSoon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                if (isSelected) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "جاري التشغيل 🎬",
                        color = Color(0xFFE50914),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isAlooy && !isComingSoon) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isDownloaded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تم التحميل", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isDownloading) {
                        Column {
                            Text("جاري التحميل... $progressPct%", color = com.agon.app.ui.theme.NetflixRed, fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = (progressPct ?: 0) / 100f,
                                modifier = Modifier.fillMaxWidth(0.8f).height(3.dp).clip(RoundedCornerShape(2.dp)),
                                color = com.agon.app.ui.theme.NetflixRed,
                                trackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .clickable { onDownload() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = "تحميل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "تنزيل الحلقة",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingSoonBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "تعرض قريباً على تطبيقنا",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "هذا العمل غير متوفر حالياً وسيتم إضافته فور صدوره.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
