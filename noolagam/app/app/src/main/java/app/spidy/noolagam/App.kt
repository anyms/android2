package app.spidy.noolagam

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import app.spidy.noolagam.utils.C
import com.google.android.gms.ads.MobileAds

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                C.CHANNEL_ID, getString(R.string.channel_description),
                NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}