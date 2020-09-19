package app.spidy.spidy.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Html
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import app.spidy.kotlinutils.onUiThread
import org.json.JSONArray


fun WebView.js(s: String, callback: ((String) -> Unit)? = null) {
    onUiThread {
        if (callback == null) {
            this.evaluateJavascript("(function() {$s})();", null)
        } else {
            this.evaluateJavascript("(function() {$s})();") {
                if (it.startsWith("\"") && it.endsWith("\"")) {
                    callback(it.replace("\\n", "\n").dropLast(1).drop(1))
                } else {
                    callback(it.replace("\\n", "\n"))
                }
            }
        }
    }
}

fun View.click(x: Float, y: Float) {
    dispatchTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN, x,y,0.5f,5f,0,1f,1f,1,0));
    Thread.sleep(100)
    dispatchTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_UP, x,y,0.5f,5f,0,1f,1f,1,0));
}


fun String.base64Encode(): String {
    return String(Base64.encode(this.toByteArray(), Base64.NO_WRAP))
}

fun String.base64Decode(): String {
    return String(Base64.decode(this.toByteArray(), Base64.NO_WRAP))
}

fun TextView.html(s: String) {
    text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(s, Html.FROM_HTML_MODE_COMPACT);
    } else {
        Html.fromHtml(s);
    }
}

fun Context.vibrate() {
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(200)
    }
}


fun JSONArray.loop(callback: (JSONArray, Int) -> Boolean) {
    for (i in 0 until this.length()) {
        val ret = callback(this, i)
        if (ret) break
    }
}


fun WebView.injectScript(
    script: String,
    callback: ((String) -> Unit)? = null
) {
    js("""
        ${Injector.HAMMER_JS}
        ${Injector.SPIDY_JS}
        $script
    """.trimIndent()) {
        callback?.invoke(it)
    }
}
