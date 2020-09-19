package app.spidy.pirum.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.room.Room
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.toast
import app.spidy.pirum.R
import app.spidy.pirum.data.Music
import app.spidy.pirum.databases.PyrumDatabase
import app.spidy.pirum.services.MusicPlayerService
import app.spidy.pirum.utils.Ads
import app.spidy.pirum.utils.DownloadStatus
import app.spidy.pirum.utils.IO
import app.spidy.pirum.utils.IO.getFileNameFromURL
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_downloads.*
import kotlinx.android.synthetic.main.activity_video_player.*
import kotlinx.android.synthetic.main.music_exo_player_control_view.*
import kotlinx.android.synthetic.main.music_exo_player_control_view.adView
import kotlinx.android.synthetic.main.video_exo_player_control_view.nameView
import kotlinx.android.synthetic.main.video_exo_player_control_view.playerCloseButton
import org.json.JSONArray
import java.net.URLDecoder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class MusicPlayerActivity : AppCompatActivity() {
    private lateinit var database: PyrumDatabase
    private lateinit var tinyDB: TinyDB

    private var playerService: MusicPlayerService? = null
    private var currentTime: String? = null
    private var isBound = false
    private val music = ArrayList<Music>()
    private var isServiceRunning = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playerService = (service as? MusicPlayerService.MusicPlayerBinder)?.playerService
            isBound = true
            playerService!!.clientTerminator = {
                terminate()
            }
            if (music.isNotEmpty()) {
                playerService!!.music.clear()
                playerService!!.music.addAll(music)
                playerService!!.initPlayback()
            }
            initPlayer(playerService!!)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            playerService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        tinyDB = TinyDB(this)

        if (!tinyDB.getBoolean("isPro")) {
            adView.loadAd(AdRequest.Builder().build())
        } else {
            adView.visibility = View.GONE
        }

        val data = intent?.getStringExtra("data")
        val currentIndex = intent?.getIntExtra("currentIndex", 0)
        currentTime = intent?.getStringExtra("currentTime")
        val isFromLocal = intent?.getBooleanExtra("isFromLocal", false)
        database = Room.databaseBuilder(this, PyrumDatabase::class.java, "PyrumDatabase")
            .fallbackToDestructiveMigration().build()
        val playlistName = intent?.getStringExtra("playlistName") ?: IO.getPlaylistName()

        isServiceRunning = MusicPlayerService.isRunning

        if (isServiceRunning && data == null) {
            bindService(Intent(this, MusicPlayerService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        }

        if (data != null) {
            val playlist: JSONArray
            try {
                playlist = JSONArray(data)
            } catch (e: Exception) {
                debug(e.message)
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
                        music.add(Music(
                            uId = UUID.randomUUID().toString(),
                            status = DownloadStatus.STATE_NONE,
                            playlistName = playlistName,
                            title = title,
                            type = "join",
                            src = url
                        ))
                    }
                    info.getString("type") == "separate" -> {
                        val aUrl = URLDecoder.decode(info.getString("aSrc"), "UTF-8")
                        var title = URLDecoder.decode(info.getString("title"), "UTF-8")
                        if (title == "") {
                            title = getFileNameFromURL(aUrl) ?: "-"
                        }

                        music.add(Music(
                            uId = UUID.randomUUID().toString(),
                            status = DownloadStatus.STATE_NONE,
                            playlistName = playlistName,
                            title = title,
                            type = "separate",
                            src = aUrl
                        ))
                    }
                    info.getString("type") == "stream" -> {
                        val url = URLDecoder.decode(info.getString("src"), "UTF-8")
                        var title = URLDecoder.decode(info.getString("title"), "UTF-8")
                        if (title == "") {
                            title = getFileNameFromURL(url) ?: "-"
                        }

                        music.add(Music(
                            uId = UUID.randomUUID().toString(),
                            status = DownloadStatus.STATE_NONE,
                            playlistName = playlistName,
                            title = title,
                            type = "stream",
                            src = url
                        ))
                    }
                }
            }

            if (isFromLocal == null || !isFromLocal) {
                thread {
                    for (m in music) {
                        database.pyrumDao().putMusic(m)
                    }
                }
            }

            if (!tinyDB.getBoolean("isPro")) {
                thread {
                    Thread.sleep(5000)
                    runOnUiThread {
                        Ads.showInterstitial()
                        Ads.loadInterstitial()
                    }
                }
            }

            val playerServiceIntent = Intent(this, MusicPlayerService::class.java)
            Util.startForegroundService(this, playerServiceIntent)
            bindService(playerServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else if (isFromLocal == true) {
            thread {
                database.pyrumDao().getMusicByPlaylist(playlistName).forEachIndexed {i, m ->
                    if (i == currentIndex) music.add(0, m) else music.add(m)
                }

                runOnUiThread {
                    if (!tinyDB.getBoolean("isPro")) {
                        thread {
                            Thread.sleep(5000)
                            runOnUiThread {
                                Ads.showInterstitial()
                                Ads.loadInterstitial()
                            }
                        }
                    }
                    val playerServiceIntent = Intent(this, MusicPlayerService::class.java)
                    Util.startForegroundService(this, playerServiceIntent)
                    bindService(playerServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                }
            }
        }
    }

    override fun onDestroy() {
        playerView.player = null
        ignore {
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }

    private fun initPlayer(service: MusicPlayerService) {
        debug("init player")
        playerView.player = service.player
        playerView.defaultArtwork = getDrawable(R.drawable.default_poster)
        playerView.showController()
        nameView.text = service.music[service.player.currentWindowIndex].title

        playerCloseButton.setOnClickListener {
            finish()
        }

        service.player.addListener(object : Player.EventListener {
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
                val currentIndex = service.player.currentWindowIndex
                nameView.text = playerService!!.music[currentIndex].title
                if (currentTime != null) {
                    service.player.seekTo(currentTime!!.toLong() * 1000)
                }
            }

        })


        playerTerminateBtn.setOnClickListener {
            if (playerService != null) {
                playerService!!.terminate()
            }
            finish()
        }
    }

    private fun terminate() {
        ignore {
            unbindService(serviceConnection)
        }
    }
}
