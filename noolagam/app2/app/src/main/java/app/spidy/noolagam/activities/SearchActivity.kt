package app.spidy.noolagam.activities

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.noolagam.R
import app.spidy.noolagam.adapters.BookAdapter
import app.spidy.noolagam.data.Book
import app.spidy.noolagam.utils.API
import app.spidy.noolagam.utils.Ads
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_search.toolbar
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.activity_search.emptyView
import kotlinx.android.synthetic.main.activity_search.nestedScrollView
import kotlinx.android.synthetic.main.activity_search.progressBar
import kotlinx.android.synthetic.main.activity_search.recyclerView
import org.json.JSONObject

class SearchActivity : AppCompatActivity() {
    private lateinit var bookAdapter: BookAdapter

    private var query: String = ""
    private val books = ArrayList<Book>()
    private var pageNum = 1
    private var isNextPageExists = false
    private var isRecyclerViewWaitingToLoadData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        adView.loadAd(AdRequest.Builder().build())

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener { finish() }

        bookAdapter = BookAdapter(this, books)
        recyclerView.adapter = bookAdapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)

        searchField.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                query = searchField.text.toString().trim()
                if (query != "") performSearch(true)
                return@OnEditorActionListener true
            }
            false
        })

        emptyView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE

        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > oldScrollY) {
                debug("Scroll DOWN")
            }
            if (scrollY < oldScrollY) {
                debug("Scroll UP")
            }
            if (scrollY == 0) {
                debug("TOP SCROLL")
            }
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                debug("BOTTOM SCROLL")
                if (!isRecyclerViewWaitingToLoadData && isNextPageExists) {
                    performSearch()
                }
            }
        })
    }

    private fun performSearch(isReset: Boolean = false) {
        isRecyclerViewWaitingToLoadData = true

        if (isReset) {
            Ads.showInterstitial()
            Ads.loadInterstitial()
            books.clear()
            bookAdapter.notifyDataSetChanged()
        }

        hideKeyboardFrom(searchField)
        emptyView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        API.get("/search/$query/$pageNum").then {
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
                bookAdapter.notifyItemRangeChanged(startPos, bks.length())
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
                        performSearch()
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }

    private fun hideKeyboardFrom(view: View) {
        val imm: InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}