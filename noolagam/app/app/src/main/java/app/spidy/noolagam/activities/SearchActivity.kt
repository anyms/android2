package app.spidy.noolagam.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.spidy.noolagam.R
import app.spidy.noolagam.adapters.ShelfAdapter
import app.spidy.noolagam.data.Book
import app.spidy.noolagam.utils.Ads
import app.spidy.noolagam.utils.Noolagam
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.activity_search.adView
import kotlinx.android.synthetic.main.activity_search.toolbar

class SearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        adView.loadAd(AdRequest.Builder().build())

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        title = ""

        val books = ArrayList<Book>()
        val allBooks = Noolagam.getBooks(this)
        allBooks.subList(0, 50).forEach {
            books.add(it)
        }

        val shelfAdapter = ShelfAdapter(this, books)
        recyclerView.hasFixedSize()
        recyclerView.adapter = shelfAdapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        searchField.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val s = searchField.text.toString().trim()
                if (s != "") {
                    books.clear()
                    s.split(" ").forEach { query ->
                        allBooks.forEach {  book ->
                            if (book.title.contains(query)) books.add(book)
                        }
                    }
                    shelfAdapter.notifyDataSetChanged()

                    if (books.isEmpty()) {
                        nothingFoundView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        Ads.showInterstitial()
                        Ads.loadInterstitial()
                        nothingFoundView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }

                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }
    }
}