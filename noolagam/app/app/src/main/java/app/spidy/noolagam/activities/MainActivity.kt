package app.spidy.noolagam.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.GravityCompat
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.ignore
import app.spidy.noolagam.R
import app.spidy.noolagam.data.Book
import app.spidy.noolagam.fragments.ShelfFragment
import app.spidy.noolagam.utils.Ads
import app.spidy.noolagam.utils.Noolagam
import com.google.android.gms.ads.AdRequest
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val books = ArrayList<Book>()
    private val categories = ArrayList<String>()
    private var lastSelectedItemIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Ads.initInterstitial(this)
        Ads.initReward(this)
        Ads.loadInterstitial()
        Ads.loadReward()
        adView.loadAd(AdRequest.Builder().build())

        Noolagam.getBooks(this).forEach {  book ->
            books.add(book)
            if (!categories.contains(book.category)) categories.add(book.category)
        }

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_menu)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START, true)
        }
        navView.setNavigationItemSelectedListener(this)
        val menu = navView.menu.getItem(0).subMenu
        categories.forEachIndexed {i, cat ->
            menu.add(Menu.NONE, i, 0, cat)
            menu.getItem(i)?.icon = getDrawable(R.drawable.ic_book)
        }

        openPage(categories[0])
        navView.menu.getItem(0).subMenu.getItem(0).isChecked = true;
    }

    private fun openPage(category: String) {
        val catBooks = ArrayList<Book>()

        for (book in books) {
            if (book.category == category) catBooks.add(book)
        }

        debug(catBooks)

        supportFragmentManager.beginTransaction()
            .replace(R.id.pageContainer, ShelfFragment.newInstance(catBooks))
            .commit()
        title = category
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        navView.menu.getItem(0).subMenu.getItem(lastSelectedItemIndex).isChecked = false
        navView.menu.getItem(0).subMenu.getItem(item.itemId).isChecked = true
        lastSelectedItemIndex = item.itemId
        drawerLayout.closeDrawer(GravityCompat.START, true)

        thread {
            Thread.sleep(500)
            runOnUiThread { openPage(categories[item.itemId]) }
        }
        return true
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuSearch -> startActivity(Intent(this, SearchActivity::class.java))
            R.id.menuShare -> {
                ignore {
                    val shareIntent = Intent(Intent.ACTION_SEND);
                    shareIntent.type = "text/plain";
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "A Portable Tamil Library");
                    var shareMessage = "\nRead 3000+ Tamil books on all kinds of categories\n\n";
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + packageName +"\n\n";
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "Share with"));
                }
            }
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