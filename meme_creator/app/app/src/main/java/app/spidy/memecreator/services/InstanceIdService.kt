package app.spidy.memecreator.services

import com.google.firebase.messaging.FirebaseMessagingService

class InstanceIdService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
    }
}