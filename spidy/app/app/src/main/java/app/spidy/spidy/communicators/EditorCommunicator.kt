package app.spidy.spidy.communicators

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import app.spidy.kotlinutils.onUiThread
import app.spidy.spidy.activities.BrowserActivity
import app.spidy.spidy.activities.EditorActivity
import app.spidy.spidy.utils.C
import app.spidy.spidy.viewmodels.EditorActivityViewModel

class EditorCommunicator(private val context: Context, private val viewModel: EditorActivityViewModel) {
    @JavascriptInterface
    fun openBrowser() {
        val intent = Intent(context, BrowserActivity::class.java)
        intent.putExtra("isElementSelector", true)
        intent.putExtra("lastUrl", viewModel.lastUrl)
        (context as EditorActivity).startActivityForResult(intent, C.EDITOR_COMMUNICATOR_RESULT_REQUEST_CODE)
    }
}