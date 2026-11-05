package com.agon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.agon.app.viewmodel.AppViewModel
import com.agon.app.ui.theme.NetflixRed

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(vm: AppViewModel, onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "أهلاً بك في عالم الترفيه",
            description = "استمتع بتجربة سينمائية فريدة واكتشف آلاف الأفلام والمسلسلات الحصرية بجودة عالية.",
            icon = androidx.compose.material.icons.Icons.Default.Cloud
        ),
        OnboardingPage(
            title = "كل شيء في متناول يدك",
            description = "شاهد أحدث الإصدارات، ابحث عن أعمالك المفضلة، وشارك تجاربك مع مجتمعنا.",
            icon = androidx.compose.material.icons.Icons.Default.Movie
        ),
        OnboardingPage(
            title = "جاهز للانطلاق؟",
            description = "ابدأ الآن واكتشف عالمًا لا ينتهي من القصص الممتعة.",
            icon = androidx.compose.material.icons.Icons.Default.PlayArrow
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF200000), Color.Black)
                )
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = pages[page].icon,
                    contentDescription = null,
                    tint = NetflixRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(120.dp).padding(bottom = 32.dp)
                )
                Text(
                    text = pages[page].title,
                    color = NetflixRed,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = pages[page].description,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
            }
        }

        // Indicators and Next button
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) NetflixRed else Color.DarkGray)
                    )
                }
            }

            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        vm.setOnboardingCompleted(true)
                        onComplete()
                    }
                },
                containerColor = NetflixRed,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next"
                )
            }
        }
    }
}

data class OnboardingPage(val title: String, val description: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
