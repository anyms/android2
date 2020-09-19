package app.spidy.wikireader.engine

import android.content.Context
import android.os.Build
import android.os.Environment
import android.webkit.WebView
import app.spidy.hiper.Hiper
import app.spidy.hiper.data.HiperResponse
import app.spidy.kotlinutils.debug
import app.spidy.wikireader.data.Element
import app.spidy.wikireader.utils.IO
import org.apache.commons.io.FileUtils
import zeroonezero.android.audio_mixer.AudioMixer
import zeroonezero.android.audio_mixer.input.BlankAudioInput
import zeroonezero.android.audio_mixer.input.GeneralAudioInput
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class IBMWatson(private val context: Context) {
    private val hiper = Hiper.getSyncInstance()
    private val webView = WebView(context)
    private val userAgent = webView.settings.userAgentString

    var ibmListener: IBMListener? = null

    private fun mergeAudio(title: String, lastIndex: Int) {
        val outPath = "${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath}${File.separator}${IO.slugify(title)}.mp3"
        val audioMixer  = AudioMixer(outPath)
        for (i in 0 until lastIndex) {
            val path = "${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath}${File.separator}book_${i}.mp3"
            audioMixer.addDataSource(GeneralAudioInput(path))
            audioMixer.addDataSource(BlankAudioInput(3000000))
        }
        audioMixer.mixingType = AudioMixer.MixingType.SEQUENTIAL
        audioMixer.setProcessingListener(object : AudioMixer.ProcessingListener {
            override fun onProgress(progress: Double) {
                debug("Mixer Progress: $progress")
            }

            override fun onEnd() {
                IO.copyToSdCard(context, File(outPath), Environment.DIRECTORY_DOWNLOADS, mimeType = "audio/*") {
                    FileUtils.deleteDirectory(File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath))
                }
                debug("Mixer Done")
            }
        })
        audioMixer.start()
        audioMixer.processAsync()
    }

    fun saveToFile(title: String, paras: List<Element>) {
        var isError = false
        for (i in paras.indices) {
            val file = File("${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath}${File.separator}book_${i}.mp3")
            val resp: HiperResponse
            try {
                resp = hiper.get("https://text-to-speech-demo.ng.bluemix.net/api/v3/synthesize?text=${paras[i].text}&voice=en-US_EmilyV3Voice&download=true&accept=audio%2Fmp3", headers = hashMapOf(
                    "User-Agent" to userAgent
                ), isStream = true)
            } catch (e: Exception) {
                ibmListener?.onError()
                break
            }
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(1024 * 4)
            var read = 0
            try {
                while (resp.stream?.read(buffer)?.also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                ibmListener?.onProgress(((i+1) / paras.size.toFloat() * 100).toInt())
            } catch (e: Exception) {
                isError = true
                ibmListener?.onError()
                break
            } finally {
                outputStream.flush()
                outputStream.close()
                resp.stream?.close()
            }
        }
        if (!isError) {
            mergeAudio(title, paras.lastIndex)
            ibmListener?.onFinish()
        }
    }


    interface IBMListener {
        fun onProgress(progress: Int)
        fun onError()
        fun onFinish()
    }
}