package app.spidy.spidy.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.FrameLayout
import app.spidy.browser.controllers.ChromeClient
import app.spidy.kotlinutils.TinyDB
import app.spidy.spidy.R
import app.spidy.spidy.communicators.GettingStartedCommunicator
import kotlinx.android.synthetic.main.activity_getting_started.*

class GettingStartedActivity : AppCompatActivity() {
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalOrientation: Int? = null
    private var originalSystemUiVisibility: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_getting_started)

        webView.settings.apply {
            javaScriptEnabled = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            /* Allow videos to go fullscreen */
            override fun getDefaultVideoPoster(): Bitmap? {
                if (customView == null) {
                    return null
                }
                return BitmapFactory.decodeResource(resources, 2130837573)
            }

            override fun onHideCustomView() {
                (window.decorView as FrameLayout).removeView(customView)
                customView = null
                window.decorView.systemUiVisibility = originalSystemUiVisibility!!
                requestedOrientation = originalOrientation!!
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    onHideCustomView()
                    return
                }

                customView = view
                customView?.setBackgroundColor(Color.BLACK)
                originalSystemUiVisibility = window.decorView.systemUiVisibility
                originalOrientation = requestedOrientation
                customViewCallback = callback
                (window.decorView as FrameLayout).addView(customView, FrameLayout.LayoutParams(-1, -1))
                window.decorView.systemUiVisibility = 3846
            }
        }
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(GettingStartedCommunicator(this), "spidy")
        webView.loadUrl("file:///android_asset/getting_started.html")
    }
}