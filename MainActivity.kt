package com.kronos.quiz

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var rewardedAd: RewardedAd? = null
    private val AD_UNIT_ID = "ca-app-pub-1263987533360207/6126114475"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        webView = WebView(this)
        setContentView(webView)

        MobileAds.initialize(this) {
            loadRewardedAd()
        }

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(AdMobBridge(), "AndroidAdMob")

        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }

    inner class AdMobBridge {
        @JavascriptInterface
        fun showRewardedAd(rewardType: String) {
            runOnUiThread {
                val ad = rewardedAd
                if (ad != null) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            loadRewardedAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            rewardedAd = null
                            loadRewardedAd()
                            webView.evaluateJavascript("window.onAdMobRewardFailed && window.onAdMobRewardFailed();", null)
                        }
                    }
                    ad.show(this@MainActivity) {
                        webView.evaluateJavascript("window.onAdMobRewardGranted && window.onAdMobRewardGranted('$rewardType');", null)
                    }
                } else {
                    loadRewardedAd()
                    webView.evaluateJavascript("window.onAdMobRewardGranted && window.onAdMobRewardGranted('$rewardType');", null)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
