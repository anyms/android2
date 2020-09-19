package app.spidy.pirum.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import app.spidy.express.Express
import app.spidy.pirum.utils.C
import app.spidy.pirum.R
import app.spidy.pirum.activities.ImageSliderActivity
import app.spidy.pirum.activities.MainActivity
import app.spidy.pirum.activities.MusicPlayerActivity
import app.spidy.pirum.activities.VideoPlayerActivity
import app.spidy.pirum.data.Other
import app.spidy.pirum.databases.PyrumDatabase
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLDecoder
import java.util.*
import kotlin.concurrent.thread


class Server : Service() {
    companion object {
        const val STOP_SERVICE = "app.spidy.pirum.services.STOP_SERVICE"
        const val RESTART_SERVICE = "app.spidy.pirum.services.RESTART_SERVICE"

        var isRunning = false

        var kill: (() -> Unit)? = null
    }

    private lateinit var notification: NotificationCompat.Builder
    private lateinit var stopBroadCastReceiver: StopBroadCastReceiver
    private lateinit var notificationManager: NotificationManager
    private lateinit var database: PyrumDatabase

    private var express: Express? = null
    private val port: Int = 49670
    private val host: String
        get() = getIpv4HostAddress()
    private var requestCount = 0
    private var keepRunning = true

    private var lastHost = ""

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        database = Room.databaseBuilder(this, PyrumDatabase::class.java, "PyrumDatabase")
            .fallbackToDestructiveMigration().build()
        thread {
            while (keepRunning) {
                Thread.sleep(500)
                if (lastHost != host) {
                    runServer()
                }
            }
        }

        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        kill = {
            keepRunning = false
            express?.terminate()
            stopSelf()
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notification = NotificationCompat.Builder(this, C.CHANNEL_ID)
        val resultIntent = Intent(this, MainActivity::class.java)
        resultIntent.action = Intent.ACTION_MAIN
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            resultIntent, 0
        )
        stopBroadCastReceiver = StopBroadCastReceiver()

        registerReceiver(stopBroadCastReceiver, IntentFilter(STOP_SERVICE))

        val stopIntent = Intent()
        stopIntent.action = STOP_SERVICE

        val stopPendingIntent =
            PendingIntent.getBroadcast(applicationContext, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)


        notification
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("127.0.0.1:${port}")
            .setSubText("$requestCount")
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop), stopPendingIntent)
            .priority = NotificationCompat.PRIORITY_MIN

        startForeground(C.SERVER_NOTIFICATION_ID, notification.build())
        runServer()

        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(stopBroadCastReceiver)
        express?.terminate()
        express = null
        kill = null
        isRunning = false
        super.onDestroy()
    }


    inner class StopBroadCastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            express?.terminate()
            stopSelf()
        }
    }


    private fun getIpv4HostAddress(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress }
        }
        return "127.0.0.1"
    }


    private fun runServer() {
        vibrate()
        lastHost = host
        express?.terminate()
        express = Express(port = port, host = lastHost)

        express?.post("/handler") { req, res ->
            requestCount++
            val payload = req.form

            when (payload.get("cmd")) {
                "open" -> {
                    val url = URLDecoder.decode(req.form.get("url"), "UTF-8")
                    thread {
                        database.pyrumDao().putOther(Other(
                            uId = UUID.randomUUID().toString(),
                            playlistName = "",
                            src = url,
                            title = req.form.get("title") ?: url,
                            type = Other.TYPE_PAGE
                        ))
                    }

                    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(appIntent)
                }
                "read" -> {
                    val url = URLDecoder.decode(req.form.get("url"), "UTF-8")
                    thread {
                        database.pyrumDao().putOther(Other(
                            uId = UUID.randomUUID().toString(),
                            playlistName = "",
                            src = url,
                            title = req.form.get("title") ?: url,
                            type = Other.TYPE_PAGE,
                            isToRead = true
                        ))
                    }
                    openToRead(url)
                }
                "inapp" -> {
                    when {
                        payload.get("app") == "videoPlayer" -> {
                            val appIntent = Intent(this, VideoPlayerActivity::class.java)
                            appIntent.putExtra("data", payload.get("data"))
                            appIntent.putExtra("currentTime", payload.get("currentTime"))
                            appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(appIntent)
                        }
                        payload.get("app") == "musicPlayer" -> {
                            val appIntent = Intent(this, MusicPlayerActivity::class.java)
                            appIntent.putExtra("data", payload.get("data"))
                            appIntent.putExtra("currentTime", payload.get("currentTime"))
                            appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(appIntent)
                        }
                        payload.get("app") == "imageSlider" -> {
                            val appIntent = Intent(this, ImageSliderActivity::class.java)
                            appIntent.putExtra("data", payload.get("data"))
                            appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(appIntent)
                        }
                    }
                }
            }

            notification.setSubText("$requestCount")
            notificationManager.notify(C.SERVER_NOTIFICATION_ID, notification.build())
            res.send("ok")
        }

        express?.get("/check") {req, res ->
            res.html("success")
        }

        express?.runAsync()
        if (::notification.isInitialized) {
            notification
                .setContentText("${lastHost}:${port}")
            notificationManager.notify(C.SERVER_NOTIFICATION_ID, notification.build())
        }
    }

    private fun openToRead(url: String) {
        val builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(ContextCompat.getColor(this, android.R.color.white))
        builder.setExitAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out)
        builder.setShowTitle(true)
        val tabIntent = builder.build()
        tabIntent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        tabIntent.launchUrl(this, Uri.parse(url))
    }

    private fun vibrate() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(500)
        }
    }
}