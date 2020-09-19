package app.spidy.pirum.services

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
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import app.spidy.kotlinutils.debug
import app.spidy.pirum.utils.C
import app.spidy.pirum.R
import app.spidy.pirum.activities.MusicPlayerActivity
import app.spidy.pirum.data.Music
import app.spidy.pirum.utils.DownloadStatus
import app.spidy.pirum.utils.DownloadUtil
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlin.collections.ArrayList


class MusicPlayerService : Service() {
    companion object {

        const val MEDIA_SESSION_TAG = "app.spidy.pirum.MEDIA_SESSION_TAG"

        var isRunning = false
    }

    lateinit var player: SimpleExoPlayer
    val music = ArrayList<Music>()
    var clientTerminator: (() -> Unit)? = null
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    override fun onCreate() {
        debug("onCreate")
        super.onCreate()

        player = SimpleExoPlayer.Builder(this).build()
        player.playWhenReady = true

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            this, C.CHANNEL_ID, R.string.app_name,
            R.string.channel_description,
            C.MUSIC_PLAYER_NOTIFICATION_ID,
            object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val playerIntent = Intent(applicationContext, MusicPlayerActivity::class.java)
                    return PendingIntent.getActivity(applicationContext, 0, playerIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT)
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return music[player.currentWindowIndex].playlistName // "Sample Description ${player.currentWindowIndex}"
                }

                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return music[player.currentWindowIndex].title //Sample Title ${player.currentWindowIndex}
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    return  BitmapFactory.decodeResource(applicationContext.resources, R.drawable.default_poster)
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


    private fun getMusicDescription(index: Int): MediaDescriptionCompat {
        val extras = Bundle()
        val bitmap = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.default_poster)
        extras.putParcelable(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        extras.putParcelable(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
        return MediaDescriptionCompat.Builder()
            .setMediaId("audio_${index}")
            .setIconBitmap(bitmap)
            .setTitle("Sample title")
            .setDescription("Sample description")
            .setExtras(extras)
            .build()
    }

    fun initPlayback() {
        val dataSource = DefaultDataSourceFactory(this@MusicPlayerService, Util.getUserAgent(this@MusicPlayerService, getString(R.string.app_name)))

        val cacheDataSourceFactory =
            CacheDataSourceFactory(DownloadUtil.getRecentCache(this@MusicPlayerService), dataSource)
        val downloadedCacheDataSourceFactory =
            CacheDataSourceFactory(DownloadUtil.getDownloadedCache(this), dataSource)

        val concatenatedSource = ConcatenatingMediaSource()

        for (m  in music) {
            when (m.type) {
                "join" -> {
                    val mediaSource = if (m.status == DownloadStatus.STATE_COMPLETED) {
                        ProgressiveMediaSource.Factory(downloadedCacheDataSourceFactory)
                            .createMediaSource(Uri.parse(m.src))
                    } else {
                        ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                            .createMediaSource(Uri.parse(m.src))
                    }
                    concatenatedSource.addMediaSource(mediaSource)
                }
                "separate" -> {
                    val mediaSource = if (m.status == DownloadStatus.STATE_COMPLETED) {
                        ProgressiveMediaSource.Factory(downloadedCacheDataSourceFactory)
                            .createMediaSource(Uri.parse(m.src))
                    } else {
                        ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                            .createMediaSource(Uri.parse(m.src))
                    }
                    concatenatedSource.addMediaSource(mediaSource)
                }
                "stream" -> {
                    val hlsSource = if (m.status == DownloadStatus.STATE_COMPLETED) {
                        HlsMediaSource.Factory(downloadedCacheDataSourceFactory)
                            .createMediaSource(Uri.parse(m.src))
                    } else {
                        HlsMediaSource.Factory(cacheDataSourceFactory)
                            .createMediaSource(Uri.parse(m.src))
                    }
                    concatenatedSource.addMediaSource(hlsSource)
                }
            }
        }
        player.prepare(concatenatedSource)
    }

    fun terminate() {
        clientTerminator?.invoke()
        stopSelf()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        debug("onStartCommand")
        clientTerminator?.invoke()
        return START_STICKY
    }

    override fun onDestroy() {
        debug("onDestroy")
        mediaSession.release()
        playerNotificationManager.setPlayer(null)
        mediaSessionConnector.setPlayer(null, null)
        player.release()
        isRunning = false
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? {
        debug("onBind")
        return MusicPlayerBinder()
    }



    inner class MusicPlayerBinder: Binder() {
        val playerService: MusicPlayerService
            get() = this@MusicPlayerService
    }
}