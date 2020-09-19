package app.spidy.wikireader.engine

import android.content.Context
import android.webkit.WebView
import app.spidy.hiper.Hiper
import app.spidy.kotlinutils.debug
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder

class Spider(private val context: Context) {
    private val hiper = Hiper.getAsyncInstance()
    private val webView = WebView(context)
    private var spiderListener: Listener? = null

    fun search(q: String, currentLangCode: String) {
        val query = URLEncoder.encode("$q wikipedia", "UTF-8")
        debug(query)
        hiper.get("https://www.google.com/search?q=$query&num=20",
            headers = hashMapOf("User-Agent" to webView.settings.userAgentString))
            .then {
                val doc = Jsoup.parse(it.text)
                val links = doc.select("a")
                debug(links.size)
                var isFound = false
                var firstLink: String? = null
                for (link in links) {
                    var href = URLDecoder.decode(link.attr("href").replace("/url?q=", ""), "UTF-8")
                    if (href.contains("wikipedia.org")) {
                        href = href.replace("m.wikipedia.org", "wikipedia.org")
                        if (firstLink == null) firstLink = href
                        if (href.contains("${currentLangCode}.wikipedia.org")) {
                            spiderListener?.onFound(href)
                            isFound = true
                            break
                        }
                    }
                }

                if (!isFound) {
                    if (firstLink != null) {
                        spiderListener?.onFound(firstLink)
                    } else {
                        spiderListener?.onFail()
                    }
                }
            }
            .catch {
                debug(it)
                spiderListener?.onFail()
            }
    }

    fun addListener(spiderListener: Listener) {
        this.spiderListener = spiderListener
    }


    interface Listener {
        fun onFound(url: String)
        fun onFail()
    }
}