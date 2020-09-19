package app.spidy.lankanews.utils

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener

object Ads {
    const val APPLICATION_ID = "ca-app-pub-1517962596069817~5871042736"
    private const val INTERSTITIAL_ID = "ca-app-pub-1517962596069817/9427144364"

    /* TEST ADS */
//    const val APPLICATION_ID = "ca-app-pub-3940256099942544~3347511713"
//    private const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    fun initInterstitial(context: Context) {
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