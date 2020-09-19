package app.spidy.lankanews.parsers

import app.spidy.kotlinutils.debug
import app.spidy.lankanews.data.SearchNews
import org.jsoup.Jsoup

class SearchParser {
    fun parse(html: String): List<SearchNews> {
        val doc = Jsoup.parse(html)
        val tmpPosts = doc.select("div[id^='post']")
        val posts = ArrayList<SearchNews>()

        for (tmp in tmpPosts) {
            val desc = tmp.select(".text-left").text().trim()

            if (desc != "") {
                posts.add(SearchNews(
                    title = tmp.select("h2").text().trim(),
                    desc = desc,
                    date = tmp.select(".news-set-date").text().trim(),
                    url = tmp.select("a").attr("href")
                ))
            }
        }
        debug(posts.size)
        debug(posts)
        return posts
    }
}