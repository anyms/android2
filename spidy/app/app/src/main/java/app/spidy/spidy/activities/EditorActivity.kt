package app.spidy.spidy.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.room.Room
import app.spidy.kotlinutils.*
import app.spidy.spidy.R
import app.spidy.spidy.communicators.EditorCommunicator
import app.spidy.spidy.data.Script
import app.spidy.spidy.databases.SpidyDatabase
import app.spidy.spidy.utils.Ads
import app.spidy.spidy.utils.C
import app.spidy.spidy.utils.base64Encode
import app.spidy.spidy.utils.js
import app.spidy.spidy.viewmodels.EditorActivityViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_editor.*
import org.json.JSONArray
import kotlin.concurrent.thread

class EditorActivity : AppCompatActivity() {
    private lateinit var viewModel: EditorActivityViewModel
    private lateinit var database: SpidyDatabase
    private lateinit var permission: Permission
    private lateinit var tinyDB: TinyDB

    private var isExistAsked = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        viewModel = ViewModelProviders.of(this).get(EditorActivityViewModel::class.java)

        database = Room.databaseBuilder(this, SpidyDatabase::class.java, "SpidyDatabase")
            .addMigrations(SpidyDatabase.migration).build()
        permission = Permission(this)

        tinyDB = TinyDB(this)
        val adView: AdView = findViewById(R.id.adView)
        if (!tinyDB.getBoolean("isPro")) {
            Ads.showInterstitial()
            Ads.loadInterstitial()
            adView.loadAd(AdRequest.Builder().build())
        } else {
            adView.visibility = View.GONE
        }

        setSupportActionBar(toolbar)
        title = getString(R.string.editor)

