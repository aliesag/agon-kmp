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
        SingletonImageLoader.setSafe { ctx: PlatformContext ->
            ImageLoader.Builder(ctx)
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
