package com.agon.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import agon_kmp.shared.generated.resources.Res
import agon_kmp.shared.generated.resources.logo
import coil3.compose.AsyncImage
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agon.app.data.EpisodeDownloaderManager
import com.agon.app.data.PrefsManager
import com.agon.app.data.matches.MatchesRepository
import com.agon.app.platform.PlatformUi
import com.agon.app.platform.UserPrefs
import com.agon.app.ui.AuthState
import com.agon.app.ui.AuthViewModel
import com.agon.app.ui.components.AdMobBanner
import com.agon.app.ui.components.ReportIssueDialog
import com.agon.app.ui.screens.*
import com.agon.app.ui.theme.AgonAppTheme
import com.agon.app.viewmodel.AppViewModel
import com.agon.app.viewmodel.MatchesViewModel
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private data class Tab(val route: String, val label: String, val filled: ImageVector, val outlined: ImageVector)

// Same order/labels as the Android bottom bar.
private val tabs = listOf(
    Tab("mylist", "حلقات محملة", Icons.Filled.Download, Icons.Outlined.Download),
    Tab("matches", "المباريات", Icons.Filled.SportsSoccer, Icons.Outlined.SportsSoccer),
    Tab("home", "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
    Tab("shorts", "شورتس", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
    Tab("chat", "الدردشة", Icons.Filled.Forum, Icons.Outlined.Forum),
    Tab("profile", "المزيد", Icons.Filled.Menu, Icons.Outlined.Menu),
)

private val NetflixRed = Color(0xFFE50914)

// ---- route args ------------------------------------------------------------------------------
// java.net.URLEncoder / android Base64 don't exist in commonMain, and nav-compose may or may not
// percent-decode path args. Hex of the UTF-8 bytes (prefixed with "x" so it's never empty) is immune.
private fun encArg(s: String): String =
    "x" + s.encodeToByteArray().joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

private fun decArg(a: String?): String {
    if (a.isNullOrEmpty() || a[0] != 'x') return a.orEmpty()
    val hex = a.substring(1)
    return try {
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }.decodeToString()
    } catch (_: Throwable) { "" }
}

private suspend fun checkOnline(): Boolean = try {
    Http.client.get("https://www.gstatic.com/generate_204"); true
} catch (_: Throwable) { false }

@Composable
fun App() {
    AgonAppTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            MainApp()
        }
    }
}

