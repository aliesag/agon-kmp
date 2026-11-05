package com.agon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agon.app.data.Episode
import com.agon.app.data.TmdbHelper
import com.agon.app.ui.tvClickable
import com.agon.app.ui.theme.NetflixRed

private val NetflixRedLocal = Color(0xFFE50914)

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
                if (episode.poster.isNotEmpty()) {
                    val cleanedEpPoster = remember(episode.poster) { TmdbHelper.cleanPosterUrl(episode.poster) }
                    AsyncImage(
                        model = cleanedEpPoster,
                        contentDescription = episode.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isSelected) NetflixRedLocal.copy(alpha = 0.6f)
                            else Color.Black.copy(alpha = if (isComingSoon) 0.6f else 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(
                            imageVector = if (isComingSoon) Icons.Filled.Info else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (isComingSoon) NetflixRedLocal else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                if (episode.episodeNum.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(
                                color = if (isComingSoon) Color.Gray else NetflixRedLocal,
                                shape = RoundedCornerShape(bottomEnd = 8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(episode.episodeNum, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isComingSoon) "الحلقة القادمة" else "الحلقة ${episode.episodeNum}",
                    color = if (isSelected) NetflixRedLocal else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.title,
                    color = if (isComingSoon) NetflixRedLocal else Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isSelected) {
                    Spacer(Modifier.height(4.dp))
                    Text("جاري التشغيل 🎬", color = NetflixRedLocal, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                if (isAlooy && !isComingSoon) {
                    Spacer(Modifier.height(8.dp))
                    if (isDownloaded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تم التحميل", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isDownloading) {
                        Column {
                            Text("جاري التحميل... $progressPct%", color = NetflixRedLocal, fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (progressPct ?: 0) / 100f },
                                modifier = Modifier.fillMaxWidth(0.8f).height(3.dp).clip(RoundedCornerShape(2.dp)),
                                color = NetflixRedLocal,
                                trackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NetflixRedLocal.copy(alpha = 0.12f))
                                .border(1.dp, NetflixRedLocal.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .clickable { onDownload() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.Download, contentDescription = "تحميل", tint = NetflixRedLocal, modifier = Modifier.size(16.dp))
                                Text("تنزيل الحلقة", color = NetflixRedLocal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
