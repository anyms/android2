package app.spidy.pirum.activities

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.toast
import app.spidy.pirum.R
import app.spidy.pirum.data.Video
import app.spidy.pirum.databases.PyrumDatabase
import app.spidy.pirum.utils.Ads
import app.spidy.pirum.utils.DownloadStatus
import app.spidy.pirum.utils.DownloadUtil
import app.spidy.pirum.utils.IO
import app.spidy.pirum.utils.IO.getFileNameFromURL
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_video_player.*
import kotlinx.android.synthetic.main.video_exo_player_control_view.*
import org.json.JSONArray
import java.net.URLDecoder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var player: SimpleExoPlayer
    private lateinit var database: PyrumDatabase
    private lateinit var tinyDB: TinyDB


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        tinyDB = TinyDB(this)

        val data = intent?.getStringExtra("data")
        var currentIndex = intent?.getIntExtra("currentIndex", 0) ?: 0
        val isFromLocal = intent?.getBooleanExtra("isFromLocal", false)
        val currentTime = intent?.getStringExtra("currentTime")
        val videos = ArrayList<Video>()
        player = SimpleExoPlayer.Builder(this).build()
        player.playWhenReady = true
        playerView.player = player
        val playlistName = intent?.getStringExtra("playlistName") ?: IO.getPlaylistName()
        database = Room.databaseBuilder(this, PyrumDatabase::class.java, "PyrumDatabase")
            .fallbackToDestructiveMigration().build()

        if (data != null) {
            val playlist: JSONArray
            try {
                playlist = JSONArray(data)
            } catch (e: Exception) {
                toast("Invalid request")
                finish()
                return
            }


            for (i in 0 until   playlist.length()) {
                val info = playlist.getJSONObject(i)
                when {
                    info.getString("type") == "join" -> {
                        val url = URLDecoder.decode(info.getString("src"), "UTF-8")
                        var title = URLDecoder.decode(info.getString("title"), "UTF-8")
                        if (title == "") {
                            title = getFileNameFromURL(url) ?: "-"
                        }
                        val v = Video(
                            uId = UUID.randomUUID().toString(),
                            status = DownloadStatus.STATE_NONE,
                            playlistName = playlistName,
                            title = title,
                            type = "join",
                            src = url
                        )
                        if (i == currentIndex) videos.add(0, v) else videos.add(v)
                    }
                    info.getString("type") == "separate" -> {
                        val vUrl = URLDecoder.decode(info.getString("vSrc"), "UTF-8")
                        val aUrl = URLDecoder.decode(info.getString("aSrc"), "UTF-8")
                        var title = URLDecoder.decode(info.getString("title"), "UTF-8")
                        if (title == "") {
                            title = getFileNameFromURL(vUrl) ?: "-"
                        }
                        val v = Video(
                            uId = UUID.randomUUID().toString(),
                            status = DownloadStatus.STATE_NONE,
                            playlistName = playlistName,
                            title = title,
                            type = "separate",
                            vSrc = vUrl,
                            aSrc = aUrl
                        )
                        if (i == currentIndex) videos.add(0, v) else videos.add(v)
                    }
                    info.getString("type") == "stream" -> {
                        val url = URLDecoder.decode(info.getString("src"), "UTF-8")
                        var title = URLDecoder.decode(info.getString("title"), "UTF-8")
                        if (title == "") {
                            title = getFileNameFromURL(url) ?: "-"
                        }
                        val v = Video(
                            uId = UUID.randomUUID().toString(),
                            status = DownloadStatus.STATE_NONE,
                            playlistName = playlistName,
                            title = title,
                            type = "stream",
                            src = url
                        )
                        if (i == currentIndex) videos.add(0, v) else videos.add(v)
                    }
                }
            }

            initPlayer(videos, currentIndex)
        } else if (isFromLocal == true) {
            thread {
                database.pyrumDao().getVideoByPlaylist(playlistName).forEachIndexed {i, v ->
                    if (i == currentIndex) videos.add(0, v) else videos.add(v)
                }

                runOnUiThread {
                    initPlayer(videos, currentIndex)
                }
            }
        } else {
            val url = "https://www.masstamilandownload.com/tamil/Melody%20Songs/Nadhiye%20Nadhiye-Masstamilan.In.mp3"
            videos.add(Video(
                uId = UUID.randomUUID().toString(),
                status = DownloadStatus.STATE_NONE,
                playlistName = playlistName,
                title = getFileNameFromURL(url) ?: "-",
                type = "stream",
                src = url
            ))
        }

        if (isFromLocal == null || !isFromLocal) {
            thread {
                for (v in videos) {
                    database.pyrumDao().putVideo(v)
                }
            }
        }


        playerCloseButton.setOnClickListener {
            finish()
        }

        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                loadingBar.visibility = if (playbackState == Player.STATE_BUFFERING) { View.VISIBLE } else { View.INVISIBLE }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                debug(error)
                loadingBar.visibility = View.INVISIBLE
                playerErrorView.visibility = View.VISIBLE
            }

            override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
            ) {
                currentIndex = player.currentWindowIndex
                nameView.text = videos[currentIndex].title
                if (currentTime != null) {
                    player.seekTo(currentTime.toLong() * 1000)
                }
            }

        })


