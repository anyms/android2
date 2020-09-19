package app.spidy.pirum.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.room.Room
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.pirum.R
import app.spidy.pirum.activities.MainActivity
import app.spidy.pirum.activities.MusicPlayerActivity
import app.spidy.pirum.activities.VideoPlayerActivity
import app.spidy.pirum.adapters.MusicPlaylistAdapter
import app.spidy.pirum.adapters.VideoPlaylistAdapter
import app.spidy.pirum.data.Music
import app.spidy.pirum.data.Video
import app.spidy.pirum.databases.PyrumDatabase
import app.spidy.pirum.interfaces.DownloadListener
import app.spidy.pirum.services.MediaDownloadService
import app.spidy.pirum.utils.DownloadStatus
import com.google.android.exoplayer2.offline.Download
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.concurrent.thread

class PlaylistBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var database: PyrumDatabase
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var musicPlaylistAdapter: MusicPlaylistAdapter
    private lateinit var videoPlaylistAdapter: VideoPlaylistAdapter
    private lateinit var countView: TextView
    private lateinit var playAllBtn: Button

    var listener: Listener? = null
    var playlistName: String = ""
    var isMusic = false
    private val musics = ArrayList<Music>()
    private val videos = ArrayList<Video>()

    private val downloadListener = object : DownloadListener {
        override fun onProgress(downloads: MutableList<Download>) {
            for (download in downloads) {
                val sepVideo = isSeparateVideo(download.request.id)
                if (sepVideo != null) {
                    handleSepVideoDownload(sepVideo, download)
                } else {
                    val progress = download.percentDownloaded.toInt()
                    if (download.state == DownloadStatus.STATE_COMPLETED) {
                        updateMedia(download.request.id, progress, DownloadStatus.STATE_COMPLETED)
                    } else {
                        updateMedia(download.request.id, progress, download.state)
                        debug("progress: $progress%, uId: ${download.request.id}, state: ${download.state}")
                    }
                }
            }
        }
    }

    private fun handleSepVideoDownload(sepVideo: Video, download: Download) {
        if (download.request.id.contains("://")) {
            if (download.state == DownloadStatus.STATE_COMPLETED) {
                sepVideo.isSepAudioDownloaded = true
                updateMedia(download.request.id, download.percentDownloaded.toInt(), DownloadStatus.STATE_QUEUED)
            } else {
                updateMedia(download.request.id, download.percentDownloaded.toInt(), download.state)
            }
        } else {
            if (download.state == DownloadStatus.STATE_COMPLETED && sepVideo.isSepAudioDownloaded) {
                updateMedia(download.request.id, download.percentDownloaded.toInt(), DownloadStatus.STATE_COMPLETED)
            } else {
                updateMedia(download.request.id, download.percentDownloaded.toInt(), download.state)
            }
        }
    }

    private fun isSeparateVideo(uId: String): Video? {
        for (v in videos) {
            if (v.uId == uId && v.type == "separate") {
                return v
            }
        }
        return null
    }

    private fun updateMedia(uId: String, progress: Int, status: Int) {
        var index = -1
        if (!isMusic) {
            for (i in videos.indices) {
                if (videos[i].uId == uId) {
                    index = i
                    break
                }
            }
            if (index != -1) {
                videos[index].progress = progress
                videos[index].status = status
                videoPlaylistAdapter.notifyItemChanged(index)
            }
        } else {
            for (i in musics.indices) {
                if (musics[i].uId == uId) {
                    index = i
                    break
                }
            }
            if (index != -1) {
                musics[index].progress = progress
                musics[index].status = status
                musicPlaylistAdapter.notifyItemChanged(index)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = Room.databaseBuilder(requireContext(), PyrumDatabase::class.java, "PyrumDatabase")
            .fallbackToDestructiveMigration().build()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.layout_playlist_bottom_sheet, container, false)
        historyRecyclerView = v.findViewById(R.id.historyRecyclerView)
        (historyRecyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        countView = v.findViewById(R.id.countView)
        playAllBtn = v.findViewById(R.id.playAllBtn)

        if (isMusic) {
            musicPlaylistAdapter = MusicPlaylistAdapter(requireContext(), musics, {
                MediaDownloadService.downloadListener = downloadListener
            }, {
                (activity as? MainActivity)?.purchase()
            })
        } else {
            videoPlaylistAdapter = VideoPlaylistAdapter(requireContext(), videos, {
                MediaDownloadService.downloadListener = downloadListener
            }, {
                (activity as? MainActivity)?.purchase()
            })
        }
        historyRecyclerView.adapter = if (isMusic) musicPlaylistAdapter else videoPlaylistAdapter
        historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        playAllBtn.setOnClickListener {
            val appIntent = if (isMusic) {
                Intent(context, MusicPlayerActivity::class.java)
            } else {
                Intent(context, VideoPlayerActivity::class.java)
            }
            appIntent.putExtra("playlistName", playlistName)
            appIntent.putExtra("isFromLocal", true)
            appIntent.putExtra("currentIndex", 0)
            appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(appIntent)
        }
        return v
    }

    override fun onDestroy() {
        listener = null
        super.onDestroy()
    }

    override fun onResume() {
        updatePlaylist()
        if (MediaDownloadService.isRunning) {
            MediaDownloadService.downloadListener = downloadListener
        } else {
            MediaDownloadService.downloadListener = null
        }
        super.onResume()
    }

    @SuppressLint("SetTextI18n")
    private fun updatePlaylist() {
        musics.clear()
        videos.clear()
        thread {
            if (isMusic) {
                database.pyrumDao().getMusicByPlaylist(playlistName).forEach {
                    musics.add(it)
                }
                onUiThread {
                    countView.text = "Music • ${musics.size}"
                    musicPlaylistAdapter.notifyDataSetChanged()
                }
            } else {
                database.pyrumDao().getVideoByPlaylist(playlistName).forEach {
                    videos.add(it)
                }
                onUiThread {
                    countView.text = "Video(s) • ${videos.size}"
                    videoPlaylistAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    interface Listener {

    }
}