package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agon.app.WebPlayer
import com.agon.app.data.matches.MatchItem
import com.agon.app.data.matches.MatchServer
import com.agon.app.data.matches.MatchStatus

private val NetflixRed = Color(0xFFE50914)

private fun matchEmbedHtml(embedUrl: String): String = """
<!DOCTYPE html>
<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>
<style>html,body{margin:0;padding:0;background:#000;height:100%;overflow:hidden}
iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style></head>
<body><iframe src="$embedUrl" allow="autoplay; fullscreen; encrypted-media" allowfullscreen></iframe></body>
</html>
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchPlayerScreen(
    match: MatchItem,
    otherMatches: List<MatchItem> = emptyList(),
    onBack: () -> Unit,
    onSelectMatch: (MatchItem) -> Unit = {}
) {
    var selectedServer by remember(match.id) { mutableStateOf(match.servers.firstOrNull()) }
    var showServerSheet by remember { mutableStateOf(false) }
    var showMatchSheet by remember { mutableStateOf(false) }

    val embedUrl = selectedServer?.embedUrl ?: match.embedUrl
    val html = remember(embedUrl) { matchEmbedHtml(embedUrl) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            WebPlayer(html = html, url = null, modifier = Modifier.fillMaxSize())
        }

        // Match header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16161C))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            MatchTeams(match = match, modifier = Modifier.weight(1f))
        }

        // Controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showServerSheet = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) { Text("السيرفر: ${selectedServer?.name ?: "افتراضي"}", fontSize = 13.sp) }

            if (otherMatches.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showMatchSheet = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("مباراة أخرى", fontSize = 13.sp) }
            }
        }

        // Match info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(match.tournament, color = Color(0xFFB3B3B3), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("${match.channel} • ${match.commentator}", color = Color(0xFF8C8C94), fontSize = 11.sp)
        }
    }

    if (showServerSheet) {
        ModalBottomSheet(onDismissRequest = { showServerSheet = false }, containerColor = Color(0xFF16161C)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("اختر السيرفر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                match.servers.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedServer = s; showServerSheet = false }
                            .background(if (s == selectedServer) NetflixRed.copy(alpha = 0.2f) else Color.Transparent)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(s.name, color = Color.White)
                        Text(s.qualityBadge, color = Color(0xFF8C8C94), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showMatchSheet) {
        ModalBottomSheet(onDismissRequest = { showMatchSheet = false }, containerColor = Color(0xFF16161C)) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text("مباريات أخرى", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                }
                items(otherMatches) { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectMatch(m); showMatchSheet = false }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MatchTeams(match = m, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchTeams(match: MatchItem, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = match.team1Logo,
            contentDescription = match.team1Name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(match.team1Name, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(match.team2Name, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(6.dp))
        AsyncImage(
            model = match.team2Logo,
            contentDescription = match.team2Name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(28.dp)
        )
    }
}