//        if (url != null && isHls != null) {
//            player = SimpleExoPlayer.Builder(this).build()
//            playerView.player = player
//            val dataSource = DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)))
//            val videoSource = if (isHls) {
//                HlsMediaSource.Factory(dataSource)
//                    .createMediaSource(Uri.parse(url))
//            } else {
//                ProgressiveMediaSource.Factory(dataSource)
//                    .createMediaSource(Uri.parse(url))
//            }
//            val videoSource = ProgressiveMediaSource.Factory(dataSource)
//                    .createMediaSource(Uri.parse("https://r3---sn-nau-jhcs.googlevideo.com/videoplayback?expire=1589343255&ei=tx-7XpPkJL2JssUP6NKkoA4&ip=112.135.47.133&id=o-AKwdjrhz2mEGT4w-7pK79kUkKEnEwwmlziW0ySQQFpPt&itag=243&aitags=133%2C134%2C160%2C242%2C243%2C278&source=youtube&requiressl=yes&mh=No&mm=31%2C29&mn=sn-nau-jhcs%2Csn-npoe7ney&ms=au%2Crdu&mv=m&mvi=2&pl=22&initcwndbps=385000&vprv=1&mime=video%2Fwebm&gir=yes&clen=4927922&dur=153.235&lmt=1540588251794485&mt=1589321616&fvip=5&keepalive=yes&c=WEB&txp=5532432&sparams=expire%2Cei%2Cip%2Cid%2Caitags%2Csource%2Crequiressl%2Cvprv%2Cmime%2Cgir%2Cclen%2Cdur%2Clmt&sig=AOq0QJ8wRAIgOpe3woit0qj5WoHGr3DlniRtcMsVsg7lFDV4ySDHvnECIG6oNaNNgeJKQ-mZDZLzmYTovGGxXolh3OqmVpsKD9yW&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRQIgaBX65nxJwarmYvAGa0iVzHVM9rpAdsPVUKYvwh_M9ZICIQDiY6X-TqAXodi5Z88kH9HadLV-z-QGddHmBwV0jwvFbg%3D%3D&alr=yes&cpn=oJWQu3wvcJ5dKph0&cver=2.20200508.00.01&range=0-999999999&rn=1&rbuf=0"))
//            val audioSource = ProgressiveMediaSource.Factory(dataSource)
//                    .createMediaSource(Uri.parse("https://r3---sn-nau-jhcs.googlevideo.com/videoplayback?expire=1589343255&ei=tx-7XpPkJL2JssUP6NKkoA4&ip=112.135.47.133&id=o-AKwdjrhz2mEGT4w-7pK79kUkKEnEwwmlziW0ySQQFpPt&itag=251&source=youtube&requiressl=yes&mh=No&mm=31%2C29&mn=sn-nau-jhcs%2Csn-npoe7ney&ms=au%2Crdu&mv=m&mvi=2&pl=22&initcwndbps=385000&vprv=1&mime=audio%2Fwebm&gir=yes&clen=2532470&dur=153.261&lmt=1540588549024211&mt=1589321616&fvip=5&keepalive=yes&c=WEB&txp=5511222&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cvprv%2Cmime%2Cgir%2Cclen%2Cdur%2Clmt&sig=AOq0QJ8wRgIhAKMiiv4MtNlRmFi_JeS-p_S-F3HfbSrAwUl47W5nokJDAiEAnRADJr3Jz3lt7OZJCmea-7ynmxWUp5H_lUwPb8uw5Rw%3D&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRQIgaBX65nxJwarmYvAGa0iVzHVM9rpAdsPVUKYvwh_M9ZICIQDiY6X-TqAXodi5Z88kH9HadLV-z-QGddHmBwV0jwvFbg%3D%3D&alr=yes&cpn=oJWQu3wvcJ5dKph0&cver=2.20200508.00.01&range=0-999999999&rn=2&rbuf=0"));
//            val mergedSource = MergingMediaSource(videoSource, audioSource)

