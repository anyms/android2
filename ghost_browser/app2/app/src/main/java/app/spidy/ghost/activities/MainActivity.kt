package app.spidy.ghost.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import app.spidy.ghost.R
import app.spidy.ghost.utils.Ads
import app.spidy.ghost.utils.TinyDB
import app.spidy.kookaburra.fragments.BrowserFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var browserFragment: BrowserFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        TinyDB(applicationContext).putBoolean(IntroActivity.IS_SHOWN, true)

        Ads.init(this)
        Ads.loadInterstitial()
        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        browserFragment = BrowserFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.browserHolder, browserFragment)
            .commit()
        var adTime = 20L
        thread {
            while (true) {
                Thread.sleep(adTime * 1000)

                runOnUiThread {
                    Ads.showInterstitial()
                    Ads.loadInterstitial()
                }
                adTime = 60 * 5
            }
        }
    }

    override fun onBackPressed() {
        if (!browserFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun finish() {
        LoadingActivity.vpnController.cleanup()
        super.finish()
    }

    override fun onDestroy() {
        LoadingActivity.vpnController.init()
        TinyDB(applicationContext).putBoolean(IntroActivity.IS_SHOWN, true)
        super.onDestroy()
    }
}
