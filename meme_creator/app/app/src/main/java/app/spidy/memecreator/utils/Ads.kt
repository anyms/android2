package app.spidy.memecreator.utils

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener

object Ads {
    /*
    APPLICATION_ID:  ca-app-pub-1517962596069817~8780968726
    BANNER1:         ca-app-pub-1517962596069817/2742173986
    INTERSTITIAL1:   ca-app-pub-1517962596069817/2103879738
    REWARD:          ca-app-pub-1517962596069817/7579374174
     */

    const val APPLICATION_ID = "ca-app-pub-1517962596069817~8780968726"
    private const val INTERSTITIAL_ID = "ca-app-pub-1517962596069817/2103879738"
    private const val REWARD_ID = "ca-app-pub-1517962596069817/7579374174"

    /* TEST ADS */
//    const val APPLICATION_ID = "ca-app-pub-3940256099942544~3347511713"
//    private const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
//    private const val REWARD_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedVideoAd: RewardedVideoAd? = null

    fun init(context: Context) {
        interstitialAd = InterstitialAd(context)
        interstitialAd!!.adUnitId = INTERSTITIAL_ID
        rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(context)
    }

    fun loadInterstitial() = interstitialAd?.loadAd(AdRequest.Builder().build())
    fun showInterstitial() = interstitialAd?.show()

    private val rewardedAdListener = object : RewardedVideoAdListener {
        override fun onRewardedVideoAdClosed() {}
        override fun onRewardedVideoAdLeftApplication() {}
        override fun onRewardedVideoAdLoaded() {}
        override fun onRewardedVideoAdOpened() {}
        override fun onRewardedVideoCompleted() {}
        override fun onRewarded(rewardItem: RewardItem?) {}
        override fun onRewardedVideoStarted() {}
        override fun onRewardedVideoAdFailedToLoad(i: Int) {}
    }
    fun loadReward() = rewardedVideoAd?.loadAd(REWARD_ID, AdRequest.Builder().build())
    fun showReward() = rewardedVideoAd?.show()
}