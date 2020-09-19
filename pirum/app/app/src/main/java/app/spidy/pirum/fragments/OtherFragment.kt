package app.spidy.pirum.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread

import app.spidy.pirum.R
import app.spidy.pirum.adapters.OtherAdapter
import app.spidy.pirum.data.Other
import app.spidy.pirum.data.OtherHistory
import app.spidy.pirum.databases.PyrumDatabase
import kotlin.concurrent.thread


class OtherFragment : Fragment() {
    private lateinit var database: PyrumDatabase
    private lateinit var otherAdapter: OtherAdapter
    private lateinit var nothingView: TextView
    private lateinit var historyRecyclerView: RecyclerView

    private val otherHistory = ArrayList<OtherHistory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        database = Room.databaseBuilder(requireContext(), PyrumDatabase::class.java, "PyrumDatabase")
            .fallbackToDestructiveMigration().build()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_other, container, false)
        historyRecyclerView = v.findViewById(R.id.historyRecyclerView)
        nothingView = v.findViewById(R.id.nothingView)

        otherAdapter = OtherAdapter(requireContext(), otherHistory) {
            updateHistory()
        }
        historyRecyclerView.adapter = otherAdapter
        historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        return v
    }

    override fun onResume() {
        updateHistory()
        super.onResume()
    }


    private fun updateHistory() {
        otherHistory.clear()
        thread {
            val playlistNames = ArrayList<String>()
            database.pyrumDao().getOthers().forEach { other ->
                debug(other)
                if (other.type == Other.TYPE_IMAGE) {
                    if (!playlistNames.contains(other.playlistName)) {
                        playlistNames.add(other.playlistName)
                    }
                } else {
                    otherHistory.add(OtherHistory(
                        playlistName = other.playlistName,
                        title = other.title,
                        type = Other.TYPE_PAGE,
                        itemCount = 1,
                        src = other.src,
                        isToRead = other.isToRead
                    ))
                }
            }

            for (playlistName in playlistNames) {
                val dbOthers = database.pyrumDao().getOtherByPlaylist(playlistName)
                if (dbOthers.isNotEmpty()) {
                    otherHistory.add(OtherHistory(
                        playlistName = playlistName,
                        title = "",
                        type = Other.TYPE_IMAGE,
                        itemCount = dbOthers.size
                    ))
                }
            }

            onUiThread {
                if (otherHistory.isEmpty()) {
                    nothingView.visibility = View.VISIBLE
                    historyRecyclerView.visibility = View.INVISIBLE
                } else {
                    nothingView.visibility = View.GONE
                    historyRecyclerView.visibility = View.VISIBLE
                }
                otherAdapter.notifyDataSetChanged()
            }
        }
    }

}
