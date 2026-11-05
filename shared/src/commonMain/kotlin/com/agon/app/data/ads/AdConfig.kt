package com.agon.app.data.ads

object AdConfig {
    const val APP_ID = "ca-app-pub-5985288941774654~6503958852"
    const val BANNER_ID = "ca-app-pub-5985288941774654/9586347955"
    const val INTERSTITIAL_ID = "ca-app-pub-5985288941774654/1520602782"
    const val NATIVE_ID = "ca-app-pub-5985288941774654/2502064851"

    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"

    // Was BuildConfig.DEBUG (Android-only). Kept false in shared; wire
    // per-platform expect/actual once AdMob is ported to shared.
    val isDebugMode: Boolean get() = false

    fun getBannerAdUnitId(): String = if (isDebugMode) TEST_BANNER_ID else BANNER_ID
    fun getInterstitialAdUnitId(): String = if (isDebugMode) TEST_INTERSTITIAL_ID else INTERSTITIAL_ID
    fun getNativeAdUnitId(): String = if (isDebugMode) TEST_NATIVE_ID else NATIVE_ID
}
