package com.agon.app

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agon.app.data.PrefsManager
import com.agon.app.data.matches.MatchesRepository
import com.agon.app.ui.screens.*
import com.agon.app.ui.theme.AgonAppTheme
import com.agon.app.viewmodel.AppViewModel
import com.agon.app.viewmodel.MatchesViewModel
import kotlinx.coroutines.launch

private val NetflixRed = Color(0xFFE50914)
private val BgColor = Color(0xFF0B0B0F)
private val SurfaceColor = Color(0xFF16161C)
private val Grey = Color(0xFF8C8C94)

@Composable
fun App() {
    AgonAppTheme {
        val prefs = remember { PrefsManager() }
        val vm = remember { AppViewModel(prefs) }
        val matchesVm = remember { MatchesViewModel() }
        val nav = rememberNavController()
        val scope = rememberCoroutineScope()

        val deviceType by vm.deviceType.collectAsStateWithLifecycle()
        // Kick off full catalog fetch once, in background, after boot finishes
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(8000) // wait for boot to settle
            if (!vm.fullCatalogLoaded.value) {
                vm.fetchFullCatalogFromSite()
            }
        }
        val onboardingCompleted by vm.onboardingCompleted.collectAsStateWithLifecycle()
        val skipIntro by vm.skipIntro.collectAsStateWithLifecycle()
        var hasPlayedIntro by remember { mutableStateOf(false) }
        var bootComplete by remember { mutableStateOf(false) }

        // Boot gates (mirror of original MainActivity start flow):
        // device selection -> intro/preload -> onboarding -> main nav
        when {
            deviceType.isBlank() -> {
                DeviceSelectionScreen(onDeviceSelected = { selected ->
                    scope.launch { vm.setDeviceType(selected) }
                })
            }
            !bootComplete -> {
                if (!skipIntro && !hasPlayedIntro) {
                    HekayatnaProfessionalIntro(
                        deviceType = deviceType,
                        onIntroFinished = {
                            hasPlayedIntro = true
                            vm.markIntroCompleted()
                        }
                    )
                } else {
                    AppHomePreloadScreen(onPreloadComplete = { bootComplete = true })
                }
            }
            !onboardingCompleted -> {
                OnboardingScreen(
                    vm = vm,
                    onComplete = {
                        scope.launch { prefs.setOnboardingCompleted(true) }
                        bootComplete = true
                    }
                )
            }
            else -> {
                MainContent(vm = vm, matchesVm = matchesVm, nav = nav)
            }
        }
    }
}

