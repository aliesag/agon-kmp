package com.agon.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.tvClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: Dp = 2.dp,
    borderColor: Color = Color(0xFFE50914), // NetflixRed
    scaleOnFocus: Float = 1.08f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleOnFocus else 1f,
        label = "focusScale"
    )
    
    this
        .scale(scale)
        .then(
            if (isFocused) {
                Modifier.border(borderWidth, borderColor, shape)
            } else {
                Modifier
            }
        )
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Clear TV indicator
            enabled = enabled,
            onClick = onClick
        )
        .focusable(interactionSource = interactionSource)
}
