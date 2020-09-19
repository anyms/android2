package app.spidy.proxyserver.activities

import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.chaiui.ChaiEditText
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.toast
import app.spidy.proxyserver.R
import app.spidy.proxyserver.adapters.BlockedAdapter
import app.spidy.proxyserver.data.BlockedDomain
import app.spidy.proxyserver.databases.ProxyDatabase
import app.spidy.proxyserver.utils.Ads
import app.spidy.proxyserver.utils.newDialog
import com.google.android.gms.ads.AdRequest
import com.google.android.material.snackbar.Snackbar
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_user_config.*
import kotlinx.android.synthetic.main.activity_user_config.toolbar
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class UserConfigActivity : AppCompatActivity() {
    private lateinit var blockedAdapter: BlockedAdapter
    private lateinit var database: ProxyDatabase
    private lateinit var tinyDB: TinyDB

    private val domains = ArrayList<BlockedDomain>()

    private var deletedDomain: BlockedDomain? = null
    private val simpleCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition

            if (direction == ItemTouchHelper.RIGHT) {
                deletedDomain = domains[position]
                domains.removeAt(position)
                blockedAdapter.notifyItemRemoved(position)

                thread {
                    database.proxyDao().removeBlockedDomain(deletedDomain!!)

                    runOnUiThread {
                        Snackbar.make(recyclerView, "${deletedDomain!!.value} removed", Snackbar.LENGTH_LONG)
                            .setAction("Undo") {
                                domains.add(position, deletedDomain!!)
                                blockedAdapter.notifyItemInserted(position)
                                thread { database.proxyDao().putBlockedDomain(deletedDomain!!) }
                            }
                            .show()
                    }
                }
            }
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                .addBackgroundColor(
                    ContextCompat.getColor(
                        this@UserConfigActivity,
                        R.color.colorRed
                    )
                )
                .addActionIcon(R.drawable.ic_delete)
                .create()
                .decorate()
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_config)

        database = Room.databaseBuilder(this, ProxyDatabase::class.java, "ProxyDatabase")
            .fallbackToDestructiveMigration().build()
        tinyDB = TinyDB(this)

        if (tinyDB.getBoolean("isPro")) {
            adView.visibility = View.GONE
        } else {
            adView.loadAd(AdRequest.Builder().build())
        }

        setSupportActionBar(toolbar)
        blockedAdapter = BlockedAdapter(this, domains, recyclerView)
        recyclerView.adapter = blockedAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        domains.clear()
        thread {
            val blockedDomains = database.proxyDao().getBlockedDomains()
            for (blockedDomain in blockedDomains) domains.add(blockedDomain)

            runOnUiThread {
                if (blockedDomains.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    nothingView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    nothingView.visibility = View.GONE

                    blockedAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_block_a_website, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuAdd -> {
                val v = LayoutInflater.from(this).inflate(R.layout.layout_add_blocked_domain_dialog, null)
                val domainField: ChaiEditText = v.findViewById(R.id.domainField)
                newDialog().withCustomView(v)
                    .withTitle("Add a Domain")
                    .withPositiveButton("Add") {_, _ ->
                        val domain = domainField.text.toString().trim().toLowerCase(Locale.ROOT)
                        if (domain != "") {
                            thread {
                                database.proxyDao().putBlockedDomain(
                                    BlockedDomain(domain, domain.contains("*"))
                                )

                                runOnUiThread {
                                    updateRecyclerView()
                                    if (!tinyDB.getBoolean("isPro")) {
                                        Ads.showInterstitial {  }
                                        Ads.loadInterstitial()
                                    }
                                }
                            }
                        } else {
                            toast("Domain should not be empty")
                        }
                    }
                    .withNegativeButton("Cancel") {dialog, _ -> dialog.dismiss() }
                    .show()
            }

            R.id.menuClearAll -> {
                thread {
                    database.proxyDao().clearAllBlockedDomains()

                    runOnUiThread {
                        updateRecyclerView()
                        toast("All cleared!")
                    }
                }
            }
        }

        return true
    }
}