package com.agon.app.data.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

enum class AdState {
    NO_AD,
    READY,
    SHOWING,
    COOLDOWN
}

object AdManager {
    private const val TAG = "AdManager"

    // Default protection cooldown period: 180 seconds (3 minutes) between interstitials
    var cooldownMillis: Long = 180_000L
    var lastInterstitialShownTime: Long = 0L

    var interstitialState: AdState = AdState.NO_AD
        private set

    // Tracking context & player status
    var isInsidePlayer: Boolean = false
    var isWatchingVideo: Boolean = false
    var isPlayerInteracting: Boolean = false
    var currentScreen: String = "home"

    private var activeInterstitialAd: InterstitialAd? = null
    private var isPreloadingInterstitial = false
    private val isInitialized = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                // Register lifecycle callbacks to monitor and protect full-screen AdActivity
                val app = context.applicationContext as? Application
                app?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                    private var adAutoDismissRunnable: Runnable? = null

                    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                    override fun onActivityStarted(activity: Activity) {}

                    override fun onActivityResumed(activity: Activity) {
                        if (activity.javaClass.name.contains("AdActivity", ignoreCase = true)) {
                            Log.i(TAG, "AdActivity detected: ${activity.javaClass.name}")

                            // Inject immediate, high-contrast professional close 'X' button
                            mainHandler.postDelayed({
                                injectProfessionalCloseButton(activity)
                            }, 500L)

                            // Safety timer: Automatically dismiss ad after 6 seconds to prevent user blocking
                            adAutoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
                            adAutoDismissRunnable = Runnable {
                                if (!activity.isFinishing && !activity.isDestroyed) {
                                    Log.i(TAG, "Auto-dismissing AdActivity after 6 seconds to prevent user blocking")
                                    activity.finish()
                                }
                            }
                            mainHandler.postDelayed(adAutoDismissRunnable!!, 6000L)
                        }
                    }

                    override fun onActivityPaused(activity: Activity) {
                        if (activity.javaClass.name.contains("AdActivity", ignoreCase = true)) {
                            adAutoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
                        }
                    }

