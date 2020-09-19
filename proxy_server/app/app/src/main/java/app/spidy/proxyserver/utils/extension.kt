package app.spidy.proxyserver.utils

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.text.Html
import android.widget.TextView
import app.spidy.kotlinutils.ignore
import app.spidy.proxyserver.handlers.DialogHandler
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.pow
import kotlin.math.roundToInt


fun InputStream.readLines(): List<String> {
    val lines = ArrayList<String>()
    var reader: BufferedReader? = null
    try {
        reader = BufferedReader(InputStreamReader(this))

        var line: String
        while (reader.readLine().also { line = it } != null) {
            if (line.trim() != "") lines.add(line)
        }
    } catch (e: Exception) {

    } finally {
        if (reader != null) {
            ignore {
                reader.close()
            }
        }
    }
    return lines
}

fun Context.newDialog(): DialogHandler {
    val builder = AlertDialog.Builder(this)
    return DialogHandler(builder)
}


fun Long.formatBytes(isSpeed: Boolean = false): String {
    val unit = if (isSpeed) 1000.0 else 1024.0
    return when {
        this < unit * unit -> "${((this / unit).toFloat() * 100.0).roundToInt() / 100.0}KB"
        this < (unit.pow(2.0) * 1000) -> "${((this / unit.pow(2.0)).toFloat() * 100.0).roundToInt() / 100.0}MB"
        else -> "${((this / unit.pow(3.0)).toFloat() * 100.0).roundToInt() / 100.0}GB"
    }
}


fun TextView.html(s: String) {
    text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(s, Html.FROM_HTML_MODE_COMPACT);
    } else {
        Html.fromHtml(s);
    }
}

