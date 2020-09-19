package app.spidy.lankanews.services

import android.R.attr.bitmap
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.spidy.lankanews.R
import app.spidy.lankanews.activities.MainActivity
import app.spidy.lankanews.utils.C
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FCM : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body
        val data = remoteMessage.data
        val image = remoteMessage.notification?.imageUrl

        if (title != null && body != null && image != null) {
            sendNotification(title, body, data, image)
        }
    }

    private fun sendNotification(
        title: String,
        message: String,
        data: Map<String, String>,
        image: Uri
    ) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        for ((k, v) in data) {
            intent.putExtra(k, v)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            301,
            intent,
            PendingIntent.FLAG_ONE_SHOT
        )
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        Glide.with(applicationContext)
            .asBitmap()
            .load(image)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
                    val notification = NotificationCompat.Builder(this@FCM, C.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setLargeIcon(resource)
                        .setColorized(true)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                    notification.color = ContextCompat.getColor(this@FCM, R.color.colorAccent)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    notificationManager?.notify(302, notification.build())
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })
    }
}