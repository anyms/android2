package app.spidy.spidy.communicators

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import app.spidy.kotlinutils.TinyDB
import app.spidy.spidy.activities.GettingStartedActivity
import app.spidy.spidy.activities.MainActivity

class GettingStartedCommunicator(private val context: Context) {
    @JavascriptInterface
    fun start() {
        val tinyDB = TinyDB(context)
        tinyDB.putBoolean("isGettingStartedShown", true)
        val intent = Intent(context, MainActivity::class.java)
        context.startActivity(intent)
        (context as GettingStartedActivity).finish()
    }
}