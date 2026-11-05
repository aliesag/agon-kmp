package com.agon.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = NetflixWhite,
    secondary = Gold,
    onSecondary = NetflixBlack,
    tertiary = NetflixRedDark,
    background = NetflixBlack,
    onBackground = NetflixWhite,
    surface = NetflixSurface,
    onSurface = NetflixWhite,
    surfaceVariant = NetflixSurfaceVariant,
    onSurfaceVariant = NetflixLightGrey,
    outline = NetflixGrey,
)

private val LightColorScheme = lightColorScheme(
    primary = NetflixRed,
    onPrimary = NetflixWhite,
    secondary = NetflixRedDark,
    onSecondary = NetflixWhite,
    tertiary = Gold,
    background = LightBg,
    onBackground = NetflixBlack,
    surface = LightSurface,
    onSurface = NetflixBlack,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF44444C),
    outline = NetflixGrey,
)

@Composable
fun AgonAppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
