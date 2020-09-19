package app.spidy.proxyserver.services

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.room.Room
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.debug
import app.spidy.proxyserver.handlers.ProxyHandler
import app.spidy.proxyserver.R
import app.spidy.proxyserver.activities.MainActivity
import app.spidy.proxyserver.databases.ProxyDatabase
import app.spidy.proxyserver.utils.AdBlocker
import app.spidy.proxyserver.utils.C
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.SocketException
import kotlin.concurrent.thread

class ProxyServer: Service() {
    companion object {
        const val STOP_SERVICE = "app.spidy.pirum.services.STOP_SERVICE"
        var isRunning = false

        var serverKill: (() -> Unit)? = null
        var requestCallback: ((request: String, isBlocked: Boolean) -> Unit)? = null
    }

    private lateinit var tinyDB: TinyDB
    private var database: ProxyDatabase? = null
    private lateinit var serverSocket: ServerSocket
    private lateinit var notification: NotificationCompat.Builder
    private lateinit var stopBroadCastReceiver: StopBroadCastReceiver

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
        val port = tinyDB.getInt(C.TAG_PROXY_PORT)
        serverKill = {
            kill()
        }
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
            .setContentText("running on port $port")
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop), stopPendingIntent)
            .priority = NotificationCompat.PRIORITY_MIN

        startForeground(C.SERVER_NOTIFICATION_ID, notification.build())


        thread {
            serverSocket = ServerSocket(port, 50, null)
            serverSocket.reuseAddress = true
            while (true) {
                try {
                    val client = serverSocket.accept()
                    thread {
                        ProxyHandler(client) { request, d, isHttps ->
                            val domain = d.replace("www.", "")
                            if (tinyDB.getBoolean(C.SETTINGS_INSECURE_WEBSITES) && !isHttps) {
                                requestCallback?.invoke(request, true)
                                return@ProxyHandler true
                            }
                            if (tinyDB.getBoolean(C.SETTINGS_PRIVACY_PROTECTION)) {
                                val result =
                                    database?.proxyDao()?.getTrackDomainsByName(domain) ?: listOf()
                                if (result.isNotEmpty()) {
                                    requestCallback?.invoke(request, true)
                                    return@ProxyHandler true
                                }
                            }
                            if (tinyDB.getBoolean(C.SETTINGS_INAPPROP_WEBSITES)) {
                                val result = database?.proxyDao()?.getInappropDomainsByName(domain)
                                    ?: listOf()
                                if (result.isNotEmpty()) {
                                    requestCallback?.invoke(request, true)
                                    return@ProxyHandler true
                                }
                            }

                            if (tinyDB.getBoolean(C.SETTINGS_DATA_SERVER)) {
                                if (AdBlocker.isAd(domain, true)) {
                                    requestCallback?.invoke(request, true)
                                    return@ProxyHandler true
                                }
                            }

                            database?.proxyDao()?.getBlockedDomains()?.forEach { blockedDomain ->
                                if (blockedDomain.isPattern) {
                                    val pattern = blockedDomain.value.replace(".", "\\.").replace("*", "(.*?)")
                                    val regex = Regex(pattern)
                                    if (regex.matches(domain)) {
                                        requestCallback?.invoke(request, true)
                                        return@ProxyHandler true
                                    }
                                } else {
                                    if (blockedDomain.value == domain) {
                                        requestCallback?.invoke(request, true)
                                        return@ProxyHandler true
                                    }
                                }
                            }

                            requestCallback?.invoke(request, false)
                            return@ProxyHandler false
                        }.run()
                    }
                } catch (e: SocketException) {
                    break
                }
            }
        }

        return START_STICKY
    }

    private fun kill() {
        serverSocket.close()
        stopSelf()
    }


    override fun onDestroy() {
        unregisterReceiver(stopBroadCastReceiver)
        isRunning = false
        serverKill = null
        database = null
        requestCallback = null
        super.onDestroy()
    }


    inner class StopBroadCastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            kill()
        }
    }
}