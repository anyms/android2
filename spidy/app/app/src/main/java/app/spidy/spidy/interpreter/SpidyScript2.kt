package app.spidy.spidy.interpreter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.LayoutInflater
import android.webkit.WebView
import android.widget.EditText
import app.spidy.browser.controllers.Browser
import app.spidy.browser.controllers.Headless
import app.spidy.kotlinutils.*
import app.spidy.spider.data.Function
import app.spidy.spider.data.Node
import app.spidy.spidy.R
import app.spidy.spidy.activities.BrowserActivity
import app.spidy.spidy.data.Variable
import app.spidy.spidy.services.HeadlessService
import app.spidy.spidy.utils.*
import app.spidy.spidy.viewmodels.BrowserActivityViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread

class SpidyScript2(
    private val context: Context,
    private val getWebView: () -> WebView?,
    private val viewModel: BrowserActivityViewModel? = null,
    private val getBrowser: (() -> Browser)? = null,
    private val headless: Headless? = null,
    private val canShowDialogs: Boolean = true,
    private val debugLog: ((s: String) -> Unit)? = null,
    private val askInput: ((s: String) -> Unit)? = null
) {
    private val error = Error(context)
    private val variableStack = ArrayList<Variable>()
    private val functionStack = ArrayList<Function>()
    var isTerminated = false
    var isReadyToExecute = false
    var listener: Listener? = null

    interface Listener {
        fun onFinish()
    }

    private fun showError(message: String) {
        debugLog?.invoke(message)
        error.show(message)
        SpidyScript.isTerminated = true
    }

    fun run(codeEncoded: String) {
        debugLog?.invoke("debug console initialized")
        val code = codeEncoded.base64Decode()
        if (code.startsWith("[null")) return
        isTerminated = false

        val arr = JSONArray(code)
        val statements = ArrayList<JSONObject>()
        for (i in 0 until  arr.length()) {
            val statement = arr.get(i)
            if (statement::class.simpleName != "Int") {
                statements.add(statement as JSONObject)
            }
        }
        execute(statements)
        Thread.sleep(1000)
        if (!isTerminated) {
            onUiThread {
                ignore {
                    context.vibrate()
                    if (canShowDialogs) {
                        context.newDialog().withTitle("Finished")
                            .withMessage("The script execution has been finished")
                            .withCancelable(false)
                            .withNeutralButton(context.getString(R.string.exit)) {
                                (context as BrowserActivity).finish()
                                it.dismiss()
                            }
                            .withPositiveButton(context.getString(R.string.cancel)) {
                                it.dismiss()
                            }
                            .create().show()
                    } else {
                        debugLog?.invoke("The script execution has been finished")
                        context.toast("The script execution has been finished")
                    }
                }
            }
        }
        isTerminated = true
        listener?.onFinish()
    }

    private fun execute(statements: List<JSONObject>) {
        for (statement in statements) {
            if (isTerminated) break
            debug("STATEMENT: $statement")
            executeStatement(statement)
        }
        while (!isReadyToExecute) Thread.sleep(100)
        debug("VARIABLE_STACK: $variableStack")
    }


    private fun executeStatement(statement: JSONObject) {
        while (!isReadyToExecute) Thread.sleep(100)
        when (statement.getString("cmd")) {
            "set_variable" -> setVariable(statement)
            "for_loop" -> runForLoop(statement)
            "if" -> runIfElse(statement)
            "go_to_website" -> goToWebsite(statement)
            "event_click" -> clickEvent(statement)
            "type" -> type(statement)
            "press_key" -> pressKey(statement)
            "event_wait_and_click" -> clickEvent(statement, shouldWait = true)
            "wait_for_element" -> waitForElement(statement)
            "for_loop_with_number" -> runForLoopWithNumber(statement)
            "function" -> setFunction(statement)
            "call_function" -> callFunction(statement)
            "scroll_bottom" -> scrollToBottom(statement)
            "scroll_top" -> scrollToTop(statement)
            "download_from_url" -> downloadFromUrl(statement)
            "wait_for" -> waitForSecs(statement)
            "debug" -> printDebug(statement)
            "open_a_new_tab" -> openNewTab(statement)
            "switch_tab" -> switchTab(statement)
            "close_tab" -> closeTab(statement)
            "append_text_to_var" -> appendTextToVar(statement)
            "write_to_a_file" -> writeToFile(statement)
        }
    }


    // blocks

    private fun writeToFile(statement: JSONObject) {
        val fileName = getNode(statement.getJSONObject("fileName")).value
        val value = getNode(statement.getJSONObject("input")).value
        debugLog?.invoke("file '${fileName}' written to directory ${Environment.DIRECTORY_DOWNLOADS}")
        onUiThread {
            context.toast("file '${fileName}' written to directory ${Environment.DIRECTORY_DOWNLOADS}")
        }

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
        val path = "$dir${File.separator}${fileName}"
        val destination = RandomAccessFile(path, "rw")
        destination.seek(0)
        destination.writeBytes(value)
        destination.close()

        IO.copyToSdCard(context, File(path), Environment.DIRECTORY_DOWNLOADS, "plain/text")
    }

    private fun appendTextToVar(statement: JSONObject) {
        val variableName = statement.getString("variable")
        val value = getNode(statement.getJSONObject("input")).value
        val variable = getVariable(variableName)

        debugLog?.invoke("appending text '$value' to variable '${variable}'")

        if (variable == null) {
            showError("InvalidVariable: variable '$variableName' doesn't exist")
            return
        }

        variable.value = variable.value.toString() + value
    }

    private fun closeTab(statement: JSONObject) {
        Thread.sleep(1000)
        val index = statement.getInt("index")
        debugLog?.invoke("closing tab at index '$index'")
        onUiThread {
            getBrowser?.invoke()?.closeTab(index)
            headless?.closeTab(index)
        }
    }

    private fun switchTab(statement: JSONObject) {
        Thread.sleep(1000)
        val index = statement.getInt("index")
        debugLog?.invoke("switching tab at index '$index'")
        onUiThread {
            getBrowser?.invoke()?.switchTab(index)
            headless?.switchTab(index)
        }
    }

    private fun openNewTab(statement: JSONObject) {
        Thread.sleep(1000)
        debugLog?.invoke("opening a new tab")
        onUiThread {
            getBrowser?.invoke()?.newTab()
            headless?.newTab()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun printDebug(statement: JSONObject) {
        val input = getNode(statement.getJSONObject("input")).value
        debugLog?.invoke("\n* $input\n")
        onUiThread {
            if (viewModel != null) {
                val s = if (viewModel.logs.value == null) "+ debug console initiated" else viewModel.logs.value
                viewModel.logs.value = "$s\n+ $input"
            }
        }
    }

    private fun waitForSecs(statement: JSONObject) {
        val secs = statement.getLong("secs")
        debugLog?.invoke("waiting for $secs seconds")
        Thread.sleep(secs * 1000)
    }

    private fun downloadFromUrl(statement: JSONObject) {
        val url = getNode(statement.getJSONObject("url")).value
        debugLog?.invoke("triggering download from url '$url'")
        // Create request for android download manager

        // Create request for android download manager
        val uri = Uri.parse(url)
        val fileName = File(uri.path ?: "download").name
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
        val request = DownloadManager.Request(uri)
        request.setAllowedNetworkTypes(
            DownloadManager.Request.NETWORK_WIFI or
                    DownloadManager.Request.NETWORK_MOBILE
        )

        // set title and description
        request.setTitle(fileName)
        request.setDescription("download triggered by spidy automation")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            fileName
        )
        request.setMimeType("*/*")
        downloadManager?.enqueue(request)
    }

    private fun scrollToTop(statement: JSONObject) {
        debugLog?.invoke("scrolling to top")
        getWebView()?.js("""
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        """.trimIndent())
    }

    private fun scrollToBottom(statement: JSONObject) {
        debugLog?.invoke("scrolling to bottom")
        getWebView()?.js("""
            window.scrollTo({
                top: document.body.scrollHeight,
                behavior: 'smooth'
            });
        """.trimIndent())
    }

    private fun callFunction(statement: JSONObject) {
        val funcName = statement.getString("name")
        var isFuncExists = false
        for (func in functionStack) {
            if (func.name == funcName) {
                for (state in func.statements) {
                    if (SpidyScript.isTerminated) break
                    executeStatement(state)
                }
                isFuncExists = true
                break
            }
        }
        if (!isFuncExists) showError("InvalidFunction: function $funcName does not exists")
    }

    private fun setFunction(statement: JSONObject) {
        val funcName = statement.getString("name")
        val statements = ArrayList<JSONObject>()
        val states = statement.getJSONArray("statements")
        for (i in 0 until states.length()) statements.add(states.getJSONObject(i))

        functionStack.add(Function(funcName, statements))
    }

    private fun runForLoopWithNumber(statement: JSONObject) {
        val range = statement.getString("from").toFloat().toInt() .. statement.getString("to").toFloat().toInt()
        val by = statement.getString("by").toFloat().toInt()
        val innerStatements = ArrayList<JSONObject>()
        val innerStatementsObj = statement.getJSONArray("statements")
        for (j in 0 until innerStatementsObj.length()) {
            innerStatements.add(innerStatementsObj.getJSONObject(j))
        }
        val indexVariableName = statement.getString("index_variable_name")
        for (i in range step by) {
            updateVariable( indexVariableName, Variable(indexVariableName, "number", i.toString()) )
            var isBreak = false
            for (state in innerStatements) {
                if (SpidyScript.isTerminated) {
                    isBreak = true
                    break
                }
                if (state.getString("cmd") == "break_or_continue") {
                    if (state.getString("action") == "BREAK_OUT") {
                        isBreak = true
                        break
                    } else if (state.getString("action") == "CONTINUE") {
                        break
                    }
                }
                executeStatement(state)
            }
            if (isBreak) break
        }
    }

    private fun waitForElement(statement: JSONObject) {
        val input = getNode(statement.getJSONObject("input"))
        if (input.type != "element" && input.type != "all_elements")  {
            showError("InvalidElement: an HTML element(s) required but found '${input.type}'")
            return
        }
        waitForElements(JSONObject(input.value))
    }

    private fun pressKey(statement: JSONObject) {
        debugLog?.invoke("pressing key ${statement.getString("name")}")
        when (statement.getString("name")) {
            "ENTER" -> {
                getWebView()?.apply {
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0))
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0))
                }
            }
            "SHIFT" -> {
                getWebView()?.apply {
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0))
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, 0))
                }
            }
            "CTRL" -> {
                getWebView()?.apply {
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT, 0))
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT, 0))
                }
            }
            "CAPS_LOCK" -> {
                getWebView()?.apply {
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CAPS_LOCK, 0))
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CAPS_LOCK, 0))
                }
            }
            "TAB" -> {
                getWebView()?.apply {
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB, 0))
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB, 0))
                }
            }
            "ESC" -> {
                getWebView()?.apply {
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE, 0))
                    dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE, 0))
                }
            }
        }
    }

    private fun type(statement: JSONObject) {
        val input = getNode(statement.getJSONObject("input"))
        debugLog?.invoke("typing '${input.value}'")
        val chars = input.value.toCharArray()
        val charMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        val events = charMap.getEvents(chars)
        var isTypingDone = false
        getWebView()?.apply {
            for (e in events) {
                dispatchKeyEvent(e)
                Thread.sleep(10)
            }
            isTypingDone = true
        }

        while (!isTypingDone) Thread.sleep(100)
    }

    private fun clickEvent(statement: JSONObject, shouldWait: Boolean = false) {
        val input = getNode(statement.getJSONObject("input"))

        if (input.type != "element" && input.type != "all_elements")  {
            showError("InvalidElement: an HTML element(s) required but found '${input.type}'")
            return
        }

        if (shouldWait) waitForElements(JSONObject(input.value))
        val isEverythingToSelect = input.type == "all_elements"
        debugLog?.invoke("clicking on an element")
        var isClickDone = false
        debug("HELLO: ${input.value}, isEverythingToSelect: $isEverythingToSelect")
        getWebView()?.injectScript("""
            var els = spidy.getElements(${input.value});
            if ($isEverythingToSelect) {
                var node = ${input.value};
                els = document.querySelectorAll(node.selector);
            }
            for (var i = 0; i < els.length; i++) {
                var element = els[i];
                if (element.tagName.toLowerCase() == "svg") {
                    element.parentElement.style.border = "1px solid yellow";
                    element.parentElement.focus();
                    element.parentElement.click();
                } else {
                    element.style.border = "1px solid yellow";
                    element.focus();
                    element.click();
                }
            }
        """.trimIndent()
        ) {
            isClickDone = true
        }

        while (!isClickDone) Thread.sleep(100)
    }

    private fun runIfElse(statement: JSONObject) {
        val condition = getNode(statement.getJSONObject("condition")).value.toBoolean()
        if (condition) {
            val innerStatements = ArrayList<JSONObject>()
            val innerStatementsObj = statement.getJSONArray("statements")
            for (j in 0 until innerStatementsObj.length()) {
                innerStatements.add(innerStatementsObj.getJSONObject(j))
            }
            execute(innerStatements)
        }
    }

    private fun runForLoop(statement: JSONObject) {
        val input = getNode(statement.getJSONObject("input"))
        if (input.type != "element" && input.type != "text_array" && input.type != "number_array" && input.type != "all_elements") {
            showError("TypeError: '${input.type}' is not iterable")
            return
        }
        val type = when (input.type) {
            "element" -> "element"
            "text_array" -> "text"
            "number_array" -> "number"
            else -> "text"
        }

        val iterator = if (input.type == "element") {
            var arr: JSONArray? = null
            getWebView()?.injectScript("""
                return spidy.getVElements(${input.value});
            """.trimIndent()
            ) {
                arr = JSONArray(it)
            }
            while (arr == null) Thread.sleep(100)
            arr!!
        } else if (input.type == "all_elements") {
            var arr: JSONArray? = null
            getWebView()?.injectScript("""
                return spidy.createAllElements(${input.value});
            """.trimIndent()
            ) {
                arr = JSONArray(it)
            }
            while (arr == null) Thread.sleep(100)
            arr!!
        } else {
            JSONArray(input.value)
        }

        val innerStatements = ArrayList<JSONObject>()
        val innerStatementsObj = statement.getJSONArray("statements")
        for (j in 0 until innerStatementsObj.length()) {
            innerStatements.add(innerStatementsObj.getJSONObject(j))
        }
        val indexVariableName = statement.getString("index_variable_name")
        for (i in 0 until iterator.length()) {
            var isBreak = false
            if (isTerminated) break
            for (state in innerStatements) {
                if (isTerminated) break
                if (state.getString("cmd") == "break_or_continue") {
                    if (state.getString("action") == "BREAK_OUT") {
                        isBreak = true
                        break
                    } else if (state.getString("action") == "CONTINUE") {
                        continue
                    }
                }

                updateVariable(
                    indexVariableName,
                    Variable(indexVariableName, type, iterator.get(i).toString())
                )
                executeStatement(state)
            }
            if (isBreak) break
        }
    }

    private fun getTagName(statement: JSONObject, index: Int = 0): String {
        debugLog?.invoke("getting tag name from an element")
        val element = statement.getJSONObject("element")
        if (element.getString("cmd") == "get_variable") {
            val variable = getVariable(element.getString("name"))
            if (variable == null || variable.type != "element") {
                showError("InvalidElement: variable doesn't exist or the variable is not an HTML element")
                return ""
            }
        }

        val value = when {
            element.getString("cmd") == "get_variable" -> {
                val variable = getVariable(element.getString("name"))
                (variable!!.value as JSONArray).getJSONObject(index)
            }
            element.getString("cmd") == "get_html_elements" -> {
                element.getJSONObject("node")
            }
            else -> {
                showError("InvalidElement: value is not an HTML element")
                return ""
            }
        }

        var tagName: String? = null

        getWebView()?.injectScript("""
                var el = document.querySelectorAll("${value.getString("selector")}")[${value.getInt(
                "elIndex"
            )}];
                if (el == undefined) {
                    return "";
                }
                
                var els = spidy.createSimilarElements(spidy.generateTree(el), el);
                els.unshift({selector: "${value.getString("selector")}", elIndex: ${value.getInt("elIndex")}});
                var element = document.querySelectorAll(els[$index].selector)[els[$index].elIndex];
                element.style.border = "1px solid yellow";
                return element.tagName.toLowerCase();
            """.trimIndent()
        ) {
            tagName = it
        }

        while (tagName == null) Thread.sleep(100)

        return tagName!!
    }

    private fun getTextContent(statement: JSONObject, index: Int = 0): String {
        debugLog?.invoke("getting text content from an element")
        val element = statement.getJSONObject("element")
        if (element.getString("cmd") == "get_variable") {
            val variable = getVariable(element.getString("name"))
            if (variable == null || variable.type != "element") {
                showError("InvalidElement: variable doesn't exist or the variable is not an HTML element")
                return ""
            }
        }

        val value = when {
            element.getString("cmd") == "get_variable" -> {
                val variable = getVariable(element.getString("name"))
                (variable!!.value as JSONArray).getJSONObject(index)
            }
            element.getString("cmd") == "get_html_elements" -> {
                element.getJSONObject("node")
            }
            else -> {
                showError("InvalidElement: value is not an HTML element")
                return ""
            }
        }

        var text: String? = null


        getWebView()?.injectScript("""
                var el = document.querySelectorAll("${value.getString("selector")}")[${value.getInt("elIndex")}];
                if (el == undefined) {
                    return "";
                }
                
                var els = spidy.createSimilarElements(spidy.generateTree(el), el);
                els.unshift({selector: "${value.getString("selector")}", elIndex: ${value.getInt("elIndex")}});
                var element = document.querySelectorAll(els[$index].selector)[els[$index].elIndex];
                element.style.border = "1px solid yellow";
                return element.textContent.trim();
            """.trimIndent()
        ) {
            text = it
        }

        while (text == null) Thread.sleep(100)

        return text!!
    }

    private fun ask(statement: JSONObject): String {
        debugLog?.invoke("asking for user input")
        var inputValue: String? = null

        onUiThread {
            if (headless == null) {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(statement.getString("title"))
                val v = LayoutInflater.from(context).inflate(R.layout.layout_edittext, null)
                val input: EditText = v.findViewById(R.id.editText)
                builder.setView(input)
                builder.setPositiveButton("OK") { dialog, which ->
                    inputValue = input.text.toString().replace("\n", "\\n")
                }
                builder.setNegativeButton("Cancel") { dialog, _ ->
                    inputValue = ""
                    dialog.cancel()
                }

                builder.show()
            } else {
                askInput!!.invoke(statement.getString("title"))
            }
        }
        if (headless != null) {
            while (HeadlessService.askValue == null) Thread.sleep(100)
            inputValue = HeadlessService.askValue
            HeadlessService.askValue = null
        }
        while (inputValue == null) {
            Thread.sleep(100)
        }
        return inputValue!!
    }

    private fun goToWebsite(statement: JSONObject) {
        val node = getNode(statement.getJSONObject("input"))
        debugLog?.invoke("going to website '${node.value}'")
        if (node.value.startsWith("http://") || node.value.startsWith("https://")) {
            onUiThread { getWebView()?.loadUrl(node.value) }
        } else {
            onUiThread { getWebView()?.loadUrl("http://${node.value}") }
        }
        isReadyToExecute = false
        Thread.sleep(1000)
    }

    private fun setVariable(statement: JSONObject) {
        val variableName = statement.getString("name")
        val variable = getVariable(variableName)
        val node = getNode(statement.getJSONObject("input"))
        if (variable == null) {
            variableStack.add(Variable(variableName, node.type, node.value))
        } else {
            variable.type = node.type
            variable.value = node.value
        }
    }

    private fun getVariable(variableName: String): Variable? {
        for (variable in variableStack) {
            if (variable.name == variableName) {
                return variable
            }
        }
        return null
    }

    private fun updateVariable(variableName: String, variable: Variable) {
        var isExist = false
        for (i in variableStack.indices) {
            if (variableName == variableStack[i].name) {
                isExist = true
                variableStack[i] = variable
                break
            }
        }
        if (!isExist) {
            variableStack.add(variable)
        }
    }

    private fun getAttributes(statement: JSONObject): JSONArray {
        val element = statement.getJSONObject("element")
        val attrName = statement.getString("attr_name")
        val input = getNode(element)

        if (input.type != "element" && input.type != "all_elements") {
            showError("InvalidElement: require an HTML element but found '${input.type}'")
            return JSONArray("[]")
        }

        val isEverythingToSelect = input.type == "all_elements"

        val el = JSONObject(input.value)

        var attrs: JSONArray? = null
        debug("el: $el")
        debug("isEverythingToSelect: $isEverythingToSelect")
        getWebView()?.injectScript("""
                var els = [];
                if ($isEverythingToSelect) {
                    els = spidy.createAllElements($el);
                } else {
                    var el = document.querySelectorAll("${el.getString("selector")}")[${el.getInt("elIndex")}];
                    if (el == undefined) {
                        return "";
                    }
                    
                    var els = spidy.createSimilarElements(spidy.generateTree(el), el);
                    els.unshift({selector: "${el.getString("selector")}", elIndex: ${el.getInt("elIndex")}});
                }
                var attrs = [];
                for (var i = 0; i < els.length; i++) {
                    var element = document.querySelectorAll(els[i].selector)[els[i].elIndex];
                    element.style.border = "1px solid yellow";
                    if ("$attrName" == "src") {
                        attrs.push(element.currentSrc);
                    } else if ("$attrName" == "action") {
                        attrs.push(element.action);
                    } else if ("$attrName" == "href") {
                        attrs.push(element.href);
                    } else {
                        attrs.push(element.getAttribute("$attrName"));
                    }
                }
                return attrs;
            """.trimIndent()
        ) {
            attrs = JSONArray(it)
        }

        while(attrs == null) Thread.sleep(100)
        return attrs!!
    }

    private fun getAttribute(statement: JSONObject, index: Int = 0): String {
        val element = statement.getJSONObject("element")
        val attrName = statement.getString("attr_name")
        debugLog?.invoke("getting attribute '${attrName}' from an element")
        val input = getNode(element)

        if (input.type != "element" && input.type != "all_elements") {
            showError("InvalidElement: require an HTML element but found '${input.type}'")
            return ""
        }

        val el = JSONObject(input.value)
        val isEverythingToSelect = input.type == "all_elements"

        var attrValue: String? = null

        getWebView()?.injectScript("""
                var element = document.querySelectorAll("${el.getString("selector")}")[${el.getInt("elIndex")}];
                if ($isEverythingToSelect) {
                    element = document.querySelector("${el.getString("selector")}");
                }
                if (element == undefined) {
                    return "";
                }
                
                element.style.border = "1px solid yellow";
                if ("$attrName" == "src") {
                    return element.currentSrc;
                } else if ("$attrName" == "action") {
                    return element.action;
                } else if ("$attrName" == "href") {
                    return element.href;
                } else {
                    return element.getAttribute("$attrName");
                }
            """.trimIndent()
        ) {
            attrValue = it
        }

        while (attrValue == null) {
            Thread.sleep(100)
        }

        return attrValue!!
    }

    private fun getCondition(statement: JSONObject): Boolean {
        val firstCondition = getNode(statement.getJSONObject("first_condition"))
        val secondCondition = getNode(statement.getJSONObject("second_condition"))
        var bool: Boolean? = null

        val val1 = if (firstCondition.type == "text") "\"${firstCondition.value}\"" else firstCondition.value
        val val2 = if (secondCondition.type == "text") "\"${secondCondition.value}\"" else secondCondition.value

        val operator = when (statement.getString("operator")) {
            "EQUAL" -> "=="
            "NOT_EQUAL" -> "!="
            "LESS_THAN" -> "<"
            "LESS_THAN_OR_EQUAL" -> "<="
            "GTREATER_THAN" -> ">"
            "GTREATER_THAN_OR_EQUAL" -> ">="
            else -> ""
        }

        getWebView()?.js("""
                    return $val1 $operator $val2;
                """.trimIndent()) {
            bool = it.toBoolean()
        }

        while (bool == null) Thread.sleep(100)
        return bool!!
    }

    private fun getBoolean(statement: JSONObject): Boolean {
        return statement.getString("input").toString().toBoolean()
    }

    private fun getTrimSpace(statement: JSONObject): String {
        debugLog?.invoke("trimming space of text")
        return getNode(statement.getJSONObject("text")).value.trim()
    }

    private fun getNumber(statement: JSONObject): String {
        return statement.getString("input")
    }

    private fun getCalcValue(statement: JSONObject): String {
        val inp1 = getNode(statement.getJSONObject("first"))
        val inp2 = getNode(statement.getJSONObject("second"))
        val operator = when (statement.getString("operator")) {
            "PLUS" -> "+"
            "MINUS" -> "-"
            "MULTIPLY" -> "*"
            "DIVISION" -> "/"
            else -> ""
        }
        if (inp1.type != "number" || inp2.type != "number") {
            showError("TypeError: unsupported operand type(s) for $operator: 'number' and '${if (inp1.type != "number") inp1.type else inp2.type}'")
            return ""
        }
        val isFloating = (inp1.value.contains(".") || inp2.value.contains("."))
        return when(operator) {
            "+" -> {
                if (isFloating) {
                    (inp1.value.toFloat() + inp2.value.toFloat()).toString()
                } else {
                    (inp1.value.toFloat() + inp2.value.toFloat()).toInt().toString()
                }
            }
            "-" -> {
                if (isFloating) {
                    (inp1.value.toFloat() - inp2.value.toFloat()).toString()
                } else {
                    (inp1.value.toFloat() - inp2.value.toFloat()).toInt().toString()
                }
            }
            "*" -> {
                if (isFloating) {
                    (inp1.value.toFloat() * inp2.value.toFloat()).toString()
                } else {
                    (inp1.value.toFloat() * inp2.value.toFloat()).toInt().toString()
                }
            }
            "/" -> {
                if (isFloating) {
                    (inp1.value.toFloat() / inp2.value.toFloat()).toString()
                } else {
                    (inp1.value.toFloat() / inp2.value.toFloat()).toInt().toString()
                }
            }
            else -> ""
        }
    }

    private fun getJoinText(statement: JSONObject): String {
        debugLog?.invoke("joining two text")
        return getNode(statement.getJSONObject("text1")).value + getNode(statement.getJSONObject("text2")).value
    }

    private fun getTextContains(statement: JSONObject): Boolean {
        val text = getNode(statement.getJSONObject("text")).value
        val toFind = getNode(statement.getJSONObject("value")).value
        if (text.contains(toFind)) {
            return true
        }
        return false
    }

    private fun isElementExist(statement: JSONObject): Boolean {
        val input = getNode(statement.getJSONObject("input"))
        if (input.type != "element" && input.type != "all_elements")  {
            showError("InvalidElement: an HTML element(s) required but found '${input.type}'")
            return false
        }

        val isEverythingToSelect = input.type == "all_elements"
        var isAllExist: Boolean? = null
        getWebView()?.injectScript("""
            var els = spidy.getElements(${input.value});
            if ($isEverythingToSelect) {
                var node = ${input.value};
                if (document.querySelectorAll(node.selector).length == 0) {
                    return false;
                }
            }
            for (var i = 0; i < els.length; i++) {
                if (els[i] == undefined) {
                    return false;
                }
            }
            return true;
        """.trimIndent()
        ) {
            isAllExist = it.toBoolean()
        }

        while(isAllExist == null) Thread.sleep(100)
        return isAllExist!!
    }

    private fun getRegexResult(statement: JSONObject): JSONArray {
        val regex = Regex(statement.getString("regex").base64Decode())
        val text = getNode(statement.getJSONObject("text")).value
        val founds = ArrayList<String>()
        regex.findAll(text).forEach {
            founds.add(it.value)
        }
        return JSONArray(founds)
    }

    private fun getPageSource(statement: JSONObject): String {
        var source: String? = null
        getWebView()?.js("""
            return "<html>" + document.getElementsByTagName('html')[0].innerHTML + "</html>";
        """.trimIndent()) {
            source = it
        }

        while (source == null) Thread.sleep(100)
        return source!!
    }


    // helper

    private fun waitForElements(element: JSONObject, totalWaitedMills: Long = 0) {

        var isElementsExists: Boolean? = null
        if (totalWaitedMills == 0L) {
            debugLog?.invoke("waiting for an element")
        }
        getWebView()?.injectScript("""
            var els = spidy.getElements($element);
            for (var i = 0; i < els.length; i++) {
                if (els[i] == undefined) {
                    return false;
                }
            }
            return true;
        """.trimIndent()
        ) {
            isElementsExists = it.toBoolean()
        }
        while (isElementsExists == null) Thread.sleep(100)
        if (!isElementsExists!!) {
            if (totalWaitedMills/1000 < 30) {
                waitForElements(element, totalWaitedMills + 100)
            }
        }
    }

