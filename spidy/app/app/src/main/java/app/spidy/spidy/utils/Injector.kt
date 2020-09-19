package app.spidy.spidy.utils

import android.webkit.WebView

object Injector {

    lateinit var HAMMER_JS: String
    lateinit var SPIDY_JS: String

    fun preventNavigation(webView: WebView?) {
        webView?.js("""
            document.addEventListener('click', function (e) {
                e.stopPropagation();
            }, true);
        """.trimIndent())
    }

//    fun injectScript(webView: WebView?, s: String, isInit: Boolean = true) {
//        val script = if (isInit) {
//            """
//                $s
//                spidy.init();
//            """.trimIndent()
//        } else {
//            s
//        }
//        webView?.js("""
//            $script
//        """.trimIndent()) {
//
//        }
//    }
}