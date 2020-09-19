package app.spidy.lankanews.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.GravityCompat
import app.spidy.kotlinutils.*
import app.spidy.lankanews.R
import app.spidy.lankanews.adapters.NewsAdapter
import app.spidy.lankanews.adapters.TabAdapter
import app.spidy.lankanews.data.News
import app.spidy.lankanews.data.Category
import app.spidy.lankanews.utils.API
import app.spidy.lankanews.utils.Ads
import com.google.android.gms.ads.AdRequest
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*
import org.jsoup.Jsoup
import java.lang.Exception

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var tinyDB: TinyDB
    private lateinit var tabAdapter: TabAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        Ads.initInterstitial(this)
        Ads.loadInterstitial()
        adView.loadAd(AdRequest.Builder().build())

        tinyDB = TinyDB(applicationContext)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_menu)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START, true)
        }
        navView.setNavigationItemSelectedListener(this)

        initialFetch(object : API.Listener {
            override fun onFail(e: Exception) {
                progressBar.visibility = View.GONE
                newDialog().withTitle("Network Error!")
                    .withMessage("A network error occurred! please check your internet connection.")
                    .withCancelable(false)
                    .withPositiveButton(getString(R.string.understood)) { dialog ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
            override fun onCategory(cats: List<Category>) {
                for (cat in cats) {
                    tabLayout.addTab(tabLayout.newTab().setText(cat.title))
                }

                tabAdapter = TabAdapter(supportFragmentManager, cats)
                viewPager.adapter = tabAdapter
                viewPager.offscreenPageLimit = 3
                tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        tab?.position?.also { position ->
                            if (position != viewPager.currentItem) {
                                viewPager.currentItem = position
                            }
                        }
                    }
                })
                viewPager.addOnPageChangeListener(object : TabLayout.TabLayoutOnPageChangeListener(tabLayout) {
                })
                tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
                progressBar.visibility = View.GONE
                tabLayout.visibility = View.VISIBLE
            }
        })

        FirebaseMessaging.getInstance().subscribeToTopic("lanka_news${tinyDB.getString("lang")}")

    }

    private fun initialFetch(listener: API.Listener) {
        progressBar.visibility = View.VISIBLE
        API.get("https://www.newsfirst.lk/${tinyDB.getString("lang")}").then {
            val cats = parseCats(it.text!!)
            onUiThread { listener.onCategory(cats) }
        }.catch {
            onUiThread { listener.onFail(it) }
        }
    }

    private fun parseCats(text: String): List<Category> {
        val doc = Jsoup.parse(text)
        val cats = ArrayList<Category>()
        val catEls = doc.select(".nav li:not([class])")

        for (el in catEls) {
            if (el.text().trim() != "") {
                cats.add(Category(
                    title = el.text(),
                    url = el.select("a").attr("href")
                ))
            }
        }
        return cats
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START, true)
        when(item.itemId) {
            R.id.navShare -> {
                ignore {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Lanka News")
                    var shareMessage = "\nWe report. You decide\n\n"
                    shareMessage += "https://play.google.com/store/apps/details?id=app.spidy.lankanews\n\n"
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
            R.id.navBookmark -> startActivity(Intent(this, BookmarkActivity::class.java))
        }
        return true
    }
}