package app.spidy.spidy.services

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import app.spidy.browser.controllers.Browser
import app.spidy.browser.controllers.Headless
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import app.spidy.spidy.R
import app.spidy.spidy.activities.DialogActivity
import app.spidy.spidy.communicators.Communicator
import app.spidy.spidy.data.Process
import app.spidy.spidy.interpreter.SpidyScript2
import app.spidy.spidy.utils.*
import kotlin.concurrent.thread

class HeadlessService: Service() {
    companion object {
        var isRunning = false
        var askValue: String? = null
    }

    private lateinit var notification: NotificationCompat.Builder
    private lateinit var hammerJs: String
    private lateinit var elementSelectorJs: String
    private lateinit var tinyDB: TinyDB

    var listener: Listener? = null
    var processes = ArrayList<Process>()

    override fun onBind(intent: Intent?): IBinder? {
        return RunnerBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        notification = NotificationCompat.Builder(this, C.CHANNEL_ID)
            .setContentTitle("Headless Browser")
            .setContentText("Total ${processes.size} script(s) are running")
            .setSmallIcon(R.drawable.ic_notification)

        tinyDB = TinyDB(this)
        hammerJs = IO.readFromAsset(this, "libs/hammer.js")!!
        elementSelectorJs = IO.readFromAsset(this, "spidy.js")!!

        startForeground(C.SCRIPT_RUNNER_NOTIFICATION_ID, notification.build())
        return START_NOT_STICKY
    }

    private fun startListener() {
        thread {
            while (processes.isNotEmpty()) Thread.sleep(100)

            onUiThread {
                if (processes.isEmpty()) {
                    vibrate()
                    stopSelf()
                    listener?.onUnbind()
                } else {
                    startListener()
                }
            }
        }
    }

    private fun findProcessIndex(id: Int): Int {
        var index = -1
        for (i in processes.indices) {
            if (processes[i].id == id) {
                index = i
                break
            }
        }
        return index
    }

    fun terminateScript(id: Int) {
        val index = findProcessIndex(id)
        if (id != -1) {
            processes[index].interpreter.isTerminated = true
            processes.removeAt(index)
            listener?.onUpdateRecyclerView(processes)
        }
        notification.setContentText("Total ${processes.size} script(s) are running")
        startForeground(C.SCRIPT_RUNNER_NOTIFICATION_ID, notification.build())
    }

    private fun debugLog(id: Int, s: String) {
        debug(s)
        val index = findProcessIndex(id)
        if (index != -1) {
            processes[index].log += "$s\n"
            listener?.onLog(id, processes[index].log)
        }
    }

    fun startNewScript(id: Int, code: String) {
        val headless = Headless(this)
        val spidyScript = SpidyScript2(
            context = this,
            getWebView = {
                return@SpidyScript2 headless.currentTab
            },
            headless = headless,
            canShowDialogs = false,
            debugLog = { debugLog(id, it) },
            askInput = {
                debug("Opening dialog")
                val intent = Intent(applicationContext, DialogActivity::class.java)
                intent.putExtra("title", it)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        )
        spidyScript.listener = object : SpidyScript2.Listener {
            override fun onFinish() {
                val index = findProcessIndex(id)
                if (index != -1) {
                    processes[index].interpreter.isTerminated = true
                    processes.removeAt(index)
                    listener?.onUpdateRecyclerView(processes)
                }
                notification.setContentText("Total ${processes.size} script(s) are running")
                startForeground(C.SCRIPT_RUNNER_NOTIFICATION_ID, notification.build())
            }
        }

        var isAlreadyRan = false
        var isCodeExecuted = false
        val browserListener = object : Browser.Listener {
            override fun onPageStarted(view: WebView, url: String, favIcon: Bitmap?): Boolean {
                isAlreadyRan = false
                spidyScript.isReadyToExecute = false
                return super.onPageStarted(view, url, favIcon)
            }
            override fun onPageFinished(view: WebView, url: String): Boolean {
                if (!isAlreadyRan) {
//                    Injector.injectScript(view, elementSelectorJs, isInit = false)
                    thread {
                        Thread.sleep(1000)
                        spidyScript.isReadyToExecute = true
                    }
                    isAlreadyRan = true
                }
                return super.onPageFinished(view, url)
            }

            override fun onNewWebView(view: WebView): Boolean {
                view.addJavascriptInterface(
                    Communicator(this@HeadlessService, view),
                    "spidycom"
                )
                return super.onNewWebView(view)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError?
            ): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    onUiThread { toast("A network error occurred. (${error?.description})", isLong = true) }
                } else {
                    onUiThread { toast("A network error occurred.", isLong = true) }
                }
                val index = findProcessIndex(id)
                if (index != -1) {
                    processes[index].interpreter.isTerminated = true
                    processes.removeAt(index)
                    listener?.onUpdateRecyclerView(processes)
                }
                notification.setContentText("Total ${processes.size} script(s) are running")
                startForeground(C.SCRIPT_RUNNER_NOTIFICATION_ID, notification.build())
                return super.onReceivedError(view, request, error)
            }
        }

        headless.listener = browserListener
        headless.newTab("file:///android_asset/blank.html")

        if (processes.isEmpty()) {
            processes.add(Process(id, code, spidyScript))
            startListener()
        } else {
            processes.add(Process(id, code, spidyScript))
        }
        listener?.onUpdateRecyclerView(processes)
        thread { spidyScript.run(code.base64Encode()) }

        notification.setContentText("Total ${processes.size} script(s) are running")
        startForeground(C.SCRIPT_RUNNER_NOTIFICATION_ID, notification.build())
    }

    override fun onDestroy() {
        isRunning = false
        listener?.onTerminate()
        super.onDestroy()
    }

    inner class RunnerBinder: Binder() {
        val service: HeadlessService
            get() = this@HeadlessService
    }

    interface Listener {
        fun onUnbind()
        fun onUpdateRecyclerView(processes: List<Process>)
        fun onLog(id: Int, s: String)
        fun onTerminate()
    }
}