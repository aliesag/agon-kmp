package com.agon.app.data.ads

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

enum class AdState { NO_AD, LOADING, LOADED, SHOWING }

/**
 * KMP port of AdManager.
 * Google Mobile Ads iOS SDK is wired in stage 8; this stub preserves the exact
 * public surface PlayerScreen + HomeScreen expect, and mirrors the cooldown
 * + video/interaction gating rules from the Android original.
 */
object AdManager {
    var cooldownMillis: Long = 180_000L
    var lastInterstitialShownTime: Long = 0L
    var interstitialState: AdState = AdState.NO_AD

    var isInsidePlayer: Boolean = false
    var isWatchingVideo: Boolean = false
    var isPlayerInteracting: Boolean = false
    var currentScreen: String = "home"

    fun initialize() { /* FILL: GADMobileAds.start() on iOS */ }
    fun preloadInterstitial() { interstitialState = AdState.LOADED }

    fun canShowInterstitial(): Boolean {
        if (interstitialState != AdState.LOADED) return false
        if (isWatchingVideo || isPlayerInteracting || isInsidePlayer) return false
        val now = Clock.System.now().toEpochMilliseconds()
        return (now - lastInterstitialShownTime) >= cooldownMillis
    }

    /** No-op on iOS until GADInterstitialAd is wired; still calls [onComplete]. */
    fun showTransitionAd(onComplete: () -> Unit) {
        onComplete()
    }

    /** No-op on iOS until GADInterstitialAd is wired; still calls [onComplete]. */
    fun showBeforeEpisodeAd(onComplete: () -> Unit) {
        onComplete()
    }
}
