package app.spidy.tamilvidstatus.activities

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import androidx.viewpager2.widget.ViewPager2
import app.spidy.kotlinutils.*
import app.spidy.tamilvidstatus.R
import app.spidy.tamilvidstatus.adapters.VideoPagerAdapter
import app.spidy.tamilvidstatus.data.Video
import app.spidy.tamilvidstatus.databases.VidStatusDatabase
import app.spidy.tamilvidstatus.utils.Ads
import app.spidy.tamilvidstatus.utils.Http
import app.spidy.tamilvidstatus.ytdl.YtDL
import kotlinx.android.synthetic.main.activity_player.*
import org.json.JSONObject
import java.lang.Exception
import kotlin.concurrent.thread


class PlayerActivity : AppCompatActivity() {
    private lateinit var videoAdapter: VideoPagerAdapter
    private lateinit var database: VidStatusDatabase
    private lateinit var tinyDB: TinyDB
    private lateinit var timeMachine: TimeMachine

    private var currentPage = 1
    private var isNextExists = false

    private var endPoint = "page"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        database = Room.databaseBuilder(this, VidStatusDatabase::class.java, "VidStatusDatabase")
            .fallbackToDestructiveMigration().build()
        tinyDB = TinyDB(this)

        timeMachine = TimeMachine(this)

        Ads.showInterstitial()
        Ads.loadInterstitial()

        videoAdapter = VideoPagerAdapter(supportFragmentManager, lifecycle)
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewPager.adapter = videoAdapter

        val isIntent = intent?.getBooleanExtra("is_intent", false) ?: false
        val intentTag = intent?.getStringExtra("tag")

        if (!isIntent) {
            loadPage(currentPage)
        } else if (intentTag != null) {
            when (intentTag) {
                "latest" -> loadPage(currentPage)
                "popular" -> {
                    endPoint = "popular"
                    loadPage(currentPage)
                }
                else -> {
                    endPoint = "category/${intentTag}"
                    debug(endPoint)
                    loadPage(currentPage)
                }
            }

            if (!tinyDB.getBoolean("swipe_info_already_shown")) {
                newDialog().withTitle("Info")
                    .withMessage("Swipe up to see more videos")
                    .withCancelable(false)
                    .withPositiveButton("Got it!") { dialog ->
                        tinyDB.putBoolean("swipe_info_already_shown", true)
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        } else {
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
//                val dbVids = database.dao().getVideo(video.uId)
//                if (dbVids.isEmpty() || video.isExpired) {
//                    debug("fetching video data")
//                    val videoData = Http.sync.get("${Http.apiUrl}/video/${video.videoId}?key=${Http.apiKey}")
//                    video.data = videoData.text!!
//                    database.dao().putVideo(video)
//                    onUiThread { videoAdapter.add(video) }
//                }else {
//                    debug("using local data")
//                    video.data = dbVids[0].data
//                    database.dao().updateVideo(video)
//                    onUiThread { videoAdapter.add(video) }
//                }

                try {
                    val dl = YtDL()
                    video.data = dl.getInfo(video.videoId)
                } catch (e: Exception) {
                    onUiThread {
                        toast("Video removed", true)
                        finish()
                    }
                }

//                val streamInfo = YoutubeDL.getInstance().getInfo("https://www.youtube.com/watch?v=${video.videoId}")
//                val info = streamInfo.formats.last()
//                debug(info.url)
//                video.data = info.url

                onUiThread {
                    videoAdapter.add(video)
                    progressBar.visibility = View.GONE
                }
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                debug(position)
                if (isNextExists && position >= (currentPage * 12) - 4) {
                    currentPage++
                    loadPage(currentPage)
                }

                Ads.showInterstitial()
                Ads.loadInterstitial()
            }
        })
    }

    private fun loadPage(num: Int) {
        Http.async.get("${Http.apiUrl}/${endPoint}/${num}?key=${Http.apiKey}").then {
            onUiThread { progressBar.visibility = View.GONE }
            val data = JSONObject(it.text!!)
            isNextExists = data.getBoolean("is_next_exists")
            val vids = data.getJSONArray("videos")
            for (i in 0 until vids.length()) {
                val vid = vids.getJSONObject(i)
                val dbVids = database.dao().getVideo(vid.getLong("id"))
                var video = Video(
                    uId = vid.getLong("id"),
                    downloadCount = vid.getInt("download_count"),
                    shareCount = vid.getInt("share_count"),
                    viewCount = vid.getInt("view_count"),
                    tags = vid.getString("tags"),
                    title = vid.getString("title"),
                    videoId = vid.getString("video_id"),
                    isExpired = vid.getBoolean("is_expired"),
                    category = vid.getString("category"),
                    thumb = vid.getString("thumb"),
                    expire = vid.getLong("expire")
                )
//                if (dbVids.isEmpty() || video.expire != dbVids[0].expire) {
//                    debug("fetching video data")
//                    val videoData = Http.sync.get("${Http.apiUrl}/video/${video.videoId}?key=${Http.apiKey}")
//                    video.data = videoData.text!!
//                } else {
//                    debug("using local data")
//                    video.data = dbVids[0].data
//                }
                database.dao().putVideo(video)
                onUiThread { videoAdapter.add(video) }
            }
        }.catch {
            debug(it)
            onUiThread {
//                newDialog().withTitle("Network Failed")
//                    .withMessage("Unable to connect to the server, please check your internet connection.")
//                    .withCancelable(false)
//                    .withPositiveButton(getString(R.string.ok)) { dialog ->
//                        dialog.dismiss()
//                    }
//                    .create().show()
                toast("Network failed")
                loadPage(num)
            }
        }
    }
}