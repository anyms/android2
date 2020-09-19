package app.spidy.pirum.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.toast
import app.spidy.pirum.R
import app.spidy.pirum.adapters.HistoryAdapter
import app.spidy.pirum.data.History
import app.spidy.pirum.databases.PyrumDatabase
import app.spidy.pirum.services.MediaDownloadService
import app.spidy.pirum.utils.PlaylistStatus
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_downloads.*
import kotlinx.android.synthetic.main.layout_toolbar.*
import kotlin.concurrent.thread

class DownloadsActivity : AppCompatActivity(), BillingProcessor.IBillingHandler {
    private val history = ArrayList<History>()
    private lateinit var database: PyrumDatabase
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var billingProcessor: BillingProcessor
    private lateinit var tinyDB: TinyDB

    fun purchase() {
        billingProcessor.purchase(this, "app.spidy.pirum")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        database = Room.databaseBuilder(this, PyrumDatabase::class.java, "PyrumDatabase")
            .fallbackToDestructiveMigration().build()
        tinyDB = TinyDB(this)

        val adView: AdView = findViewById(R.id.adView)
        if (!tinyDB.getBoolean("isPro")) {
            recyclerView.visibility = View.GONE
            nothingView.visibility = View.GONE
            adView.loadAd(AdRequest.Builder().build())
        } else {
            upgradeBox.visibility = View.GONE
            adView.visibility = View.GONE
        }

        billingProcessor = BillingProcessor(this,
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkqz95QM+5Yd4Vwd/kjQGJJjp4Molpa3DJ9eLd4heQIORZBsFTC5yCdDYbhgD3dQzzxUuWguEWTrUbHRdSSVIqEXMQkez51WcCLOONHcK1PLjF5euELIJOTe4hWHCg9cFpeb17Nu5L0Bub2T+5lAaZh8zvTUVdMT31YyDFtWdKoW90ab1uJcc7eBXiZpsHxksU4MQHpDnpCoSG5it1kVrKdNd7W6QjIDO4LnLxsDctI2d8ebmgR1UgpwqWaWLUoE5YWmcVgLhYh67yW3wFAdloQSjlHhmc79JaBCbJrxgZhps5iPVulAPpx7lpZhK22eAodib4mFrHTeHECaP0gUTbwIDAQAB",
            this)
        billingProcessor.initialize()

        upgradeBtn.setOnClickListener {
            purchase()
        }

        setSupportActionBar(toolbar)
        titleView.text = getString(R.string.downloads)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        historyAdapter = HistoryAdapter(this, history, {
            return@HistoryAdapter supportFragmentManager
        }, {
            if (tinyDB.getBoolean("isPro")) {
                updateHistory()
            }
        })
        recyclerView.adapter = historyAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onPause() {
        MediaDownloadService.downloadListener = null
        super.onPause()
    }

    override fun onResume() {
        if (tinyDB.getBoolean("isPro")) {
            updateHistory()
        }
        super.onResume()
    }


    private fun updateHistory() {
        history.clear()
        thread {
            val playlistNames = ArrayList<String>()
            database.pyrumDao().getMusic().forEach { music ->
                if (!playlistNames.contains(music.playlistName)) {
                    playlistNames.add(music.playlistName)
                }
            }
            database.pyrumDao().getVideos().forEach { video ->
                if (!playlistNames.contains(video.playlistName)) {
                    playlistNames.add(video.playlistName)
                }
            }

            for (playlistName in playlistNames) {
                val music = database.pyrumDao().getMusicDownloadsByPlaylist(playlistName)
                if (music.isNotEmpty()) {
                    val his = History(playlistName, music.size, "music", PlaylistStatus.STATE_COMPLETED)
                    history.add(his)
                }


                val videos = database.pyrumDao().getVideoDownloadsByPlaylist(playlistName)
                if (videos.isNotEmpty()) {
                    val his = History(playlistName, videos.size, "video", PlaylistStatus.STATE_COMPLETED)
                    history.add(his)
                }

            }

            app.spidy.kotlinutils.onUiThread {
                if (history.isEmpty()) {
                    nothingView.visibility = View.VISIBLE
                    recyclerView.visibility = View.INVISIBLE
                } else {
                    nothingView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
                historyAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        billingProcessor.release()
        super.onDestroy()
    }

    override fun onBillingInitialized() {

    }

    override fun onPurchaseHistoryRestored() {

    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        tinyDB.putBoolean("isPro", true)
        toast("Great! you've purchased pro version, restart the app to enable the changes.")
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        toast("billing failed")
    }
}
