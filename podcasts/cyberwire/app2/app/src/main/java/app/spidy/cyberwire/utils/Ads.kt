package app.spidy.cyberwire.utils

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd

object Ads {
    const val APPLICATION_ID = "ca-app-pub-1517962596069817~4726151919"
    private const val INTERSTITIAL_ID = "ca-app-pub-1517962596069817/4932614591"

    /* TEST ADS */
//    const val APPLICATION_ID = "ca-app-pub-3940256099942544~3347511713"
//    const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    fun init(context: Context) {
        interstitialAd = InterstitialAd(context)
        interstitialAd!!.adUnitId = INTERSTITIAL_ID
    }

    fun loadInterstitial() {
        interstitialAd?.loadAd(AdRequest.Builder().build())
    }

    fun showInterstitial() {
        interstitialAd?.show()
    }
}