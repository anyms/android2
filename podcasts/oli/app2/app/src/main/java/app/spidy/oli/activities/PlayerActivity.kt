package app.spidy.oli.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import app.spidy.kotlinutils.*
import app.spidy.oli.R
import app.spidy.oli.data.Episode
import app.spidy.oli.services.DownloadService
import app.spidy.oli.services.PlayerService
import app.spidy.oli.utils.API
import app.spidy.oli.utils.Ads
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.music_exo_player_control_view.*
import org.json.JSONArray
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread


class PlayerActivity : AppCompatActivity() {
    private lateinit var tinyDB: TinyDB

    private var playerService: PlayerService? = null
    private var currentTime: String? = null
    private var isBound = false
    private val episodes = ArrayList<Episode>()
    private var isServiceRunning = false
    private var coverImageUrl: String = ""
    private var pageNum = 0
    private var isNextPageExists = false
    private var currentIndex = 0

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playerService = (service as? PlayerService.PlayerBinder)?.playerService
            isBound = true
            playerService?.apply {
                clientTerminator = {
                    this@PlayerActivity.unbind()
                }
                if (episodes.isNotEmpty()) {
                    coverImageUrl = this@PlayerActivity.coverImageUrl
                    isNextPageExists = this@PlayerActivity.isNextPageExists
                    pageNum = this@PlayerActivity.pageNum
                    music.clear()
                    music.addAll(episodes)
                    initPlayback()
                } else {
                    episodes.addAll(music)
                    this@PlayerActivity.coverImageUrl = coverImageUrl
                    debug("URL: $coverImageUrl")
                    thread {
                        val poster = drawableFromUrl(this@PlayerActivity.coverImageUrl)

                        onUiThread {
                            if (poster == null) {
                                playerView.defaultArtwork = getDrawable(R.drawable.default_poster)
                            } else {
                                playerView.defaultArtwork = poster
                            }
                        }
                    }
                }
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
        setContentView(R.layout.activity_player)

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())
        Ads.showInterstitial()
        Ads.loadInterstitial()

        tinyDB = TinyDB(this)

        if (!PlayerService.isRunning || intent?.getStringExtra("coverImageUrl") != null) {
            val isChannel = intent!!.getBooleanExtra("is_channel", false)
            coverImageUrl = intent!!.getStringExtra("coverImageUrl")!!

            if (isChannel) {
                val eps = JSONArray(intent!!.getStringExtra("data"))
                debug(eps.length())
                pageNum = intent!!.getIntExtra("current_page", 0)
                isNextPageExists = intent!!.getBooleanExtra("is_next_exists", false)
                currentIndex = intent!!.getIntExtra("index", 0)
                for (i in 0 until eps.length()) {
                    val ep = eps.getJSONObject(i)
                    val episode = Episode(
                        uId = ep.getInt("uId"),
                        title = ep.getString("title"),
                        audio = ep.getString("audio"),
                        channelId = ep.getString("channelId"),
                        date = ep.getString("date"),
                        timestamp = ep.getLong("timestamp"),
                        viewCount = ep.getInt("viewCount"),
                        downloadedLocation = ""
                    )
                    if (i == currentIndex) episodes.add(0, episode) else episodes.add(episode)
                }
            } else {
                val uId = intent!!.getIntExtra("uId", 0)
                val title = intent!!.getStringExtra("title")!!
                val audio = intent!!.getStringExtra("audio")!!
                val channelId = intent!!.getStringExtra("channelId")!!
                val date = intent!!.getStringExtra("date")!!
                val timestamp = intent!!.getLongExtra("timestamp", 0)
                val viewCount = intent!!.getIntExtra("viewCount", 0)
                val downloadedLocation = intent!!.getStringExtra("downloadedLocation")!!

                episodes.add(
                    Episode(
                        uId = uId,
                        title = title,
                        audio = audio,
                        channelId = channelId,
                        date = date,
                        timestamp = timestamp,
                        viewCount = viewCount,
                        downloadedLocation = downloadedLocation
                    )
                )
            }
        }


        isServiceRunning = PlayerService.isRunning
        debug(episodes)
        if (isServiceRunning) {
            bindService(Intent(this, PlayerService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            val playerServiceIntent = Intent(this, PlayerService::class.java)
            Util.startForegroundService(this, playerServiceIntent)
            bindService(playerServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        optionMenu.setOnClickListener {
            val popupMenu =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    PopupMenu(
                        this,
                        optionMenu,
                        Gravity.NO_GRAVITY,
                        android.R.attr.actionOverflowMenuStyle,
                        0
                    )
                } else {
                    PopupMenu(this, optionMenu)
                }
            popupMenu.inflate(R.menu.menu_player)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menuDownload -> {
                        if (!DownloadService.isRunning) {
                            val intent = Intent(this, DownloadService::class.java)
                            intent.putExtra("uId", episodes[currentIndex].uId)
                            intent.putExtra("title", episodes[currentIndex].title)
                            intent.putExtra("audio", episodes[currentIndex].audio)
                            intent.putExtra("channelId", episodes[currentIndex].channelId)
                            intent.putExtra("date", episodes[currentIndex].date)
                            intent.putExtra("timestamp", episodes[currentIndex].timestamp)
                            intent.putExtra("viewCount", episodes[currentIndex].viewCount)
                            intent.putExtra("downloadedLocation", episodes[currentIndex].downloadedLocation)
                            intent.putExtra("coverImage", coverImageUrl)
                            startService(intent)
                            Ads.showInterstitial()
                            Ads.loadInterstitial()
                        } else {
                            newDialog().withTitle("Info")
                                .withMessage("Already an episode is downloading. Please wait until it's completed")
                                .withPositiveButton(getString(R.string.got_it)) { dialog ->
                                    dialog.dismiss()
                                }
                                .create()
                                .show()
                        }
                    }
                }
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }
    }

    override fun onDestroy() {
        playerService?.apply {
            if (!player.playWhenReady) {
                terminate()
            }
        }

        playerView.player = null
        unbind()
        super.onDestroy()
    }

    private fun drawableFromUrl(url: String?): Drawable? {
        val x: Bitmap
        val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
        connection.connect()
        val input: InputStream = connection.inputStream
        x = BitmapFactory.decodeStream(input)
        return BitmapDrawable(Resources.getSystem(), x)
    }

    private fun initPlayer(service: PlayerService) {
        playerView.player = service.player
        if (coverImageUrl == "") {
            playerView.defaultArtwork = getDrawable(R.drawable.default_poster)
        } else {
            thread {
                val poster = drawableFromUrl(coverImageUrl)

                onUiThread {
                    if (poster == null) {
                        playerView.defaultArtwork = getDrawable(R.drawable.default_poster)
                    } else {
                        playerView.defaultArtwork = poster
                    }
                }
            }
        }
        playerView.showController()
        nameView.text = service.music[service.player.currentWindowIndex].title

        playerCloseButton.setOnClickListener {
            finish()
        }

        service.listener = object : PlayerService.Listener {
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
                trackSelections: TrackSelectionArray,
                episode: Episode,
                coverImageUrl: String
            ) {
                currentIndex = service.player.currentWindowIndex
                nameView.text = playerService!!.music[currentIndex].title
                if (currentTime != null) {
                    service.player.seekTo(currentTime!!.toLong() * 1000)
                }
                API.async.get(API.url("/episode/update_view/${episodes[currentIndex].uId}")).catch()
            }

            override fun onQueueEpisode(episode: Episode) {
                episodes.add(episode)
            }
        }
    }

    private fun unbind() {
        if (!isBound) {
            isBound = false
            unbindService(serviceConnection)
        }
    }
}