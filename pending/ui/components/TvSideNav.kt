package com.agon.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.navigation.NavHostController
import com.agon.app.ui.theme.NetflixSurface
import com.agon.app.ui.theme.NetflixRed
import com.agon.app.ui.tvClickable

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@Composable
fun TvSideNav(
    navController: NavHostController,
    currentRoute: String?,
    focusRequester: FocusRequester? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(targetValue = if (isExpanded) 220.dp else 72.dp, label = "width")

    // Define items for the TV sidebar
    val navItems = listOf(
        TvNavItem("home", "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
        TvNavItem("matches", "المباريات", Icons.Filled.SportsSoccer, Icons.Outlined.SportsSoccer),
        TvNavItem("mylist", "قائمتي", Icons.Filled.Bookmark, Icons.Outlined.Bookmark),
        TvNavItem("shorts", "شورتس", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
        TvNavItem("chat", "الدردشة", Icons.Filled.Forum, Icons.Outlined.Forum),
        TvNavItem("hakim", "حكيم الذكي", Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh),
        TvNavItem("search", "البحث", Icons.Filled.Search, Icons.Outlined.Search),
        TvNavItem("profile", "المزيد", Icons.Filled.Menu, Icons.Outlined.Menu)
    )

    Surface(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onRootFocusChanged { expanded ->
                isExpanded = expanded
            },
        color = NetflixSurface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App Logo / Title at the Top
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NetflixRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ح",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "حكايتنا",
                        color = NetflixRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Navigation Items with vertical scroll for small TV viewports
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    TvSideNavItem(
                        item = item,
                        selected = selected,
                        isExpanded = isExpanded,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo("home") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }

            // Footer / Settings indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvClickable(shape = RoundedCornerShape(8.dp)) {
                        // Navigate to settings or profile
                        navController.navigate("profile")
                    }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "الاعدادات",
                        tint = Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                    if (isExpanded) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "الإعدادات",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSideNavItem(
    item: TvNavItem,
    selected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundCol = when {
        isFocused -> NetflixRed.copy(alpha = 0.25f)
        selected -> NetflixRed.copy(alpha = 0.18f)
        else -> Color.Transparent
    }

    val borderCol = if (isFocused) NetflixRed else Color.Transparent
    val contentCol = if (selected || isFocused) NetflixRed else Color.LightGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .border(1.5.dp, borderCol, RoundedCornerShape(8.dp))
            .background(backgroundCol, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = if (selected) item.filledIcon else item.outlinedIcon,
            contentDescription = item.title,
            tint = contentCol,
            modifier = Modifier.size(22.dp)
        )
        if (isExpanded) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = item.title,
                color = if (selected) Color.White else Color.LightGray,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// Custom modifier helper to track focus changes on child items to expand/collapse sidebar
@Composable
private fun Modifier.onRootFocusChanged(onFocusChanged: (Boolean) -> Unit): Modifier {
    var hasFocus by remember { mutableStateOf(false) }
    return this.focusable().onFocusChanged { state ->
        // Jetpack Compose FocusState can tell if a descendant has focus
        val currentHasFocus = state.hasFocus
        if (currentHasFocus != hasFocus) {
            hasFocus = currentHasFocus
            onFocusChanged(currentHasFocus)
        }
    }
}

data class TvNavItem(
    val route: String,
    val title: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)
