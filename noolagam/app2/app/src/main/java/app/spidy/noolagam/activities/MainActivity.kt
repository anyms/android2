package app.spidy.noolagam.activities

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.room.Room
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.noolagam.BuildConfig
import app.spidy.noolagam.R
import app.spidy.noolagam.adapters.BookAdapter
import app.spidy.noolagam.data.Book
import app.spidy.noolagam.databases.NoolagamDatabase
import app.spidy.noolagam.utils.API
import app.spidy.noolagam.utils.Ads
import com.google.android.gms.ads.AdRequest
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var adapter: BookAdapter
    private lateinit var database: NoolagamDatabase

    private val books = ArrayList<Book>()
    private var pageNum = 1
    private var isNextPageExists = true
    private var isRecyclerViewWaitingToLoadData = false
    private var pointer = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        Ads.initInterstitial(this)
        Ads.initReward(this)
        Ads.loadInterstitial()
        Ads.loadReward()
        adView.loadAd(AdRequest.Builder().build())

        database = Room.databaseBuilder(this, NoolagamDatabase::class.java, "NoolagamDatabase")
            .fallbackToDestructiveMigration().build()

        thread {
            val bks = database.dao().getBooks()
            if (bks.size > 100) {
                for (i in 101..bks.size) {
                    database.dao().removeBook(bks[i])
                }
            }
        }

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_menu)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START, true)
        }
        navView.setNavigationItemSelectedListener(this)
        onNavigationItemSelected(navView.menu.getItem(0).setChecked(true))

        adapter = BookAdapter(this, books)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)

        loadBooks()

        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > oldScrollY) {
                // debug("Scroll DOWN")
            }
            if (scrollY < oldScrollY) {
                // debug("Scroll UP")
            }
            if (scrollY == 0) {
                // debug("TOP SCROLL")
            }
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                // debug("BOTTOM SCROLL")
                if (!isRecyclerViewWaitingToLoadData && isNextPageExists) {
                    loadBooks()
                }
            }
        })
    }

    private fun loadBooks() {
        emptyView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        isRecyclerViewWaitingToLoadData = true
        API.get("/books/$pointer$pageNum").then {
            val data = JSONObject(it.text!!)
            val bks = data.getJSONArray("books")
            isNextPageExists = data.getBoolean("is_next_exists")
            val startPos = books.size
            for (i in 0 until bks.length()) {
                val b = bks.getJSONObject(i)
                books.add(Book(
                    bookId = b.getString("book_id"),
                    category = b.getString("category"),
                    cover = b.getString("cover"),
                    id = b.getInt("id"),
                    pageCount = b.getInt("page_count"),
                    published = b.getString("published"),
                    timestamp = b.getLong("timestamp"),
                    title = b.getString("title"),
                    viewCount = b.getInt("view_count")
                ))
            }
            onUiThread {
                progressBar.visibility = View.GONE
                adapter.notifyItemRangeChanged(startPos, bks.length())
                pageNum++
                isRecyclerViewWaitingToLoadData = false

                if (books.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                }
            }
        }.catch {
            onUiThread {
                newDialog().withTitle("Network Error!")
                    .withMessage("A network error occurred! please check your internet connection.")
                    .withCancelable(false)
                    .withNegativeButton(getString(R.string.cancel)) { dialog -> dialog.dismiss() }
                    .withPositiveButton(getString(R.string.try_again)) { dialog ->
                        loadBooks()
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.navShare -> {
                ignore {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "நூலகம்")
                    var shareMessage = "\nஉங்கள் மொபைலில் இருந்து ஆயிரக்கணக்கான புத்தகங்களுக்கான அணுகல்\n\n"
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID +"\n\n"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                    startActivity(Intent.createChooser(shareIntent, "Share with"))
                }
            }
            R.id.navFeedback -> {
                val uri = Uri.parse("market://details?id=$packageName");
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    startActivity(goToMarket);
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName"))
                    )
                }
            }
            R.id.navRecent -> startActivity(Intent(this, RecentActivity::class.java))
            R.id.navDownloads -> startActivity(Intent(this, DownloadsActivity::class.java))
        }
        drawerLayout.closeDrawer(GravityCompat.START, true)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuCategory -> {
                val v = LayoutInflater.from(this).inflate(R.layout.layout_category_dialog, null)
                val dialog = newDialog().withCustomView(v)
                    .withCancelable(false)
                    .withNeutralButton(getString(R.string.reset)) { dialog ->
                        books.clear()
                        adapter.notifyDataSetChanged()
                        pageNum = 1
                        pointer = ""
                        loadBooks()
                        toolbar.title = getString(R.string.explore)
                        dialog.dismiss()
                    }
                    .withPositiveButton(getString(R.string.dismiss)) { dialog -> dialog.dismiss() }
                    .create()
                dialog.show()
                bindCategory(dialog, v)
            }

            R.id.menuFeedback -> {
                val uri = Uri.parse("market://details?id=$packageName");
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    startActivity(goToMarket);
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName"))
                    )
                }
            }

            R.id.menuSearch -> startActivity(Intent(this, SearchActivity::class.java))
        }
        return true
    }


    private fun bindCategory(dialog: AlertDialog, v: View) {
        val ids = arrayOf(
            R.id.ancient, R.id.art, R.id.articles, R.id.biography, R.id.children, R.id.religion,
            R.id.comics, R.id.cooking, R.id.drama, R.id.fiction, R.id.history, R.id.horror,
            R.id.literary, R.id.music, R.id.mystery, R.id.poetry, R.id.programming, R.id.psychology,
            R.id.romance, R.id.science, R.id.short_stories, R.id.sports, R.id.thriller, R.id.travel
        )
        for (id in ids) {
            v.findViewById<TextView>(id).setOnClickListener {
                books.clear()
                adapter.notifyDataSetChanged()
                pageNum = 1
                pointer = "${(it as TextView).text}/"
                loadBooks()
                toolbar.title = it.text
                dialog.dismiss()
            }
        }
    }
}