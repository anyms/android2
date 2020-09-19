package app.spidy.browser.controllers

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import app.spidy.browser.TinyDB
import app.spidy.browser.data.Tab

class Headless(
    private val context: Context
) {
    private val tinyDB = TinyDB(context)
    val tabs = ArrayList<WebView>()

    var listener: Browser.Listener? = null
    val isJavaScriptEnabled: Boolean
        get() = !tinyDB.getBoolean("disable_javascript")
    val searchEngine: String
        get() {
            val engine = tinyDB.getString("search_engine")
            return if (engine == "" || engine == null) "https://duckduckgo.com/?q=" else engine
        }
    var currentTabIndex = 0

    val currentTab: WebView
        get() = tabs[currentTabIndex]

    var isLoading = false

    fun newTab(urlToLoad: String? = null) {
        val webView = WebView(context)
        applySettings(webView)
        urlToLoad?.let {
            webView.loadUrl(it)
        }
        tabs.add(webView)
        currentTabIndex = tabs.size - 1
    }

    fun closeTab(index: Int) {
        if (tabs.size == 1) {
            listener?.onNoTabs()
            return
        }

        tabs.removeAt(index)
        if (index == currentTabIndex) {
            currentTabIndex = if (index - 1 < 0) 0 else index - 1
            updateUrl(null, currentTab.url)
        }
    }

    fun switchTab(index: Int) {
        currentTabIndex = if (index > tabs.size - 1 || index < 0) 0 else index
        updateUrl(null, currentTab.url)
    }


    fun browse(q: String) {
        if (q.startsWith("http://") || q.startsWith("https://")) {
            currentTab.loadUrl(q)
        } else if (
            q.contains("[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]\\.[a-zA-Z]{2,}".toRegex()) ||
            q.contains("\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|\$)){4}\\b".toRegex()) ||
            q.startsWith("localhost")
        ) {
            currentTab.loadUrl("http://$q")
        } else {
            currentTab.loadUrl("$searchEngine$q")
        }
    }

    fun updateUrl(view: WebView?, url: String) {

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applySettings(webView: WebView) {
        webView.settings.apply {
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            useWideViewPort = true
            setSupportMultipleWindows(true)
            builtInZoomControls = true
            loadWithOverviewMode = true
            supportZoom()
            displayZoomControls = false
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setNeedInitialFocus(true)
            setAppCacheEnabled(true)
        }
        webView.clearCache(true)
        webView.webViewClient = WebClient(headless = this)
        webView.webChromeClient = ChromeClient(context, headless = this)
    }
}