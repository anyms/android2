package app.spidy.oli.services

import android.app.Service
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import androidx.room.Room
import app.spidy.hiper.Hiper
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import app.spidy.oli.R
import app.spidy.oli.data.Episode
import app.spidy.oli.databases.PodcastDatabase
import app.spidy.oli.utils.API
import app.spidy.oli.utils.C
import app.spidy.oli.utils.IO
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

class DownloadService: Service() {
    companion object {
        var isRunning = false
    }

    private lateinit var notification: NotificationCompat.Builder
    private lateinit var userAgent: String
    private lateinit var database: PodcastDatabase

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        val episode = Episode(
            uId = intent!!.getIntExtra("uId", 0),
            title = intent.getStringExtra("title")!!,
            audio = intent.getStringExtra("audio")!!,
            channelId = intent.getStringExtra("channelId")!!,
            date = intent.getStringExtra("date")!!,
            timestamp = intent.getLongExtra("timestamp", 0),
            viewCount = intent.getIntExtra("viewCount", 0),
            downloadedLocation = intent.getStringExtra("downloadedLocation")!!,
            coverImage = intent.getStringExtra("coverImage")!!
        )

        database = Room.databaseBuilder(this, PodcastDatabase::class.java, "PodcastDatabase")
            .fallbackToDestructiveMigration().build()
        userAgent = WebView(this).settings.userAgentString

        notification = NotificationCompat.Builder(this, C.CHANNEL_ID)
        notification.setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(episode.title)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        startForeground(99, notification.build())
        val fileName = "${IO.slugify(episode.title)}_${episode.channelId}.mp3"
        val file = File("${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath}${File.separator}$fileName")

        API.async.get(episode.audio, headers = hashMapOf("User-Agent" to userAgent), isStream = true)
            .then { resp ->
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(1024 * 4)
                var read = 0
                debug(episode.audio)
                debug(resp.headers)
                val contentLength = resp.headers.get("content-length")!!.toLong().toFloat()
                var downloaded = 0L
                var startTime = System.currentTimeMillis()
                while (resp.stream?.read(buffer)?.also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    downloaded += buffer.size

                    onUiThread {
                        if (System.currentTimeMillis() >= (startTime + 1000)) {
                            notification.setProgress(100, (downloaded / contentLength * 100).toInt(), false)
                            startForeground(99, notification.build())
                            startTime = System.currentTimeMillis()
                        }
                    }
                }
                outputStream.flush()
                outputStream.close()
                resp.stream?.close()

                thread {
                    episode.downloadedLocation = file.absolutePath
                    database.dao().putEpisode(episode)

                    onUiThread {
                        stopSelf()
                    }
                }
            }
            .catch {
                debug(it)
                onUiThread {
                    toast("An error occurred while downloading")
                    stopSelf()
                }
            }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }
}