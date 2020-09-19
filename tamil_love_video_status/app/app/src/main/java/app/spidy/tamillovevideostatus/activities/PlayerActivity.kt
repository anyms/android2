package app.spidy.tamillovevideostatus.activities

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import androidx.viewpager2.widget.ViewPager2
import app.spidy.kotlinutils.*
import app.spidy.tamillovevideostatus.R
import app.spidy.tamillovevideostatus.adapters.VideoPagerAdapter
import app.spidy.tamillovevideostatus.data.Video
import app.spidy.tamillovevideostatus.databases.VidStatusDatabase
import app.spidy.tamillovevideostatus.utils.Ads
import app.spidy.tamillovevideostatus.utils.Http
import app.spidy.tamillovevideostatus.ytdl.YtDL
import kotlinx.android.synthetic.main.activity_player.*
import org.json.JSONObject
import java.lang.Exception
import kotlin.concurrent.thread


class PlayerActivity : AppCompatActivity() {
    private lateinit var videoAdapter: VideoPagerAdapter
    private lateinit var database: VidStatusDatabase
    private lateinit var tinyDB: TinyDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        database = Room.databaseBuilder(this, VidStatusDatabase::class.java, "VidStatusDatabase")
            .fallbackToDestructiveMigration().build()
        tinyDB = TinyDB(this)

        Ads.showInterstitial()
        Ads.loadInterstitial()

        videoAdapter = VideoPagerAdapter(supportFragmentManager, lifecycle)
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewPager.adapter = videoAdapter

        val i = intent!!
        val video = Video(
            uId = i.getLongExtra("uId", 0L),
            title = i.getStringExtra("title")!!,
            videoId = i.getStringExtra("videoId")!!,
            tags = i.getStringExtra("tags")!!,
            viewCount = i.getIntExtra("viewCount", 0),
            downloadCount = i.getIntExtra("downloadCount", 0),
            shareCount = i.getIntExtra("shareCount", 0),
            isExpired = i.getBooleanExtra("isExpire", true),
            category = i.getStringExtra("category")!!,
            thumb = i.getStringExtra("thumb")!!,
            data = i.getStringExtra("data")!!,
            expire = i.getLongExtra("expire", 0L)
        )
        thread {
//            val dbVids = database.dao().getVideo(video.uId)
//            if (dbVids.isEmpty() || video.expire != dbVids[0].expire) {
//                debug("fetching video data")
//                val videoData = Http.sync.get("${Http.apiUrl}/video/${video.videoId}?key=${Http.apiKey}")
//                video.data = videoData.text!!
//                database.dao().putVideo(video)
//                onUiThread { videoAdapter.add(video) }
//            }else {
//                debug("using local data")
//                video.data = dbVids[0].data
//                database.dao().updateVideo(video)
//                onUiThread { videoAdapter.add(video) }
//            }
            try {
                val dl = YtDL()
                video.data = dl.getInfo(video.videoId)
            } catch (e: Exception) {
                onUiThread {
                    toast("Video removed", true)
                    finish()
                }
            }

            onUiThread {
                videoAdapter.add(video)
                progressBar.visibility = View.GONE
            }
        }
    }
}