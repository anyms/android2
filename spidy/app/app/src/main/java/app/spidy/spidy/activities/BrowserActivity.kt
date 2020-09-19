package app.spidy.spidy.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import app.spidy.browser.controllers.Browser
import app.spidy.browser.fragments.BrowserFragment
import app.spidy.kotlinutils.*
import app.spidy.spidy.R
import app.spidy.spidy.communicators.Communicator
import app.spidy.spidy.interpreter.SpidyScript2
import app.spidy.spidy.utils.*
import app.spidy.spidy.viewmodels.BrowserActivityViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_browser.*
import java.net.URI
import java.util.*
import kotlin.concurrent.thread

class BrowserActivity : AppCompatActivity() {
    private lateinit var viewModel: BrowserActivityViewModel
    private lateinit var browserFragment: BrowserFragment
    private lateinit var logCatView: TextView
    private lateinit var logCatScrollView: ScrollView
    private lateinit var terminalView: View
    private lateinit var terminalDialog: AlertDialog
    private lateinit var tinyDB: TinyDB
    private lateinit var fabOpenAnim: Animation
    private lateinit var fabCloseAnim: Animation
    private lateinit var fabRotateClockWiseAnim: Animation
    private lateinit var fabRotateAntiClockWiseAnim: Animation
    private lateinit var spidyScript: SpidyScript2

    private var isFabOpen = false
    private var isInspector = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel = ViewModelProviders.of(this).get(BrowserActivityViewModel::class.java)
        browserFragment = BrowserFragment()

        tinyDB = TinyDB(this)
        val adView: AdView = findViewById(R.id.adView)
        if (!tinyDB.getBoolean("isPro")) {
            adView.loadAd(AdRequest.Builder().build())
        } else {
            adView.visibility = View.GONE
        }

