package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agon.app.ui.components.PosterArt
import com.agon.app.ui.components.RatingBadge
import com.agon.app.ui.components.VoiceSearchOverlay
import com.agon.app.ui.tvClickable
import com.agon.app.viewmodel.AppViewModel

@Composable
fun SearchScreen(vm: AppViewModel, onOpenShow: (String) -> Unit) {
    val deviceType by vm.deviceType.collectAsStateWithLifecycle()
    val isSearchingRemote by vm.isSearchingRemote.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("الكل") }
    var initialLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Trigger full catalog if empty
        try {
            if (!vm.fullCatalogLoaded.value) {
                vm.fetchFullCatalogFromSite()
            }
        } catch (_: Throwable) { }
        kotlinx.coroutines.delay(500)
        initialLoading = false
    }

    LaunchedEffect(query) {
        if (query.isNotEmpty() && query.length >= 2) {
            vm.performRemoteSearch(query)
        }
    }

    val searchFilters = remember {
        listOf(
            "الكل", "أفلام", "مسلسلات عربية", "مسلسلات تركية", "مدبلج", "مترجم",
            "خليجي", "رمضان", "دراما", "كوميدي", "أكشن", "رومانسي"
        )
    }

    val results = try { vm.search(query, filterType) } catch (_: Throwable) { emptyList() }

    var showVoiceSearchOverlay by remember { mutableStateOf(false) }

    if (showVoiceSearchOverlay) {
        VoiceSearchOverlay(
            visible = true,
            onDismiss = { showVoiceSearchOverlay = false },
            onResult = { text ->
                query = text
                showVoiceSearchOverlay = false
            }
        )
    }

    val horizontalPadding = if (deviceType == "tv") 64.dp else 16.dp

    Column(modifier = Modifier.fillMaxSize().padding(top = if (deviceType == "tv") 24.dp else 12.dp)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("ابحث عن مسلسل أو فيلم...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "مسح", tint = Color.Gray)
                        }
                    }
                    IconButton(onClick = { showVoiceSearchOverlay = true }) {
                        Icon(Icons.Filled.Mic, contentDescription = "بحث بالصوت", tint = Color(0xFFE50914))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
                focusedIndicatorColor = Color(0xFFE50914),
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFE50914),
                focusedLeadingIconColor = Color(0xFFE50914),
                unfocusedLeadingIconColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(searchFilters) { f ->
                val selected = f == filterType
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Color(0xFFE50914) else Color(0xFF1E1E1E))
                        .tvClickable(shape = RoundedCornerShape(50)) { filterType = f }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        f,
                        color = if (selected) Color.White else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "النتائج: ${results.size}",
                color = Color(0xFF8C8C94),
                fontSize = 12.sp
            )
            if (query.isEmpty()) {
                Text(
                    text = "جلب كل المسلسلات",
                    color = Color(0xFFE50914),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { vm.fetchFullCatalogFromSite() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            isSearchingRemote || (initialLoading && query.isEmpty()) -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (initialLoading && query.isEmpty()) "جاري التحميل..." else "جاري البحث...",
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.SearchOff, contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("لا توجد نتائج مطابقة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            else -> {
                val columns = if (deviceType == "tv") GridCells.Fixed(5) else GridCells.Fixed(3)
                LazyVerticalGrid(
                    columns = columns,
                    contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    items(results.take(200)) { show ->
                        val isDubbed = show.genres.contains("مدبلج") || show.rating == "مدبلج"
                        val isArabic = show.genres.any { it.contains("عرب") }
                        val isTurkish = show.genres.any { it.contains("تركي") } || show.link.contains("3cktv")
                        val isSubbed = show.genres.contains("مترجم") || (isTurkish && !isDubbed)
                        val isRamadan = show.genres.contains("رمضان") || show.title.contains("رمضان")
                        val isKhaleeji = show.genres.contains("خليجي")

                        val tagText = when {
                            show.isMovie -> "فيلم"
                            isRamadan -> "رمضان"
                            isKhaleeji -> "خليجي"
                            isDubbed -> "مدبلج"
                            isSubbed -> "مترجم"
                            isArabic -> "عربي"
                            else -> show.rating.ifEmpty { "حصري" }
                        }
                        val tagBg = when {
                            show.isMovie -> Color(0xFFE50914)
                            isRamadan -> Color(0xFFFF9800)
                            isKhaleeji -> Color(0xFF00ACC1)
                            isDubbed -> Color(0xFF2E7D32)
                            isSubbed -> Color(0xFF1565C0)
                            isArabic -> Color(0xFF8E24AA)
                            else -> Color(0xFFE50914).copy(alpha = 0.8f)
                        }

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .tvClickable(shape = RoundedCornerShape(8.dp)) { onOpenShow(show.id) }
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
                                PosterArt(
                                    showId = show.id,
                                    posterUrl = show.poster,
                                    title = show.title,
                                    modifier = Modifier.fillMaxSize(),
                                    titleSize = 14,
                                    onClick = { onOpenShow(show.id) }
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(tagBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(tagText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            RatingBadge(show.imdb)
                        }
                    }
                }
            }
        }
    }
}
