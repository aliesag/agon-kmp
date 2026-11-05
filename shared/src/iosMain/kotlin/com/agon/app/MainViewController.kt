package com.agon.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import kotlin.experimental.ExperimentalNativeApi
import platform.UIKit.UIViewController

private class IOSViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

@Composable
private fun WithViewModelStoreOwner(content: @Composable () -> Unit) {
    val owner = remember { IOSViewModelStoreOwner() }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { content() }
}

private fun configureCoil() {
    try {
        val http = io.ktor.client.HttpClient(io.ktor.client.engine.darwin.Darwin) {
            expectSuccess = false
            install(io.ktor.client.plugins.HttpSend) {
                // per-host Referer + UA (3cktv / fosta reject missing Referer)
                intercept { request ->
                    val url = request.url.toString().lowercase()
                    val referer = when {
                        url.contains("3cktv") -> "https://e.3cktv.com/"
                        url.contains("ffosta") || url.contains("fosta") -> "https://ffosta-tv.quest/"
                        url.contains("alooytv") -> "https://ep.alooytv16.xyz/"
                        url.contains("yalla.moda") -> "https://yalla.moda/"
                        url.contains("tmdb") -> "https://www.themoviedb.org/"
                        else -> "https://e.3cktv.com/"
                    }
                    request.headers.append("Referer", referer)
                    request.headers.append(
                        "User-Agent",
                        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
                    )
                    execute(request)
                }
            }
        }
        SingletonImageLoader.setSafe { ctx: PlatformContext ->
            val loader = ImageLoader.Builder(ctx)
                .components {
                    add(coil3.network.ktor3.KtorNetworkFetcherFactory(httpClient = http))
                }
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(ctx, 0.10)
                        .build()
                }
                .build()
            com.agon.app.data.ImageLoaderHolder.loader = loader
            loader
        }
    } catch (_: Throwable) { }
}

fun MainViewController(): UIViewController {
    configureCoil()
    return ComposeUIViewController { WithViewModelStoreOwner { App() } }
}
