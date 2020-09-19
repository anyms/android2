package app.spidy.lankanews.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.room.Room
import app.spidy.kotlinutils.async
import app.spidy.kotlinutils.onUiThread
import app.spidy.lankanews.R
import app.spidy.lankanews.adapters.NewsAdapter
import app.spidy.lankanews.data.News
import app.spidy.lankanews.utils.isTablet
import app.spidy.oli.databases.LankaDatabase
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_bookmark.*
import kotlinx.android.synthetic.main.activity_bookmark.adView
import kotlinx.android.synthetic.main.activity_bookmark.toolbar

class BookmarkActivity : AppCompatActivity() {
    private lateinit var database: LankaDatabase
    private lateinit var newsAdapter: NewsAdapter

    private val newses = ArrayList<News>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)

        adView.loadAd(AdRequest.Builder().build())

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        database = Room.databaseBuilder(this, LankaDatabase::class.java, "LankaDatabase")
            .fallbackToDestructiveMigration().build()
        newsAdapter = NewsAdapter(this, newses)
        recyclerView.adapter = newsAdapter
        if (isTablet()) {
            recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
        } else {
            recyclerView.layoutManager = StaggeredGridLayoutManager(1, LinearLayoutManager.VERTICAL)
        }

        async {
            for (n in database.dao().getNewses()) {
                newses.add(n)
            }

            onUiThread { newsAdapter.notifyDataSetChanged() }
        }
    }
}