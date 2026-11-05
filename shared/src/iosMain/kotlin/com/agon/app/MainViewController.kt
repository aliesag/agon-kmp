package com.agon.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import platform.UIKit.UIViewController

private class IOSViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

@Composable
private fun WithViewModelStoreOwner(content: @Composable () -> Unit) {
    val owner = remember { IOSViewModelStoreOwner() }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { content() }
}

fun MainViewController(): UIViewController = ComposeUIViewController {
    WithViewModelStoreOwner { App() }
}
