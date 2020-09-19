package app.spidy.spidy.activities

import android.content.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.spidy.R
import app.spidy.spidy.adapters.ScriptAdapter
import app.spidy.spidy.data.Process
import app.spidy.spidy.data.Script
import app.spidy.spidy.databases.SpidyDatabase
import app.spidy.spidy.services.HeadlessService
import app.spidy.spidy.utils.Ads
import app.spidy.spidy.viewmodels.MainActivityViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var scriptAdapter: ScriptAdapter
    private lateinit var database: SpidyDatabase
    private lateinit var tinyDB: TinyDB

    private val scripts = ArrayList<Script>()
    private var headlessService: HeadlessService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        database = Room.databaseBuilder(this, SpidyDatabase::class.java, "SpidyDatabase")
            .addMigrations(SpidyDatabase.migration).build()

        tinyDB = TinyDB(this)
        val adView: AdView = findViewById(R.id.adView)
        if (!tinyDB.getBoolean("isPro")) {
            Ads.initInterstitial(this)
            Ads.initReward(this)
            Ads.loadInterstitial()
            Ads.loadReward()
            adView.loadAd(AdRequest.Builder().build())
        } else {
            adView.visibility = View.GONE
        }

        setSupportActionBar(toolbar)
        title = getString(R.string.app_name)

        val widthDp = resources.displayMetrics.run { widthPixels / density }

        if (widthDp > 600) {
            recyclerView.layoutManager = GridLayoutManager(this, 2)
        } else {
            recyclerView.layoutManager = LinearLayoutManager(this)
        }
        recyclerView.itemAnimator = null
        scriptAdapter = ScriptAdapter(this, scripts, viewModel, runHeadless = { id, code ->
            runHeadless(id, code)
        }, terminateScript = {
            headlessService?.terminateScript(it.id)
        })
        recyclerView.adapter = scriptAdapter

        fab.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            startActivity(intent)
        }

        var isUpdated = false
        viewModel.viewUpdateSwitch.observe(this, Observer {
            if (!isUpdated) {
                isUpdated = true
                scripts.clear()
                thread {
                    database.spidyDao().getScripts().forEach {
                        scripts.add(it)
                    }
                    onUiThread {
                        if (HeadlessService.isRunning && headlessService != null) {
                            updateRecyclerView(headlessService!!.processes)
                        }
                        scriptAdapter.notifyDataSetChanged()
                        if (scripts.isEmpty()) {
                            recyclerView.visibility = View.GONE
                            emptyView.visibility = View.VISIBLE
                        } else {
                            recyclerView.visibility = View.VISIBLE
                            emptyView.visibility = View.GONE
                        }
                    }

                    Thread.sleep(1000)
                    isUpdated = false
                }
            }
        })

        // TODO: remove this on the next version
//        if (!tinyDB.getBoolean("isWarningShown")) {
//            newDialog().withTitle("We are sorry!")
//                .withMessage("We really sorry to inform you that scripts made with the previous version of this app, will no longer execute with this version. Because we had an issue with the core scripting engine, such as dynamic variable and get html elements(s). Please re-create your scripts. We promise that the scripting engine is stable now, if there any changes in the future will be backward compatible.\n\nThank you for your support.\nSpidy Team.")
//                .withPositiveButton("Got it") {
//                    tinyDB.putBoolean("isWarningShown", true)
//                    it.dismiss()
//                }
//                .withCancelable(false)
//                .create().show()
//
//        }

        viewModel.isBackgroundProcessBinned.observe(this, Observer {

        })
        viewModel.isBackgroundProcessBinned.value = false
    }

    private fun updateRecyclerView(processes: List<Process>) {
        for (i in scripts.indices) {
            scripts[i].isBackgroundRunning = false
            for (process in processes) {
                if (process.id == scripts[i].id) {
                    scripts[i].isBackgroundRunning = true
                }
            }
            onUiThread { scriptAdapter.notifyItemChanged(i) }
        }
    }

    private fun runHeadless(id: Int, code: String) {
        if (viewModel.isBackgroundProcessBinned.value!!) {
            headlessService?.startNewScript(id, code)
        } else {
            val scriptRunnerIntent = Intent(this, HeadlessService::class.java)
            startService(scriptRunnerIntent)
            bindService(scriptRunnerIntent, connection, Context.BIND_AUTO_CREATE)

            thread {
                while (headlessService?.listener == null) Thread.sleep(100)
                onUiThread {
                    headlessService?.startNewScript(id, code)
                }
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val connection = this
            headlessService = (service as? HeadlessService.RunnerBinder)?.service
            viewModel.isBackgroundProcessBinned.value = true
            headlessService?.listener = object : HeadlessService.Listener {
                override fun onUnbind() {
                    if (viewModel.isBackgroundProcessBinned.value!!) unbindService(connection)
                    viewModel.isBackgroundProcessBinned.value = false
                    headlessService = null
                }
                override fun onLog(id: Int, s: String) {}
                override fun onTerminate() {}
                override fun onUpdateRecyclerView(processes: List<Process>) {
                    updateRecyclerView(processes)
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    override fun onResume() {
        if (HeadlessService.isRunning) {
            bindService(Intent(this, HeadlessService::class.java), connection, Context.BIND_AUTO_CREATE)
        }
        viewModel.updateView()
        super.onResume()
    }

    override fun onPause() {
        debug("Unbinding... ${headlessService?.listener}")
        headlessService?.listener?.onUnbind()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuFeedback -> {
                val uri = Uri.parse("market://details?id=$packageName");
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName"))
                    )
                }
            }
        }
        return true
    }
}