//    private fun waitForSpidy() {
//        var isReady: Boolean? = null
//        getWebView()?.js("""
//            return window.spidy != undefined;
//        """.trimIndent()) {
//            isReady = it.toBoolean()
//        }
//
//        while (isReady == null) Thread.sleep(100)
//
//        if (!isReady!!) {
//            waitForSpidy()
//        }
//    }

    private fun getNode(statement: JSONObject): Node {
        return when (statement.getString("cmd")) {
            "get_text" -> Node(statement.getString("text"), "text")
            "get_variable" -> {
                val v = getVariable(statement.getString("name"))

                if (v != null) {
                    return Node(v.value.toString(), v.type)
                }
                return Node("", "text")
            }
            "get_html_elements" -> Node(statement.getJSONObject("node").toString(), "element")
            "get_html_element_by_selector" -> Node(statement.getJSONObject("node").toString(), "all_elements")
            "get_attribute" -> Node(getAttribute(statement), "text")
            "get_attributes" -> Node(getAttributes(statement).toString(), "text_array")
            "get_text_content" -> Node(getTextContent(statement), "text")
            "get_tag_name" -> Node(getTagName(statement), "text")
            "get_input" -> Node(ask(statement), "text")
            "get_condition" -> Node(getCondition(statement).toString(), "boolean")
            "get_boolean" -> Node(getBoolean(statement).toString(), "boolean")
            "trim_space" -> Node(getTrimSpace(statement), "text")
            "get_number" -> Node(getNumber(statement), "number")
            "get_calculation" -> Node(getCalcValue(statement), "number")
            "get_join_text" -> Node(getJoinText(statement), "text")
            "text_contains" -> Node(getTextContains(statement).toString(), "boolean")
            "is_element_exist" -> Node(isElementExist(statement).toString(), "boolean")
            "regex" -> Node(getRegexResult(statement).toString(), "text_array")
            "get_page_source" -> Node(getPageSource(statement), "text")
            else -> Node("", "text")
        }
    }
}