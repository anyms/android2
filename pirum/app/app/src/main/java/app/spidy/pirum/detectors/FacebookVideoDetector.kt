package app.spidy.pirum.detectors

import android.webkit.MimeTypeMap
import app.spidy.hiper.Hiper
import app.spidy.pirum.interfaces.DetectListener
import app.spidy.pirum.utils.StringUtil.randomUUID
import app.spidy.pirum.utils.StringUtil.slugify
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FacebookVideoDetector(private val detectListener: DetectListener) {
    private val videoIds = Collections.synchronizedList(ArrayList<String>())
    private val detectedUrls = Collections.synchronizedList(ArrayList<String>())
    private val hiper = Hiper.getAsyncInstance()

    fun run(url: String, title: String, videoId: String, headers: HashMap<String, Any>, cookies: HashMap<String, String>) {
        if (!videoIds.contains(videoId)) {
            videoIds.add(videoId)
            headers["user-agent"] =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.92 Safari/537.36"
            hiper.get("https://facebook.com/watch/?v=$videoId", headers = headers, cookies = cookies).then { response ->
                val hdRegex = "hd_src:\"(.+?)\"".toRegex()
                val sdRegex = "sd_src:\"(.+?)\"".toRegex()
                val hdSrc = hdRegex.find(response.text.toString())?.groups?.get(1)?.value
                val sdSrc = sdRegex.find(response.text.toString())?.groups?.get(1)?.value
                val src = hdSrc ?: sdSrc

                detectListener.onDetect(hashMapOf("src" to src.toString(), "title" to title, "type" to "join"))
            }.catch()
        }
    }

    fun isIn(url: String): Boolean {
        return detectedUrls.contains(url)
    }

    private fun getFileName(title: String, url: String): String {
        val name = url.split("?")[0].split("#")[0].split("/").last().split(".")[0]
        val ext = MimeTypeMap.getFileExtensionFromUrl(url)
        return "${slugify(title)}_${name}_${randomUUID()}.$ext"
    }

    fun clear() {
        videoIds.clear()
        detectedUrls.clear()
    }
}