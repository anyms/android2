package app.spidy.oli.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.oli.R
import app.spidy.oli.adapters.SearchAdapter
import app.spidy.oli.data.Episode
import app.spidy.oli.utils.API
import org.json.JSONArray
import org.json.JSONObject

class RecentFragment : Fragment() {
    private lateinit var adapter: SearchAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val episodes = ArrayList<Episode>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_recent, container, false)
        recyclerView = v.findViewById(R.id.recyclerView)
        progressBar = v.findViewById(R.id.progressBar)

        adapter = SearchAdapter(requireContext(), episodes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fetchRecent()

        return v
    }

    private fun fetchRecent() {
        episodes.clear()
        progressBar.visibility = View.VISIBLE
        API.async.get(API.url("/recent/episodes")).then { resp ->
            val eps = JSONArray(resp.text!!)
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
                progressBar.visibility = View.GONE
                adapter.notifyItemRangeChanged(startPos, eps.length())
            }
        }.catch {
            debug(it)
            onUiThread {
                requireContext().newDialog().withTitle("Network Error!")
                    .withMessage("Network error occurred. Please check your internet connection and try again.")
                    .withCancelable(false)
                    .withPositiveButton(getString(R.string.retry)) { dialog ->
                        dialog.dismiss()
                        fetchRecent()
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