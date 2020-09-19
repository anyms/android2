package app.spidy.lankanews.activities

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import app.spidy.kotlinutils.*
import app.spidy.lankanews.R
import app.spidy.lankanews.adapters.SearchAdapter
import app.spidy.lankanews.data.SearchNews
import app.spidy.lankanews.parsers.SearchParser
import app.spidy.lankanews.utils.API
import app.spidy.lankanews.utils.Ads
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_search.*

class SearchActivity : AppCompatActivity() {
    private lateinit var tinyDB: TinyDB
    private lateinit var parser: SearchParser
    private lateinit var adapter: SearchAdapter

    private var query = ""
    private val newses = ArrayList<SearchNews>()
    private var pageNum = 1
    private var isRecyclerViewWaitingToLoadData = false
    private var isNextPageExists = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        adView.loadAd(AdRequest.Builder().build())

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        tinyDB = TinyDB(applicationContext)
        parser = SearchParser()
        adapter = SearchAdapter(this, newses)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        searchField.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboardFrom(v)
                query = searchField.text.toString().trim()
                if (query != "") performSearch(true)
                return@OnEditorActionListener true
            }
            false
        })

        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                if (!isRecyclerViewWaitingToLoadData && isNextPageExists) {
                    performSearch()
                }
            }
        })
    }


    private fun createUrl(part: String): String {
        var url = "https://www.newsfirst.lk/${tinyDB.getString("lang")}"
        if (tinyDB.getString("lang") != "") {
            url += "/"
        }
        return "$url$part"
    }

    private fun performSearch(isReset: Boolean = false) {
        isRecyclerViewWaitingToLoadData = true
        emptyView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        val url = createUrl("page/$pageNum/?s=$query")

        if (isReset) {
            Ads.showInterstitial()
            Ads.loadInterstitial()
            newses.clear()
            adapter.notifyDataSetChanged()
        }

        API.get(url).then {
            val tmpNewses = parser.parse(it.text!!)
            if (tmpNewses.isEmpty()) {
                onUiThread { toast("No more pages") }
                isNextPageExists = false
            }

            val startPos = newses.size
            for (news in tmpNewses) {
                newses.add(news)
            }

            onUiThread {
                adapter.notifyItemRangeChanged(startPos, tmpNewses.size)
                if (newses.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                }
                progressBar.visibility = View.GONE
                pageNum += 1
            }
            isRecyclerViewWaitingToLoadData = false
        }.catch {
            onUiThread {
                progressBar.visibility = View.GONE
                isRecyclerViewWaitingToLoadData = false

                newDialog().withTitle("Network Error!")
                    .withMessage("A network error occurred! please check your internet connection.")
                    .withCancelable(false)
                    .withPositiveButton(getString(R.string.understood)) { dialog ->
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