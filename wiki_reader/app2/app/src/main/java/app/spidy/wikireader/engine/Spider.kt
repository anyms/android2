package app.spidy.wikireader.engine

import android.content.Context
import android.webkit.WebView
import app.spidy.wikireader.data.Element
import app.spidy.hiper.Hiper
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.wikireader.data.Article
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList

class Spider(private val context: Context) {
    private val hiper = Hiper.getAsyncInstance()
    private val webView = WebView(context)
    private var spiderListener: Listener? = null
    private val headings = listOf("h1", "h2", "h3", "h4", "h5")


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
                            val article = parseArticle(href)
                            onUiThread { spiderListener?.onFound(article) }
                            isFound = true
                            break
                        }
                    }
                }

                if (!isFound) {
                    if (firstLink != null) {
                        val article = parseArticle(firstLink)
                        onUiThread { spiderListener?.onFound(article) }
                    } else {
                        onUiThread { spiderListener?.onFail() }
                    }
                }
            }
            .catch {
                debug(it)
                onUiThread { spiderListener?.onFail() }
            }
    }

    private fun parseArticle(url: String): Article {
        val elements = ArrayList<Element>()
        val doc = Jsoup.connect(url).get()
        val coverImage = try {
            changeImageSize(doc.select(".infobox img")[0].attr("src"))
        } catch (e: Exception) {
            null
        }
        val imgs = doc.select("#bodyContent img.thumbimage")
        val images = ArrayList<String>()
        imgs.forEach {
            images.add(it.attr("src"))
        }

        doc.select("sup, .haudio, style, .mw-editsection, table, .toc").remove()
        val tmpEls = doc.select(".mw-parser-output > *:not([class]), #bodyContent img.thumbimage, ul li, ol li")
        val tmpElements = ArrayList<Element>()
        val endings = listOf(
            "References",
            "See also"
        )

        var lastTagName = ""
        for (i in tmpEls.indices) {
            val el = tmpEls[i]
            val childs = el.select("b")
            if (childs.size == 1) continue
            if (endings.contains(el.text().trim())) {
                break
            }
            if (headings.contains(lastTagName) && el.tagName() == "ul") break
            when (el.tagName()) {
                "p", "dl", "h1", "h2", "h3", "h4", "h5", "li" -> {
                    if (coverImage != null) {
                        tmpElements.add(Element(el.tagName(), i+1, el.text(), UUID.randomUUID().toString()))
                    } else {
                        tmpElements.add(Element(el.tagName(), i, el.text(), UUID.randomUUID().toString()))
                    }
                }
                "img" -> {
                    val src = el.attr("src")
                    if (images.contains(src)) {
                        if (coverImage != null) {
                            tmpElements.add(Element(el.tagName(), i+1, changeImageSize(src), UUID.randomUUID().toString()))
                        } else {
                            tmpElements.add(Element(el.tagName(), i, changeImageSize(src), UUID.randomUUID().toString()))
                        }
                    }
                }
            }
            debug(lastTagName)
            lastTagName = el.tagName()
        }

        if (coverImage != null) {
            elements.add(0, Element(tagName = "img", index = 0, text = coverImage, uId = UUID.randomUUID().toString()))
        }
        cleanArticle(tmpElements).forEach { elements.add(it) }

        val titleNodes = doc.title().split(" - ").toMutableList()
        if (titleNodes.size > 1) {
            titleNodes.removeAt(titleNodes.lastIndex)
        }

        return Article(
            title = titleNodes.joinToString(" - "),
            elements = elements,
            url = url
        )
    }

    private fun changeImageSize(u: String): String {
        var url = u
        if (url.startsWith("//")) {
            url = "https:${url}"
        }
        val nodes = url.split("/").toMutableList()
        val imagePathNodes = nodes.last().split("-").toMutableList()
        imagePathNodes[0] = "500px"
        nodes.removeAt(nodes.lastIndex)

        val s = nodes.joinToString("/")
        val imagePath = imagePathNodes.joinToString("-")
        return "$s/$imagePath"
    }

    private fun cleanArticle(elements: ArrayList<Element>): List<Element> {
        if (headings.contains(elements.last().tagName)) {
            elements.removeAt(elements.lastIndex)
            cleanArticle(elements)
        }

        return elements
    }

    fun addListener(spiderListener: Listener) {
        this.spiderListener = spiderListener
    }


    interface Listener {
        fun onFound(article: Article)
        fun onFail()
    }
}