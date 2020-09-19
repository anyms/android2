package app.spidy.freeproxylist.utils

import android.content.Context
import app.spidy.kotlinutils.debug
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd

object Ads {
    const val APPLICATION_ID = "ca-app-pub-1517962596069817~1551173711"
    private const val INTERSTITIAL_ID = "ca-app-pub-1517962596069817/6857193489"


    /* TEST ADS */
//    const val APPLICATION_ID = "ca-app-pub-3940256099942544~3347511713"
//    private const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null

    fun initInterstitial(context: Context) {
        interstitialAd = InterstitialAd(context)
        interstitialAd!!.adUnitId = INTERSTITIAL_ID
    }

    fun showInterstitial() {
        interstitialAd?.show()
    }

    fun loadInterstitial() {
        interstitialAd?.loadAd(AdRequest.Builder().build())

        interstitialAd?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                debug("Ad Loaded")
                super.onAdLoaded()
            }

            override fun onAdFailedToLoad(p0: Int) {
                debug("Ad failed to load: $p0")
                super.onAdFailedToLoad(p0)
            }
        }
    }
}