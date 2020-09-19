package app.spidy.lankanews.parsers

import app.spidy.kotlinutils.debug
import app.spidy.lankanews.data.Article
import org.jsoup.Jsoup
import java.lang.Exception

class NewsPageParser {
    fun parse(html: String): Article {
        val doc = Jsoup.parse(html)
        doc.select(".adsense-blk").remove()
        var videoId: String? = null
        val topVideo = doc.select(".article-innervideo-top")
        if (topVideo.size > 0) {
            videoId = topVideo.select("iframe").attr("src").split("?").first().split("/").last()
        }
        val paras = doc.select(".read-more-news-main p:not([class])").eachText()
        val image = try {
            doc.select(".main-news-block img")[0].attr("data-cfsrc")
        } catch (e: Exception) {
            null
        }

        return Article(videoId, paras, image)
    }
}