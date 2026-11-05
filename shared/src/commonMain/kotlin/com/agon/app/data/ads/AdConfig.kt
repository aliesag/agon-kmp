package com.agon.app.data.ads

import com.agon.app.BuildConfig

object AdConfig {
    // Official Google AdMob identifiers for "حكايتنا"
    const val APP_ID = "ca-app-pub-5985288941774654~6503958852"
    const val BANNER_ID = "ca-app-pub-5985288941774654/9586347955"
    const val INTERSTITIAL_ID = "ca-app-pub-5985288941774654/1520602782"
    const val NATIVE_ID = "ca-app-pub-5985288941774654/2502064851"

    // Google AdMob Standard Test IDs (For development & safety)
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"

    // Use test ads during debug builds to prevent AdMob policy invalid traffic violations
    // Automatically uses production IDs in release builds
    val isDebugMode: Boolean
        get() = BuildConfig.DEBUG

    fun getBannerAdUnitId(): String {
        return if (isDebugMode) TEST_BANNER_ID else BANNER_ID
    }

    fun getInterstitialAdUnitId(): String {
        return if (isDebugMode) TEST_INTERSTITIAL_ID else INTERSTITIAL_ID
    }

    fun getNativeAdUnitId(): String {
        return if (isDebugMode) TEST_NATIVE_ID else NATIVE_ID
    }
}
