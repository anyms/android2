package app.spidy.tamillovevideostatus.activities

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import app.spidy.tamillovevideostatus.R
import app.spidy.tamillovevideostatus.adapters.VideoListAdapter
import app.spidy.tamillovevideostatus.data.Video
import app.spidy.tamillovevideostatus.utils.Http
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_main.nestedScrollView
import kotlinx.android.synthetic.main.activity_main.progressBar
import kotlinx.android.synthetic.main.activity_main.recyclerView
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.activity_search.*
import org.json.JSONObject


class SearchActivity : AppCompatActivity() {
    private var currentPage = 1
    private var isRecyclerViewWaitingtoLaadData = false
    private var isNextExists = true
    private lateinit var adapter: VideoListAdapter
    private val videos = ArrayList<Video>()

    private var query: String = "love"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_arr_back)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val adView: AdView = findViewById(R.id.adView)
        adView.loadAd(AdRequest.Builder().build())

        adapter = VideoListAdapter(this, videos)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, GridLayoutManager.VERTICAL)

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
                if (!isRecyclerViewWaitingtoLaadData && isNextExists) {
                    loadNextPage()
                }
            }
        })

        searchField.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                query = searchField.text.toString().trim()
                if (query != "") {
                    currentPage = 1
                    videos.clear()
                    adapter.notifyDataSetChanged()
                    hideKeyboardFrom(searchField)
                    loadNextPage()
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        loadNextPage()
    }

    private fun loadNextPage() {
        emptyView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        isRecyclerViewWaitingtoLaadData = true
        debug("Page: $currentPage")
        Http.async.get("${Http.apiUrl}/search/${query}/${currentPage}?key=${Http.apiKey}").then {
            val data = JSONObject(it.text!!)
            isNextExists = data.getBoolean("is_next_exists")
            val vids = data.getJSONArray("videos")
            for (i in 0 until vids.length()) {
                val vid = vids.getJSONObject(i)
                val video = Video(
                    uId = vid.getLong("id"),
                    downloadCount = vid.getInt("download_count"),
                    shareCount = vid.getInt("share_count"),
                    viewCount = vid.getInt("view_count"),
                    tags = vid.getString("tags"),
                    title = vid.getString("title"),
                    videoId = vid.getString("video_id"),
                    isExpired = vid.getBoolean("is_expired"),
                    category = vid.getString("category"),
                    thumb = vid.getString("thumb"),
                    expire = vid.getLong("expire")
                )
                videos.add(video)
            }
            onUiThread {
                progressBar.visibility = View.GONE
                adapter.notifyDataSetChanged()
                isRecyclerViewWaitingtoLaadData = false

                if (currentPage == 1 && videos.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                }

                currentPage++
            }
        }.catch {
            debug(it)
            onUiThread {
                toast("Network failed")
                loadNextPage()
            }
        }
    }

    fun hideKeyboardFrom(view: View) {
        val imm: InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}