@Composable
private fun MainApp() {
    val prefs = remember { PrefsManager() }
    val vm = remember { AppViewModel(prefs) }
    val authViewModel = remember { AuthViewModel() }
    val matchesVm = remember { MatchesViewModel() }
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBars = currentRoute in tabs.map { it.route }

    val isInitialized by vm.isInitialized.collectAsState()
    val updateInfo by vm.updateInfo.collectAsState()
    val deviceType by vm.deviceType.collectAsState()
    val skipIntro by vm.skipIntro.collectAsState()
    val hasPlayedIntroSession by vm.hasPlayedIntroSession.collectAsState()

    LaunchedEffect(Unit) { EpisodeDownloaderManager.init() }

    // ---- force update (same version rules as MainActivity) ----
    val currentVer = BuildConfig.VERSION_NAME.trim().removePrefix("v").removePrefix("V")
    val targetVer = updateInfo?.versionName?.trim()?.removePrefix("v")?.removePrefix("V")
    val shouldShowUpdate = updateInfo != null &&
        targetVer != currentVer &&
        updateInfo!!.versionCode > BuildConfig.VERSION_CODE &&
        // update.json points at an .apk: useless on iOS, skip unless the file offers an iOS link
        (PlatformUi.isAndroid || !updateInfo!!.downloadUrl.substringBefore('?').endsWith(".apk", ignoreCase = true))

    when {
        !isInitialized -> Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0F)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NetflixRed)
        }
        shouldShowUpdate -> {
            val info = updateInfo!!
            ForceUpdateScreen(info = info, onDismiss = if (info.isMandatory) null else ({ vm.dismissUpdate(info.versionCode) }))
        }
        deviceType.isEmpty() -> DeviceSelectionScreen(onDeviceSelected = { vm.setDeviceType(it) })
        !skipIntro && !hasPlayedIntroSession -> HekayatnaProfessionalIntro(
            deviceType = deviceType,
            onIntroFinished = { vm.markIntroCompleted() }
        )
        else -> {
            var isNetworkOnline by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                while (true) {
                    isNetworkOnline = checkOnline()
                    delay(if (isNetworkOnline) 8_000 else 3_000)
                }
            }

            // portrait everywhere except the player screens (they rotate themselves / by device)
            LaunchedEffect(currentRoute) {
                val r = currentRoute ?: "home"
                if (!r.startsWith("player") && !r.startsWith("match_player") && !r.startsWith("offline_player")) {
                    PlatformUi.setLandscape(false)
                }
            }

            val offlineRoutes = currentRoute == "mylist" || currentRoute == "downloaded_episodes" ||
                currentRoute?.startsWith("offline_player") == true

            if (!isNetworkOnline && !offlineRoutes) {
                OfflineScreen(
                    onRetry = { scope.launch { isNetworkOnline = checkOnline() } },
                    onOpenDownloads = { nav.navigate("downloaded_episodes") },
                    onPlayEpisode = { path, title ->
                        nav.navigate("offline_player/${encArg(path)}/${encArg(title.ifEmpty { "بدون عنوان" })}")
                    }
                )
            } else {
                var showGlobalReportDialog by remember { mutableStateOf(false) }
                if (showGlobalReportDialog) {
                    ReportIssueDialog(deviceType = deviceType, onDismiss = { showGlobalReportDialog = false })
                }

                val isGuestState by authViewModel.isGuest.collectAsState()
                val authState by authViewModel.authState.collectAsState()
                val isGuest = isGuestState || authState is AuthState.Guest

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        if (showBars && currentRoute == "home") {
                            BrandBar(
                                vm = vm,
                                onSearchClick = { nav.navigate("search") },
                                onHakimClick = { nav.navigate("hakim") },
                                onReportClick = { showGlobalReportDialog = true }
                            )
                        }
                    },
                    bottomBar = {
                        if (showBars) {
                            Column {
                                if (currentRoute in listOf("home", "mylist", "search", "matches")) AdMobBanner()
                                BottomNav(nav, currentRoute)
                            }
                        }
                    }
                ) { innerPadding ->
                    val fullBleed = currentRoute?.startsWith("player") == true ||
                        currentRoute?.startsWith("match_player") == true ||
                        currentRoute?.startsWith("offline_player") == true ||
                        currentRoute in listOf("onboarding", "setup_profile", "login", "register", "forgot_password", "email_verification")

                    NavHost(
                        navController = nav,
                        startDestination = "home",
                        modifier = Modifier.padding(if (fullBleed) PaddingValues(0.dp) else innerPadding)
                    ) {
                        composable("setup_profile") {
                            SetupProfileScreen(onContinue = {
                                nav.navigate("onboarding") { popUpTo("setup_profile") { inclusive = true } }
                            })
                        }
                        composable("onboarding") {
                            OnboardingScreen(vm = vm, onComplete = {
                                nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                            })
                        }
                        composable("login") {
                            ChatLoginScreen(
                                vm = vm, authViewModel = authViewModel,
                                onSuccess = { nav.navigate("home") { popUpTo("login") { inclusive = true } } },
                                onBack = { nav.popBackStack() }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                authViewModel = authViewModel,
                                onRegisterSuccess = { nav.navigate("home") { popUpTo("register") { inclusive = true } } },
                                onNavigateVerification = { nav.navigate("email_verification") { popUpTo("register") { inclusive = true } } },
                                onNavigateLogin = { nav.navigate("login") { popUpTo("register") { inclusive = true } } },
                                onStoreExtra = { username, age ->
                                    UserPrefs.putString("username", username)
                                    UserPrefs.putString("age", age)
                                }
                            )
                        }
                        composable("email_verification") {
                            EmailVerificationScreen(
                                authViewModel = authViewModel,
                                onNavigateNext = { nav.navigate("setup_profile") { popUpTo("email_verification") { inclusive = true } } },
                                onNavigateLogin = {
                                    authViewModel.logout()
                                    nav.navigate("login") { popUpTo("email_verification") { inclusive = true } }
                                }
                            )
                        }
                        composable("forgot_password") {
                            ForgotPasswordScreen(authViewModel = authViewModel, onBackToLogin = { nav.navigateUp() })
                        }
                        composable("home") {
                            HomeScreen(
                                vm = vm,
                                matchesVm = matchesVm,
                                isGuest = isGuest,
                                onNavigateLogin = { nav.navigate("login") },
                                onOpenMatches = { nav.navigate("matches") },
                                onOpenShow = { nav.navigate("detail/${encArg(it)}") },
                                onPlay = { show, ep -> nav.navigate("player/${encArg(show)}/${encArg(ep ?: "default")}") }
                            )
                        }
                        composable("matches") {
                            MatchesScreen(vm = matchesVm, onOpenMatch = { nav.navigate("match_player/${encArg(it)}") })
                        }
                        composable("match_player/{matchId}") { entry ->
                            val id = decArg(entry.arguments?.getString("matchId"))
                            val matches = MatchesRepository.getInitialMatches()
                            val match = matches.find { it.id == id } ?: matches.first()
                            MatchPlayerScreen(
                                match = match,
                                otherMatches = matches.filter { it.id != match.id },
                                onBack = { nav.popBackStack() },
                                onSelectMatch = { m ->
                                    nav.navigate("match_player/${encArg(m.id)}") {
                                        popUpTo("match_player/{matchId}") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("search") {
                            SearchScreen(vm = vm, onOpenShow = { nav.navigate("detail/${encArg(it)}") })
                        }
                        composable("mylist") {
                            DownloadedEpisodesScreen(
                                onBack = { nav.popBackStack() },
                                onPlayOffline = { path, title ->
                                    nav.navigate("offline_player/${encArg(path)}/${encArg(title.ifEmpty { "بدون عنوان" })}")
                                }
                            )
                        }
                        composable("downloaded_episodes") {
                            DownloadedEpisodesScreen(
                                onBack = { nav.popBackStack() },
                                onPlayOffline = { path, title ->
                                    nav.navigate("offline_player/${encArg(path)}/${encArg(title.ifEmpty { "بدون عنوان" })}")
                                }
                            )
                        }
                        composable("offline_player/{path}/{title}") { entry ->
                            OfflinePlayerScreen(
                                filePath = decArg(entry.arguments?.getString("path")),
                                title = decArg(entry.arguments?.getString("title")),
                                onBack = { nav.popBackStack() }
                            )
                        }
                        composable("shorts") {
                            ShortsScreen(vm = vm, onOpenShow = { nav.navigate("detail/${encArg(it)}") })
                        }
                        composable("hakim") {
                            HakimChatScreen(
                                vm = vm,
                                onBack = { nav.popBackStack() },
                                onOpenShow = { nav.navigate("detail/${encArg(it)}") }
                            )
                        }
                        composable("chat") {
                            val profileName by vm.profileName.collectAsState()
                            val userEmail by vm.userEmail.collectAsState()
                            val isChatRegistered = profileName.isNotBlank() && profileName != "ضيف" && userEmail.isNotBlank()
                            if (!isChatRegistered) {
                                ChatLoginScreen(
                                    vm = vm, authViewModel = authViewModel,
                                    onSuccess = { /* profile set -> recomposition shows ChatScreen */ },
                                    onBack = { nav.popBackStack() }
                                )
                            } else {
                                ChatScreen(
                                    vm = vm,
                                    onBack = { nav.popBackStack() },
                                    onOpenShow = { nav.navigate("detail/${encArg(it)}") }
                                )
                            }
                        }
                        composable("profile") {
                            SettingsScreen(
                                vm = vm,
                                authViewModel = authViewModel,
                                onLogout = {
                                    authViewModel.logout()
                                    nav.navigate("login") { popUpTo("home") { inclusive = true } }
                                }
                            )
                        }
                        composable("detail/{id}") { entry ->
                            val id = decArg(entry.arguments?.getString("id"))
                            DetailScreen(
                                showId = id,
                                vm = vm,
                                onBack = { nav.popBackStack() },
                                onPlay = { show, ep -> nav.navigate("player/${encArg(show)}/${encArg(ep ?: "default")}") },
                                onOpenShow = { nav.navigate("detail/${encArg(it)}") },
                                onNavigateToDownloads = { nav.navigate("mylist") }
                            )
                        }
                        composable("player/{id}/{episodeId}") { entry ->
                            val id = decArg(entry.arguments?.getString("id"))
                            val epId = decArg(entry.arguments?.getString("episodeId")).let { if (it == "default") null else it }
                            PlayerScreen(
                                showId = id,
                                episodeId = epId,
                                vm = vm,
                                onBack = { nav.popBackStack() },
                                onOpenShow = { nav.navigate("detail/${encArg(it)}") }
                            )
                        }
                        composable("browse_all") {
                            BrowseAllScreen(
                                title = "تصفح الكل",
                                shows = vm.shows.value,
                                onOpenShow = { nav.navigate("detail/${encArg(it)}") },
                                onBack = { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandBar(
    vm: AppViewModel,
    onSearchClick: () -> Unit,
    onHakimClick: () -> Unit,
    onReportClick: () -> Unit
) {
    val remoteLogoUrl by vm.remoteLogoUrl.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (remoteLogoUrl != null) {
                AsyncImage(model = remoteLogoUrl, contentDescription = "Logo", modifier = Modifier.height(32.dp), contentScale = ContentScale.Fit)
            } else {
                Image(painter = painterResource(Res.drawable.logo), contentDescription = "Logo", modifier = Modifier.height(32.dp), contentScale = ContentScale.Fit)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.clickable { onHakimClick() }, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF4A1515))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "مستشار\nالدراما", color = Color(0xFFFF2D3A), fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 9.sp
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("حكيم الذكي", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(
                            Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFF9C27B0)))
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, "Avatar", tint = Color.White, modifier = Modifier.size(20.dp))
                        Box(
                            modifier = Modifier.size(10.dp).align(Alignment.BottomEnd).offset(x = (-2).dp, y = (-2).dp)
                                .clip(CircleShape).background(Color(0xFF00E676)).border(1.5.dp, Color(0xFF0F0F0F), CircleShape)
                        )
                    }
                }
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).clickable { onSearchClick() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Search, "بحث", tint = Color.White, modifier = Modifier.size(19.dp)) }
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).clickable { onReportClick() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.BugReport, "إبلاغ عن مشكلة", tint = Color(0xFFFFA000), modifier = Modifier.size(19.dp)) }
            }
        }
    }
}

@Composable
private fun BottomNav(nav: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                icon = { Icon(if (selected) tab.filled else tab.outlined, contentDescription = tab.label) },
                label = { Text(tab.label, fontSize = 11.sp) },
                selected = selected,
                onClick = {
                    if (currentRoute != tab.route) {
                        nav.navigate(tab.route) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    unselectedIconColor = MaterialTheme.colorScheme.outline,
                    unselectedTextColor = MaterialTheme.colorScheme.outline,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
