package app.spidy.cyberwire.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.room.Room
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.cyberwire.R
import app.spidy.cyberwire.data.Episode
import app.spidy.cyberwire.databases.PodcastDatabase
import app.spidy.cyberwire.utils.API
import app.spidy.cyberwire.utils.C
import app.spidy.cyberwire.utils.DownloadUtil
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class PlayerService : Service() {
    companion object {

        const val MEDIA_SESSION_TAG = "app.spidy.pirum.MEDIA_SESSION_TAG"

        var isRunning = false
    }

    private lateinit var database: PodcastDatabase
    lateinit var player: SimpleExoPlayer
    lateinit var concatenatedSource: ConcatenatingMediaSource
    lateinit var cacheDataSourceFactory: CacheDataSourceFactory
    val music = ArrayList<Episode>()
    var coverImageUrl = ""
    var currentIndex = 0
    var clientTerminator: (() -> Unit)? = null
    var listener: Listener? = null
    var pageNum = 0
    var isNextPageExists = false

    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(this, PodcastDatabase::class.java, "PodcastDatabase")
            .fallbackToDestructiveMigration().build()
        player = SimpleExoPlayer.Builder(this).build()
        player.playWhenReady = true

        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                listener?.onPlayerStateChanged(playWhenReady, playbackState)
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                listener?.onPlayerError(error)
            }

            override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
            ) {
                currentIndex = player.currentWindowIndex
                listener?.onTracksChanged(trackGroups, trackSelections, music[currentIndex], coverImageUrl)
                if (currentIndex >= music.size - 3 && isNextPageExists) {
                    loadEpisodes()
                }
            }

        })

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            this, C.CHANNEL_ID, R.string.app_name,
            R.string.channel_description,
            C.MUSIC_PLAYER_NOTIFICATION_ID,
            object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val playerIntent = Intent(applicationContext, PlayerService::class.java)
                    return PendingIntent.getActivity(applicationContext, 0, playerIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return music[player.currentWindowIndex].date // "Sample Description ${player.currentWindowIndex}"
                }

                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return music[player.currentWindowIndex].title //Sample Title ${player.currentWindowIndex}
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    if (coverImageUrl == "") {
                        return BitmapFactory.decodeResource(
                            applicationContext.resources,
                            R.drawable.default_poster
                        )
                    } else {
                        thread {
                            val bitmap = bitmapFromUrl(coverImageUrl)
                            onUiThread { callback.onBitmap(bitmap) }
                        }
                    }
                    return null
                }

            },
            object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    startForeground(notificationId, notification)
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopSelf()
                }
            }
        )
        playerNotificationManager.setPlayer(player)


        mediaSession = MediaSessionCompat(applicationContext, MEDIA_SESSION_TAG)
        mediaSession.isActive = true
        playerNotificationManager.setMediaSessionToken(mediaSession.sessionToken)
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
            override fun getMediaDescription(windowIndex: Int): MediaDescriptionCompat {
                return getMusicDescription(windowIndex)
            }
        })
        mediaSessionConnector.setPlayer(player, null)

        isRunning = true
    }


    private fun loadEpisodes() {
        API.async.get(API.url("/episodes/${music[0].channelId}/$pageNum")).then {
            val data = JSONObject(it.text!!)
            isNextPageExists = data.getBoolean("is_next_exists")
            val eps = data.getJSONArray("episodes")
            val startPos = music.size
            for (i in 0 until eps.length()) {
                val ep = eps.getJSONObject(i)
                val episode = Episode(
                    uId = ep.getInt("id"),
                    title = ep.getString("title"),
                    audio = ep.getString("audio"),
                    channelId = ep.getString("channel_id"),
                    date = ep.getString("date"),
                    timestamp = ep.getLong("raw_date"),
                    viewCount = ep.getInt("view_count"),
                    downloadedLocation = ""
                )
                music.add(episode)
                onUiThread { queueEpisode(episode) }
            }
            pageNum++
        }.catch {

        }
    }


    private fun getMusicDescription(index: Int): MediaDescriptionCompat {
        val extras = Bundle()
        // val bitmap = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.default_poster)
        return MediaDescriptionCompat.Builder()
            .setMediaId("audio_${index}")
            .setIconUri(Uri.parse(coverImageUrl))
            .setTitle("Sample title")
            .setDescription("Sample description")
            .setExtras(extras)
            .build()
    }

    fun initPlayback() {
        val dataSource = DefaultDataSourceFactory(this@PlayerService, Util.getUserAgent(this@PlayerService, getString(R.string.app_name)))
        cacheDataSourceFactory = CacheDataSourceFactory(DownloadUtil.getRecentCache(this@PlayerService), dataSource)
        concatenatedSource = ConcatenatingMediaSource()

        thread {
            val savedEpisodes = database.dao().getEpisodes()
            val channelIds = savedEpisodes.map { it.channelId }
            val titles = savedEpisodes.map { it.title }

            for (m  in music) {
                val mediaSource = if (channelIds.contains(m.channelId) && titles.contains(m.title)) {
                    debug("Downloaded")
                    ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                        .createMediaSource(Uri.parse(savedEpisodes.find { it.title == m.title && it.channelId == m.channelId }!!.downloadedLocation))
                } else {
                    debug("Not Downloaded")
                    ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                        .createMediaSource(Uri.parse(m.audio))
                }
                concatenatedSource.addMediaSource(mediaSource)
            }
            onUiThread {
                player.prepare(concatenatedSource)
            }
        }
    }

    fun queueEpisode(episode: Episode) {
        music.add(episode)
        listener?.onQueueEpisode(episode)
        val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
            .createMediaSource(Uri.parse(episode.audio))
        concatenatedSource.addMediaSource(mediaSource)
    }

    fun terminate() {
        clientTerminator?.invoke()
        stopSelf()
    }

    private fun bitmapFromUrl(url: String?): Bitmap {
        val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
        connection.connect()
        val input: InputStream = connection.inputStream
        return BitmapFactory.decodeStream(input)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        clientTerminator?.invoke()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        mediaSession.release()
        playerNotificationManager.setPlayer(null)
        mediaSessionConnector.setPlayer(null, null)
        player.release()
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return PlayerBinder()
    }



    inner class PlayerBinder: Binder() {
        val playerService: PlayerService
            get() = this@PlayerService
    }


    interface Listener {
        fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int)
        fun onPlayerError(error: ExoPlaybackException)
        fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray, episode: Episode, coverImageUrl: String)
        fun onQueueEpisode(episode: Episode)
    }
}