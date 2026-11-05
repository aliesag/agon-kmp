package com.agon.app.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.agon.app.data.ads.AdConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

private const val TAG = "AdManager"

@Composable
fun AdMobNativeCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loadedNativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, AdConfig.getNativeAdUnitId())
            .forNativeAd { nativeAd ->
                loadedNativeAd?.destroy()
                loadedNativeAd = nativeAd
                isAdLoaded = true
                Log.i(TAG, "Native loaded")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoaded = false
                    Log.w(TAG, "Ad failed to load (Native): ${loadAdError.message} (code: ${loadAdError.code})")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            try {
                loadedNativeAd?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying NativeAd: ${e.message}")
            }
            loadedNativeAd = null
        }
    }

    if (isAdLoaded && loadedNativeAd != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    createNativeAdView(ctx, loadedNativeAd!!)
                },
                update = { view ->
                    loadedNativeAd?.let { populateNativeAdView(it, view) }
                }
            )
        }
    }
}

/**
 * Creates the programmatic NativeAdView with custom "حكايتنا" dark & red styling
 * separated visually from standard show cards.
 */
private fun createNativeAdView(context: Context, ad: NativeAd): NativeAdView {
    val nativeAdView = NativeAdView(context)
    nativeAdView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    // Root Card Container
    val cardLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        // Background: Charcoal with subtle red border
        val bgDrawable = GradientDrawable().apply {
            setColor(AndroidColor.parseColor("#151720"))
            cornerRadius = dpToPx(context, 16f)
            setStroke(dpToPx(context, 1.5f).toInt(), AndroidColor.parseColor("#2A2C38"))
        }
        background = bgDrawable
        val p = dpToPx(context, 14f).toInt()
        setPadding(p, p, p, p)
    }

    // Top Header: "إعلان ممول" Badge + Advertiser Title
    val topHeader = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Clear "إعلان" Badge
    val adBadge = TextView(context).apply {
        text = "إعلان مروّج"
        setTextColor(AndroidColor.parseColor("#FFFFFF"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        val badgeBg = GradientDrawable().apply {
            setColor(AndroidColor.parseColor("#E50914"))
            cornerRadius = dpToPx(context, 6f)
        }
        background = badgeBg
        val px = dpToPx(context, 8f).toInt()
        val py = dpToPx(context, 3f).toInt()
        setPadding(px, py, px, py)
        gravity = Gravity.CENTER
    }
    topHeader.addView(adBadge)

    // Headline View
    val headlineView = TextView(context).apply {
        setTextColor(AndroidColor.parseColor("#FFFFFF"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(dpToPx(context, 10f).toInt(), 0, dpToPx(context, 10f).toInt(), 0)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }
    topHeader.addView(headlineView)
    nativeAdView.headlineView = headlineView
    cardLayout.addView(topHeader)

    // Middle Content: Icon + Body Text
    val middleLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(context, 10f).toInt()
            bottomMargin = dpToPx(context, 10f).toInt()
        }
    }

    val iconView = ImageView(context).apply {
        val size = dpToPx(context, 48f).toInt()
        layoutParams = LinearLayout.LayoutParams(size, size).apply {
            marginEnd = dpToPx(context, 12f).toInt()
        }
        val iconBg = GradientDrawable().apply {
            setColor(AndroidColor.parseColor("#1F222E"))
            cornerRadius = dpToPx(context, 10f)
        }
        background = iconBg
        clipToOutline = true
    }
    middleLayout.addView(iconView)
    nativeAdView.iconView = iconView

    val bodyView = TextView(context).apply {
        setTextColor(AndroidColor.parseColor("#A0A5B5"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }
    middleLayout.addView(bodyView)
    nativeAdView.bodyView = bodyView
    cardLayout.addView(middleLayout)

    // MediaView (Optional media container)
    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(context, 140f).toInt()
        ).apply {
            bottomMargin = dpToPx(context, 10f).toInt()
        }
        val mediaBg = GradientDrawable().apply {
            setColor(AndroidColor.parseColor("#0C0E14"))
            cornerRadius = dpToPx(context, 8f)
        }
        background = mediaBg
        clipToOutline = true
    }
    cardLayout.addView(mediaView)
    nativeAdView.mediaView = mediaView

    // Call to Action Button
    val ctaButton = Button(context).apply {
        setTextColor(AndroidColor.parseColor("#FFFFFF"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        val ctaBg = GradientDrawable().apply {
            setColor(AndroidColor.parseColor("#E50914"))
            cornerRadius = dpToPx(context, 10f)
        }
        background = ctaBg
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(context, 42f).toInt()
        )
    }
    cardLayout.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    nativeAdView.addView(cardLayout)
    populateNativeAdView(ad, nativeAdView)
    return nativeAdView
}

private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline

    if (nativeAd.body == null) {
        nativeAdView.bodyView?.visibility = View.GONE
    } else {
        nativeAdView.bodyView?.visibility = View.VISIBLE
        (nativeAdView.bodyView as? TextView)?.text = nativeAd.body
    }

    if (nativeAd.callToAction == null) {
        nativeAdView.callToActionView?.visibility = View.GONE
    } else {
        nativeAdView.callToActionView?.visibility = View.VISIBLE
        (nativeAdView.callToActionView as? Button)?.text = nativeAd.callToAction
    }

    if (nativeAd.icon == null) {
        nativeAdView.iconView?.visibility = View.GONE
    } else {
        nativeAdView.iconView?.visibility = View.VISIBLE
        (nativeAdView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
    }

    if (nativeAd.mediaContent != null && nativeAd.mediaContent!!.hasVideoContent()) {
        nativeAdView.mediaView?.visibility = View.VISIBLE
    } else if (nativeAd.images.isNotEmpty()) {
        nativeAdView.mediaView?.visibility = View.VISIBLE
    } else {
        nativeAdView.mediaView?.visibility = View.GONE
    }

    nativeAdView.setNativeAd(nativeAd)
}

private fun dpToPx(context: Context, dp: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics
    )
}
