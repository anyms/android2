package app.spidy.wikireader.engine

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import app.spidy.kotlinutils.debug
import java.io.File
import java.util.*

class TTS(private val context: Context, private val listener: Listener? = null) {
    var languageCode = "en"
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(java.util.Locale(languageCode))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    listener?.onUnsupported()
                }

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onDone(utteranceId: String?) {
                        listener?.onFinishSpeaking(utteranceId)
                    }

                    override fun onError(utteranceId: String?) {
                        listener?.onSpeakingError(utteranceId)
                    }

                    override fun onStart(utteranceId: String?) {
                        listener?.onSpeakingStart()
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        listener?.onSpeakingStop()
                    }
                })
            } else {
                listener?.onUnavailable()
            }
        })
    }

    fun speak(s: String, uId: String): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(s, TextToSpeech.QUEUE_ADD,null, uId);
        } else {
            tts.speak(s, TextToSpeech.QUEUE_ADD, null);
        }
    }

    fun stop() = tts.stop()
    fun setSpeed(speed: Float) = tts.setSpeechRate(speed)
    fun setPitch(pitch: Float) = tts.setPitch(pitch)

    fun writeToFile(text: CharSequence, file: File, uId: String): Int {
        return tts.synthesizeToFile(text, null, file, uId)
    }


    interface Listener {
        fun onUnavailable()
        fun onUnsupported()
        fun onFinishSpeaking(uId: String?)
        fun onSpeakingError(uId: String?)
        fun onSpeakingStart()
        fun onSpeakingStop()
    }
}