        fabOpenAnim = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_open)
        fabCloseAnim = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_close)
        fabRotateClockWiseAnim = AnimationUtils.loadAnimation(applicationContext, R.anim.rotate_clockwise)
        fabRotateAntiClockWiseAnim = AnimationUtils.loadAnimation(applicationContext, R.anim.rotate_anticlockwise)


        fab.setOnClickListener {
            toggleElementSelectorsMenu()
        }

        thread {
            Thread.sleep(10_000)
            onUiThread {
                Ads.showInterstitial()
                Ads.loadInterstitial()
            }
        }

        terminalView = LayoutInflater.from(this).inflate(R.layout.layout_terminal_dialog, null)
        logCatView = terminalView.findViewById(R.id.logCat)
        logCatScrollView = terminalView.findViewById(R.id.scrollView)
        terminalDialog = newDialog().withTitle(getString(R.string.debug_console))
            .withCancelable(false)
            .withCustomView(terminalView)
            .withPositiveButton(getString(R.string.dismiss)) {
                it.dismiss()
            }
            .create()

        viewModel.isRecordButtonVisible.observe(this, Observer {
            browserFragment.getRecordButton()?.visibility = if (it) View.VISIBLE else View.GONE
        })
        viewModel.isTerminalButtonVisible.observe(this, Observer {
            browserFragment.getTerminalButton()?.visibility = if (it) View.VISIBLE else View.GONE
        })
        viewModel.logs.observe(this, Observer {
            val log = it.replace("\\u003C", "<").replace("\\\"", "\"")
            logCatView.text = log
            logCatScrollView.fullScroll(View.FOCUS_DOWN)
        })
        viewModel.isFabVisible.observe(this, Observer {
            if (it) fab.show() else fab.hide()
        })

        val isElementSelector = intent!!.getBooleanExtra("isElementSelector", false)
        intent?.getBooleanExtra("isInspector", false)?.also {
            isInspector = it
        }
        val code = intent.getStringExtra("code")
        viewModel.isRecordButtonVisible.value = isElementSelector
        viewModel.isTerminalButtonVisible.value = !isElementSelector
        if (isElementSelector) {
            browserFragment.urlToLoad = intent?.getStringExtra("lastUrl")
        }

        viewModel.code = code?.replace("\\\"", "\"")?.replace("\\\\\"", "\\\"")
        browserFragment.setBrowserListener = { browserListener }
        viewModel.isFabVisible.value = false

        selectSimilarBtn.setOnClickListener {
            browserFragment.browser.currentTab.fragment.webView?.injectScript("""
                    spidy.canRecord = false;
                    return spidy.addSimilarElements(${tinyDB.getString("node")});
                """.trimIndent()) {
                debug(it)
                tinyDB.putString("node", it)
                viewModel.isRecordButtonVisible.value = false
                toast(getString(R.string.adding_similar_elements))
            }
            toggleElementSelectorsMenu()
        }
        addSelectedBtn.setOnClickListener {
            browserFragment.browser.currentTab.fragment.webView?.injectScript("""
                    spidy.canRecord = false;
                """.trimIndent()) {
                viewModel.isRecordButtonVisible.value = false
                browserListener.onCloseBrowser(browserFragment.browser.currentTab.fragment.webView)
            }
            toggleElementSelectorsMenu()
        }
        switchNearestBtn.setOnClickListener {
            val webView = browserFragment.browser.currentTab.fragment.webView
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            newDialog().withTitle(getString(R.string.enter_a_css_selector))
                .withCustomView(input)
                .withPositiveButton(getString(R.string.find)) {
                    val tagName = input.text.toString().toLowerCase(Locale.ROOT)
                    webView?.injectScript("""
                            return spidy.switchToNearestElement(${tinyDB.getString("node")}, "$tagName");
                        """.trimIndent()) {
                        toast(getString(R.string.trying_to_find_nearest_element))
                    }
                }
                .withNegativeButton(getString(R.string.cancel)) {
                    webView?.injectScript("""
                            spidycom.confirmElements(JSON.stringify(spidy.node));
                        """.trimIndent())
                }
                .create()
                .show()
            toggleElementSelectorsMenu()
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.browserHolder, browserFragment)
            .commit()

        spidyScript = SpidyScript2(
            this@BrowserActivity,
            viewModel = viewModel,
            getBrowser = {
                return@SpidyScript2 browserFragment.browser
            },
            getWebView = {
                return@SpidyScript2 browserFragment.browser.currentTab.fragment.webView
            }
        )
    }

    private fun toggleElementSelectorsMenu() {
        if (isFabOpen) {
            selectSimilarBtn.startAnimation(fabCloseAnim)
            addSelectedBtn.startAnimation(fabCloseAnim)
            switchNearestBtn.startAnimation(fabCloseAnim)

            fab.startAnimation(fabRotateAntiClockWiseAnim)

            selectSimilarBtn.isClickable = false
            addSelectedBtn.isClickable = false
            switchNearestBtn.isClickable = false

            selectSimilarBtn.visibility = View.INVISIBLE
            addSelectedBtn.visibility = View.INVISIBLE
            switchNearestBtn.visibility = View.INVISIBLE
            isFabOpen = false
        } else {
            selectSimilarBtn.startAnimation(fabOpenAnim)
            addSelectedBtn.startAnimation(fabOpenAnim)
            switchNearestBtn.startAnimation(fabOpenAnim)

            fab.startAnimation(fabRotateClockWiseAnim)

            selectSimilarBtn.isClickable = true
            addSelectedBtn.isClickable = true
            switchNearestBtn.isClickable = true

            selectSimilarBtn.visibility = View.VISIBLE
            addSelectedBtn.visibility = View.VISIBLE
            switchNearestBtn.visibility = View.VISIBLE
            isFabOpen = true
        }
    }

    override fun onBackPressed() {
        if (browserFragment.canGoBack()) {
            browserFragment.browser.currentTab.fragment.webView?.goBack()
        } else {
            browserListener.onCloseBrowser(browserFragment.browser.currentTab.fragment.webView)
        }
    }

    override fun onDestroy() {
        spidyScript.isTerminated = true
        super.onDestroy()
    }


    private var isAlreadyRan = false
    private var isCodeExecuted = false
    private val browserListener = object : Browser.Listener {
        override fun onPageStarted(view: WebView, url: String, favIcon: Bitmap?): Boolean {
            isAlreadyRan = false
            spidyScript.isReadyToExecute = false
            return super.onPageStarted(view, url, favIcon)
        }
        override fun onPageFinished(view: WebView, url: String): Boolean {
            if (!isAlreadyRan) {
                if (viewModel.code == null) {
                    if (browserFragment.browser.isRecordingEnabled) {
                        Injector.preventNavigation(view)
                        view.injectScript("spidy.init();")
//                        Injector.injectScript(view, viewModel.hammerJs)
//                        Injector.injectScript(view, viewModel.spidyJs)
                    }
                } else {
//                    Injector.injectScript(view, viewModel.spidyJs, isInit = false)
                    if (!isCodeExecuted) {
                        thread { spidyScript.run(viewModel.code!!.base64Encode()) }
                        isCodeExecuted = true
                    }
                }
                isAlreadyRan = true

                thread {
                    Thread.sleep(1000)
                    spidyScript.isReadyToExecute = true
                }
            }
            return super.onPageFinished(view, url)
        }


        override fun onNewWebView(view: WebView): Boolean {
            view.addJavascriptInterface(
                Communicator(this@BrowserActivity, view, viewModel),
                "spidycom"
            )
            return super.onNewWebView(view)
        }

        override fun onRecordingEnabled(view: WebView?): Boolean {
            Injector.preventNavigation(view)
            view?.injectScript("spidy.init();")

//            Injector.injectScript(view, viewModel.hammerJs)
//            Injector.injectScript(view, viewModel.spidyJs)

            if (!tinyDB.getBoolean("isSelectionAlreadyHappened")) {
                onUiThread {
                    newDialog().withTitle("Info")
                        .withCancelable(false)
                        .withMessage("Long press on any element to select")
                        .withPositiveButton("Got it") {
                            tinyDB.putBoolean("isSelectionAlreadyHappened", true)
                            it.dismiss()
                        }
                        .create().show()
                }
            }

            return super.onRecordingEnabled(view)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!browserFragment.browser.isRecordingEnabled) {
                return super.shouldOverrideUrlLoading(view, request)
            }
            return true
        }

        override fun onCloseBrowser(view: WebView?): Boolean {
            val webView = browserFragment.browser.currentTab.fragment.webView
            val resultIntent = Intent()
            resultIntent.putExtra("lastUrl", view?.url)
            resultIntent.putExtra("isElementSelector", viewModel.code == null)
            if (webView == null) {
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                resultIntent.putExtra("selectors", tinyDB.getString("node"))
                setResult(Activity.RESULT_OK, resultIntent)
                tinyDB.putString("node", "")
                finish()
            }
            return super.onCloseBrowser(view)
        }


        override fun onOpenTerminal(): Boolean {
            terminalDialog.show()
            return super.onOpenTerminal()
        }

    }
}