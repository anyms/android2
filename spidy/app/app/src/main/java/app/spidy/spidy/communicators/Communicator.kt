package app.spidy.spidy.communicators

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import app.spidy.kotlinutils.*
import app.spidy.spidy.R
import app.spidy.spidy.viewmodels.BrowserActivityViewModel
import org.json.JSONArray
import org.json.JSONObject

class Communicator(
    private val context: Context,
    private val webView: WebView,
    private val viewModel: BrowserActivityViewModel? = null
) {

    private val tinyDB = TinyDB(context)

    @JavascriptInterface
    fun showToast(s: String) {
        context.toast(s)
    }

    @JavascriptInterface
    fun confirmElements(s: String, attrString: String) {
        tinyDB.putString("node", s)
        val vEl = JSONObject(s)
        val attrs = JSONArray(attrString)
        val tagName = vEl.getString("selector").split(" ").last().split(":").first()

        var attrStr = ""

        for (i in 0 until attrs.length()) {
            val attr = attrs.getJSONObject(i)
            attrStr += "${attr.getString("name")} : ${attr.getString("value")}\n"
        }

        onUiThread {
            context.newDialog().withTitle("Element Selector")
                .withMessage("Element(s) with tag name '${tagName}' were selected\n\nAttributes:\n\n${attrStr.trim()}")
                .withPositiveButton(context.getString(R.string.ok)) {
                    it.dismiss()
                }
                .create().show()
            viewModel?.isFabVisible?.value = true
        }
    }
}