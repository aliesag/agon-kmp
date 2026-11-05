package com.agon.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.data.Catalog
import com.agon.app.data.Show
import com.agon.app.ui.tvClickable
import com.agon.app.viewmodel.Progress

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp, top = 4.dp),
    )
}

@Composable
fun PosterCard(
    show: Show,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 135,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .width(width.dp)
            .tvClickable(shape = shape, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(Color(0xFF1E1E28))
        ) {
            PosterArt(
                showId = show.id,
                posterUrl = show.poster,
                title = show.title,
                modifier = Modifier.fillMaxSize(),
                titleSize = 13,
                onClick = onClick
            )

            // Gradient bottom overlay for subtle title readability if needed
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Ribbon Badge (e.g. فيلم / رمضان / خليجي / مترجم / مدبلج)
            val isAlooyShow = show.link.contains("alooytv") || show.id.contains("alooy") || show.id.contains("ramadan") || show.id.contains("kleeji")
            val ribbonText = when {
                show.isMovie -> if (!show.ribbon.isNullOrBlank() && show.ribbon.contains("حصري")) "فيلم حصري" else "فيلم"
                isAlooyShow -> {
                    when {
                        show.genres.contains("رمضان") || show.title.contains("رمضان") || show.id.contains("ramadan") -> "رمضان"
                        show.genres.contains("خليجي") || show.genres.contains("مسلسلات خليجية") || show.id.contains("kleeji") -> "خليجي"
                        !show.ribbon.isNullOrBlank() && !show.ribbon.contains("مدبلج") && !show.ribbon.contains("مترجم") -> show.ribbon
                        else -> "خليجي"
                    }
                }
                !show.ribbon.isNullOrBlank() -> show.ribbon
                show.rating == "مدبلج" || show.genres.contains("مدبلج") -> "مدبلج"
                show.rating == "مترجم" || show.genres.contains("مترجم") -> "مترجم"
                else -> null
            }

            if (ribbonText != null) {
                val badgeBg = when {
                    ribbonText.contains("فيلم") -> Brush.horizontalGradient(listOf(Color(0xFFE50914), Color(0xFF8E24AA)))
                    ribbonText == "رمضان" -> Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFF57C00)))
                    ribbonText == "خليجي" -> Brush.horizontalGradient(listOf(Color(0xFF00ACC1), Color(0xFF00838F)))
                    ribbonText == "حلقة جديدة" || ribbonText == "مضاف حديثاً" || ribbonText == "جديد حكايتنا" -> Brush.horizontalGradient(listOf(Color(0xFFE50914), Color(0xFFB81D24)))
                    ribbonText == "مدبلج" -> Brush.horizontalGradient(listOf(Color(0xFFFF9800), Color(0xFFF57C00)))
                    else -> Brush.horizontalGradient(listOf(Color(0xFF00E676), Color(0xFF00A152)))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = ribbonText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // IMDB rating in bottom end
            if (show.imdb > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = formatOneDecimal(show.imdb),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(6.dp))
        Text(
            text = show.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ShowRow(title: String, shows: List<Show>, onClick: (Show) -> Unit) {
    Column {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shows, key = { show -> show.id }) { show ->
                PosterCard(show = show, onClick = { onClick(show) })
            }
        }
    }
}

@Composable
fun Top10Row(title: String, shows: List<Show>, onClick: (Show) -> Unit) {
    Column {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(shows.take(10), key = { _, show -> show.id }) { index, show ->
                Box {
                    PosterCard(
                        show = show,
                        onClick = { onClick(show) },
                        width = 130,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(topEnd = 8.dp, bottomStart = 16.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFFE91E63), Color(0xFFFF1744))))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "TOP",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueRow(
    title: String,
    items: List<Progress>,
    onClick: (Show) -> Unit,
) {
    if (items.isEmpty()) return
    Column {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items) { p ->
                val show = Catalog.byId(p.showId) ?: return@items
                Column(modifier = Modifier.width(220.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .tvClickable(shape = RoundedCornerShape(8.dp)) { onClick(show) }
                    ) {
                        PosterArt(
                            showId = show.id, posterUrl = show.poster,
                            title = show.title,
                            modifier = Modifier.fillMaxSize(),
                            titleSize = 15,
                            onClick = { onClick(show) }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(44.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { p.fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        text = p.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun RatingBadge(imdb: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = " " + formatOneDecimal(imdb),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
