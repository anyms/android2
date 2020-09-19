package app.spidy.wikireader.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.*
import android.widget.NumberPicker
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.toast
import app.spidy.wikireader.R
import app.spidy.wikireader.adapters.ArticleAdapter
import app.spidy.wikireader.data.Article
import app.spidy.wikireader.data.Element
import app.spidy.wikireader.engine.Spider
import app.spidy.wikireader.engine.TTS
import app.spidy.wikireader.utils.Ads
import app.spidy.wikireader.utils.C
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_reader.*


class ReaderActivity : AppCompatActivity() {
    private lateinit var query: String
    private lateinit var langCode: String
    private lateinit var spider: Spider
    private lateinit var tts: TTS
    private lateinit var tinyDB: TinyDB
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var smoothScroller: RecyclerView.SmoothScroller

    private var isSpeaking = true
    private val elements = ArrayList<Element>()
    private var currentPosition = -1
    private val textSizes = arrayOf("0.5x", "1x", "1.5x", "2x", "2.5x", "3x")
    private val textSizeValues = arrayOf(.5f, 1f, 1.5f, 2f, 2.5f, 3f)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tinyDB = TinyDB(applicationContext)
        spider = Spider(this)

        if (!tinyDB.getBoolean("is_info_shown")) {
            newDialog().withTitle("Did you know?")
                .withMessage("Tap on any text to start listening from there.")
                .withCancelable(false)
                .withPositiveButton(getString(R.string.got_it)) { dialog ->
                    tinyDB.putBoolean("is_info_shown", true)
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        val adView: AdView = findViewById(R.id.adView)
        if (tinyDB.getBoolean("isPro")) {
            adView.visibility = View.GONE
        } else {
            adView.loadAd(AdRequest.Builder().build())
        }

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            tts.stop()
            finish()
        }

        query = intent!!.getStringExtra("query")!!
        langCode = intent.getStringExtra("langCode")!!
        toolbar.title = query

        articleAdapter = ArticleAdapter(this, elements, switchSpeak = {
            tts.stop()
            for (i in it until elements.size) {
                if (elements[i].tagName != "img") {
                    tts.speak(elements[i].text, elements[i].uId)
                }
            }
        })
        smoothScroller = object : LinearSmoothScroller(this) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }

        loadingView.visibility = View.VISIBLE
        recyclerView.adapter = articleAdapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.isSmoothScrollbarEnabled = true
        recyclerView.layoutManager = layoutManager


        tts = TTS(this, object : TTS.Listener {
            override fun onUnavailable() {
                showError("Error!", "Text to speech is unavailable on your device")
            }

            override fun onUnsupported() {
                showError("Error!", "Text to speech is unsupported on your device")
            }

            override fun onFinishSpeaking(uId: String?) {
                stopBtn.setImageResource(R.drawable.ic_speaker)
                stopBtn.setBackgroundResource(R.drawable.rounded_corners_green)
            }

            override fun onSpeakingError(uId: String?) {
                toast("Unable to synthesis audio")
                stopBtn.setImageResource(R.drawable.ic_speaker)
                stopBtn.setBackgroundResource(R.drawable.rounded_corners_green)
            }

            override fun onSpeakingStart(uId: String?) {
                loadingView.visibility = View.GONE
                stopBtn.setImageResource(R.drawable.ic_stop)
                stopBtn.setBackgroundResource(R.drawable.rounded_corners_red)

                findIndex(uId)?.also {
                    articleAdapter.highlight(it)
                    currentPosition = it

                    for (i in 0 until it+1) {
                        recyclerView.findViewHolderForAdapterPosition(i)?.also { viewHolder ->
                            val v = when (viewHolder::class.simpleName) {
                                "ParaHolder" -> {
                                    (viewHolder as ArticleAdapter.ParaHolder).paraView
                                }
                                "HeadingHolder" -> {
                                    (viewHolder as ArticleAdapter.HeadingHolder).headingView
                                }
                                else -> {
                                    null
                                }
                            }
                            if (v != null) {
                                val s = v.text.toString()
                                val spannableString = SpannableString(s)
                                spannableString.setSpan(BackgroundColorSpan(Color.WHITE), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                v.text = spannableString
                            }
                        }
                    }

                    setLineBackgroundColor(currentPosition, Color.YELLOW)

//                    smoothScroller.targetPosition = it
//                    (recyclerView.layoutManager as? LinearLayoutManager)?.startSmoothScroll(smoothScroller)
                    val scrollPos = recyclerView.getChildAt(currentPosition).y.toInt()
                    nestedScrollView.smoothScrollTo(0, scrollPos)
                }
            }

            override fun onSpeakingStop(uId: String?) {
                stopBtn.setImageResource(R.drawable.ic_speaker)
                stopBtn.setBackgroundResource(R.drawable.rounded_corners_green)
            }
        })

        spider.addListener(spiderListener)
        spider.search(query, langCode)

        stopBtn.setOnClickListener {
            if (isSpeaking) {
                if (currentPosition != -1) {
                    setLineBackgroundColor(currentPosition, Color.WHITE)
                }

                tts.stop()
                isSpeaking = false
            } else {
                loadingView.visibility = View.VISIBLE
                elements.forEach { element ->
                    if (element.tagName != "img") {
                        tts.speak(element.text, element.uId)
                    }
                }
                isSpeaking = true
            }
        }

        val savedTextSize = tinyDB.getInt(C.TAG_READER_TEXT_SIZE, -1)
        if (savedTextSize != -1 && savedTextSize != 1) {
            articleAdapter.updateTextSize(textSizeValues[savedTextSize])
        }
    }

