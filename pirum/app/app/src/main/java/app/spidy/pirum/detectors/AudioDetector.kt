package app.spidy.pirum.detectors

import android.webkit.MimeTypeMap
import app.spidy.hiper.data.HiperResponse
import app.spidy.pirum.interfaces.DetectListener
import app.spidy.pirum.utils.StringUtil.randomUUID
import app.spidy.pirum.utils.StringUtil.slugify
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AudioDetector(private val detectListener: DetectListener) {
    private val detectedUrls = Collections.synchronizedList(ArrayList<String>())

    fun run(url: String, title: String, response: HiperResponse, headers: HashMap<String, Any>, cookies: HashMap<String, String>) {
        if (!detectedUrls.contains(url)) {
            detectedUrls.add(url)
            detectListener.onDetect(hashMapOf("src" to url, "title" to title, "type" to "join"))
        }
    }

    fun isIn(url: String): Boolean {
        return detectedUrls.contains(url)
    }

    private fun getFileName(title: String, url: String, mimetype: String?): String {
        val name = url.split("?")[0].split("#")[0].split("/").last().split(".")[0]
        val singleton = MimeTypeMap.getSingleton()
        val ext = singleton.getExtensionFromMimeType(mimetype)
        if (ext != null) {
            return "${slugify(title)}_${name}_${randomUUID()}.$ext"
        }
        return "${slugify(title)}_${name}_${randomUUID()}"
    }

    fun clear() {
        detectedUrls.clear()
    }
}