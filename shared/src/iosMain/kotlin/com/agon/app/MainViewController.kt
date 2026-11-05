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
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.UIKit.UIViewController

private class IOSViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

@Composable
private fun WithViewModelStoreOwner(content: @Composable () -> Unit) {
    val owner = remember { IOSViewModelStoreOwner() }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { content() }
}

private fun installCrashLogger() {
    // Native Obj-C exceptions
    NSSetUncaughtExceptionHandler { ex ->
        if (ex != null) {
            println("VANTA_CRASH NSException: name=${ex.name} reason=${ex.reason}")
            ex.callStackSymbols.forEach { println("  at $it") }
        }
    }
    // Kotlin unhandled coroutine exceptions
    kotlinx.coroutines.setUnhandledExceptionHook?.let { hook ->
        try { hook { t -> println("VANTA_CRASH Kotlin: ${t.message}\n${t.stackTraceToString()}") } } catch (_: Throwable) {}
    }
}

private fun configureCoil() {
    SingletonImageLoader.setSafe { ctx: PlatformContext ->
        ImageLoader.Builder(ctx)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(ctx, 0.10)   // 10% of RAM — very conservative for iOS
                    .build()
            }
            .build()
    }
}

fun MainViewController() = run {
    installCrashLogger()
    configureCoil()
    ComposeUIViewController { WithViewModelStoreOwner { App() } }
}
