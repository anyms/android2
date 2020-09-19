/*
var isExecuted = false
var isCodeExecuted = false
var isFroze = false
webView!!.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    javaScriptCanOpenWindowsAutomatically = true
    setNeedInitialFocus(true)
    cacheMode = WebSettings.LOAD_NO_CACHE
}
webView!!.webChromeClient = object : WebChromeClient() {
}
webView!!.webViewClient = object : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        isExecuted = false
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        onUiThread {
            view?.requestFocus()
            if (!isExecuted) {
                context?.toast("LOADED")
//                        view?.evaluateJavascript("""
//                            (function () {
//                                document.addEventListener('click', function (e) {
//                                    e.stopPropagation();
//                                    spidy.showToast();
//                                }, true);
//                            })()
//                        """.trimIndent()) {}
                isFroze = !isFroze
                if (code == null) {
                    elementSelectorScript?.also {
                        webView?.js("""
                            $hammerJs
                            $it
                        """.trimIndent())
                    }
                } else {
                    if (!isCodeExecuted) {
                        val script = SpidyScript(requireContext(), code!!) {
                            return@SpidyScript webView
                        }
                        thread { script.run() }
                        isCodeExecuted = true
                    }
                }
                isExecuted = true
            }
        }
        super.onPageFinished(view, url)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {

        if (!isFroze) {
            return super.shouldOverrideUrlLoading(view, request)
        }
        return true
    }
}
webView!!.addJavascriptInterface(Communicator(requireContext()), "spidy")

webView!!.loadUrl("https://google.com")
    */