package com.agon.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.agon.app.data.matches.MatchDay
import com.agon.app.data.matches.MatchItem
import com.agon.app.data.matches.MatchStatus
import com.agon.app.data.matches.SportsNewsItem
import com.agon.app.ui.theme.Gold
import com.agon.app.ui.theme.NetflixBlack
import com.agon.app.ui.theme.NetflixRed
import com.agon.app.ui.theme.NetflixSurface
import com.agon.app.ui.theme.NetflixSurfaceVariant
import com.agon.app.viewmodel.MatchesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    vm: MatchesViewModel,
    onOpenMatch: (String) -> Unit
) {
    val matches by vm.filteredMatches.collectAsStateWithLifecycle()
    val allMatches by vm.matches.collectAsStateWithLifecycle()
    val news by vm.news.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val selectedDay by vm.selectedDay.collectAsStateWithLifecycle()
    val selectedFilter by vm.selectedLeagueFilter.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val liveCount by vm.liveMatchesCount.collectAsStateWithLifecycle()

    var toastMessage by remember { mutableStateOf<String?>(null) }

    if (toastMessage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { toastMessage = null },
            title = { androidx.compose.material3.Text("تنبيه", color = Color.White) },
            text = { androidx.compose.material3.Text(toastMessage ?: "", color = Color.White) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { toastMessage = null }) {
                    androidx.compose.material3.Text("حسناً", color = Color(0xFFE50914))
                }
            },
            containerColor = Color(0xFF16161C)
        )
    }
    val handleMatchClick: (MatchItem) -> Unit = { match ->
        if (match.status == MatchStatus.LIVE && match.day == MatchDay.TODAY) {
            onOpenMatch(match.id)
        } else if (match.status == MatchStatus.NOT_STARTED || match.day == MatchDay.TOMORROW) {
            toastMessage = "المباراة لم تبدأ بعد ⏳ سيتوفر البث المباشر فور انطلاق صافرة البداية (${match.matchTime})"
        } else {
            toastMessage = "المباراة انتهت ⚽ البث المباشر متاح فقط للمباريات المباشرة الآن"
        }
    }

    var showSearchField by remember { mutableStateOf(false) }
    var selectedNewsItem by remember { mutableStateOf<SportsNewsItem?>(null) }
    val focusManager = LocalFocusManager.current

    val liveHighlightMatch = remember(allMatches) {
        allMatches.firstOrNull { it.status == MatchStatus.LIVE }
    }

    val leagueFilters = listOf(
        "الكل",
        "مباشر الآن",
        "الدوري الإسباني",
        "الدوري الإنجليزي",
        "الدوري الأوروبي",
        "الدوري المصري"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // --- Top Bar ---
        Surface(
            color = NetflixSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NetflixRed, Color(0xFF931800)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SportsSoccer,
                                contentDescription = "مباريات",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "مباريات اليوم",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (liveCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    LivePulseBadge(count = liveCount)
                                }
                            }
                            Text(
                                text = "بث مباشر وأهداف لحظة بلحظة ⚡",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                showSearchField = !showSearchField
                                if (!showSearchField) vm.setSearchQuery("")
                            }
                        ) {
                            Icon(
                                imageVector = if (showSearchField) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "بحث",
                                tint = if (showSearchField) NetflixRed else Color.White
                            )
                        }

                        IconButton(
                            onClick = { vm.refresh() },
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = NetflixRed,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "تحديث",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Search field dropdown
                AnimatedVisibility(visible = showSearchField) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { vm.setSearchQuery(it) },
                        placeholder = { Text("ابحث عن فريق، بطولة، أو معلق...", fontSize = 13.sp, color = Color.Gray) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { vm.setSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NetflixRed,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedContainerColor = Color(0xFF1F1F24),
                            unfocusedContainerColor = Color(0xFF1F1F24),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Day Selector Tabs ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF101014))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MatchDayTabItem(
                        title = "مباريات الأمس",
                        selected = selectedDay == MatchDay.YESTERDAY,
                        badge = null,
                        onClick = { vm.selectDay(MatchDay.YESTERDAY) },
                        modifier = Modifier.weight(1f)
                    )
                    MatchDayTabItem(
                        title = "مباريات اليوم",
                        selected = selectedDay == MatchDay.TODAY,
                        badge = if (liveCount > 0) "$liveCount مباشر" else null,
                        onClick = { vm.selectDay(MatchDay.TODAY) },
                        modifier = Modifier.weight(1f)
                    )
                    MatchDayTabItem(
                        title = "مباريات الغد",
                        selected = selectedDay == MatchDay.TOMORROW,
                        badge = null,
                        onClick = { vm.selectDay(MatchDay.TOMORROW) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- Filter Chips (Leagues & Status) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leagueFilters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) NetflixRed else Color(0xFF1C1D24)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NetflixRed else Color(0xFF2C2D35),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { vm.setLeagueFilter(filter) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // --- Main Content (Matches + Live Highlight + News) ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Highlight Card if on Today and live match exists
            if (selectedDay == MatchDay.TODAY && liveHighlightMatch != null && selectedFilter in listOf("الكل", "مباشر الآن")) {
                item {
                    LiveHighlightBanner(
                        match = liveHighlightMatch,
                        onClick = { handleMatchClick(liveHighlightMatch) }
                    )
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (selectedDay) {
                            MatchDay.YESTERDAY -> "نتائج مباريات الأمس"
                            MatchDay.TODAY -> "جدول مباريات اليوم (${matches.size})"
                            MatchDay.TOMORROW -> "مواعيد مباريات الغد"
                        },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedFilter,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            if (matches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.SportsSoccer,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد مباريات مطابقة للبحث أو الفلتر المحدد",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(matches, key = { it.id }) { match ->
                    MatchCard(
                        match = match,
                        onClick = { handleMatchClick(match) }
                    )
                }
            }

            // Sports News section
            if (news.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp, 16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(NetflixRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أحدث الأخبار والتقارير الرياضية",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "حكايتنا لايف",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                items(news, key = { it.id }) { newsItem ->
                    SportsNewsCard(
                        news = newsItem,
                        onClick = { selectedNewsItem = newsItem }
                    )
                }
            }
        }

        // News Reader Modal / Dialog
        selectedNewsItem?.let { newsItem ->
            SportsNewsDetailDialog(
                news = newsItem,
                onDismiss = { selectedNewsItem = null }
            )
        }
    }
}

@Composable
private fun MatchDayTabItem(
    title: String,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) NetflixRed else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = if (selected) Color.White else Color(0xFFAAAAAA),
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (badge != null) {
                Text(
                    text = badge,
                    color = if (selected) Color.White else NetflixRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LivePulseBadge(count: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NetflixRed.copy(alpha = 0.2f))
            .border(1.dp, NetflixRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(NetflixRed.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count مباشر",
            color = NetflixRed,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LiveHighlightBanner(
    match: MatchItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2B0E10),
                        Color(0xFF191A23)
                    )
                )
            )
            .border(1.5.dp, NetflixRed.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NetflixRed)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🔴 بث مباشر الآن",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (match.liveMinute != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = match.liveMinute,
                            color = Gold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = match.tournament,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Teams and Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Team 1
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    AsyncImage(
                        model = match.team1Logo,
                        contentDescription = match.team1Name,
                        modifier = Modifier.size(54.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = match.team1Name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center Score
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = match.score1,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = " : ",
                            color = NetflixRed,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = match.score2,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF333340))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = match.channel,
                            color = Color.LightGray,
                            fontSize = 10.sp
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
                        modifier = Modifier.size(54.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = match.team2Name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Watch Now Button
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "مشاهدة البث المباشر بجودة عالية",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MatchCard(
    match: MatchItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NetflixSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Tournament & Time info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = match.tournament,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge
                val statusColor = Color(match.status.colorHex)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = match.status.label,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teams Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team 1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AsyncImage(
                        model = match.team1Logo,
                        contentDescription = match.team1Name,
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = match.team1Name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center Score or Time
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    if (match.status == MatchStatus.NOT_STARTED) {
                        Text(
                            text = match.matchTime,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "بتوقيت مكة",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = match.score1,
                                color = if (match.status == MatchStatus.LIVE) NetflixRed else Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = " - ",
                                color = Color.Gray,
                                fontSize = 18.sp
                            )
                            Text(
                                text = match.score2,
                                color = if (match.status == MatchStatus.LIVE) NetflixRed else Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Team 2
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = match.team2Name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AsyncImage(
                        model = match.team2Logo,
                        contentDescription = match.team2Name,
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFF262630), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Channel and Commentator Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📺 ", fontSize = 11.sp)
                    Text(
                        text = match.channel,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "🎤 ", fontSize = 11.sp)
                    Text(
                        text = match.commentator,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Text(
                        text = if (match.status == MatchStatus.LIVE) "🔴 شاهد الآن" else if (match.status == MatchStatus.NOT_STARTED) "لم تبدأ بعد ⏳" else "انتهت ⚽",
                        color = if (match.status == MatchStatus.LIVE) NetflixRed else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = if (match.status == MatchStatus.LIVE) NetflixRed else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SportsNewsCard(
    news: SportsNewsItem,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NetflixSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = news.imageUrl,
                contentDescription = news.title,
                modifier = Modifier
                    .size(90.dp, 64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = news.category,
                        color = NetflixRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = news.date,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = news.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SportsNewsDetailDialog(
    news: SportsNewsItem,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14151B)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .border(1.dp, Color(0xFF2C2D3A), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Action Bar with category & close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NetflixRed.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = news.category,
                            color = NetflixRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222430))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Full Uncropped Image Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = news.imageUrl,
                            contentDescription = news.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 240.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = news.date,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = news.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF2E303B), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = news.content.ifEmpty { "تابع آخر التحديثات والتحليلات الحصرية حول هذا الخبر من خلال تطبيق حكايتنا." },
                        color = Color(0xFFE2E4EB),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("إغلاق الخبر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
