package app.spidy.cyberwire.activities

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.cyberwire.R
import app.spidy.cyberwire.adapters.SearchAdapter
import app.spidy.cyberwire.data.Episode
import app.spidy.cyberwire.utils.API
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.activity_search.*
import org.json.JSONObject


class SearchActivity : AppCompatActivity() {
    private var pageNum = 1
    private var isNextExists = false
    private var isRecyclerViewWaitingToLoadData = false
    private var query: String = ""
    private val episodes = ArrayList<Episode>()

    private lateinit var adapter: SearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow_dark)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        searchField.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                query = searchField.text.toString().trim()
                if (query != "") performSearch(true)
                return@OnEditorActionListener true
            }
            false
        })

        adapter = SearchAdapter(this, episodes)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

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
                if (!isRecyclerViewWaitingToLoadData && isNextExists) {
                    performSearch()
                }
            }
        })
    }

    private fun performSearch(isReset: Boolean = false) {
        isRecyclerViewWaitingToLoadData = true

        if (isReset) {
            episodes.clear()
            adapter.notifyDataSetChanged()
        }

        hideKeyboardFrom(searchField)
        nothingView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        API.async.get(API.url("/search/episodes/$query/$pageNum")).then { resp ->
            val data = JSONObject(resp.text!!)
            isNextExists = data.getBoolean("is_next_exists")
            val eps = data.getJSONArray("episodes")
            val startPos = episodes.size
            for (i in 0 until eps.length()) {
                val ep = eps.getJSONObject(i)
                val episode = Episode(
                    uId = ep.getInt("id"),
                    audio = ep.getString("audio"),
                    channelId = ep.getString("channel_id"),
                    date = ep.getString("date"),
                    timestamp = ep.getLong("raw_date"),
                    title = ep.getString("title"),
                    viewCount = ep.getInt("view_count"),
                    downloadedLocation = "",
                    coverImage = ep.getString("cover_image")
                )
                episodes.add(episode)
            }

            onUiThread {
                if (episodes.isEmpty()) {
                    nothingView.visibility = View.VISIBLE
                } else {
                    nothingView.visibility = View.GONE
                }
                progressBar.visibility = View.GONE
                adapter.notifyItemRangeChanged(startPos, eps.length())
                isRecyclerViewWaitingToLoadData = false
            }
        }.catch {
            onUiThread {
                newDialog().withTitle("Network Error!")
                    .withMessage("Network error occurred. Please check your internet connection and try again.")
                    .withCancelable(false)
                    .withPositiveButton(getString(R.string.retry)) { dialog ->
                        dialog.dismiss()
                        performSearch()
                    }
                    .withNegativeButton(getString(R.string.cancel)) { dialog ->
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuSearch -> {
                query = searchField.text.toString().trim()
                if (query != "") performSearch(true)
            }
        }

        return true
    }
}