package app.spidy.spidy.interpreter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.webkit.WebView
import android.widget.EditText
import app.spidy.browser.controllers.Browser
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.spider.data.Condition
import app.spidy.spider.data.Function
import app.spidy.spidy.data.Variable
import app.spidy.spidy.R
import app.spidy.spidy.utils.IO
import app.spidy.spidy.utils.js
import app.spidy.spidy.utils.vibrate
import app.spidy.spidy.viewmodels.BrowserActivityViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile


class SpidyScript (
    private val context: Context,
    private val code: String,
    private val viewModel: BrowserActivityViewModel,
    private val getBrowser: () -> Browser,
    private val getWebView: () -> WebView?
) {
    companion object {
        var isReadyToExecute = false
        var isPageStarted = false
        var isTerminated = false
    }

    private val variableStack = ArrayList<Variable>()
    private val functionStack = ArrayList<Function>()
    private val jsonStack = ArrayList<Variable>()

    private val error = Error(context)

    fun run() {
        if (code.startsWith("[null")) return
        debug(code)
        isTerminated = false

        try {
            val arr = JSONArray(code.replace("\\\"", "\"").replace("\\\\\"", "\\\""))
            val lastVarIndex = arr.getInt(arr.length() - 1)
            val statements = ArrayList<JSONObject>()
            for (i in 0 until  arr.length()) {
                val statement = arr.get(i)
                if (statement::class.simpleName != "Int") {
                    statements.add(statement as JSONObject)
                }
            }
            execute(statements)
        } catch (e: Exception) {
            debug(e)
            val ex = e.toString().split(": ").toMutableList()
            val exType = ex.removeAt(0).split(".")
            showError("${exType.last()}: ${ex.joinToString(": ")}")
        }
        Thread.sleep(1000)
        if (!isTerminated) {
            onUiThread {
                context.vibrate()
                context.newDialog().withTitle("Finished")
                    .withMessage("The script execution has been finished")
                    .withCancelable(false)
                    .withPositiveButton(context.getString(R.string.cancel)) {
                        it.dismiss()
                    }
                    .create().show()
            }
        }
    }

    private fun execute(statements: List<JSONObject>) {
        for (statement in statements) {
            if (isTerminated) break
            executeStatement(statement)
        }
        while (!isReadyToExecute) Thread.sleep(100)
    }

    private fun executeStatement(statement: JSONObject) {
        while (!isReadyToExecute) Thread.sleep(100)

        when (statement.getString("cmd")) {
            "set_variable" -> setVariable(statement)
            "get_input" -> ask(statement)
            "for_loop" -> runForLoop(statement)
            "if" -> runIfElse(statement)
            "go_to_website" -> goToWebsite(statement)
            "event_click" -> clickEvent(statement)
            "type" -> type(statement)
            "press_key" -> pressKey(statement)
            "event_click_if_exists" -> clickIfExistsEvent(statement)
            "event_wait_and_click" -> waitAndClick(statement)
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
            "create_json_object" -> createJsonObject(statement)
            "create_json_array" -> createJsonArray(statement)
            "insert_to_json_array" -> insertToJsonArray(statement)
            "insert_to_json_object" -> insertToJsonObject(statement)
        }
    }

    private fun showError(message: String) {
        error.show(message)
        isTerminated = true
    }



    // Top level commands

    private fun waitAndClick(statement: JSONObject) {
        clickEvent(statement, shouldWait = true)
    }

    private fun insertToJsonObject(statement: JSONObject) {
        debug("VALUE_STATEMENT")
        val variableName = statement.getString("variable")
        val valueStatement = statement.getJSONObject("value")
        val key = getNodeValue(statement.getJSONObject("key")).value
        val node = getNodeValue(valueStatement)
        for (i in jsonStack.indices) {
            if (variableName == jsonStack[i].name) {
                if (valueStatement.getString("cmd") == "get_variable") {
                    val json = getJson(valueStatement.getString("name"))
                    if (json != null && json.type == "json") {
                        if (json.value::class.simpleName == "ArrayList") {
                            (jsonStack[i].value as HashMap<String, Any>)[key] = JSONArray(json.value as ArrayList<Any>)
                        } else {
                            (jsonStack[i].value as HashMap<String, Any>)[key] = JSONObject((json.value as HashMap<String, Any>).toMap())
                        }
                        break
                    }
                }

                if (node.type == "text") {
                    (jsonStack[i].value as HashMap<String, Any>)[key] = node.value
                } else if (node.type == "number") {
                    if (node.value.contains(".")) {
                        (jsonStack[i].value as HashMap<String, Any>)[key] = node.value.toFloat()
                    } else {
                        (jsonStack[i].value as HashMap<String, Any>)[key] = node.value.toInt()
                    }
                } else {
                    (jsonStack[i].value as HashMap<String, Any>)[key] = node.value
                }
                break
            }
        }
    }

    private fun insertToJsonArray(statement: JSONObject) {
        val variableName = statement.getString("variable")
        val valueStatement = statement.getJSONObject("value")
        val node = getNodeValue(valueStatement)

        for (i in jsonStack.indices) {
            if (variableName == jsonStack[i].name) {
                if (valueStatement.getString("cmd") == "get_variable") {
                    val json = getJson(valueStatement.getString("name"))
                    if (json != null && json.type == "json") {
                        if (json.value::class.simpleName == "ArrayList") {
                            (jsonStack[i].value as ArrayList<Any>).add(JSONArray(json.value as ArrayList<Any>))
                        } else {
                            (jsonStack[i].value as ArrayList<Any>).add(JSONObject((json.value as HashMap<String, Any>).toMap()))
                        }
                        break
                    }
                }

                if (node.type == "text") {
                    (jsonStack[i].value as ArrayList<Any>).add(node.value)
                } else if (node.type == "number") {
                    if (node.value.contains(".")) {
                        (jsonStack[i].value as ArrayList<Any>).add(node.value.toFloat())
                    } else {
                        (jsonStack[i].value as ArrayList<Any>).add(node.value.toInt())
                    }
                } else {
                    (jsonStack[i].value as ArrayList<Any>).add(node.value)
                }
                break
            }
        }
    }

    private fun createJsonArray(statement: JSONObject) {
        setJson(statement)
    }

    private fun createJsonObject(statement: JSONObject) {
        setJson(statement)
    }

    private fun writeToFile(statement: JSONObject) {
        val fileName = getNodeValue(statement.getJSONObject("fileName")).value
        val value = getNodeValue(statement.getJSONObject("value")).value

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
        val value = getNodeValue(statement.getJSONObject("value")).value
        val variable = getVariable(variableName)

        if (variable == null) {
            showError("InvalidVariable: variable '$variableName' doesn't exist")
            return
        }

        if (variable.type != "text") {
            showError("InvalidVariable: variable '$variableName' is not type of 'text'")
            return
        }

        variable.value = variable.value.toString() + value
    }

    private fun closeTab(statement: JSONObject) {
        Thread.sleep(1000)
        val index = statement.getInt("index")
        onUiThread { getBrowser().closeTab(index) }
    }

    private fun switchTab(statement: JSONObject) {
        Thread.sleep(1000)
        val index = statement.getInt("index")
        onUiThread { getBrowser().switchTab(index) }
    }

    private fun openNewTab(statement: JSONObject) {
        Thread.sleep(1000)
        onUiThread { getBrowser().newTab() }
    }

    @SuppressLint("SetTextI18n")
    private fun printDebug(statement: JSONObject) {
        var value = getNodeValue(statement.getJSONObject("value")).value
        debug("PRINT_DEBUG: $value")
        if (value.contains("\"selector\"") && value.contains("\"iframeIndex\"") && value.startsWith("[")) {
            val tagName = JSONArray(value).getJSONObject(0).getString("selector").split(" ").last().split(":").first()
            value = "HTMLElements($tagName)"
        }
        onUiThread {
            val s = if (viewModel.logs.value == null) "+ debug console initiated" else viewModel.logs.value
            viewModel.logs.value = "$s\n+ $value"
        }
    }

    private fun waitForSecs(statement: JSONObject) {
        val secs = statement.getLong("secs")
        Thread.sleep(secs * 1000)
    }

    private fun downloadFromUrl(statement: JSONObject) {
        val url = getNodeValue(statement.getJSONObject("url")).value
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
        getWebView()?.js("""
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        """.trimIndent())
    }

    private fun scrollToBottom(statement: JSONObject) {
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
                    if (isTerminated) break
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
        val innerStatements = ArrayList<JSONObject>()
        val innerStatementsObj = statement.getJSONArray("statements")
        for (j in 0 until innerStatementsObj.length()) {
            innerStatements.add(innerStatementsObj.getJSONObject(j))
        }

        val indexVariableName = statement.getString("index_variable_name")
        for (i in range) {
            updateVariable( indexVariableName, Variable(indexVariableName, "number", i.toString()) )
            var isBreak = false
            for (state in innerStatements) {
                if (isTerminated) {
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
        val element = statement.getJSONObject("element")
        if (element.getString("cmd") == "get_variable") {
            val variable = getVariable(element.getString("name"))
            if (variable == null || variable.type != "elements") {
                showError("InvalidVariable: variable doesn't exist or the variable is not an HTML element")
                return
            }
        }

        val value = when {
            element.getString("cmd") == "get_variable" -> {
                val variable = getVariable(element.getString("name"))
                variable!!.value as JSONArray
            }
            element.getString("cmd") == "get_html_elements" -> {
                element.getJSONArray("selectors")
            }
            else -> {
                showError("InvalidElement: value is not an HTML element")
                return
            }
        }
        waitForEls(value)
    }

    private fun clickIfExistsEvent(statement: JSONObject) {
        val element = statement.getJSONObject("element")
        if (element.getString("cmd") == "get_variable") {
            val variable = getVariable(element.getString("name"))
            if (variable == null || variable.type != "elements") {
                showError("InvalidElement: variable doesn't exist or the variable is not an HTML element")
                return
            }
        }

        val value = when {
            element.getString("cmd") == "get_variable" -> {
                val variable = getVariable(element.getString("name"))
                variable!!.value as JSONArray
            }
            element.getString("cmd") == "get_html_elements" -> {
                element.getJSONArray("selectors")
            }
            else -> {
                showError("InvalidElement: value is not an HTML element")
                return
            }
        }

        var isClickDone = false
        var els = "["

        for (i in 0 until value.length()) {
            val el = value.getJSONObject(i)
            els += "{selector: '${el.getString("selector")}', iframeIndex: ${el.getInt("iframeIndex")}, elIndex: ${el.getInt("elIndex")}},"
        }
        els = els.dropLast(1) + "]"

//        element.dispatchEvent(new MouseEvent('click'));
        getWebView()?.js("""
                var els = $els;
                var iframes = document.querySelectorAll("iframe");
                for (var i = 0; i < els.length; i++) {
                    var iframeIndex = els[i].iframeIndex;
                    var element = document.querySelectorAll(els[i].selector)[els[i].elIndex];
                    if (iframeIndex != -1) {
                        element = iframes[iframeIndex].contentDocument.querySelectorAll(els[i].selector)[els[i].elIndex];
                    }
                    if (element != undefined) {
                        if (element.tagName.toLowerCase() == "svg") {
                            element.parentElement.style.border = "1px solid yellow";
                            element.parentElement.click();
                            element.parentElement.focus();
                        } else {
                            element.style.border = "1px solid yellow";
                            element.click();
                            element.focus();
                        }
                    }
                }        
            """.trimIndent()) {
            isClickDone = true
        }

        while (!isClickDone) {
            Thread.sleep(100)
        }
    }

    private fun pressKey(statement: JSONObject) {
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
        debug("TYPING: ${getNodeValue(statement.getJSONObject("value"))}")
        val value = getNodeValue(statement.getJSONObject("value")).value.toCharArray()
        val charMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        val events = charMap.getEvents(value)

        getWebView()?.apply {
                for (e in events) {
                    dispatchKeyEvent(e)
                    Thread.sleep(10)
                }
            }
    }

    private fun clickEvent(statement: JSONObject, shouldWait: Boolean = false) {
        val element = statement.getJSONObject("element")
        if (element.getString("cmd") == "get_variable") {
            val variable = getVariable(element.getString("name"))
            if (variable == null || variable.type != "elements") {
                showError("InvalidElement: variable doesn't exist or the variable is not an HTML element")
                return
            }
        }

        val value = when {
            element.getString("cmd") == "get_variable" -> {
                val variable = getVariable(element.getString("name"))
                variable!!.value as JSONArray
            }
            element.getString("cmd") == "get_html_elements" -> {
                element.getJSONArray("selectors")
            }
            else -> {
                showError("InvalidElement: value is not an HTML element")
                return
            }
        }

        var isClickDone = false
        var els = "["

        for (i in 0 until value.length()) {
            val el = value.getJSONObject(i)
            els += "{selector: '${el.getString("selector")}', iframeIndex: ${el.getInt("iframeIndex")}, elIndex: ${el.getInt("elIndex")}},"
        }
        els = els.dropLast(1) + "]"

        if (shouldWait) {
            waitForEls(JSONArray(els))
        }

//        element.dispatchEvent(new MouseEvent('click'));
        getWebView()?.js("""
                var els = $els;
                var iframes = document.querySelectorAll("iframe");
                for (var i = 0; i < els.length; i++) {
                    var iframeIndex = els[i].iframeIndex;
                    var element = document.querySelectorAll(els[i].selector)[els[i].elIndex];
                    if (iframeIndex != -1) {
                        element = iframes[iframeIndex].contentDocument.querySelectorAll(els[i].selector)[els[i].elIndex];
                    }
                    if (element != undefined) {
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
                }        
            """.trimIndent()) {
            isClickDone = true
        }

        while (!isClickDone) {
            Thread.sleep(100)
        }


    }

    private fun goToWebsite(statement: JSONObject) {
        val nodeValue = getNodeValue(statement.getJSONObject("value"))
        debug(statement.getJSONObject("value"))
        if (nodeValue.value.startsWith("http://") || nodeValue.value.startsWith("https://")) {
            onUiThread { getWebView()?.loadUrl(nodeValue.value) }
        } else {
            onUiThread { getWebView()?.loadUrl("http://${nodeValue.value}") }
        }
        while (!isPageStarted) Thread.sleep(100)
    }

    private fun runIfElse(statement: JSONObject) {
        val conditions = statement.getJSONObject("condition")
        var canExecute: Boolean? = null
        if (conditions.getString("cmd") == "get_condition") {
            val firstCondition = getCondition(conditions.getJSONObject("first_condition"))
            val secondCondition = getCondition(conditions.getJSONObject("second_condition"))
            val con1 = if (firstCondition.type == "number") firstCondition.value else "'${firstCondition.value}'"
            val con2 =  if (secondCondition.type == "number") secondCondition.value else "'${secondCondition.value}'"
            val operator = when (conditions.getString("operator")) {
                "EQUAL" -> "=="
                "NOT_EQUAL" -> "!="
                "LESS_THAN" -> "<"
                "LESS_THAN_OR_EQUAL" -> "<="
                "GTREATER_THAN" -> ">"
                "GTREATER_THAN_OR_EQUAL" -> ">="
                else -> ""
            }
            debug("$con1 $operator $con2")
            getWebView()?.js("""
                return ($con1 $operator $con2);
            """.trimIndent()) {
                debug(it)
                canExecute = it.toBoolean()
            }
        } else {
            canExecute = getNodeValue(conditions).value.toBoolean()
        }

        while (canExecute == null) {
            Thread.sleep(100)
        }
        debug("CONDITION: ${getNodeValue(conditions).value}")
        debug("canExecute: $canExecute")
        if (canExecute!!) {
            val innerStatements = ArrayList<JSONObject>()
            val innerStatementsObj = statement.getJSONArray("statements")
            for (j in 0 until innerStatementsObj.length()) {
                innerStatements.add(innerStatementsObj.getJSONObject(j))
            }
            execute(innerStatements)
        }
    }

    private fun runForLoop(statement: JSONObject) {
        var type = "elements"
        debug("ATTR: $statement")
        val input = statement.getJSONObject("input")
        val elements = when {
            input.getString("cmd") == "get_variable" -> {
                val variable = getVariable(input.getString("name"))
                if (variable == null || variable.type != "elements" && variable.type != "array") {
                    showError("InvalidList: require an element list to loop through")
                    return
                }
                if (variable.type == "array") {
                    type = "array"
                }
                variable.value as JSONArray
            }
            input.getString("cmd") == "get_html_elements" -> {
                input.getJSONArray("selectors")
            }
            input.getString("cmd") == "get_attributes" -> {
                type = "array"
                getAttributes(input)
            }
            else -> {
                showError("InvalidList: require an element list to loop through")
                return
            }
        }

        val innerStatements = ArrayList<JSONObject>()
        val innerStatementsObj = statement.getJSONArray("statements")
        for (j in 0 until innerStatementsObj.length()) {
            innerStatements.add(innerStatementsObj.getJSONObject(j))
        }

        val indexVariableName = statement.getString("index_variable_name")
        for (i in 0 until elements.length()) {
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
                if (type == "elements") {
                    updateVariable(indexVariableName, Variable(indexVariableName, type, JSONArray("[" + elements.getJSONObject(i).toString() + "]")))
                } else {
                    updateVariable(indexVariableName, Variable(indexVariableName, type, elements.getString(i)))
                }
                executeStatement(state)
            }

            if (isBreak) break
        }
    }

    private fun setJson(statement: JSONObject) {
        val variableName = statement.getString("variable")
        var index = -1
        for (i in jsonStack.indices) {
            if (variableName == jsonStack[i].name) {
                index = i
                break
            }
        }
        if (index != -1) jsonStack.removeAt(index) else index = jsonStack.size
        when (statement.getString("cmd")) {
            "create_json_object" -> {
                jsonStack.add(index, Variable(variableName, "json", HashMap<String, Any>()))
            }
            "create_json_array" -> {
                jsonStack.add(index, Variable(variableName, "json", ArrayList<Any>()))
            }
        }
    }

    private fun setVariable(statement: JSONObject) {
        val value = statement.get("value")
        val variableName = statement.getString("name")
        when ((value as JSONObject).getString("cmd")) {
            "get_text" -> updateVariable(variableName, Variable(variableName, "text", value.getString("text")))
            "get_variable" -> {
                val variable = getVariable(value.getString("name"))
                if (variable == null) {
                    showError("UnknownVariable: ${value.getString("name")} is not defined")
                } else {
                    updateVariable(variable.name, Variable(variable.name, variable.type, variable.value))
                }
            }
            "get_html_elements" -> updateVariable(variableName, Variable(variableName, "elements", value.getJSONArray("selectors") ))
            "get_attribute" -> updateVariable(variableName, Variable(variableName, "text", getAttribute(value)))
            "get_attributes" -> updateVariable(variableName, Variable(variableName, "array", getAttributes(value)))
            "get_text_content" -> updateVariable(variableName, Variable(variableName, "text", getTextContent(value)))
            "get_tag_name" -> updateVariable(variableName, Variable(variableName, "text", getTagName(value)))
            "get_input" -> updateVariable(variableName, Variable(variableName, "text", ask(value)))
            "get_condition" -> {
                val firstCondition = getCondition(value.getJSONObject("first_condition"))
                val secondCondition = getCondition(value.getJSONObject("second_condition"))
                val operator = value.getString("operator")
                var bool = false

                val con1 = firstCondition.value
                val con2 = secondCondition.value

                when (operator) {
                    "EQUAL" -> if (con1 == con2) bool = true
                    "NOT_EQUAL" -> if (con1 != con2) bool = true
                    "LESS_THAN" -> if (con1 < con2) bool = true
                    "LESS_THAN_OR_EQUAL" -> if (con1 <= con2) bool = true
                    "GTREATER_THAN" -> if (con1 > con2) bool = true
                    "GTREATER_THAN_OR_EQUAL" -> if (con1 >= con2) bool = true
                }
                updateVariable(variableName, Variable(variableName, "text", bool.toString()))
            }
            "get_boolean" -> updateVariable(variableName, Variable(variableName, "text", getBoolean(value)))
            "trim_space" -> updateVariable(variableName, Variable(variableName, "text", getTrimSpace(value)))
            "get_number" -> updateVariable(variableName, Variable(variableName, "number", getNumber(value)))
            "get_calculation" -> updateVariable(variableName, Variable(variableName, "number", getCalculationValue(value)))
            "get_join_text" -> updateVariable(variableName, Variable(variableName, "text", getJoinText(value)))
            "text_contains" -> updateVariable(variableName, Variable(variableName, "text", getTextContains(value)))
        }
        debug(variableStack)
    }

    // Second level commands

    private fun getJoinText(statement: JSONObject): String {
        return getNodeValue(statement.getJSONObject("text1")).value + getNodeValue(statement.getJSONObject("text2")).value
    }

    private fun getCalculationValue(statement: JSONObject): String {
        val inp1 = getNodeValue(statement.getJSONObject("first"))
        val inp2 = getNodeValue(statement.getJSONObject("second"))
        val operator = when (statement.getString("operator")) {
            "PLUS" -> "+"
            "MINUS" -> "-"
            "MULTIPLY" -> "*"
            "DIVISION" -> "/"
            else -> ""
        }
        if (inp1.type != "number" || inp2.type != "number") {
            showError("TypeError: unsupported operand type(s) for $operator: 'number' and 'text'")
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

    private fun getNumber(statement: JSONObject): String {
        return statement.getString("value")
    }

    private fun getCondition(condition: JSONObject): Condition {
        val con = getNodeValue(condition)
        return Condition(con.value, con.type)
    }

    private fun ask(statement: JSONObject): String {
        var inputValue: String? = null

        onUiThread {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(statement.getString("title"))
            val input = EditText(context)

            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)
            builder.setPositiveButton("OK") { dialog, which ->
                inputValue = input.text.toString()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                inputValue = ""
                dialog.cancel()
            }

            builder.show()
        }
        while (inputValue == null) {
            Thread.sleep(100)
        }
        return inputValue!!
    }

    private fun getVariable(variableName: String): Variable? {
        for (variable in variableStack) {
            if (variable.name == variableName) {
                return variable
            }
        }
        return null
    }

    private fun getJson(variableName: String): Variable? {
        for (variable in jsonStack) {
            if (variable.name == variableName) {
                return variable
            }
        }
        return null
    }

    private fun getTagName(statement: JSONObject): String {
        val element = statement.getJSONObject("element")
        if (element.getString("cmd") == "get_variable") {
            val variable = getVariable(element.getString("name"))
            if (variable == null || variable.type != "elements") {
                showError("InvalidElement: variable doesn't exist or the variable is not an HTML element")
                return ""
            }
        }

        val value = when {
            element.getString("cmd") == "get_variable" -> {
                val variable = getVariable(element.getString("name"))
                (variable!!.value as JSONArray).getJSONObject(0)
            }
            element.getString("cmd") == "get_html_elements" -> {
                element.getJSONArray("selectors").getJSONObject(0)
            }
            else -> {
                showError("InvalidElement: value is not an HTML element")
                return ""
            }
        }

        var tagName: String? = null

        getWebView()?.js("""
                var iframeIndex = ${value.getInt("iframeIndex")};
                var element = document.querySelectorAll("${value.getString("selector")}")[${value.getInt("elIndex")}];
                if (iframeIndex != -1) {
                    var iframes = document.querySelectorAll("iframe");
                    element = iframes[iframeIndex].contentDocument.querySelectorAll("${value.getString("selector")}")[${value.getInt("elIndex")}];
                }
                if (element == undefined) {
                    return "";
                }
                element.style.border = "1px solid yellow";
                return element.tagName.toLowerCase();            
            """.trimIndent()) {
            debug("here")
            tagName = it
        }

        while (tagName == null) {
            Thread.sleep(100)
        }

        return tagName!!
    }

    private fun getTextContent(statement: JSONObject): String {
        val element = statement.getJSONObject("element")
        if (element.getString("cmd") == "get_variable") {
            val variable = getVariable(element.getString("name"))
            if (variable == null || variable.type != "elements") {
                showError("InvalidElement: variable doesn't exist or the variable is not an HTML element")
                return ""
            }
        }

        val value = when {
            element.getString("cmd") == "get_variable" -> {
                val variable = getVariable(element.getString("name"))
                (variable!!.value as JSONArray).getJSONObject(0)
            }
            element.getString("cmd") == "get_html_elements" -> {
                element.getJSONArray("selectors").getJSONObject(0)
            }
            else -> {
                showError("InvalidElement: value is not an HTML element")
                return ""
            }
        }

        var text: String? = null

        getWebView()?.js("""
                var iframeIndex = ${value.getInt("iframeIndex")};
                var element = document.querySelectorAll("${value.getString("selector")}")[${value.getInt("elIndex")}];
                if (iframeIndex != -1) {
                    var iframes = document.querySelectorAll("iframe");
                    element = iframes[iframeIndex].contentDocument.querySelectorAll("${value.getString("selector")}")[${value.getInt("elIndex")}];
                }
                if (element == undefined) {
                    return "";
                }
                element.style.border = "1px solid yellow";
                return element.textContent.trim();            
            """.trimIndent()) {
            debug("here")
            text = it
        }

        while (text == null) {
            Thread.sleep(100)
        }

        return text!!
    }

    private fun getAttributes(statement: JSONObject): JSONArray {
        val element = statement.getJSONObject("element")
        if (element.getString("cmd") == "get_variable") {
            val variable = getVariable(element.getString("name"))
            if (variable == null || variable.type != "elements") {
                showError("InvalidElement: variable doesn't exist or the variable is not an HTML element")
                return JSONArray("[]")
            }
        }

        val selectors = when {
            element.getString("cmd") == "get_variable" -> {
                val variable = getVariable(element.getString("name"))
                (variable!!.value as JSONArray)
            }
            element.getString("cmd") == "get_html_elements" -> {
                element.getJSONArray("selectors")
            }
            else -> {
                showError("InvalidElement: value is not an HTML element")
                return JSONArray("[]")
            }
        }
        val arrayList = ArrayList<String>()
        for (i in 0 until selectors.length()) {
            arrayList.add(getAttribute(statement, i))
        }
        return JSONArray(arrayList)
    }

    private fun getAttribute(statement: JSONObject, index: Int = 0): String {
        val element = statement.getJSONObject("element")
        val attrName = statement.getString("attr_name")
        if (element.getString("cmd") == "get_variable") {
            val variable = getVariable(element.getString("name"))
            if (variable == null || variable.type != "elements") {
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
                element.getJSONArray("selectors").getJSONObject(index)
            }
            else -> {
                showError("InvalidElement: value is not an HTML element")
                return ""
            }
        }

        var attrValue: String? = null

        getWebView()?.js("""
                var iframeIndex = ${value.getInt("iframeIndex")};
                var element = document.querySelectorAll("${value.getString("selector")}")[${value.getInt("elIndex")}];
                if (iframeIndex != -1) {
                    var iframes = document.querySelectorAll("iframe");
                    element = iframes[iframeIndex].contentDocument.querySelectorAll("${value.getString("selector")}")[${value.getInt("elIndex")}];
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
            """.trimIndent()) {
            attrValue = it
        }

        while (attrValue == null) {
            Thread.sleep(100)
        }

        return attrValue!!
    }

    private fun getTextContains(statement: JSONObject): String {
        val text = getNodeValue(statement.getJSONObject("text")).value
        val toFind = getNodeValue(statement.getJSONObject("value")).value

        if (text.contains(toFind)) {
            return "true"
        }
        return "false"
    }

    private fun getBoolean(statement: JSONObject): String {
        return statement.getString("value")
    }


    // helper methods

    private fun getNodeValue(statement: JSONObject): app.spidy.spider.data.Node {
        return when (statement.getString("cmd")) {
            "get_text" -> app.spidy.spider.data.Node(statement.getString("text"), "text")
            "get_variable" -> {
                val v = getVariable(statement.getString("name")) ?: getJson(statement.getString("name"))

                if (v != null) {
                    return when (v.type) {
                        "number" -> app.spidy.spider.data.Node(v.value.toString(), "number")
                        "elements" -> app.spidy.spider.data.Node(v.value.toString(), "elements")
                        "json" -> {
                            if (v.value::class.simpleName == "ArrayList") {
                                app.spidy.spider.data.Node(
                                    JSONArray((v.value as ArrayList<Any>)).toString(),
                                    "text"
                                )
                            } else {
                                app.spidy.spider.data.Node(
                                    JSONObject((v.value as HashMap<String, Any>).toMap()).toString(),
                                    "text"
                                )
                            }
                        }
                        else -> app.spidy.spider.data.Node(v.value.toString(), "text")
                    }
                }
                return app.spidy.spider.data.Node("", "text")
            }
            "get_html_elements" -> app.spidy.spider.data.Node(statement.getJSONArray("selectors").toString(), "text")
            "get_attribute" -> app.spidy.spider.data.Node(getAttribute(statement), "text")
            "get_attributes" -> app.spidy.spider.data.Node(getAttributes(statement).toString(), "array")
            "get_text_content" -> app.spidy.spider.data.Node(getTextContent(statement), "text")
            "get_tag_name" -> app.spidy.spider.data.Node(getTagName(statement), "text")
            "get_input" -> app.spidy.spider.data.Node(ask(statement), "text")
            "get_condition" -> {
                val firstCondition = getCondition(statement.getJSONObject("first_condition"))
                val secondCondition = getCondition(statement.getJSONObject("second_condition"))
                val operator = statement.getString("operator")
                var bool = false

                val con1 = firstCondition.value
                val con2 = secondCondition.value

                when (operator) {
                    "EQUAL" -> if (con1 == con2) bool = true
                    "NOT_EQUAL" -> if (con1 != con2) bool = true
                    "LESS_THAN" -> if (con1 < con2) bool = true
                    "LESS_THAN_OR_EQUAL" -> if (con1 <= con2) bool = true
                    "GTREATER_THAN" -> if (con1 > con2) bool = true
                    "GTREATER_THAN_OR_EQUAL" -> if (con1 >= con2) bool = true
                }
                app.spidy.spider.data.Node(bool.toString(), "text")
            }
            "get_boolean" -> app.spidy.spider.data.Node(getBoolean(statement), "text")
            "trim_space" -> app.spidy.spider.data.Node(getTrimSpace(statement), "text")
            "get_number" -> app.spidy.spider.data.Node(getNumber(statement), "number")
            "get_calculation" -> app.spidy.spider.data.Node(getCalculationValue(statement), "number")
            "get_join_text" -> app.spidy.spider.data.Node(getJoinText(statement), "text")
            "text_contains" -> app.spidy.spider.data.Node(getTextContains(statement), "text")
            else -> app.spidy.spider.data.Node("", "text")
        }
    }

    private fun getTrimSpace(statement: JSONObject): String {
        return getNodeValue(statement).value.trim()
    }

    private fun waitForEls(selectors: JSONArray, totalWaitedMills: Long = 0) {
        var isElementsExists: Boolean? = null
        getWebView()?.js("""
                var selectors = $selectors;
                var iframes = document.querySelectorAll("iframe");
                for (var i = 0; i < selectors.length; i++) {
                    if (selectors[i].iframeIndex == -1) {
                        if (document.querySelectorAll(selectors[i].selector)[selectors[i].elIndex] == undefined) {
                            return false;
                        } else if (document.querySelectorAll(selectors[i].selector)[selectors[i].elIndex].offsetParent == null) {
                            return false;
                        }
                    } else {
                        if (iframes[i].contentDocument.querySelectorAll(selectors[i].selector)[selectors[i].elIndex] == undefined) {
                            return false;
                        } else if (iframes[i].contentDocument.querySelectorAll(selectors[i].selector)[selectors[i].elIndex].offsetParent == null) {
                            return false;
                        }
                    }
                }
                return true;
            """.trimIndent()) {
            isElementsExists = it == "true"
        }

        while (isElementsExists == null) {
            Thread.sleep(100)
        }

        if (!isElementsExists!!) {
            if (totalWaitedMills/1000 < 30) {
                waitForEls(selectors, totalWaitedMills + 100)
            }
        }
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
}