package app.spidy.cyberwire.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.cyberwire.R
import app.spidy.cyberwire.adapters.ChannelAdapter
import app.spidy.cyberwire.adapters.PopularAdapter
import app.spidy.cyberwire.data.Channel
import app.spidy.cyberwire.data.Episode
import app.spidy.cyberwire.utils.API
import org.json.JSONArray
import org.json.JSONObject

class HomeFragment : Fragment() {
    private lateinit var popularAdapter: PopularAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private val popularEpisodes = ArrayList<Episode>()
    private val channels = ArrayList<Channel>()
    private var channelPageNum = 1
    private var isNextPageExists = true
    private var isRecyclerViewWaitingToLoadData = false

    private var isPopularLoaded = false
    private var isChannelsLoaded = false

    private lateinit var pageLoadProgressBar: ProgressBar
    private lateinit var popularRecyclerView: RecyclerView
    private lateinit var channelsRecyclerView: RecyclerView
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_home, container, false)
        popularRecyclerView = v.findViewById(R.id.popularRecyclerView)
        channelsRecyclerView = v.findViewById(R.id.channelsRecyclerView)
        nestedScrollView = v.findViewById(R.id.nestedScrollView)
        pageLoadProgressBar = v.findViewById(R.id.pageLoadProgressBar)
        progressBar = v.findViewById(R.id.progressBar)


        popularAdapter = PopularAdapter(requireContext(), popularEpisodes)
        popularRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        popularRecyclerView.adapter = popularAdapter

        channelAdapter = ChannelAdapter(requireContext(), channels)
        channelsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        channelsRecyclerView.adapter = channelAdapter

        loadPopular()
        loadChannels()

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
                    loadChannels()
                }
            }
        })

        return v
    }


    private fun loadPopular() {
        API.async.get(API.url("/popular/episodes")).then {
            val eps = JSONArray(it.text)
            for (i in 0 until eps.length()) {
                val ep = eps.getJSONObject(i)
                popularEpisodes.add(Episode(
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
            onUiThread {
                popularAdapter.notifyDataSetChanged()
                isPopularLoaded = true
                if (isPopularLoaded && isChannelsLoaded) {
                    progressBar.visibility = View.GONE
                    nestedScrollView.visibility = View.VISIBLE
                }
            }
        }.catch {
            onUiThread {
                loadPopular()
            }
        }
    }

    private fun loadChannels() {
        isRecyclerViewWaitingToLoadData = true
        pageLoadProgressBar.visibility = View.VISIBLE
        API.async.get(API.url("/channels/$channelPageNum")).then {
            val data = JSONObject(it.text!!)
            isNextPageExists = data.getBoolean("is_next_exists")
            val chans = data.getJSONArray("channels")
            val startPos = channels.size
            for (i in 0 until chans.length()) {
                val channel = chans.getJSONObject(i)
                channels.add(Channel(
                    uId = channel.getInt("id"),
                    title = channel.getString("title"),
                    channelId = channel.getString("channel_id"),
                    viewCount = channel.getInt("view_count"),
                    author = channel.getString("author"),
                    category = channel.getString("category"),
                    description = channel.getString("description"),
                    image = channel.getString("image"),
                    rss = channel.getString("rss"),
                    website = channel.getString("website")
                ))
            }
            channelPageNum++
            onUiThread {
                pageLoadProgressBar.visibility = View.GONE
                channelAdapter.notifyItemRangeChanged(startPos, chans.length())
                isChannelsLoaded = true
                if (isPopularLoaded && isChannelsLoaded) {
                    progressBar.visibility = View.GONE
                    nestedScrollView.visibility = View.VISIBLE
                }
                isRecyclerViewWaitingToLoadData = false
            }
        }.catch {
            onUiThread {
                requireContext().newDialog().withTitle("Network Error!")
                    .withMessage("A network error occurred! Please check your internet connection")
                    .withCancelable(false)
                    .withPositiveButton(getString(R.string.retry)) { dialog ->
                        loadChannels()
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