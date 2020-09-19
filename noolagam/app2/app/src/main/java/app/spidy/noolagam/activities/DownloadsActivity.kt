package app.spidy.noolagam.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import app.spidy.noolagam.R
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_recent.*
import kotlinx.android.synthetic.main.activity_recent.toolbar

class DownloadsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        adView.loadAd(AdRequest.Builder().build())

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener { finish() }
    }
}