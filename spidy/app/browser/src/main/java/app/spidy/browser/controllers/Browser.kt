package app.spidy.browser.controllers

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import app.spidy.browser.R
import app.spidy.browser.TinyDB
import app.spidy.browser.adapters.PagerStateAdapter
import app.spidy.browser.adapters.TabAdapter
import app.spidy.browser.data.Tab
import app.spidy.browser.fragments.BrowserFragment
import app.spidy.browser.fragments.WebViewFragment

class Browser(
    private val context: Context,
    private val progressBar: ProgressBar,
    private val getStateAdapter: () -> PagerStateAdapter,
    private val viewPager: ViewPager2,
    private val urlField: EditText,
    private val dismissCallback: () -> Unit,
    private val getCurrentTabIndex: () -> Int,
    private val tabsBtn: ImageView,
    private val getTabAdapter: () -> TabAdapter?,
    val getRefreshImageView: () -> ImageView?
) {
    private val tinyDB = TinyDB(context)

    val tabs = ArrayList<Tab>()
    var listener: Listener? = null
    var isRecordingEnabled = false
    val isJavaScriptEnabled: Boolean
        get() = !tinyDB.getBoolean("disable_javascript")
    val searchEngine: String
        get() {
            val engine = tinyDB.getString("search_engine")
            return if (engine == "" || engine == null) "https://duckduckgo.com/?q=" else engine
        }


    private val stateAdapter: PagerStateAdapter
        get() = getStateAdapter()

    val currentTabIndex: Int
        get() = getCurrentTabIndex()

    val currentTab: Tab
        get() = tabs[currentTabIndex]

    val webView: WebView?
        get() = tabs[currentTabIndex].fragment.webView

    var isLoading = false

    var isProgressBarVisible: Boolean = false
        set(value) {
            if (value) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.INVISIBLE
            }
            field = value
        }

    fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    fun newTab(urlToLoad: String? = null) {
        val fragment = WebViewFragment()
        fragment.browser = this
        fragment.urlToLoad = urlToLoad
        tabs.add(Tab("about:blank", "about:blank", null, fragment))
        stateAdapter.notifyItemInserted(tabs.lastIndex)
        viewPager.currentItem = tabs.lastIndex
        updateUrl(null,"about:blank")
        updateTabCount()
    }

    private fun updateTabCount() {
        val id = when (tabs.size) {
            1 -> R.drawable.browser_tab_count_1
            2 -> R.drawable.browser_tab_count_2
            3 -> R.drawable.browser_tab_count_3
            4 -> R.drawable.browser_tab_count_4
            5 -> R.drawable.browser_tab_count_5
            6 -> R.drawable.browser_tab_count_6
            7 -> R.drawable.browser_tab_count_7
            8 -> R.drawable.browser_tab_count_8
            9 -> R.drawable.browser_tab_count_9
            else -> R.drawable.browser_tab_count_9_plus
        }
        tabsBtn.setImageResource(id)
    }

    fun updateUrl(view: WebView?, url: String) {
        if (view == null) {
            currentTab.url = url
            urlField.setText(url)
        } else {
            for (i in tabs.indices) {
                if (tabs[i].fragment.webView == view) {
                    tabs[i].url = url
                    if (i == currentTabIndex) urlField.setText(url)
                    break
                }
            }
        }
    }

    fun browse(q: String) {
        if (q.startsWith("http://") || q.startsWith("https://")) {
            currentTab.fragment.webView?.loadUrl(q)
        } else if (
            q.contains("[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]\\.[a-zA-Z]{2,}".toRegex()) ||
            q.contains("\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|\$)){4}\\b".toRegex()) ||
            q.startsWith("localhost")
        ) {
            currentTab.fragment.webView?.loadUrl("http://$q")
        } else {
            currentTab.fragment.webView?.loadUrl("$searchEngine$q")
        }
    }

    fun closeTab(index: Int) {
        if (tabs.size == 1) {
            dismissCallback()
            return
        }

        tabs.removeAt(index)
        stateAdapter.notifyItemRemoved(index)
        getTabAdapter()?.notifyDataSetChanged()
        if (index == currentTabIndex) {
            if (index - 1 < 0) {
                viewPager.currentItem = 0
            } else {
                viewPager.currentItem = index - 1
            }
            updateUrl(null, currentTab.url)
        }
        updateTabCount()
    }

    fun switchTab(index: Int) {
        viewPager.currentItem = index
        updateUrl(null, currentTab.url)
    }

    fun updateProgress(progress: Int) {
        progressBar.progress = progress
    }


    interface Listener {
        fun onLoadResource(view: WebView, url: String, tabUrl: String): Boolean = false
        fun onPageStarted(view: WebView, url: String, favIcon: Bitmap?): Boolean = false
        fun onPageFinished(view: WebView, url: String): Boolean = false
        fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
        fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError?): Boolean =false
        fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError): Boolean = false
        fun shouldInterceptRequest(view: WebView, activity: FragmentActivity?, url: String,
                                   request: WebResourceRequest?): Boolean = false
        fun onFormResubmission(view: WebView, dontResend: Message, resend: Message): Boolean = false
        fun onPageCommitVisible(view: WebView, url: String): Boolean = false
        fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest): Boolean = false
        fun onNewUrl(view: WebView, url: String): Boolean = false
        fun onNewDownload(view: WebView, url: String, userAgent: String,
                          contentDisposition: String, mimetype: String, contentLength: Long): Boolean = false
        fun onSwitchTab(fromTabId: String, toTabId: String): Boolean = false
        fun onNewTab(tabId: String): Boolean = false
        fun onCloseTab(tabId: String): Boolean = false
        fun onReceivedTitle(view: WebView?, title: String?): Boolean = false
        fun onReceivedIcon(view: WebView?, icon: Bitmap?): Boolean = false
        fun onProgressChanged(view: WebView?, newProgress: Int): Boolean = false
        fun onRecordingEnabled(view: WebView?): Boolean = false
        fun onNewWebView(view: WebView): Boolean = false
        fun onCloseBrowser(view: WebView?): Boolean = false
        fun onOpenTerminal(): Boolean = false
        fun onNoTabs(): Boolean = false
    }
}