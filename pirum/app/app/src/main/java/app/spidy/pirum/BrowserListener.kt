package app.spidy.pirum

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import app.spidy.kookaburra.controllers.Browser
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.onUiThread
import app.spidy.pirum.detectors.Detector
import app.spidy.pirum.interfaces.DetectListener
import app.spidy.pirum.utils.UrlValidator.validateUrl

class BrowserListener(
    private val context: Context,
    private val toLoadUrl: String?,
    private val onDetect: (List<HashMap<String, String>>) -> Unit
): Browser.Listener {
    private var isFirstLaunch = true
    private val blacklist = Blacklist()
    private var currentUrl: String = ""

    private val detectListener = object : DetectListener {
        override fun onDetect(detect: HashMap<String, String>) {
            detects[currentTabId]!!.add(detect)
            onDetect(detects[currentTabId]!!)
        }
    }
    private val detector = Detector(detectListener)
    private val cookieManager = CookieManager.getInstance()
    private var cookies = HashMap<String, String>()
    private var pageUrl: String? = null
    private val detects = HashMap<String, ArrayList<HashMap<String, String>>>()
    private var currentTabId = ""

    fun getDetects(): List<HashMap<String, String>> {
        val ds = ArrayList<HashMap<String, String>>()
        if (detects[currentTabId] != null) {
            for (d in detects[currentTabId]!!) {
                ds.add(d)
            }
        }
        return ds
    }

    override fun shouldInterceptRequest(view: WebView, activity: FragmentActivity?, url: String, request: WebResourceRequest?) {
        if (validateUrl(url)) {
            ignore {
                if (currentUrl == "") {
                    onUiThread {
                        currentUrl = view.url
                    }
                }
                if (!blacklist.isBlocked(currentUrl) && currentUrl != "") {
                    detector.detect(url, request?.requestHeaders, cookies, pageUrl, view, activity)
                }
            }
        }
    }

    override fun onPageStarted(view: WebView, url: String, favIcon: Bitmap?) {
        if (isFirstLaunch && toLoadUrl != null) {
            view.loadUrl(toLoadUrl)
            isFirstLaunch = false
        }
        detector.reset()
        detects[currentTabId]?.clear()
        currentUrl = view.url
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (url.contains("tamilian.net/")) {
            view.evaluateJavascript("""
                    (function() {
                        var parent = document.querySelector("input[name='id']").parentNode;
                        parent.removeAttribute("target");
                        var button = document.createElement("input");
                        button.setAttribute("type", "submit");
                        button.value = "PLAY THE VIDEO";
                        parent.appendChild(button);
                    })();
                """.trimIndent()){}
        }
    }

    override fun onNewTab(tabId: String) {
        detects[tabId] = ArrayList()
        currentTabId = tabId
    }

    override fun onSwitchTab(fromTabId: String, toTabId: String) {
        currentTabId = toTabId
        currentUrl = ""
    }

    override fun onCloseTab(tabId: String) {
        detects.remove(tabId)
    }

    override fun onRestoreTab(tabId: String, isActive: Boolean) {
        detects[tabId] = ArrayList()
        if (isActive) currentTabId = tabId
    }

    override fun onNewUrl(view: WebView, url: String) {
        pageUrl = url
        cookies = HashMap()
        val cooks = cookieManager.getCookie(url)?.split(";")

        if (cooks != null) {
            for (cook in cooks) {
                val nodes = cook.trim().split("=")
                cookies[nodes[0].trim()] = nodes[1].trim()
            }
        }
    }

    override fun onNewDownload(
        view: WebView,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimetype: String,
        contentLength: Long
    ) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        downloadManager.enqueue(request)
    }
}