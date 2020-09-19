package app.spidy.browser.controllers

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.EditText
import app.spidy.browser.R
import app.spidy.browser.fragments.BrowserFragment

class ChromeClient(
    private val context: Context,
    private val browser: Browser? = null,
    private val headless: Headless? = null
): WebChromeClient() {
    override fun onReceivedTitle(view: WebView?, title: String?) {
        browser?.also {
            for (i in browser.tabs.indices) {
                if (browser.tabs[i].fragment.webView == view) {
                    title?.let { browser.tabs[i].title = it }
                    break
                }
            }
        }
        super.onReceivedTitle(view, title)
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_edittext, null)
        val input: EditText = v.findViewById(R.id.editText)
        if (defaultValue != null) input.setText(defaultValue)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(message ?: "")
        builder.setCancelable(false)
        builder.setView(v)
        builder.setNegativeButton("Cancel") {dialog, which ->
            result?.cancel()
            dialog.dismiss()
        }
        builder.setPositiveButton("OK") {dialog, which ->
            result?.confirm(input.text.toString())
            dialog.dismiss()
        }
        builder.create().show()
        return true
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        browser?.also {
            for (i in browser.tabs.indices) {
                if (browser.tabs[i].fragment.webView == view) {
                    browser.tabs[i].favIcon = icon
                    break
                }
            }
        }
        super.onReceivedIcon(view, icon)
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        browser?.updateProgress(newProgress)
        super.onProgressChanged(view, newProgress)
    }

    override fun onCloseWindow(window: WebView?) {
        browser?.let {
            for (i in it.tabs.indices) {
                if (it.tabs[i].fragment.webView == window) {
                    it.closeTab(i)
                    break
                }
            }
        }
        headless?.let {
            for (i in headless.tabs.indices) {
                if (headless.tabs[i] == window) {
                    headless.closeTab(i)
                }
            }
        }
        super.onCloseWindow(window)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        val href = view?.handler?.obtainMessage()
        view?.requestFocusNodeHref(href)
        val url = href?.data?.getString("url")

        Log.d("hello", url.toString())

        url?.also {
            browser?.newTab(it)
            headless?.newTab(it)
        }

        return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
    }
}