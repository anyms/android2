package app.spidy.pirum.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.room.Room
import app.spidy.pirum.utils.C
import app.spidy.pirum.R
import app.spidy.pirum.activities.MainActivity
import app.spidy.pirum.data.Music
import app.spidy.pirum.data.Video
import app.spidy.pirum.databases.PyrumDatabase
import app.spidy.pirum.interfaces.DownloadListener
import app.spidy.pirum.utils.DownloadStatus
import app.spidy.pirum.utils.DownloadUtil
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import kotlin.concurrent.thread


class MediaDownloadService : DownloadService(
    C.DOWNLOAD_NOTIFICATION_ID, 1000,
    C.CHANNEL_ID, R.string.channel_name, R.string.channel_description
) {
    companion object {
        var downloadListener: DownloadListener? = null
        var isRunning = false
    }

    private val musics = ArrayList<Music>()
    private val videos = ArrayList<Video>()
    private lateinit var database: PyrumDatabase
    private lateinit var downloadManager: DownloadManager
    private val listener = object : DownloadListener {
        override fun onProgress(downloads: MutableList<Download>) {
            downloadListener?.onProgress(downloads)

            for (download in downloads) {
                val progress = (download.bytesDownloaded / download.contentLength.toFloat() * 100).toInt()
                if (download.state == DownloadStatus.STATE_QUEUED) {
                    thread {
                        val m = database.pyrumDao().getMusicById(download.request.id)
                        if (m.isNotEmpty()) musics.add(m[0])
                        val v = database.pyrumDao().getVideoById(download.request.id)
                        if (v.isNotEmpty()) videos.add(v[0])
                    }
                }

                if (download.state == DownloadStatus.STATE_COMPLETED) {
                    updateMedia(download.request.id, progress, DownloadStatus.STATE_COMPLETED)
                } else {
                    updateMedia(download.request.id, progress, download.state)
                }
            }
        }
    }

    private fun isMusic(uId: String): Boolean {
        for (m in musics) {
            if (m.uId == uId) {
                return true
            }
        }
        return false
    }

    private fun updateMedia(uId: String, progress: Int, status: Int) {
        var index = -1
        if (!isMusic(uId)) {
            for (i in videos.indices) {
                if (videos[i].uId == uId) {
                    index = i
                    break
                }
            }
            if (index != -1) {
                videos[index].progress = progress
                videos[index].status = status

                thread {
                    database.pyrumDao().updateVideo(videos[index])
                }
            }
        } else {
            for (i in musics.indices) {
                if (musics[i].uId == uId) {
                    index = i
                    break
                }
            }
            if (index != -1) {
                musics[index].progress = progress
                musics[index].status = status

                thread {
                    database.pyrumDao().updateMusic(musics[index])
                }
            }
        }
    }

    override fun onCreate() {
        database = Room.databaseBuilder(this, PyrumDatabase::class.java, "PyrumDatabase")
            .fallbackToDestructiveMigration().build()
        downloadManager = DownloadUtil.getDownloadManager(this)
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(downloadManager: DownloadManager, download: Download) {
                listener.onProgress(mutableListOf(download))
            }
        })
        isRunning = true
        super.onCreate()
    }

    override fun getDownloadManager(): DownloadManager {
        return downloadManager
    }

    override fun getForegroundNotification(downloads: MutableList<Download>): Notification {
        listener.onProgress(downloads)
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationHelper = DownloadNotificationHelper(this, C.CHANNEL_ID)

        return notificationHelper.buildProgressNotification(
            android.R.drawable.stat_sys_download,
            pendingIntent,
            "A media is downloading...",
            downloads
        )
    }

    override fun getScheduler(): Scheduler? {
        return null
    }

    override fun onDestroy() {
        isRunning = false
        downloadListener = null
        super.onDestroy()
    }
}