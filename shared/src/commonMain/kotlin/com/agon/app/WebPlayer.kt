package com.agon.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Replaces android.webkit.WebView usage (PlayerScreen, MatchPlayerScreen, OfflinePlayerScreen).
@Composable
expect fun WebPlayer(html: String?, url: String?, modifier: Modifier = Modifier)