//            player.prepare(mergedSource)
//
//
//            playerCloseButton.setOnClickListener {
//                finish()
//            }
//        }
    }


    private fun initPlayer(videos: List<Video>, currentIndex: Int) {
        if (!tinyDB.getBoolean("isPro")) {
            thread {
                Thread.sleep(5000)
                runOnUiThread {
                    Ads.showInterstitial()
                    Ads.loadInterstitial()
                }
            }
        }

        val dataSource = DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)))
        val cacheDataSourceFactory =
            CacheDataSourceFactory(DownloadUtil.getRecentCache(this), dataSource)
        val downloadedCacheDataSourceFactory =
            CacheDataSourceFactory(DownloadUtil.getDownloadedCache(this), dataSource)
        val concatenatedSource = ConcatenatingMediaSource()

        videos.forEachIndexed {i, v ->
            when (v.type) {
                "join" -> {
                    val mediaSource = if (v.status == DownloadStatus.STATE_COMPLETED) {
                        debug("it is downloaded")
                        ProgressiveMediaSource.Factory(downloadedCacheDataSourceFactory)
                            .createMediaSource(Uri.parse(v.src))
                    } else {
                        ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                            .createMediaSource(Uri.parse(v.src))
                    }
                    concatenatedSource.addMediaSource(mediaSource)
                }
                "separate" -> {
                    val videoSource = if (v.status == DownloadStatus.STATE_COMPLETED) {
                        ProgressiveMediaSource.Factory(downloadedCacheDataSourceFactory)
                            .createMediaSource(Uri.parse(v.vSrc))
                    } else {
                        ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                            .createMediaSource(Uri.parse(v.vSrc))
                    }

                    val audioSource = if (v.status == DownloadStatus.STATE_COMPLETED) {
                        ProgressiveMediaSource.Factory(downloadedCacheDataSourceFactory)
                            .createMediaSource(Uri.parse(v.aSrc))
                    } else {
                        ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                            .createMediaSource(Uri.parse(v.aSrc))
                    }

                    val mergedSource = MergingMediaSource(videoSource, audioSource)
                    concatenatedSource.addMediaSource(mergedSource)
                }
                "stream" -> {
                    val hlsSource = if (v.status == DownloadStatus.STATE_COMPLETED) {
                        debug("it is downloaded")
                        HlsMediaSource.Factory(downloadedCacheDataSourceFactory)
                            .createMediaSource(Uri.parse(v.src))
                    } else {
                        HlsMediaSource.Factory(cacheDataSourceFactory)
                            .createMediaSource(Uri.parse(v.src))
                    }
                    concatenatedSource.addMediaSource(hlsSource)
                }
            }
        }
        player.prepare(concatenatedSource)
    }


    override fun onDestroy() {
        playerView.player = null
        player.release()
        super.onDestroy()
    }


    private fun pausePlayer() {
        player.playWhenReady = false
        player.playbackState
    }




    override fun onPause() {
        pausePlayer()
        super.onPause()
    }
}
