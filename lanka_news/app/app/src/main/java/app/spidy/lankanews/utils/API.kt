package app.spidy.lankanews.utils

import app.spidy.hiper.GetRequest
import app.spidy.hiper.Hiper
import app.spidy.lankanews.data.Category
import app.spidy.lankanews.data.News
import java.lang.Exception

object API {
    private val hiper = Hiper.getAsyncInstance()
    var userAgent: String = ""

    fun get(url: String): GetRequest.Queue {
        return hiper.get(url, headers = hashMapOf("User-Agent" to userAgent))
    }

    interface Listener {
        fun onCategory(cats: List<Category>) {}
        fun onNewses(newses: List<News>) {}
        fun onFail(e: Exception)
    }
}