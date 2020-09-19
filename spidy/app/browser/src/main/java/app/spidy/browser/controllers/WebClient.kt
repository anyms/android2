package app.spidy.browser.controllers

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import app.spidy.browser.R
import app.spidy.browser.fragments.BrowserFragment

class WebClient(private val browser: Browser? = null, private val headless: Headless? = null) : WebViewClient() {
    private var lastUrl: String? = null

    override fun onLoadResource(view: WebView?, url: String?) {
        if (lastUrl == null || !view?.url.equals(lastUrl)) {
            if (view?.url != null) {
                val tabUrl = if (view.url.endsWith('/')) view.url.trimEnd('/') else view.url
                lastUrl = view.url
                headless?.updateUrl(view, tabUrl)
                browser?.updateUrl(view, tabUrl)
            }
        }
        super.onLoadResource(view, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        headless?.isLoading = true

        browser?.isLoading = true
        browser?.getRefreshImageView?.invoke()?.setImageResource(R.drawable.ic_cancel)
        browser?.isProgressBarVisible = true
        if (view != null && url != null) {
            val ret = if (headless == null) {
                browser?.listener?.onPageStarted(view, url, favicon)
            } else {
                headless.listener?.onPageStarted(view, url, favicon)
            }
            if (ret == true) {
                return
            }
        }
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        view?.requestFocus()
        headless?.isLoading = false

        browser?.isLoading = false
        browser?.getRefreshImageView?.invoke()?.setImageResource(R.drawable.browser_refresh_icon)
        browser?.isProgressBarVisible = false
        if (view != null && url != null) {
            val ret =  if (headless == null) {
                browser?.listener?.onPageFinished(view, url)
            } else {
                headless.listener?.onPageFinished(view, url)
            }
            if (ret == true) {
                return
            }
        }
        super.onPageFinished(view, url)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (view != null && request != null) {
            val ret = if (headless != null) {
                headless.listener?.shouldOverrideUrlLoading(view, request)
            } else {
                browser?.listener?.shouldOverrideUrlLoading(view, request)
            }
            if (ret == true) return true
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (view != null && request != null) {
            if (headless != null) {
                headless.listener?.onReceivedError(view, request, error)
            } else {
                browser?.listener?.onReceivedError(view, request, error)
            }
        }
        super.onReceivedError(view, request, error)
    }
}