        toolbar.setNavigationIcon(R.drawable.ic_menu)
        var isToolboxOpen = false
        viewModel.isToolboxShown.value = false
        toolbar.setNavigationOnClickListener {
            webView.js("""
                _spidy.toggleToolbox();
            """.trimIndent())
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?
            ): Boolean {
                val v = LayoutInflater.from(this@EditorActivity).inflate(R.layout.layout_edittext, null)
                val input: EditText = v.findViewById(R.id.editText)
                if (defaultValue != null) input.setText(defaultValue.replace("\\n", "\n"))
                this@EditorActivity.newDialog().withTitle(message ?: "")
                    .withCustomView(input)
                    .withCancelable(false)
                    .withNegativeButton(this@EditorActivity.getString(R.string.cancel)) {
                        result?.cancel()
                        it.dismiss()
                    }
                    .withPositiveButton(this@EditorActivity.getString(R.string.ok)) {
                        result?.confirm(input.text.toString().replace("\n", "\\n"))
                        it.dismiss()
                    }
                    .create().show()
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val dat = intent?.getStringExtra("code")
                val scriptName = intent?.getStringExtra("scriptName")
                if (scriptName != null) {
                    viewModel.scriptTitle = scriptName
                    title = scriptName
                    isExistAsked = true
                }
                if (dat != null) {
                    val codeResult = "==CODE_START==\n(.*)\n==CODE_END==".toRegex().find(dat)
                    val elementsResult = "==ELEMENTS_START==\n(.*)\n==ELEMENTS_END==".toRegex().find(dat)
                    val workspaceResult = "==WORKSPACE_START==\n(.*)\n==WORKSPACE_END==".toRegex().find(dat)
                    if (codeResult != null && elementsResult != null && workspaceResult != null) {
                        val code = JSONArray(Base64.decode(codeResult.groupValues[1], Base64.DEFAULT).toString(charset("UTF-8")))
                        val stack = elementsResult.groupValues[1]
                        val workspace = workspaceResult.groupValues[1]
                        webView.js("""
                            return updateWorkspace("$workspace", "$stack", ${code.getInt(code.length() - 1)});
                        """.trimIndent())
                    }
                }
            }
        }
        webView.addJavascriptInterface(EditorCommunicator(this, viewModel), "spidy")

        if (!viewModel.isAlreadyCreated) {
            webView.loadUrl("file:///android_asset/editor/index.html")
            viewModel.isAlreadyCreated = true
        }

        permission.request(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            "require storage permission to download any files.\n\nWould you like to grant?",
            object : Permission.Listener {
                override fun onGranted() {

                }

                override fun onRejected() {

                }
            }
        )

        thread {
            Thread.sleep(2000)
            runOnUiThread {
                overlayView.visibility = View.GONE
            }
        }
    }

    override fun onBackPressed() {
        leaveEditor()
    }

    private fun leaveEditor() {
        webView.js("""
            return _spidy.isSaved;
        """.trimIndent()) {
            if (it == "false") {
                newDialog().withTitle(getString(R.string.are_you_sure))
                    .withMessage(getString(R.string.unsaved_message))
                    .withPositiveButton(getString(R.string.cancel)) { dialog ->
                        dialog.dismiss()
                    }
                    .withNegativeButton(getString(R.string.discard)) {
                        finish()
                    }
                    .withNeutralButton(getString(R.string.save_and_exit)) {
                        save { finish() }
                    }
                    .create()
                    .show()
            } else {
                finish()
            }
        }
    }

    private fun save(callback: (() -> Unit)? = null) {
        Ads.showInterstitial()
        Ads.loadInterstitial()

        webView.js("""
            return generateCode();
        """.trimIndent()) { code ->
            if (code == "null") {
                newDialog().withTitle("Error!")
                    .withMessage("Unable to save workspace, there are some syntax error in your code.")
                    .withPositiveButton(getString(R.string.ok)) {
                        it.dismiss()
                    }
                    .create().show()
            } else {
                thread {
                    val scripts = database.spidyDao().getScripts()
                    var isUpdate = false
                    for (script in scripts) {
                        if (script.name == viewModel.scriptTitle) {
                            if (!isExistAsked) {
                                onUiThread {
                                    newDialog().withMessage(getString(R.string.sript_already_exist_message))
                                        .withPositiveButton(getString(R.string.no)) {
                                            viewModel.scriptTitle = null
                                            it.dismiss()
                                        }
                                        .withNegativeButton(getString(R.string.yes)) {
                                            isExistAsked = true
                                            thread {
                                                script.code = code
                                                database.spidyDao().updateScript(script)
                                            }
                                            this@EditorActivity.title = script.name
                                            callback?.invoke()
                                            toast(getString(R.string.script_updated))
                                        }
                                        .create()
                                        .show()
                                }
                            } else {
                                script.code = code
                                database.spidyDao().updateScript(script)
                                onUiThread {
                                    this@EditorActivity.title = script.name
                                    toast(getString(R.string.script_updated))
                                    callback?.invoke()
                                }
                            }

                            isUpdate = true
                            break
                        }
                    }
                    if (!isUpdate) {
                        isExistAsked = true
                        database.spidyDao().putScript(Script(viewModel.scriptTitle!!, code))
                        onUiThread {
                            toast(getString(R.string.script_saved))
                            callback?.invoke()
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuRun -> {
                viewModel.isTest = true
                webView.js("return generateJson();") {
                    WebStorage.getInstance().deleteAllData()
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()

                    val intent = Intent(this, BrowserActivity::class.java)
                    intent.putExtra("isElementSelector", false)
                    intent.putExtra("code", it)
                    startActivityForResult(intent, C.EDITOR_COMMUNICATOR_RESULT_REQUEST_CODE)
                }
            }
            R.id.menuSave -> {
                if (viewModel.scriptTitle == null) {
                    val input = EditText(this)
                    input.inputType = InputType.TYPE_CLASS_TEXT
                    newDialog().withTitle(getString(R.string.enter_a_script_title))
                        .withCustomView(input)
                        .withPositiveButton(getString(R.string.save)) {
                            var title = input.text.toString().trim()
                            if (title == "") {
                                title = "Untitled"
                            }
                            viewModel.scriptTitle = title
                            save()
                        }
                        .withNegativeButton(getString(R.string.cancel)) {
                            it.dismiss()
                        }
                        .create()
                        .show()
                } else {
                    save()
                }
            }
            R.id.menuUndo -> {
                webView.js("""
                    Blockly.mainWorkspace.undo(false);
                """.trimIndent())
            }
            R.id.menuRedo -> {
                webView.js("""
                    Blockly.mainWorkspace.undo(true);
                """.trimIndent())
            }
            R.id.menuExit -> {
                leaveEditor()
            }
            R.id.menuRotate -> {
                requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
            R.id.menuInspector -> {
                val intent = Intent(this, BrowserActivity::class.java)
                intent.putExtra("isElementSelector", true)
                intent.putExtra("isInspector", true)
                intent.putExtra("lastUrl", viewModel.lastUrl)
                startActivity(intent)
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permission.execute(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.isTest = false

        if (requestCode == C.EDITOR_COMMUNICATOR_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val selectors = data.getStringExtra("selectors")
            debug(selectors)
            viewModel.lastUrl = data.getStringExtra("lastUrl")
            if (!viewModel.isTest && data.getBooleanExtra("isElementSelector", false)) {
                if (selectors == "{\"isAll\":false}" || selectors == null || selectors == "") {
                    toast(getString(R.string.no_element_selected_message))
                    webView.js("return updateLastBlock(null);")
                } else {
                    toast(getString(R.string.elements_selected))
                    val s = selectors.replace("'", "\\'")
                        .replace("\\\"", "\"")
                        .replace("\\\\\"", "\\\"")
                    webView.js("return updateLastBlock('${s.base64Encode()}');")
                }
            }
        }
    }
}