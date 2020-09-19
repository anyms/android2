package app.spidy.memecreator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import app.spidy.memecreator.utils.Ads
import com.google.android.gms.ads.MobileAds


class App: Application() {
    companion object {
        const val CHANNEL_ID = "SERVICE_NOTIFICATION_CHANNEL_ID"
    }

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this, Ads.APPLICATION_ID)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "Service channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = "no sound"
            notificationChannel.setSound(null,null)
            notificationChannel.enableLights(false)
            notificationChannel.lightColor = Color.BLUE
            notificationChannel.enableVibration(false)

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(notificationChannel)
        }
    }
}