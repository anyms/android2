package app.spidy.tamillovevideostatus.ytdl

import android.net.UrlQuerySanitizer
import app.spidy.kotlinutils.debug
import app.spidy.tamillovevideostatus.utils.Http
import com.google.code.regexp.Pattern
import org.json.JSONArray
import org.json.JSONObject


class YtDL {
    var sanitizer = UrlQuerySanitizer()

    init {
        sanitizer.allowUnregisteredParamaters = true
    }

    fun getInfo(videoId: String): String {
        val ret = JSONObject()
        val response = Http.sync.get("https://www.youtube.com/watch?v=${videoId}")
        val ytconf = Pattern.compile("ytplayer.config[ =]*")
        val ytcondEnd = Pattern.compile(";[ ]*ytplayer\\.load")
        val json = ytcondEnd.split(ytconf.split(response.text!!)[1])[0]
        val ytconfig = JSONObject(json)
        val args = ytconfig.getJSONObject("args")

        val formats = JSONObject(args.getString("player_response")).getJSONObject("streamingData")
            .getJSONArray("formats")
        val adaptiveFormats = JSONObject(args.getString("player_response")).getJSONObject("streamingData")
            .getJSONArray("adaptiveFormats")
        val fallback = formats.getJSONObject(formats.length() - 1).getString("url")
        sanitizer.parseUrl(fallback)
        val expire = sanitizer.getValue("expire").toLong()
        var isAudioAdded = false
        val addedQuals = ArrayList<String>()
        val quals = JSONArray()

        ret.put("fallback", fallback)
        ret.put("expire", expire)

        for (i in 0 until adaptiveFormats.length()) {
            val format = adaptiveFormats.getJSONObject(i)
            if (format.has("qualityLabel")) {
                val qualityLabel = format.getString("qualityLabel")
                if (!addedQuals.contains(qualityLabel)) {
                    addedQuals.add(qualityLabel)
                    val qual = JSONObject()
                    qual.put("label", qualityLabel)
                    qual.put("url", format.getString("url"))
                    quals.put(qual)
                }
            } else if (!isAudioAdded) {
                debug("Audio")
                ret.put("audio", format.getString("url"))
                isAudioAdded = true
            }
            debug(format.getString("url"))
        }

        ret.put("qualities", quals)

        return ret.toString()
    }
}