                    override fun onActivityStopped(activity: Activity) {}
                    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                    override fun onActivityDestroyed(activity: Activity) {
                        if (activity.javaClass.name.contains("AdActivity", ignoreCase = true)) {
                            adAutoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
                        }
                    }
                })

                MobileAds.initialize(context.applicationContext) { status ->
                    Log.d(TAG, "AdManager initialized successfully: $status")
                    preloadInterstitial(context.applicationContext)
                }
            } catch (e: Exception) {
                Log.e(TAG, "AdManager initialization error: ${e.message}", e)
            }
        }
    }

    /**
     * Preloads an interstitial ad ahead of time without blocking the UI thread.
     */
    fun preloadInterstitial(context: Context) {
        if (activeInterstitialAd != null) {
            interstitialState = AdState.READY
            return
        }

        if (isPreloadingInterstitial) {
            return
        }

        isPreloadingInterstitial = true
        val adUnitId = AdConfig.getInterstitialAdUnitId()
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    isPreloadingInterstitial = false
                    activeInterstitialAd = interstitialAd
                    interstitialState = AdState.READY
                    Log.i(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isPreloadingInterstitial = false
                    activeInterstitialAd = null
                    interstitialState = AdState.NO_AD
                    Log.w(TAG, "Ad failed to load: ${loadAdError.message} (code: ${loadAdError.code})")

                    // Schedule a safe retry with backoff after 30 seconds
                    mainHandler.postDelayed({
                        preloadInterstitial(context)
                    }, 30_000L)
                }
            }
        )
    }

    /**
     * Checks if displaying an interstitial ad is permitted right now.
     */
    fun canShowInterstitial(): Boolean {
        // 1. Check if ad is loaded
        if (activeInterstitialAd == null) {
            Log.d(TAG, "Interstitial not shown: No ad ready")
            return false
        }

        // 2. Check if currently showing an ad
        if (interstitialState == AdState.SHOWING) {
            Log.d(TAG, "Interstitial blocked: Another ad is currently showing")
            return false
        }

        // 3. Check Cooldown protection period
        val now = System.currentTimeMillis()
        val timeSinceLastAd = now - lastInterstitialShownTime
        if (lastInterstitialShownTime > 0 && timeSinceLastAd < cooldownMillis) {
            val remainingSec = (cooldownMillis - timeSinceLastAd) / 1000
            Log.i(TAG, "Interstitial blocked by cooldown ($remainingSec seconds remaining)")
            interstitialState = AdState.COOLDOWN
            return false
        }

        // 4. Check if user is actively watching video
        if (isWatchingVideo) {
            Log.i(TAG, "Interstitial blocked because user is watching")
            return false
        }

        // 5. Check if user is actively interacting with player controls
        if (isPlayerInteracting) {
            Log.i(TAG, "Interstitial blocked because user is interacting with controls")
            return false
        }

        return true
    }

    /**
     * Displays the interstitial ad if permitted, otherwise invokes onComplete immediately.
     */
    fun showTransitionAd(activity: Activity?, onComplete: () -> Unit) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        if (!canShowInterstitial()) {
            onComplete()
            return
        }

        val ad = activeInterstitialAd
        if (ad == null) {
            onComplete()
            return
        }

        interstitialState = AdState.SHOWING
        var completedInvoked = false
        fun safeOnComplete() {
            if (!completedInvoked) {
                completedInvoked = true
                onComplete()
            }
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.i(TAG, "Interstitial shown")
                interstitialState = AdState.SHOWING
            }

            override fun onAdDismissedFullScreenContent() {
                Log.i(TAG, "Interstitial closed")
                lastInterstitialShownTime = System.currentTimeMillis()
                interstitialState = AdState.COOLDOWN
                activeInterstitialAd = null
                preloadInterstitial(activity.applicationContext)
                safeOnComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Ad failed to show: ${adError.message}")
                activeInterstitialAd = null
                interstitialState = AdState.NO_AD
                preloadInterstitial(activity.applicationContext)
                safeOnComplete()
            }
        }

        try {
            // Safety watchdog: ensure callback is always executed within 6 seconds even if ad hangs or user cannot press X
            mainHandler.postDelayed({
                if (interstitialState == AdState.SHOWING) {
                    Log.w(TAG, "Ad watchdog triggered after 6 seconds, proceeding to content")
                    safeOnComplete()
                }
            }, 6000L)

            ad.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing interstitial ad: ${e.message}", e)
            activeInterstitialAd = null
            interstitialState = AdState.NO_AD
            preloadInterstitial(activity.applicationContext)
            safeOnComplete()
        }
    }

    /**
     * Specifically used before starting a show or episode.
     * If the ad is ready and allowed, shows it immediately.
     * If preloading or not allowed, guarantees rapid completion so the video is never stuck.
     */
    fun showBeforeEpisodeAd(
        activity: Activity?,
        onProceedToVideo: () -> Unit
    ) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onProceedToVideo()
            return
        }

        // If cooldown applies or cannot show, go straight to playback
        if (!canShowInterstitial()) {
            preloadInterstitial(activity.applicationContext)
            onProceedToVideo()
            return
        }

        showTransitionAd(activity) {
            onProceedToVideo()
        }
    }

    private fun injectProfessionalCloseButton(activity: Activity) {
        try {
            val decorView = activity.window?.decorView as? ViewGroup ?: return
            val existingBtn = decorView.findViewWithTag<android.view.View>("ad_pro_close_btn")
            if (existingBtn != null) return

            val dp = activity.resources.displayMetrics.density
            val closeContainer = FrameLayout(activity).apply {
                tag = "ad_pro_close_btn"
                layoutParams = FrameLayout.LayoutParams(
                    (48 * dp).toInt(),
                    (48 * dp).toInt()
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = (24 * dp).toInt()
                    rightMargin = (18 * dp).toInt()
                    leftMargin = (18 * dp).toInt()
                }
                elevation = 100f * dp
            }

            val buttonBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E6111116"))
                setStroke((2 * dp).toInt(), Color.parseColor("#80FFFFFF"))
            }

            val closeText = TextView(activity).apply {
                text = "✕"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                background = buttonBg
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                isClickable = true
                isFocusable = true
                contentDescription = "إغلاق الإعلان"
                setOnClickListener {
                    Log.i(TAG, "User clicked professional ad close button (X)")
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        activity.finish()
                    }
                }
            }

            closeContainer.addView(closeText)
            decorView.addView(closeContainer)
        } catch (e: Exception) {
            Log.e(TAG, "Could not inject close button: ${e.message}")
        }
    }
}
