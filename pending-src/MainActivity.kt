package com.agon.app

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agon.app.ui.screens.DetailScreen
import com.agon.app.ui.screens.HomeScreen
import com.agon.app.ui.screens.MyListScreen
import com.agon.app.ui.screens.PlayerScreen
import com.agon.app.ui.screens.SearchScreen
import com.agon.app.ui.screens.SettingsScreen
import com.agon.app.ui.screens.ChatScreen
import com.agon.app.ui.screens.HakimChatScreen
import com.agon.app.ui.screens.ShortsScreen
import com.agon.app.ui.screens.LoginScreen
import com.agon.app.ui.screens.RegisterScreen
import com.agon.app.ui.screens.ForgotPasswordScreen
import com.agon.app.ui.screens.EmailVerificationScreen
import com.agon.app.ui.screens.DeviceSelectionScreen
import com.agon.app.ui.theme.AgonAppTheme
import com.agon.app.ui.theme.NetflixRed
import com.agon.app.viewmodel.AppViewModel
import com.agon.app.ui.AuthViewModel

import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.compose.LocalImageLoader
import okhttp3.OkHttpClient
import com.agon.app.data.PrefsManager
import com.agon.app.viewmodel.AppViewModelFactory

import com.agon.app.ui.components.ReportIssueDialog
import androidx.compose.material.icons.filled.BugReport

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.agon.app.data.NewEpisodesWorker

class MainActivity : ComponentActivity() {

    private val openShowIdState = mutableStateOf<String?>(null)
    private val openEpisodeIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        com.agon.app.data.NotificationHelper.createNotificationChannel(this)
        handleNotificationIntent(intent)
        
