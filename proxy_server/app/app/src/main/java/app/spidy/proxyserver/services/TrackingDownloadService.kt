package app.spidy.proxyserver.services

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.room.Room
import app.spidy.hiper.Hiper
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import app.spidy.proxyserver.R
import app.spidy.proxyserver.data.TrackDomain
import app.spidy.proxyserver.databases.ProxyDatabase
import app.spidy.proxyserver.utils.C
import app.spidy.proxyserver.utils.formatBytes
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.util.*
import kotlin.collections.ArrayList

class TrackingDownloadService: Service() {
    companion object {
        const val TAG_IS_DOWNLOADED = "app.spidy.pirum.services.TAG_IS_TRACKING_DOWNLOADED"
        const val STOP_DOWNLOAD_SERVICE = "app.spidy.pirum.services.STOP_TRACKING_DOWNLOAD_SERVICE"
        var isRunning = false
    }

    private lateinit var tinyDB: TinyDB
    private lateinit var database: ProxyDatabase
    private lateinit var notification: NotificationCompat.Builder
    private lateinit var stopBroadCastReceiver: StopBroadCastReceiver

    private var isInterrupted = false


    override fun onCreate() {
        tinyDB = TinyDB(this)
        database = Room.databaseBuilder(this, ProxyDatabase::class.java, "ProxyDatabase")
            .fallbackToDestructiveMigration().build()
        super.onCreate()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true

        notification = NotificationCompat.Builder(this, C.CHANNEL_ID)
        stopBroadCastReceiver = StopBroadCastReceiver()

        registerReceiver(stopBroadCastReceiver, IntentFilter(STOP_DOWNLOAD_SERVICE))

        val stopIntent = Intent()
        stopIntent.action = STOP_DOWNLOAD_SERVICE

        val stopPendingIntent =
            PendingIntent.getBroadcast(applicationContext, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)


        notification
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop), stopPendingIntent)
            .priority = NotificationCompat.PRIORITY_MIN


        val hiper = Hiper.getAsyncInstance()
        notification.setContentTitle("Downloading tracking list")
            .setProgress(100, 0, true)
        val url = "https://blocklist.site/app/dl/tracking"
        hiper.get(url, isStream = true).then {
            val stream = it.stream!!
            val contentLength = it.headers.get("content-length")!!.toLong()
            var downloadedLength = 0L
            var currentTime = System.currentTimeMillis()
            val fileDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (!fileDir!!.exists()) {
                fileDir.mkdirs()
            }
            val fileName = "tracking.txt"
            val file = File(fileDir, fileName)
            val streamWriter = OutputStreamWriter(file.outputStream())
            while (true) {
                val buffer = ByteArray(4096)
                val len = stream.read(buffer)
                downloadedLength += len

                if (len <= 0 || isInterrupted) break
                streamWriter.write(String(buffer), 0, len)

                if (System.currentTimeMillis() >= currentTime + 1000) {
                    currentTime = System.currentTimeMillis()
                    notification
                        .setSubText("${downloadedLength.formatBytes()}/${contentLength.formatBytes()}")
                        .setProgress(100, (downloadedLength / contentLength.toFloat() * 100).toInt(), false)
                    startForeground(C.DOWNLOAD_NOTIFICATION_ID, notification.build())
                }
            }
            stream.close()
            streamWriter.close()

            notification.setContentTitle("Updating tracking list").setProgress(100, 0, true)
            startForeground(C.DOWNLOAD_NOTIFICATION_ID, notification.build())

            if (!isInterrupted) {
                updateTrackingList()
            }

            stopSelf()
        }.catch {
            if (it.toString().contains("UnknownHostException")) {
                onUiThread {
                    toast("UnknownHostException: please check your connection or proxy", true)
                }
            }

            debug(it)
            stopSelf()
        }

        startForeground(C.DOWNLOAD_NOTIFICATION_ID, notification.build())
        return START_STICKY
    }

    private fun updateTrackingList() {
        val fileDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "tracking.txt"
        val file = File(fileDir, fileName)

        val inpStream = FileInputStream(file)
        val scanner = Scanner(inpStream, "UTF-8")
        val arr = ArrayList<TrackDomain>()
        while (scanner.hasNextLine()) {
            if (isInterrupted) break
            val line = scanner.nextLine()
            if (line.trim() != "") {
                if (arr.size == 100_000) {
                    database.proxyDao().putTrackDomains(arr)
                    arr.clear()
                }

                arr.add(TrackDomain(line))
            }
        }
        if (arr.isNotEmpty()) database.proxyDao().putTrackDomains(arr)
        inpStream.close()
        scanner.close()

        tinyDB.putBoolean(TAG_IS_DOWNLOADED, true)
    }


    override fun onDestroy() {
        unregisterReceiver(stopBroadCastReceiver)
        isRunning = false
        super.onDestroy()
    }


    inner class StopBroadCastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            debug("Interrupted")
            isInterrupted = true
            stopSelf()
        }
    }
}