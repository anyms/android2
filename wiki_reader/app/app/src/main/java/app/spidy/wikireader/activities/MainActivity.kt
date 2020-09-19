package app.spidy.wikireader.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import app.spidy.kotlinutils.*
import app.spidy.wikireader.R
import app.spidy.wikireader.data.Element
import app.spidy.wikireader.data.Language
import app.spidy.wikireader.engine.IBMWatson
import app.spidy.wikireader.engine.Spider
import app.spidy.wikireader.engine.TTS
import app.spidy.wikireader.fragments.LanguageDialogFragment
import app.spidy.wikireader.fragments.SettingsDialogFragment
import app.spidy.wikireader.utils.Ads
import app.spidy.wikireader.utils.LanguageUtil
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.github.zagum.speechrecognitionview.adapters.RecognitionListenerAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_settings_screen.*
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), SettingsDialogFragment.Listener,
    LanguageDialogFragment.Listener, BillingProcessor.IBillingHandler {

    companion object {
        const val MIC_STATUS_TEXT = "app.spidy.wikireader.MIC_STATUS_TEXT"
        const val MIC_STATUS_LISTEN = "app.spidy.wikireader.MIC_STATUS_LISTEN"
        const val MIC_STATUS_STOP = "app.spidy.wikireader.MIC_STATUS_STOP"
    }

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var ibmWatson: IBMWatson
    private lateinit var permission: Permission
    private var micStatus = MIC_STATUS_LISTEN
    private lateinit var currentLangCode: String
    private lateinit var currentLanguage: String
    private lateinit var tinyDB: TinyDB
    private lateinit var billingProcessor: BillingProcessor

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permission = Permission(this)
        tinyDB = TinyDB(this)
        permission.request(Manifest.permission.RECORD_AUDIO, null, object : Permission.Listener {
            override fun onGranted() {}
            override fun onRejected() {}
        })

        permission.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, null, object : Permission.Listener {
            override fun onGranted() {}
            override fun onRejected() {}
        })

        setSupportActionBar(toolbar)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (title != null) this@MainActivity.title = title
            }
        }
        currentLanguage = tinyDB.getString(LanguageUtil.TAG_CURRENT_LANGUAGE_NAME) ?: "English"
        currentLangCode = tinyDB.getString(LanguageUtil.TAG_CURRENT_LANGUAGE_CODE) ?: "en"

        billingProcessor = BillingProcessor(this,
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkqz95QM+5Yd4Vwd/kjQGJJjp4Molpa3DJ9eLd4heQIORZBsFTC5yCdDYbhgD3dQzzxUuWguEWTrUbHRdSSVIqEXMQkez51WcCLOONHcK1PLjF5euELIJOTe4hWHCg9cFpeb17Nu5L0Bub2T+5lAaZh8zvTUVdMT31YyDFtWdKoW90ab1uJcc7eBXiZpsHxksU4MQHpDnpCoSG5it1kVrKdNd7W6QjIDO4LnLxsDctI2d8ebmgR1UgpwqWaWLUoE5YWmcVgLhYh67yW3wFAdloQSjlHhmc79JaBCbJrxgZhps5iPVulAPpx7lpZhK22eAodib4mFrHTeHECaP0gUTbwIDAQAB",
            this)
        billingProcessor.initialize()

        if (tinyDB.getBoolean("isPro")) {
            upgradeMsgView.visibility = View.GONE
            upgradeBtn.visibility = View.GONE
        }

        val adView: AdView = findViewById(R.id.adView)
        if (tinyDB.getBoolean("isPro")) {
            adView.visibility = View.GONE
        } else {
            Ads.initInterstitial(this)
            Ads.initReward(this)
            Ads.loadInterstitial()
            Ads.loadReward()
            adView.loadAd(AdRequest.Builder().build())
        }

        val ibmListener = object : IBMWatson.IBMListener {
            override fun onError() {
                debug("IBMWatson: Error!")
            }

            override fun onFinish() {
                debug("IBMWatson: Finish!")
            }

            override fun onProgress(progress: Int) {
                debug("IBMWatson: Progress! $progress%")
            }
        }

        val tts = TTS(this, object : TTS.Listener {
            override fun onUnavailable() {
                hideRecognizerView()
                toast("Text to speech is unavailable on your device", true)
            }
            override fun onUnsupported() {
                hideRecognizerView()
                toast("Text to speech is unsupported on your device", true)
            }
            override fun onFinishSpeaking(uId: String?) {
                micStatus = if (userInputView.text.toString().trim() == "") {
                    listenView.setImageResource(R.drawable.ic_microphone)
                    MIC_STATUS_LISTEN
                } else {
                    listenView.setImageResource(R.drawable.ic_send)
                    MIC_STATUS_TEXT
                }
            }
            override fun onSpeakingError(uId: String?) {
                onUiThread { hideRecognizerView() }
                micStatus = if (userInputView.text.toString().trim() == "") {
                    listenView.setImageResource(R.drawable.ic_microphone)
                    MIC_STATUS_LISTEN
                } else {
                    listenView.setImageResource(R.drawable.ic_send)
                    MIC_STATUS_TEXT
                }
            }
            override fun onSpeakingStart() {
                onUiThread { hideRecognizerView() }
                micStatus =
                    MIC_STATUS_STOP
                listenView.setImageResource(R.drawable.ic_stop)
            }
            override fun onSpeakingStop() {
                micStatus = if (userInputView.text.toString().trim() == "") {
                    listenView.setImageResource(R.drawable.ic_microphone)
                    MIC_STATUS_LISTEN
                } else {
                    listenView.setImageResource(R.drawable.ic_send)
                    MIC_STATUS_TEXT
                }
            }
        })

        val spider = Spider(this)
        spider.addListener(object : Spider.Listener {
            override fun onFail() {
                runOnUiThread {
                    hideRecognizerView()
                    toast("Unable to find a wikipedia article")
                }
            }

            override fun onFound(url: String) {
                runOnUiThread {
                    settingScreenView.visibility = View.GONE
                    webView.loadUrl(url)
                }

                tts.languageCode = url.split("://")[1].split(".")[0]
                val doc = Jsoup.connect(url).get()
                doc.select("img, sup, .haudio, style, .mw-editsection").remove()
                val tmpEls = doc.select(".mw-parser-output > *:not([class])")
                val headings = listOf("h1", "h2", "h3", "h4", "h5")
                val els = Elements()

                for (el in tmpEls) {
                    when (el.tagName()) {
                        "p", "dl", "h1", "h2", "h3", "h4", "h5" -> els.add(el)
                    }
                }
                val elements = ArrayList<Element>()
                els.forEachIndexed { i, el ->
                    when (val tagName = el.tagName()) {
                        "p", "dl", "h1", "h2", "h3", "h4", "h5" -> {
                            if (i == els.lastIndex) {
                                if (!headings.contains(tagName)) {
                                    elements.add(Element(el.tagName(), i, el.text()))
                                }
                            } else {
                                elements.add(Element(el.tagName(), i, el.text()))
                            }
                        }
                    }
                }

                runOnUiThread {
                    if (tinyDB.getBoolean("isPro")) {
                        elements.forEach {
                            val uId = UUID.randomUUID().toString()
                            tts.speak(it.text, uId)
                        }
                    } else {
                        Ads.showInterstitial {

                            elements.forEach {
                                val uId = UUID.randomUUID().toString()
                                tts.speak(it.text, uId)
                            }
                            thread {
                                ibmWatson.saveToFile(doc.title(), elements)
                            }
                        }
                        Ads.loadInterstitial()
                    }
                }
            }
        })


        ibmWatson = IBMWatson(this)
        ibmWatson.ibmListener = ibmListener

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionView.setSpeechRecognizer(speechRecognizer)
        recognitionView.setColors(intArrayOf(
            ContextCompat.getColor(this, R.color.colorRed),
            ContextCompat.getColor(this,
                R.color.colorAccent
            ),
            ContextCompat.getColor(this, R.color.colorBlue),
            ContextCompat.getColor(this,
                R.color.colorOrange
            ),
            ContextCompat.getColor(this,
                R.color.colorPurple
            )
        ))

        recognitionView.setRecognitionListener(object : RecognitionListenerAdapter() {
            var singleResult = true

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onBeginningOfSpeech() {
                singleResult = true
            }
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                hideRecognizerView()
                Log.d("hello", "Err => $error")
            }

            override fun onResults(results: Bundle?) {
                if (singleResult) {
                    debug(results)
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        debug(matches[0])
                        spider.search(matches[0], currentLangCode)
                        singleResult = false
                    }

                }
            }
        })

        listenView.setOnClickListener {
            when (micStatus) {
                MIC_STATUS_LISTEN -> {
                    tts.stop()
                    permission.request(Manifest.permission.RECORD_AUDIO, null, object : Permission.Listener {
                        override fun onGranted() {
                            recognize(currentLangCode)
                        }
                        override fun onRejected() {}
                    })
                }
                MIC_STATUS_TEXT -> {
                    spider.search(userInputView.text.toString(), currentLangCode)
                    userInputView.setText("")
                    micStatus =
                        MIC_STATUS_LISTEN
                    listenView.setImageResource(R.drawable.ic_microphone)
                }
                MIC_STATUS_STOP -> tts.stop()
            }
        }

        userInputView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                micStatus = if (micStatus == MIC_STATUS_STOP) {
                    listenView.setImageResource(R.drawable.ic_stop)
                    MIC_STATUS_STOP
                } else if (s == null || s.trim() == "") {
                    listenView.setImageResource(R.drawable.ic_microphone)
                    MIC_STATUS_LISTEN
                } else {
                    listenView.setImageResource(R.drawable.ic_send)
                    MIC_STATUS_TEXT
                }
            }

        })

        selectLanguageView.text = "Language: $currentLanguage"
        selectLanguageView.setOnClickListener {
            LanguageDialogFragment().show(supportFragmentManager, "SELECT_LANGUAGE_DIALOG")
        }

        upgradeBtn.setOnClickListener {
            purchase()
        }
    }

    fun purchase() {
        billingProcessor.purchase(this, "app.spidy.wikireader")
    }

    private fun recognize(langCode: String) {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
        speechRecognizer.startListening(speechRecognizerIntent)
        recognitionView.play()
        showRecognizerView()
    }

    private fun hideRecognizerView() {
        recognitionView.visibility = View.INVISIBLE
        overlayView.visibility = View.INVISIBLE
        comBar.visibility = View.VISIBLE
        borderView.visibility = View.VISIBLE
    }

    private fun showRecognizerView() {
        recognitionView.visibility = View.VISIBLE
        overlayView.visibility = View.VISIBLE
        comBar.visibility = View.INVISIBLE
        borderView.visibility = View.INVISIBLE
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permission.execute(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        billingProcessor.release()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSettings -> {
                SettingsDialogFragment().show(supportFragmentManager, "SETTINGS_DIALOG")
            }
            R.id.menuShare -> {
                ignore {
                    val shareIntent = Intent(Intent.ACTION_SEND);
                    shareIntent.type = "text/plain";
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Know the unknown");
                    var shareMessage = "\nMulti language wikipedia reader, that can read the wikipedia article for you\n\n";
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + packageName +"\n\n";
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "Share with"));
                }
            }
            R.id.menuFeedback -> {
                val uri = Uri.parse("market://details?id=$packageName");
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName"))
                    )
                }
            }
        }

        return true
    }

    override fun onSettingsApply(language: Language, pitch: Int, speed: Int) {

    }

    @SuppressLint("SetTextI18n")
    override fun onLanguageChange(language: Language) {
        selectLanguageView.text = "Language: ${language.name}"
        currentLangCode = language.code
    }

    override fun onBillingInitialized() {

    }

    override fun onPurchaseHistoryRestored() {

    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        tinyDB.putBoolean("isPro", true)
        toast("Great! you've purchased pro version, restart the app to enable the changes.")
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        toast("billing failed")
    }
}