        val workRequest = PeriodicWorkRequestBuilder<NewEpisodesWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NewEpisodesCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val rawHost = original.url.host
                        val refererHost = if (rawHost.contains("3cktv") || rawHost.contains("3isk")) "z.3cktv.com" else rawHost
                        val builder = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                            .header("Referer", "https://$refererHost/")
                            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                        chain.proceed(builder.build())
                    }
                    .build()
            }
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(400 * 1024 * 1024) // 400 MB disk cache
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
        coil.Coil.setImageLoader(imageLoader)

        // Initialize AdMob AdManager centrally
        com.agon.app.data.ads.AdManager.initialize(this)

        // Start LocalStreamProxy for high-speed Alooytv and CDN video streaming
        com.agon.app.data.LocalStreamProxy.start()

        val prefsManager = PrefsManager(this)
        setContent {
            AgonAppTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalImageLoader provides imageLoader
                ) {
                    MainApp(
                        prefsManager = prefsManager,
                        initialShowId = openShowIdState.value,
                        initialEpisodeId = openEpisodeIdState.value,
                        onNotificationHandled = {
                            openShowIdState.value = null
                            openEpisodeIdState.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        val showId = intent?.getStringExtra("OPEN_SHOW_ID")
        val epId = intent?.getStringExtra("OPEN_EPISODE_ID")
        if (!showId.isNullOrEmpty()) {
            openShowIdState.value = showId
            openEpisodeIdState.value = epId
        }
    }
}

private data class Tab(val route: String, val label: String, val filled: androidx.compose.ui.graphics.vector.ImageVector, val outlined: androidx.compose.ui.graphics.vector.ImageVector)

// Bottom bar tabs: Home ("الرئيسية") is in the center-left, with Chat ("الدردشة") in the center-right.
// This perfectly maps to: "الرئيسية بالمنتصف، احذف بحث خلي بحث فوق، ضيف قسم شات"
// "شفت قائمة ريسيه خليها بل نص وشقت قائمتي خليها حلقات محمله"
private val tabs = listOf(
    Tab("mylist", "حلقات محملة", Icons.Filled.Download, Icons.Outlined.Download),
    Tab("matches", "المباريات", Icons.Filled.SportsSoccer, Icons.Outlined.SportsSoccer),
    Tab("home", "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
    Tab("shorts", "شورتس", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
    Tab("chat", "الدردشة", Icons.Filled.Forum, Icons.Outlined.Forum),
    Tab("profile", "المزيد", Icons.Filled.Menu, Icons.Outlined.Menu),
)

@Composable
fun MainApp(
    prefsManager: PrefsManager,
    initialShowId: String? = null,
    initialEpisodeId: String? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val vm: AppViewModel = viewModel(factory = AppViewModelFactory(prefsManager))
    val authViewModel: AuthViewModel = viewModel()
    val matchesViewModel: com.agon.app.viewmodel.MatchesViewModel = viewModel()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBars = currentRoute in tabs.map { it.route }
    
    val context = LocalContext.current
    val isInitialized by vm.isInitialized.collectAsStateWithLifecycle()

    LaunchedEffect(initialShowId, initialEpisodeId) {
        if (!initialShowId.isNullOrEmpty()) {
            val sId = initialShowId
            val eId = initialEpisodeId
            onNotificationHandled()
            if (!eId.isNullOrEmpty()) {
                navController.navigate("player/$sId/$eId")
            } else {
                navController.navigate("detail/$sId")
            }
        }
    }
    
    val updateInfo by vm.updateInfo.collectAsStateWithLifecycle()

    val currentVer = com.agon.app.BuildConfig.VERSION_NAME.trim().removePrefix("v").removePrefix("V")
    val targetVer = updateInfo?.versionName?.trim()?.removePrefix("v")?.removePrefix("V")
    val shouldShowUpdate = updateInfo != null &&
            targetVer != currentVer &&
            updateInfo!!.versionCode > com.agon.app.BuildConfig.VERSION_CODE

    if (!isInitialized) {
        // Show a loading screen or splash while checking onboarding status
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFE50914))
        }
    } else if (shouldShowUpdate) {
        val currentInfo = updateInfo!!
        ForceUpdateScreen(
            info = currentInfo,
            onDismiss = if (currentInfo.isMandatory) null else {
                { vm.dismissUpdate(currentInfo.versionCode) }
            }
        )
    } else {
        val deviceType by vm.deviceType.collectAsStateWithLifecycle()
        val skipIntro by vm.skipIntro.collectAsStateWithLifecycle()
        val hasPlayedIntroSession by vm.hasPlayedIntroSession.collectAsStateWithLifecycle()

        if (deviceType.isEmpty()) {
            DeviceSelectionScreen(
                onDeviceSelected = { selectedType ->
                    vm.setDeviceType(selectedType)
                }
            )
        } else if (!skipIntro && !hasPlayedIntroSession) {
            com.agon.app.ui.screens.HekayatnaProfessionalIntro(
                deviceType = deviceType,
                onIntroFinished = { vm.markIntroCompleted() }
            )
        } else {
            var showExitDialog by remember { mutableStateOf(false) }
            val context = androidx.compose.ui.platform.LocalContext.current
            val tvSideNavFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            var isTvNavFocused by remember { mutableStateOf(false) }

            fun isConnectedToInternet(): Boolean {
                return try {
                    val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                    val net = cm?.activeNetwork ?: return false
                    val caps = cm.getNetworkCapabilities(net) ?: return false
                    caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } catch (_: Exception) {
                    false
                }
            }

            var isNetworkOnline by remember { mutableStateOf(isConnectedToInternet()) }
            val networkScope = androidx.compose.runtime.rememberCoroutineScope()

            androidx.compose.runtime.LaunchedEffect(Unit) {
                val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        networkScope.launch {
                            isNetworkOnline = true
                        }
                    }
                    override fun onLost(network: android.net.Network) {
                        networkScope.launch {
                            isNetworkOnline = isConnectedToInternet()
                        }
                    }
                    override fun onCapabilitiesChanged(network: android.net.Network, capabilities: android.net.NetworkCapabilities) {
                        networkScope.launch {
                            isNetworkOnline = capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        }
                    }
                }
                try {
                    cm?.registerDefaultNetworkCallback(callback)
                } catch (_: Exception) {
                    // Fallback to periodic status checks if API call fails
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) {}
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                com.agon.app.data.EpisodeDownloaderManager.init(context)
            }

            if (!isNetworkOnline && currentRoute != "mylist" && currentRoute != "downloaded_episodes" && currentRoute?.startsWith("offline_player") != true) {
                com.agon.app.ui.screens.OfflineScreen(
                    onRetry = {
                        isNetworkOnline = isConnectedToInternet()
                    },
                    onOpenDownloads = {
                        navController.navigate("downloaded_episodes")
                    },
                    onPlayEpisode = { path, title ->
                        try {
                            val pathEncoded = android.util.Base64.encodeToString(
                                path.toByteArray(Charsets.UTF_8),
                                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                            )
                            val titleStr = title.ifEmpty { "بدون عنوان" }
                            val titleEncoded = android.util.Base64.encodeToString(
                                titleStr.toByteArray(Charsets.UTF_8),
                                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                            )
                            navController.navigate("offline_player/$pathEncoded/$titleEncoded")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            } else {

            androidx.compose.runtime.LaunchedEffect(deviceType, currentRoute) {
                val activity = context as? android.app.Activity
                if (deviceType == "tv") {
                    activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else if (currentRoute != "player" && currentRoute != "player_direct") {
                    activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }

            androidx.activity.compose.BackHandler(enabled = currentRoute == "home") {
                if (deviceType == "tv") {
                    if (!isTvNavFocused) {
                        try {
                            tvSideNavFocusRequester.requestFocus()
                            isTvNavFocused = true
                        } catch (e: Exception) {
                            showExitDialog = true
                        }
                    } else {
                        showExitDialog = true
                    }
                } else {
                    showExitDialog = true
                }
            }

            if (showExitDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = {
                        Text(
                            "تأكيد الخروج",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            "هل أنت تأكد من الخروج من تطبيق حكايتنا؟",
                            fontSize = 14.sp,
                            color = Color.LightGray
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitDialog = false
                                (context as? android.app.Activity)?.finish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                        ) {
                            Text("خروج", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { showExitDialog = false }
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF1E1E24)
                )
            }

            var showGlobalReportDialog by remember { mutableStateOf(false) }

            if (showGlobalReportDialog) {
                ReportIssueDialog(
                    deviceType = deviceType,
                    onDismiss = { showGlobalReportDialog = false }
                )
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = { 
                    if (showBars && currentRoute == "home" && deviceType != "tv") {
                        BrandBar(
                            vm = vm,
                            onSearchClick = { navController.navigate("search") },
                            onHakimClick = { navController.navigate("hakim") },
                            onReportClick = { showGlobalReportDialog = true }
                        ) 
                    } 
                },
                bottomBar = { 
                    if (showBars && deviceType != "tv") {
                        Column {
                            if (currentRoute in listOf("home", "mylist", "search", "matches")) {
                                com.agon.app.ui.components.AdMobBanner()
                            }
                            BottomNav(navController, currentRoute)
                        }
                    } 
                },
            ) { innerPadding ->
            val onboardingCompleted by vm.onboardingCompleted.collectAsStateWithLifecycle()
            val isGuestState by authViewModel.isGuest.collectAsStateWithLifecycle()
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            val isGuest = isGuestState || authState is com.agon.app.ui.AuthState.Guest
            val startDest = "home"

            LaunchedEffect(currentRoute, deviceType) {
                com.agon.app.data.ads.AdManager.currentScreen = currentRoute ?: "home"
                if (deviceType != "tv" && currentRoute?.startsWith("player") != true && currentRoute?.startsWith("match_player") != true) {
                    val act = context as? Activity
                    act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                if (deviceType == "tv" && showBars) {
                    com.agon.app.ui.components.TvSideNav(navController, currentRoute, focusRequester = tvSideNavFocusRequester)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = startDest,
                        modifier = Modifier.padding(
                            if (currentRoute?.startsWith("player") == true || currentRoute?.startsWith("match_player") == true || currentRoute in listOf("onboarding", "setup_profile", "login", "register", "forgot_password", "email_verification")) PaddingValues(0.dp) 
                            else if (deviceType == "tv") PaddingValues(0.dp)
                            else innerPadding
                        ),
                    ) {
                composable("setup_profile") {
                    com.agon.app.ui.screens.SetupProfileScreen(
                        onContinue = {
                            navController.navigate("onboarding") {
                                popUpTo("setup_profile") { inclusive = true }
                            }
                        }
                    )
                }
                composable("onboarding") {
                    com.agon.app.ui.screens.OnboardingScreen(
                        vm = vm,
                        onComplete = {
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }
                composable("login") {
                    com.agon.app.ui.screens.ChatLoginScreen(
                        vm = vm,
                        authViewModel = authViewModel,
                        onSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("register") {
                    val context = LocalContext.current
                    RegisterScreen(
                        authViewModel = authViewModel,
                        onRegisterSuccess = {
                            navController.navigate("home") {
                                popUpTo("register") { inclusive = true }
                            }
                        },
                        onNavigateVerification = {
                            navController.navigate("email_verification") {
                                popUpTo("register") { inclusive = true }
                            }
                        },
                        onNavigateLogin = { navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        } },
                        onStoreExtra = { username, age ->
                            // Here you can save the username and age locally
                            // Using standard shared preferences for simplicity
                            val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putString("username", username).putString("age", age).apply()
                        }
                    )
                }
                composable("email_verification") {
                    EmailVerificationScreen(
                        authViewModel = authViewModel,
                        onNavigateNext = {
                            navController.navigate("setup_profile") {
                                popUpTo("email_verification") { inclusive = true }
                            }
                        },
                        onNavigateLogin = {
                            authViewModel.logout()
                            navController.navigate("login") {
                                popUpTo("email_verification") { inclusive = true }
                            }
                        }
                    )
                }
                composable("forgot_password") {
                    ForgotPasswordScreen(
                        authViewModel = authViewModel,
                        onBackToLogin = { navController.navigateUp() }
                    )
                }
                 composable("home") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    HomeScreen(
                        vm = vm,
                        matchesVm = matchesViewModel,
                        isGuest = isGuest,
                        onNavigateLogin = { navController.navigate("login") },
                        onOpenMatches = { navController.navigate("matches") },
                        onOpenShow = { 
                            val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                            navController.navigate("detail/$encoded") 
                        },
                        onPlay = { show, ep -> 
                            val sEncoded = java.net.URLEncoder.encode(show, "UTF-8")
                            val eEncoded = java.net.URLEncoder.encode(ep ?: "default", "UTF-8")
                            navController.navigate("player/$sEncoded/$eEncoded") 
                        },
                    )
                }
                composable("matches") {
                    com.agon.app.ui.screens.MatchesScreen(
                        vm = matchesViewModel,
                        onOpenMatch = { matchId ->
                            navController.navigate("match_player/$matchId")
                        }
                    )
                }
                composable("match_player/{matchId}") { backStackEntry ->
                    val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                    com.agon.app.ui.screens.MatchPlayerScreen(
                        matchId = matchId,
                        vm = matchesViewModel,
                        onBack = { navController.popBackStack() },
                        onOpenOtherMatch = { otherId ->
                            navController.navigate("match_player/$otherId") {
                                popUpTo("matches")
                            }
                        }
                    )
                }
                composable("search") {
                    SearchScreen(
                        vm = vm, 
                        onOpenShow = { 
                            val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                            navController.navigate("detail/$encoded") 
                        }
                    )
                }
                composable("mylist") {
                    com.agon.app.ui.screens.DownloadedEpisodesScreen(
                        onBack = { navController.popBackStack() },
                        onPlayOffline = { path, title ->
                            try {
                                val pathEncoded = android.util.Base64.encodeToString(
                                    path.toByteArray(Charsets.UTF_8),
                                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                                )
                                val titleStr = title.ifEmpty { "بدون عنوان" }
                                val titleEncoded = android.util.Base64.encodeToString(
                                    titleStr.toByteArray(Charsets.UTF_8),
                                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                                )
                                navController.navigate("offline_player/$pathEncoded/$titleEncoded")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
                composable("downloaded_episodes") {
                    com.agon.app.ui.screens.DownloadedEpisodesScreen(
                        onBack = { navController.popBackStack() },
                        onPlayOffline = { path, title ->
                            try {
                                val pathEncoded = android.util.Base64.encodeToString(
                                    path.toByteArray(Charsets.UTF_8),
                                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                                )
                                val titleStr = title.ifEmpty { "بدون عنوان" }
                                val titleEncoded = android.util.Base64.encodeToString(
                                    titleStr.toByteArray(Charsets.UTF_8),
                                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                                )
                                navController.navigate("offline_player/$pathEncoded/$titleEncoded")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
                composable("offline_player/{path}/{title}") { entry ->
                    val pathArg = entry.arguments?.getString("path") ?: ""
                    val titleArg = entry.arguments?.getString("title") ?: ""
                    val path = try { 
                        String(
                            android.util.Base64.decode(pathArg, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING), 
                            Charsets.UTF_8
                        ) 
                    } catch (e: Exception) { 
                        try {
                            String(
                                android.util.Base64.decode(pathArg, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP), 
                                Charsets.UTF_8
                            )
                        } catch (ex: Exception) {
                            "" 
                        }
                    }
                    val title = try { 
                        String(
                            android.util.Base64.decode(titleArg, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING), 
                            Charsets.UTF_8
                        ) 
                    } catch (e: Exception) { 
                        try {
                            String(
                                android.util.Base64.decode(titleArg, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP), 
                                Charsets.UTF_8
                            )
                        } catch (ex: Exception) {
                            "" 
                        }
                    }
                    com.agon.app.ui.screens.OfflinePlayerScreen(
                        filePath = path,
                        title = title,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("shorts") {
                    ShortsScreen(
                        vm = vm,
                        onOpenShow = { 
                            val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                            navController.navigate("detail/$encoded") 
                        }
                    )
                }
                composable("hakim") {
                    HakimChatScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() },
                        onOpenShow = { showId ->
                            val encoded = java.net.URLEncoder.encode(showId, "UTF-8")
                            navController.navigate("detail/$encoded")
                        }
                    )
                }
                composable("chat") {
                    val profileName by vm.profileName.collectAsStateWithLifecycle()
                    val userEmail by vm.userEmail.collectAsStateWithLifecycle()
                    val isChatRegistered = profileName.isNotBlank() && profileName != "ضيف" && userEmail.isNotBlank()

                    if (!isChatRegistered) {
                        com.agon.app.ui.screens.ChatLoginScreen(
                            vm = vm,
                            authViewModel = authViewModel,
                            onSuccess = {
                                // Once profile is set, re-composition renders ChatScreen
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    } else {
                        ChatScreen(
                            vm = vm,
                            onBack = { navController.popBackStack() },
                            onOpenShow = { 
                                val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                                navController.navigate("detail/$encoded") 
                            }
                        )
                    }
                }
                composable("profile") { 
                    SettingsScreen(
                        vm = vm,
                        authViewModel = authViewModel,
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    ) 
                }
                composable("detail/{id}") { entry ->
                    val id = java.net.URLDecoder.decode(entry.arguments?.getString("id") ?: return@composable, "UTF-8")
                    val context = androidx.compose.ui.platform.LocalContext.current
                    DetailScreen(
                        showId = id,
                        vm = vm,
                        onBack = { navController.popBackStack() },
                        onPlay = { show, ep -> 
                            val sEncoded = java.net.URLEncoder.encode(show, "UTF-8")
                            val eEncoded = java.net.URLEncoder.encode(ep ?: "default", "UTF-8")
                            navController.navigate("player/$sEncoded/$eEncoded") 
                        },
                        onOpenShow = { 
                            val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                            navController.navigate("detail/$encoded") 
                        },
                        onNavigateToDownloads = {
                            navController.navigate("mylist")
                        }
                    )
                }
                composable("player/{id}/{episodeId}") { entry ->
                    val id = java.net.URLDecoder.decode(entry.arguments?.getString("id") ?: return@composable, "UTF-8")
                    val epId = java.net.URLDecoder.decode(entry.arguments?.getString("episodeId") ?: "default", "UTF-8").let { if (it == "default") null else it }
                    PlayerScreen(showId = id, episodeId = epId, vm = vm, onBack = { navController.popBackStack() }, onOpenShow = { 
                        val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                        navController.navigate("detail/$encoded") 
                    })
                }
            }
        }
    }
        }
    }
}
}
}

@Composable
private fun ForceUpdateScreen(
    info: com.agon.app.data.UpdateInfo,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val downloadsDir = remember { context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) }
    val targetApk = remember(info.versionCode) {
        java.io.File(
            downloadsDir,
            "hekayatna_update_v${info.versionCode}.apk"
        )
    }

    androidx.compose.runtime.LaunchedEffect(info.versionCode) {
        try {
            downloadsDir?.listFiles()?.forEach { file ->
                if (file.name.startsWith("hekayatna_update_v") && file.name != targetApk.name) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var isApkReady by remember(info.versionCode) {
        mutableStateOf(targetApk.exists() && targetApk.length() > 500 * 1024)
    }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedMb by remember { mutableFloatStateOf(0f) }
    var totalMb by remember { mutableFloatStateOf(0f) }
    var downloadStatus by remember { mutableStateOf("") }
    var downloadError by remember { mutableStateOf<String?>(null) }

    fun launchApkInstaller(file: java.io.File) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    downloadError = "تنبيه: يرجى تفعيل السماح بتثبيت التطبيقات من هذا المصدر، ثم اضغط زر التثبيت مجدداً."
                    val settingsIntent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(settingsIntent)
                    return
                }
            }
            downloadError = null
            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val installIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(android.content.Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(android.content.Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            }
            context.startActivity(installIntent)
            onDismiss?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
            downloadError = "تعذر فتح تثبيت APK تلقائياً. جرب التحميل عبر المتصفح."
            val browserIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(info.downloadUrl)
            )
            context.startActivity(browserIntent)
        }
    }

    fun handleUpdateClick() {
        if (isApkReady && targetApk.exists()) {
            launchApkInstaller(targetApk)
            return
        }

        isDownloading = true
        downloadError = null
        downloadProgress = 0f
        downloadStatus = "جاري الاتصال بسيرفر التحديث..."

        scope.launch(Dispatchers.IO) {
            try {
                var currentUrl = info.downloadUrl
                var connection: java.net.HttpURLConnection? = null
                var redirectCount = 0
                val maxRedirects = 10
                var finalResponseCode = 0

                while (redirectCount < maxRedirects) {
                    val u = java.net.URL(currentUrl)
                    connection = (u.openConnection() as java.net.HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 20000
                        readTimeout = 40000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                        setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/vnd.android.package-archive,*/*;q=0.8")
                        setRequestProperty("Accept-Language", "ar,en-US;q=0.7,en;q=0.3")
                        setRequestProperty("Accept-Encoding", "identity")
                        setRequestProperty("Connection", "keep-alive")
                    }

                    finalResponseCode = connection.responseCode
                    if (finalResponseCode in 300..399) {
                        val location = connection.getHeaderField("Location")
                        if (!location.isNullOrEmpty()) {
                            currentUrl = if (location.startsWith("http")) location else java.net.URL(u, location).toString()
                            connection.disconnect()
                            redirectCount++
                            continue
                        }
                    }
                    break
                }

                val finalConnection = connection
                if (finalConnection == null || finalResponseCode != 200) {
                    withContext(Dispatchers.Main) {
                        downloadError = "فشل الاتصال بسيرفر التحديث (رمز الاستجابة: $finalResponseCode)\nيمكنك تجربة زر التحميل عبر المتصفح الخارجي."
                        isDownloading = false
                    }
                    return@launch
                }

                val fileLength = finalConnection.contentLengthLong
                val inputStream = finalConnection.inputStream
                if (targetApk.exists()) targetApk.delete()

                val outputStream = java.io.FileOutputStream(targetApk)
                val buffer = ByteArray(32768)
                var totalRead = 0L
                var bytesRead: Int
                var lastUpdate = System.currentTimeMillis()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalRead += bytesRead
                    outputStream.write(buffer, 0, bytesRead)

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 100 || (fileLength > 0 && totalRead == fileLength)) {
                        lastUpdate = now
                        val progress = if (fileLength > 0) (totalRead.toFloat() / fileLength.toFloat()).coerceIn(0f, 0.99f) else 0.5f
                        val readMb = totalRead / (1024f * 1024f)
                        val maxMb = if (fileLength > 0) fileLength / (1024f * 1024f) else 0f

                        withContext(Dispatchers.Main) {
                            downloadProgress = progress
                            downloadedMb = readMb
                            totalMb = maxMb
                            downloadStatus = if (fileLength > 0) "جاري التحميل المباشر..." else "جاري التحميل... (${String.format(java.util.Locale.ENGLISH, "%.1f", readMb)} MB)"
                        }
                    }
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                finalConnection.disconnect()

                withContext(Dispatchers.Main) {
                    downloadProgress = 1f
                    downloadStatus = "اكتمل التحميل بنجاح! جاري التثبيت..."
                    isApkReady = true
                    isDownloading = false
                    launchApkInstaller(targetApk)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    downloadError = "تعذر التحديث المباشر: ${e.localizedMessage ?: "يرجى التأكد من الاتصال بالإنتـرنت"}"
                    isDownloading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1F0003),
                        Color(0xFF0D0D0E),
                        Color(0xFF050505)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative glowing background circle
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(y = (-120).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NetflixRed.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Logo / Icon Header with glowing ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF2B0004), Color(0xFF141416))
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(NetflixRed, NetflixRed.copy(alpha = 0.3f))
                            ),
                            shape = CircleShape
                        )
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(54.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "تحديث جديد متاح لتطبيق حكايتنا",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "تحديث إجباري للاستمرار في استخدام التطبيق والتمتع بأحدث الميزات والبث المباشر.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Professional Version Comparison Card
                Surface(
                    color = Color(0xFF161618),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Installed Version
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "الإصدار الحالي",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFF222226),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF38383E))
                            ) {
                                Text(
                                    text = "v${com.agon.app.BuildConfig.VERSION_NAME}",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Arrow Indicator
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NetflixRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = NetflixRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // New Target Version
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "الإصدار الجديد المطلوب",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = NetflixRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NetflixRed.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "v${info.versionName}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // What's New Card Container
            Surface(
                color = Color(0xFF161618),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF2A2A2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NetflixRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                tint = NetflixRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "ما الجديد في هذا التحديث:",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val rawFeatures = info.features.ifEmpty {
                        "تحديث وتثبيت تلقائي من داخل التطبيق\nتحسينات سرعة المشاهدة واستقرار البث\nعدم تكرار ظهور التحديث مجدداً بعد التثبيت"
                    }
                    val featureLines = rawFeatures.split("\n").filter { it.isNotBlank() }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        featureLines.forEach { line ->
                            val cleanLine = line.removePrefix("-").removePrefix("•").trim()
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = NetflixRed,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .offset(y = 2.dp)
                                )
                                Text(
                                    text = cleanLine,
                                    color = Color(0xFFDDDDDD),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Download progress or Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDownloading) {
                    Surface(
                        color = Color(0xFF161618),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NetflixRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = downloadStatus,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    color = NetflixRed,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = NetflixRed,
                                trackColor = Color(0xFF2A2A2E)
                            )

                            if (totalMb > 0f) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = String.format(java.util.Locale.ENGLISH, "%.1f MB / %.1f MB", downloadedMb, totalMb),
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    if (downloadError != null) {
                        Text(
                            text = downloadError!!,
                            color = Color(0xFFFF4D4D),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Direct In-App Download / Install Button
                    Button(
                        onClick = { handleUpdateClick() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFE50914),
                                            Color(0xFFB20710)
                                        )
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isApkReady) Icons.Filled.CheckCircle else Icons.Filled.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isApkReady) "تثبيت التحديث الآن (APK جاهز)" else "تحديث وتثبيت مباشر من التطبيق",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(info.downloadUrl)
                            )
                            context.startActivity(intent)
                            onDismiss?.invoke()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF333336))
                    ) {
                        Text(
                            text = "تحميل عبر المتصفح الخارجي",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!info.isMandatory) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = { onDismiss?.invoke() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تخطي هذا التحديث",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "تحديث آمن ومباشر من السيرفر الرسمي لحكايتنا",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
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
    val remoteLogoUrl by vm.remoteLogoUrl.collectAsStateWithLifecycle()

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
                AsyncImage(
                    model = remoteLogoUrl,
                    contentDescription = "Logo",
                    modifier = Modifier.height(32.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.height(32.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glow-accented magic wand "Hakim" button -> Replaced with Hakim Profile block
                Row(
                    modifier = Modifier.clickable { onHakimClick() },
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
                                    text = "مستشار\nالدراما",
                                    color = Color(0xFFFF2D3A),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "حكيم الذكي",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoFixHigh,
                            contentDescription = "Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                                .border(1.5.dp, Color(0xFF0F0F0F), CircleShape)
                        )
                    }
                }

                // Modern Search circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E1E))
                        .clickable { onSearchClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "بحث",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Report Issue Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E1E))
                        .clickable { onReportClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.BugReport,
                        contentDescription = "إبلاغ عن مشكلة",
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNav(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                icon = { Icon(if (selected) tab.filled else tab.outlined, contentDescription = tab.label) },
                label = { Text(tab.label, fontSize = 11.sp) },
                selected = selected,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
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
 
