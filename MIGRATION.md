# pending/ -> shared/src/commonMain migration

Move a file out of `pending/` only after these swaps (compile after each batch):

| android | commonMain |
|---|---|
| `coil.compose.AsyncImage` / `coil.request.*` | `coil3.compose.AsyncImage` |
| `collectAsStateWithLifecycle()` | `collectAsState()` |
| `LocalContext.current` | remove; push the Context-needing call behind `expect/actual` |
| `painterResource(R.drawable.x)` | `org.jetbrains.compose.resources.painterResource(Res.drawable.x)` |
| `okhttp3.*` | `Http.get(...)` (Http.kt) |
| `org.jsoup.*` | `com.fleeksoft.ksoup.*` |
| `AndroidViewModel` / `Application` | plain `ViewModel` + injected platform services |
| `WebView` | `WebPlayer(...)` |
| Room / DataStore / WorkManager / DownloadManager / AdMob / Firebase / speech | expect/actual, one at a time |

Order: AppViewModel + PrefsManager -> HomeScreen -> DetailScreen -> player -> scrapers -> downloads/ads/auth last.
