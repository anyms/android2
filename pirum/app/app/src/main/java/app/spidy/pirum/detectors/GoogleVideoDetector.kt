package app.spidy.pirum.detectors

import android.net.Uri
import android.webkit.MimeTypeMap
import app.spidy.hiper.data.HiperResponse
import app.spidy.pirum.interfaces.DetectListener
import app.spidy.pirum.utils.StringUtil.randomUUID
import app.spidy.pirum.utils.StringUtil.slugify
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class GoogleVideoDetector(private val detectListener: DetectListener) {
    private val videoIds = Collections.synchronizedList(ArrayList<String>())
    private val audioIds = Collections.synchronizedList(ArrayList<String>())
    private val detects = Collections.synchronizedList(ArrayList<HashMap<String, String>>())

    fun run(url: String, title: String, response: HiperResponse, headers: HashMap<String, Any>, cookies: HashMap<String, String>, isAudio: Boolean = false) {
        val uri = Uri.parse(url)
        val id = uri.getQueryParameter("id")

        if (id != null && !videoIds.contains(id) && !isAudio) {
            videoIds.add(id)
            val videoUrl = getFinalUrl(uri)
            detects.add(hashMapOf("vSrc" to videoUrl, "id" to id, "title" to title, "type" to "separate"))
            // detectListener.onDetect(detects.last())
        } else if (id != null && isAudio) {
            if (videoIds.contains(id) && !audioIds.contains(id)) {
                audioIds.add(id)
                val audioUrl = getFinalUrl(uri)
                for (i in detects.indices) {
                    if (detects[i]["id"] == id) {
                        detects[i]["aSrc"] = audioUrl
                        detectListener.onDetect(detects[i])
                        break
                    }
                }
            }
        }
    }

    fun isIn(id: String?): Boolean {
        return videoIds.contains(id) && audioIds.contains(id)
    }

    private fun getFinalUrl(uri: Uri): String {
        val params = uri.queryParameterNames
        val newUri = uri.buildUpon().clearQuery()
        for (param in params) {
            newUri.appendQueryParameter(param, if(param.equals("range")) "0-900000000000" else uri.getQueryParameter(param))
        }
        return newUri.toString()
    }

    private fun getFileName(title: String, url: String): String {
        val name = url.split("?")[0].split("#")[0].split("/").last().split(".")[0]
        return "${slugify(title)}_$name.mpg"
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
        videoIds.clear()
        audioIds.clear()
        detects.clear()
    }
}