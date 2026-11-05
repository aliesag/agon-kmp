package com.agon.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.agon.app.data.Catalog
import com.agon.app.data.TmdbHelper
import kotlin.math.abs

@Composable
fun PosterArt(
    showId: String,
    posterUrl: String = "",
    title: String,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    titleSize: Int = 18,
    fitMode: String = "cover",
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val cleanedOriginal = remember(posterUrl) { TmdbHelper.cleanPosterUrl(posterUrl) }

    val art = remember(showId) { Catalog.artFor(showId) }
    val seed = remember(showId) { abs(showId.hashCode()) }

    val imageRequest = remember(cleanedOriginal) {
        if (cleanedOriginal.isNotEmpty()) {
            val host = try {
                val h = java.net.URI(cleanedOriginal).host
                if (!h.isNullOrBlank()) h else "e.3cktv.com"
            } catch (e: Exception) { "e.3cktv.com" }

            ImageRequest.Builder(context)
                .data(cleanedOriginal)
                .crossfade(true)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .addHeader("Referer", "https://$host/")
                .build()
        } else null
    }

    Box(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    ) {
        // Base Layer: Lightweight background (draw complex canvas only if no image exists)
        if (cleanedOriginal.isEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(art.c1, art.c2),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                    )
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF14141B))
            )
        }

        // Fast Coil AsyncImage with ContentScale.FillBounds (stretches poster to fill 100% of card)
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Title Overlay: Show title text if showTitle is true OR if image URL is empty
        if (showTitle || cleanedOriginal.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = titleSize.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = (titleSize + 3).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}