@Composable
private fun MainContent(
    vm: AppViewModel,
    matchesVm: MatchesViewModel,
    nav: NavHostController
) {
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val bottomRoutes = setOf("home", "matches", "mylist", "chat", "profile")
    val showBottomBar = currentRoute in bottomRoutes

    var showReportDialog by remember { mutableStateOf(false) }
    if (showReportDialog) {
        com.agon.app.ui.components.ReportIssueDialog(
            deviceType = vm.deviceType.value.ifBlank { "mobile" },
            onDismiss = { showReportDialog = false }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BgColor) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (currentRoute == "home") {
                BrandBar(
                    onSearchClick = { nav.navigate("search") },
                    onHakimClick = { nav.navigate("hakim") },
                    onReportClick = { showReportDialog = true }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            vm = vm,
                            matchesVm = matchesVm,
                            onOpenMatches = { nav.navigate("matches") },
                            onOpenShow = { id -> nav.navigate("detail/$id") },
                            onPlay = { showId, epId ->
                                if (epId != null) nav.navigate("player/$showId/$epId")
                                else nav.navigate("detail/$showId")
                            }
                        )
                    }
                    composable("matches") {
                        MatchesScreen(
                            vm = matchesVm,
                            onOpenMatch = { id -> nav.navigate("match_player/$id") }
                        )
                    }
                    composable("match_player/{matchId}") { entry ->
                        val id = entry.arguments?.getString("matchId").orEmpty()
                        val matches = MatchesRepository.getInitialMatches()
                        val match = matches.find { it.id == id } ?: matches.first()
                        MatchPlayerScreen(
                            match = match,
                            otherMatches = matches.filter { it.id != match.id },
                            onBack = { nav.popBackStack() },
                            onSelectMatch = { m ->
                                nav.navigate("match_player/${m.id}") {
                                    popUpTo("match_player/$id") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("search") {
                        TopBarScreen(title = "البحث", onBack = { nav.popBackStack() }) {
                            SearchScreen(vm = vm, onOpenShow = { id -> nav.navigate("detail/$id") })
                        }
                    }
                    composable("mylist") {
                        MyListScreen(
                            vm = vm,
                            onOpenShow = { id -> nav.navigate("detail/$id") },
                            onOpenDownloads = { nav.navigate("downloaded_episodes") }
                        )
                    }
                    composable("downloaded_episodes") {
                        DownloadedEpisodesScreen(
                            onBack = { nav.popBackStack() },
                            onPlayOffline = { path, title ->
                                nav.navigate("offline_player/$path/$title")
                            }
                        )
                    }
                    composable("shorts") {
                        ComingSoonScreen("شورتس", "مشغّل الفيديو العمودي عبر AVPlayer في المرحلة القادمة") { nav.popBackStack() }
                    }
                    composable("chat") {
                        ChatScreen(vm = vm, onBack = { nav.popBackStack() })
                    }
                    composable("hakim") {
                        HakimChatScreen(vm = vm, onBack = { nav.popBackStack() }, onOpenShow = { id -> nav.navigate("detail/$id") })
                    }
                    composable("profile") {
                        SettingsScreen(
                            vm = vm,
                            authViewModel = null,
                            onLogout = {
                                nav.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("login") {
                        val authVm = remember { com.agon.app.ui.AuthViewModel() }
                        LoginScreen(
                            authViewModel = authVm,
                            onLoginSuccess = { nav.navigate("home") { popUpTo("home") { inclusive = true } } },
                            onNavigateRegister = { nav.navigate("register") },
                            onNavigateForgotPassword = { nav.navigate("forgot_password") }
                        )
                    }
                    composable("register") {
                        val authVm = remember { com.agon.app.ui.AuthViewModel() }
                        RegisterScreen(
                            authViewModel = authVm,
                            onRegisterSuccess = { nav.navigate("home") { popUpTo("home") { inclusive = true } } },
                            onNavigateVerification = { },
                            onNavigateLogin = { nav.navigate("login") },
                            onStoreExtra = { username, age -> }
                        )
                    }
                    composable("forgot_password") {
                        val authVm = remember { com.agon.app.ui.AuthViewModel() }
                        ForgotPasswordScreen(
                            authViewModel = authVm,
                            onBackToLogin = { nav.popBackStack() }
                        )
                    }
                    composable("chat_login") {
                        ChatLoginScreen(
                            vm = vm,
                            onSuccess = { nav.popBackStack() },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("detail/{id}") { entry ->
                        val id = entry.arguments?.getString("id").orEmpty()
                        DetailScreen(
                            showId = id,
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onPlay = { showId, epId ->
                                if (epId != null) nav.navigate("player/$showId/$epId")
                            },
                            onOpenShow = { sid -> nav.navigate("detail/$sid") },
                            onNavigateToDownloads = { nav.navigate("downloaded_episodes") }
                        )
                    }
                    composable("player/{id}/{episodeId}") { entry ->
                        val id = entry.arguments?.getString("id").orEmpty()
                        val epId = entry.arguments?.getString("episodeId")
                        PlayerScreen(
                            showId = id,
                            episodeId = epId,
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onOpenShow = { sid -> nav.navigate("detail/$sid") }
                        )
                    }
                    composable("browse_all") {
                        BrowseAllScreen(
                            title = "تصفح الكل",
                            shows = vm.shows.value,
                            onOpenShow = { id -> nav.navigate("detail/$id") },
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
            }
            if (showBottomBar) {
                BottomNav(nav = nav, currentRoute = currentRoute)
            }
        }
    }
}

@Composable
private fun BrandBar(
    onSearchClick: () -> Unit,
    onHakimClick: () -> Unit,
    onReportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgColor)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(NetflixRed),
                contentAlignment = Alignment.Center
            ) {
                Text("ح", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hakim block
            Row(
                modifier = Modifier.clickable(onClick = onHakimClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4A1515))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "مستشار الدراما",
                                color = Color(0xFFFF2D3A),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 9.sp
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "حكيم الذكي",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AutoFixHigh,
                        contentDescription = "حكيم",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier.size(10.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                            .border(1.5.dp, BgColor, CircleShape)
                    )
                }
            }

            // Search
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
                    .clickable(onClick = onSearchClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Search, contentDescription = "بحث", tint = Color.White, modifier = Modifier.size(19.dp))
            }

            // Report
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
                    .clickable(onClick = onReportClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.BugReport, contentDescription = "بلاغ", tint = NetflixRed, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun TopBarScreen(
    title: String,
    onBack: () -> Unit,
    hideBack: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        if (!hideBack) {
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceColor)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun BottomNav(nav: NavHostController, currentRoute: String?) {
    val items = listOf(
        Triple("mylist", "حلقات محملة", Icons.Filled.Download),
        Triple("matches", "المباريات", Icons.Filled.SportsSoccer),
        Triple("home", "الرئيسية", Icons.Filled.Home),
        Triple("shorts", "شورتس", Icons.Filled.PlayCircle),
        Triple("chat", "الدردشة", Icons.Filled.Forum),
        Triple("profile", "المزيد", Icons.Filled.Menu)
    )
    Surface(color = SurfaceColor, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { (route, label, icon) ->
                val selected = currentRoute == route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f).fillMaxHeight()
                        .clickable {
                            nav.navigate(route) {
                                popUpTo("home") { inclusive = route == "home" }
                                launchSingleTop = true
                            }
                        },
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (selected) NetflixRed else Grey,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label,
                        color = if (selected) Color.White else Grey,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(title: String, description: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(BgColor).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(description, color = Grey, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
            shape = RoundedCornerShape(10.dp)
        ) { Text("رجوع", color = Color.White) }
    }
}
