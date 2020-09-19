package app.spidy.pirum.utils

import android.os.Build
import android.text.Html
import android.widget.TextView

fun TextView.setHtml(s: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        text = Html.fromHtml(s, Html.FROM_HTML_MODE_COMPACT);
    } else {
        text = Html.fromHtml(s)
    }
}