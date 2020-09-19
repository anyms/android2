package app.spidy.browser.data

import android.graphics.Bitmap
import app.spidy.browser.fragments.WebViewFragment

data class Tab(
    var title: String,
    var url: String,
    var favIcon: Bitmap?,
    val fragment: WebViewFragment
) {
    val tabId: Long = kotlin.random.Random.nextLong()
}