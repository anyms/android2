package app.spidy.pirum.detectors

import app.spidy.hiper.Hiper
import app.spidy.pirum.interfaces.DetectListener
import app.spidy.pirum.utils.StringUtil.randomUUID
import app.spidy.pirum.utils.StringUtil.slugify
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class M3u8Detector(private val detectListener: DetectListener) {
    private val detectedUrls = Collections.synchronizedList(ArrayList<String>())
    private val hiper = Hiper.getAsyncInstance()

    fun run(url: String, title: String, headers: HashMap<String, Any>, cookies: HashMap<String, String>) {
        if (!detectedUrls.contains(url)) {
            detectedUrls.add(url)
            hiper.get(url, headers = headers, cookies = cookies).then { response ->
                if (response.text != null && validateResponse(response.text!!)) {
                    detectedUrls.add(url)
                    detectListener.onDetect(hashMapOf("src" to url, "title" to title, "type" to "stream"))
                }
            }.catch()
        }
    }

    fun isIn(url: String): Boolean {
        return detectedUrls.contains(url)
    }

    private fun getFileName(title: String, url: String): String {
        val name = url.split("?")[0].split("#")[0].split("/").last().split(".")[0]
        return "${slugify(title)}_${name}_${randomUUID()}.mpg"
    }

    private fun validateResponse(text: String): Boolean {
        var isValid = true
        val lines = text.split("\n")
        for (line in lines) {
            if (!line.startsWith("#")) {
                val plainUrl = line.split("?")[0]

                if (plainUrl.endsWith(".m3u8")) {
                    isValid = false
                    break
                } else if (plainUrl.endsWith(".ts")) {
                    isValid = true
                    break
                }
            }
        }

        return isValid
    }

    fun clear() {
        detectedUrls.clear()
    }
}