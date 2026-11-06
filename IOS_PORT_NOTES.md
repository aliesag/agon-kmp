# iOS port notes (what changed vs your KMP repo)

New commonMain: App.kt (full MainActivity nav), platform/Platform.kt, video/VideoSurface.kt,
data/FirebaseAuthRest.kt, ui/screens/{ForceUpdateScreen, ShortsScreen, OfflinePlayerScreen, OfflineScreen,
DownloadedEpisodesScreen, SettingsScreen, AuthScreens, ChatLoginScreen, SetupProfileScreen}.kt,
ui/components/ReportIssueDialog.kt (real), data/EpisodeDownloaderManager.kt (real, Ktor -> Documents/Downloads).
iosMain: platform/Platform.ios.kt (toast/share/picker/orientation/files), video/VideoSurface.ios.kt.

FILL before it works:
- BuildConfig.FIREBASE_API_KEY  (Web API key of Firebase project chat-516a8; the google-services.json in the Android zip is a placeholder)

Not ported (iOS-impossible or separate work): AdMob (stub), Google sign-in, TvSideNav, LocalStreamProxy,
NewEpisodesWorker/notifications, in-app APK updater, CustomMediaPicker grid (system picker used), exit dialog.
Not verified: nothing was compiled (no Gradle/Xcode in sandbox).
