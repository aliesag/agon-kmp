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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
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
    val cleanedOriginal = remember(posterUrl) { TmdbHelper.cleanPosterUrl(posterUrl) }

    val art = remember(showId) { Catalog.artFor(showId) }
    val seed = remember(showId) { abs(showId.hashCode()) }

    // FILL: custom ImageLoader with Referer/UA headers (needed by 3cktv) is wired at the app level.
    // Coil 3's AsyncImage accepts a plain URL string here; headers come from ImageLoader config.
    val imageRequest: String? = remember(cleanedOriginal) {
        if (cleanedOriginal.isNotEmpty()) cleanedOriginal else null
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




