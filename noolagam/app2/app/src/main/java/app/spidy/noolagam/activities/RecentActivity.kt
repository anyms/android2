package app.spidy.noolagam.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.room.Room
import app.spidy.kotlinutils.onUiThread
import app.spidy.noolagam.R
import app.spidy.noolagam.adapters.BookAdapter
import app.spidy.noolagam.data.Book
import app.spidy.noolagam.databases.NoolagamDatabase
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_recent.*
import kotlin.concurrent.thread

class RecentActivity : AppCompatActivity() {
    private lateinit var database: NoolagamDatabase
    private lateinit var bookAdapter: BookAdapter

    private val books = ArrayList<Book>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recent)

        adView.loadAd(AdRequest.Builder().build())

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener { finish() }

        database = Room.databaseBuilder(this, NoolagamDatabase::class.java, "NoolagamDatabase")
            .fallbackToDestructiveMigration().build()
        bookAdapter = BookAdapter(this, books)
        recyclerView.adapter = bookAdapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)

        thread {
            books.clear()
            database.dao().getBooks().reversed().forEach { books.add(it) }

            onUiThread {
                bookAdapter.notifyDataSetChanged()
                if (books.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                }
            }
        }
    }
}