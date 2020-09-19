package app.spidy.oli.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.kotlinutils.onUiThread
import app.spidy.oli.R
import app.spidy.oli.adapters.DownloadAdapter
import app.spidy.oli.adapters.EpisodeAdapter
import app.spidy.oli.data.Episode
import app.spidy.oli.databases.PodcastDatabase
import kotlinx.android.synthetic.main.activity_search.*
import kotlin.concurrent.thread

class DownloadFragment : Fragment() {
    private lateinit var database: PodcastDatabase

    private val episodes = ArrayList<Episode>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_download, container, false)
        val recyclerView: RecyclerView = v.findViewById(R.id.recyclerView)
        val adapter = DownloadAdapter(requireContext(), episodes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        database = Room.databaseBuilder(requireContext(), PodcastDatabase::class.java, "PodcastDatabase")
            .fallbackToDestructiveMigration().build()

        thread {
            val eps = database.dao().getEpisodes()
            episodes.clear()

            eps.forEach { episodes.add(it) }

            onUiThread {
                if (episodes.isEmpty()) {
                    nothingView.visibility = View.VISIBLE
                } else {
                    nothingView.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                }
            }
        }

        return v
    }
}