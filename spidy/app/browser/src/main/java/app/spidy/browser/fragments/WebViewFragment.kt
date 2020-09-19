package app.spidy.browser.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import app.spidy.browser.R
import app.spidy.browser.controllers.Browser
import app.spidy.browser.controllers.ChromeClient
import app.spidy.browser.controllers.WebClient


class WebViewFragment : Fragment() {
    private var wv: WebView? = null
    var browser: Browser? = null
    var urlToLoad: String? = null

    val webView: WebView?
        get() = wv

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_web_view, container, false)
        wv = v.findViewById(R.id.webView)

        wv!!.settings.apply {
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
        wv!!.clearCache(true)
        wv!!.webViewClient = WebClient(browser)
        wv!!.webChromeClient = ChromeClient(requireContext(), browser)

        wv!!.isFocusableInTouchMode = true
        wv!!.isFocusable = true
        wv!!.requestFocus()
        browser?.listener?.onNewWebView(wv!!)

        if (urlToLoad == null) {
            wv!!.loadUrl("file:///android_asset/blank.html")
        } else {
            wv!!.loadUrl(urlToLoad)
            wv!!.reload()
        }

        return v
    }

    override fun onResume() {
        super.onResume()

        webView?.settings?.javaScriptEnabled = browser?.isJavaScriptEnabled ?: true
    }

    override fun onDestroy() {
        browser = null
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        fun newInstance() = WebViewFragment()
    }
}