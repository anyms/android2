package app.spidy.cyberwire.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.cyberwire.R
import app.spidy.cyberwire.adapters.EpisodeAdapter
import app.spidy.cyberwire.data.Channel
import app.spidy.cyberwire.data.Episode
import app.spidy.cyberwire.utils.API
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_channel.*
import kotlinx.android.synthetic.main.activity_channel.progressBar
import kotlinx.android.synthetic.main.activity_channel.toolbar
import org.json.JSONObject

class ChannelActivity : AppCompatActivity() {
    private lateinit var channel: Channel
    private lateinit var episodeAdapter: EpisodeAdapter

    private var pageNum = 1
    private var isNextPageExists = true
    private var isRecyclerViewWaitingToLoadData = false
    private val episodes = ArrayList<Episode>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow_dark)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        channel = Channel(
            channelId = intent!!.getStringExtra("channel_id")!!,
            author = intent!!.getStringExtra("author")!!,
            description = intent!!.getStringExtra("description")!!,
            title = intent!!.getStringExtra("title")!!,
            category = intent!!.getStringExtra("category")!!,
            image = intent!!.getStringExtra("image")!!,
            rss = intent!!.getStringExtra("rss")!!,
            uId = intent!!.getIntExtra("uId", 0),
            viewCount = intent!!.getIntExtra("viewCount", 0),
            website = intent!!.getStringExtra("website")!!
        )

        title = channel.title

        Glide.with(this).load(channel.image).into(channelImageBg)
        debug(channel.channelId)
        episodeAdapter = EpisodeAdapter(this, channel.image, episodes, getPageNum = {
            return@EpisodeAdapter pageNum
        }, isNextExists = {
            return@EpisodeAdapter isNextPageExists
        })
        episodeRecyclerView.adapter = episodeAdapter
        episodeRecyclerView.layoutManager = LinearLayoutManager(this)

        loadEpisodes()

        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//            if (scrollY > oldScrollY) {
//                debug("Scroll DOWN")
//            }
//            if (scrollY < oldScrollY) {
//                debug("Scroll UP")
//            }
//            if (scrollY == 0) {
//                debug("TOP SCROLL")
//            }
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
//                debug("BOTTOM SCROLL")
                if (!isRecyclerViewWaitingToLoadData && isNextPageExists) {
                    loadEpisodes()
                }
            }
        })

        val gson = Gson()
        fab.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("is_channel", true)
            intent.putExtra("data", gson.toJson(episodes))
            intent.putExtra("index", 0)
            intent.putExtra("current_page", pageNum)
            intent.putExtra("is_next_exists", isNextPageExists)
            intent.putExtra("coverImageUrl", channel.image)
            startActivity(intent)
        }

        API.async.get(API.url("/channel/update_view/${channel.channelId}")).catch()
    }

    private fun loadEpisodes() {
        progressBar.visibility = View.VISIBLE
        isRecyclerViewWaitingToLoadData = true
        API.async.get(API.url("/episodes/${channel.channelId}/$pageNum")).then {
            val data = JSONObject(it.text!!)
            isNextPageExists = data.getBoolean("is_next_exists")
            val eps = data.getJSONArray("episodes")
            val startPos = episodes.size
            for (i in 0 until eps.length()) {
                val ep = eps.getJSONObject(i)
                episodes.add(Episode(
                    uId = ep.getInt("id"),
                    title = ep.getString("title"),
                    audio = ep.getString("audio"),
                    channelId = ep.getString("channel_id"),
                    date = ep.getString("date"),
                    timestamp = ep.getLong("raw_date"),
                    viewCount = ep.getInt("view_count"),
                    downloadedLocation = ""
                ))
            }
            pageNum++
            onUiThread {
                progressBar.visibility = View.GONE
                episodeAdapter.notifyItemRangeChanged(startPos, eps.length())
                progressBar.visibility = View.GONE
                isRecyclerViewWaitingToLoadData = false
            }
        }.catch {
            onUiThread {
                newDialog().withTitle("Network Error!")
                    .withMessage("A network error occurred! Please check your internet connection")
                    .withCancelable(false)
                    .withPositiveButton(getString(R.string.retry)) { dialog ->
                        loadEpisodes()
                        dialog.dismiss()
                    }
                    .withNegativeButton(getString(R.string.cancel)) { dialog ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }
}