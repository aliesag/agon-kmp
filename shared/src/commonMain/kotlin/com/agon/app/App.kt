package com.agon.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agon.app.ui.theme.AgonAppTheme

// Entry composable shared by Android + iOS. Screens get wired in here as they leave /pending.
@Composable
fun App() {
    AgonAppTheme {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Agon KMP — shell up")
            }
        }
    }
}