    private fun setLineBackgroundColor(position: Int, color: Int) {
        recyclerView.findViewHolderForAdapterPosition(position)?.also { viewHolder ->
            val v = if (viewHolder::class.simpleName == "ParaHolder") {
                (viewHolder as ArticleAdapter.ParaHolder).paraView
            } else {
                (viewHolder as ArticleAdapter.HeadingHolder).headingView
            }
            val s = v.text.toString()
            val spannableString = SpannableString(s)
            spannableString.setSpan(BackgroundColorSpan(color), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            v.text = spannableString
        }
    }

    private fun findIndex(uId: String?): Int? {
        var index: Int? = null
        for (i in elements.indices) {
            if (elements[i].uId == uId) {
                index = i
                break
            }
        }
        return index
    }

    private val spiderListener = object : Spider.Listener {
        override fun onFound(article: Article) {
            article.elements.forEach { elements.add(it) }
            articleAdapter.notifyDataSetChanged()
            toolbar.title = article.title
            tts.languageCode = article.url.split("://")[1].split(".")[0]
            loadingView.visibility = View.GONE
            if (tinyDB.getBoolean("isPro")) {
                article.elements.forEach {
                    if (it.tagName != "img") {
                        tts.speak(it.text, it.uId)
                    }
                }
            } else {
                Ads.showInterstitial {
                    article.elements.forEach {
                        if (it.tagName != "img") {
                            tts.speak(it.text, it.uId)
                        }
                    }
                }
                Ads.loadInterstitial()
            }
        }

        override fun onFail() {
            showError("Network Error!", "A network error occurred! please check your internet connection.")
        }
    }

    private fun showError(title: String, message: String) {
        newDialog().withTitle(title)
            .withMessage(message)
            .withCancelable(false)
            .withPositiveButton(getString(R.string.ok)) { dialog ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_reader, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuTextSize -> {
                val v = LayoutInflater.from(this).inflate(R.layout.layout_text_size_dialog, null)
                val picker: NumberPicker = v.findViewById(R.id.picker)
                val savedTextSize = tinyDB.getInt(C.TAG_READER_TEXT_SIZE, -1)
                picker.minValue = 0
                picker.maxValue = textSizes.size - 1
                picker.value = if (savedTextSize != -1) savedTextSize else 1
                picker.displayedValues = textSizes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    picker.selectionDividerHeight = 0
                }
                newDialog().withCustomView(v)
                    .withNegativeButton(getString(R.string.cancel)) { dialog ->
                        dialog.dismiss()
                    }
                    .withPositiveButton(getString(R.string.apply)) { dialog ->
                        if (tinyDB.getInt(C.TAG_READER_TEXT_SIZE, -1) != picker.value) {
                            articleAdapter.updateTextSize(textSizeValues[picker.value])
                            articleAdapter.notifyDataSetChanged()
                            tinyDB.putInt(C.TAG_READER_TEXT_SIZE, picker.value)
                        }
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
        return true
    }
}