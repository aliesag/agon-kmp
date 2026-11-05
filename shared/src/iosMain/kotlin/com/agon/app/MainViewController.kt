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
import coil3.network.ktor.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
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
        val ktor = HttpClient(Darwin) {
            defaultRequest {
                val u = url.toString().lowercase()
                val ref = when {
                    u.contains("ffosta") || u.contains("fosta") -> "https://ffosta-tv.quest/"
                    u.contains("alooytv") -> "https://ep.alooytv16.xyz/"
                    u.contains("yalla.moda") -> "https://yalla.moda/"
                    u.contains("tmdb") -> "https://www.themoviedb.org/"
                    else -> "https://e.3cktv.com/"
                }
                header("Referer", ref)
                header(
                    "User-Agent",
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
                )
            }
        }
        SingletonImageLoader.setSafe { ctx: PlatformContext ->
            ImageLoader.Builder(ctx)
                .components { add(KtorNetworkFetcherFactory(ktor)) }
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(ctx, 0.10)
                        .build()
                }
                .build()
        }
    } catch (_: Throwable) { }
}

fun MainViewController(): UIViewController {
    configureCoil()
    return ComposeUIViewController { WithViewModelStoreOwner { App() } }
}
