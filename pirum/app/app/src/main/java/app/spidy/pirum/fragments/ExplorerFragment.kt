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
import app.spidy.pirum.adapters.HistoryAdapter
import app.spidy.pirum.data.History
import app.spidy.pirum.databases.PyrumDatabase
import app.spidy.pirum.utils.DownloadStatus
import app.spidy.pirum.utils.PlaylistStatus
import kotlin.concurrent.thread

class ExplorerFragment : Fragment() {
    private lateinit var database: PyrumDatabase
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var nothingView: TextView
    private lateinit var historyRecyclerView: RecyclerView

    private val history = ArrayList<History>()

    override fun onCreate(savedInstanceState: Bundle?) {
        database = Room.databaseBuilder(requireContext(), PyrumDatabase::class.java, "PyrumDatabase")
            .fallbackToDestructiveMigration().build()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_explorer, container, false)
        historyRecyclerView = v.findViewById(R.id.historyRecyclerView)
        nothingView = v.findViewById(R.id.nothingView)

        historyAdapter = HistoryAdapter(requireContext(), history, {
            return@HistoryAdapter requireActivity().supportFragmentManager
        }, {
            updateHistory()
        })
        historyRecyclerView.adapter = historyAdapter
        historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        return v
    }

    override fun onResume() {
        updateHistory()
        super.onResume()
    }


    private fun updateHistory() {
        history.clear()
        thread {
            val playlistNames = ArrayList<String>()
            database.pyrumDao().getMusic().forEach { music ->
                if (!playlistNames.contains(music.playlistName)) {
                    playlistNames.add(music.playlistName)
                }
            }
            database.pyrumDao().getVideos().forEach { video ->
                if (!playlistNames.contains(video.playlistName)) {
                    playlistNames.add(video.playlistName)
                }
            }
            debug(playlistNames)

            for (playlistName in playlistNames) {
                val music = database.pyrumDao().getMusicByPlaylist(playlistName)
                if (music.isNotEmpty()) {
                    var isSomeLeft = false
                    val his = History(playlistName, music.size, "music")
                    for (m in music) {
                        if (m.status == DownloadStatus.STATE_QUEUED || m.status == DownloadStatus.STATE_DOWNLOADING
                            || m.status == DownloadStatus.STATE_RESTARTING) {
                            his.status = PlaylistStatus.STATE_DOWNLOADING
                            break
                        }

                        if (m.status == DownloadStatus.STATE_STOPPED || m.status == DownloadStatus.STATE_FAILED) {
                            his.status = PlaylistStatus.STATE_STOPPED
                            break
                        } else if (m.status == DownloadStatus.STATE_COMPLETED) {
                            if (!isSomeLeft) his.status = PlaylistStatus.STATE_COMPLETED
                        } else {
                            his.status = PlaylistStatus.STATE_NONE
                            isSomeLeft = true
                        }
                    }

                    history.add(his)
                }


                val videos = database.pyrumDao().getVideoByPlaylist(playlistName)
                if (videos.isNotEmpty()) {
                    var isSomeLeft = false
                    val his = History(playlistName, videos.size, "video")
                    for (m in videos) {
                        if (m.status == DownloadStatus.STATE_QUEUED || m.status == DownloadStatus.STATE_DOWNLOADING
                            || m.status == DownloadStatus.STATE_RESTARTING) {
                            his.status = PlaylistStatus.STATE_DOWNLOADING
                            break
                        }

                        if (m.status == DownloadStatus.STATE_STOPPED || m.status == DownloadStatus.STATE_FAILED) {
                            his.status = PlaylistStatus.STATE_STOPPED
                            break
                        } else if (m.status == DownloadStatus.STATE_COMPLETED) {
                            if (!isSomeLeft) his.status = PlaylistStatus.STATE_COMPLETED
                        } else {
                            his.status = PlaylistStatus.STATE_NONE
                            isSomeLeft = true
                        }
                    }

                    history.add(his)
                }

            }

            onUiThread {
                if (history.isEmpty()) {
                    nothingView.visibility = View.VISIBLE
                    historyRecyclerView.visibility = View.INVISIBLE
                } else {
                    nothingView.visibility = View.GONE
                    historyRecyclerView.visibility = View.VISIBLE
                }
                historyAdapter.notifyDataSetChanged()
            }
        }
    }
}
