package app.spidy.spidy.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.spidy.R
import app.spidy.spidy.data.Process
import app.spidy.spidy.services.HeadlessService
import app.spidy.spidy.utils.Ads
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_debug_console.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.toolbar

class DebugConsoleActivity : AppCompatActivity() {
    private var headlessService: HeadlessService? = null
    private var isBinned = false
    private lateinit var tinyDB: TinyDB
    private var scriptId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_console)

        tinyDB = TinyDB(this)
        val adView: AdView = findViewById(R.id.adView)
        if (!tinyDB.getBoolean("isPro")) {
            adView.loadAd(AdRequest.Builder().build())
        } else {
            adView.visibility = View.GONE
        }

        setSupportActionBar(toolbar)
        title = getString(R.string.debug_console)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow_light)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        scriptId = intent!!.getIntExtra("script_id", -1)
    }

    override fun onResume() {
        if (HeadlessService.isRunning) {
            bindService(Intent(this, HeadlessService::class.java), connection, Context.BIND_AUTO_CREATE)
        } else {
            finish()
        }
        super.onResume()
    }

    override fun onPause() {
        if (isBinned) {
            headlessService?.listener = null
            unbindService(connection)
            isBinned = false
        }
        super.onPause()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val connection = this
            headlessService = (service as? HeadlessService.RunnerBinder)?.service
            isBinned = true
            headlessService?.listener = object : HeadlessService.Listener {
                override fun onUnbind() {
                    if (isBinned) {
                        unbindService(connection)
                    }
                    isBinned = false
                    headlessService = null
                }
                override fun onLog(id: Int, s: String) {
                    if (id == scriptId) {
                        onUiThread { logCat.text = s }
                    }
                }
                override fun onTerminate() {
                    finish()
                }
                override fun onUpdateRecyclerView(processes: List<Process>) {}
            }
            for (process in headlessService!!.processes) {
                if (process.id == scriptId) {
                    logCat.text = process.log
                    break
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }
}