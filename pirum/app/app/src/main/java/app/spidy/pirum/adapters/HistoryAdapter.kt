package app.spidy.pirum.adapters

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import app.spidy.pirum.R
import app.spidy.pirum.data.History
import app.spidy.pirum.databases.PyrumDatabase
import app.spidy.pirum.fragments.PlaylistBottomSheetFragment
import app.spidy.pirum.interfaces.RenameListener
import app.spidy.pirum.services.MediaDownloadService
import app.spidy.pirum.utils.DownloadStatus
import com.google.android.exoplayer2.offline.DownloadService
import kotlin.concurrent.thread


class HistoryAdapter(
    private val context: Context,
    private val history: ArrayList<History>,
    private val getSupportFragmentManager: () -> FragmentManager,
    private val updateHistory: () -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val database = Room.databaseBuilder(context, PyrumDatabase::class.java, "PyrumDatabase")
        .fallbackToDestructiveMigration().build()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_history_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = history.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder

        mainHolder.playlistNameView.text = history[position].playlistName
        mainHolder.subTextView.text = if (history[position].type == "music") {
            mainHolder.playlistIconView.setImageResource(R.drawable.img_music_list)
            "Music • ${history[position].itemCount}"
        } else if (history[position].type == "video") {
            mainHolder.playlistIconView.setImageResource(R.drawable.img_video_list)
            "Video(s) • ${history[position].itemCount}"
        } else {
                mainHolder.playlistIconView.setImageResource(R.drawable.img_image_list)
            "Image(s) • ${history[position].itemCount}"
        }

        mainHolder.menuImageView.setOnClickListener {
            showOptionMenu(it, position)
        }

        mainHolder.rootView.setOnClickListener {
            openPlaylist(position)
        }
    }


    private fun showOptionMenu(v: View, position: Int) {
        val popupMenu =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                PopupMenu(
                    context,
                    v,
                    Gravity.NO_GRAVITY,
                    android.R.attr.actionOverflowMenuStyle,
                    0
                )
            } else {
                PopupMenu(context, v)
            }
        popupMenu.inflate(R.menu.menu_history)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.rename -> showRenameDialog(history[position].playlistName) { name ->
                    if (name.trim() != "") {
                        renamePlaylist(history[position], name, object : RenameListener {
                            override fun onFail() {
                                context.toast("Playlist name already exist", true)
                            }
                            override fun onSuccess() {
                                history[position].playlistName = name
                                notifyItemChanged(position)
                            }
                        })
                    }
                }
                R.id.open -> openPlaylist(position)
                R.id.delete -> {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Are you sure?")
                    builder.setMessage("This action can not be undone, do you really want to delete this playlist?")
                    builder.setIcon(android.R.drawable.stat_sys_warning)
                    val dialog = builder.create()
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "No, keep it") {d, _ ->
                        d.dismiss()
                    }

                    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Yes") {d, _ ->
                        d.dismiss()
                        deletePlaylist(history[position].playlistName, history[position].type == "music")
                    }
                    dialog.show()
                }
            }
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
    }

    private fun deletePlaylist(playlistName: String, isMusic: Boolean) {
        thread {
            if (isMusic) {
                val musics = database.pyrumDao().getMusicByPlaylist(playlistName)
                for (music in musics) {
                    if (music.status != DownloadStatus.STATE_NONE) {
                        DownloadService.sendRemoveDownload(
                            context,
                            MediaDownloadService::class.java,
                            music.uId,
                            false)
                    }
                    database.pyrumDao().removeMusic(music)
                }
            } else {
                val videos = database.pyrumDao().getVideoByPlaylist(playlistName)
                for (video in videos) {
                    if (video.status != DownloadStatus.STATE_NONE) {
                        DownloadService.sendRemoveDownload(
                            context,
                            MediaDownloadService::class.java,
                            video.uId,
                            false)
                        if (video.type == "separate") {
                            DownloadService.sendRemoveDownload(
                                context,
                                MediaDownloadService::class.java,
                                "${video.aSrc}${video.uId}",
                                false)
                        }
                    }
                    database.pyrumDao().removeVideo(video)
                }
            }

            onUiThread {
                updateHistory()
            }
        }
    }


    private fun showRenameDialog(oldName: String, onSuccess: (name: String) -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Rename")
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(oldName)
        input.setSelectAllOnFocus(true)
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ -> onSuccess(input.text.toString()) }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun renamePlaylist(history: History, newName: String, renameListener: RenameListener) {
        thread {
            if (history.type == "music") {
                val musicCheck = database.pyrumDao().getMusicByPlaylist(newName)
                if (musicCheck.isNotEmpty()) {
                    onUiThread { renameListener.onFail() }
                } else {
                    val music = database.pyrumDao().getMusicByPlaylist(history.playlistName)
                    for (m in music) {
                        m.playlistName = newName
                        database.pyrumDao().updateMusic(m)
                    }
                    onUiThread { renameListener.onSuccess() }
                }
            } else if (history.type == "video") {
                val videoCheck = database.pyrumDao().getVideoByPlaylist(newName)
                if (videoCheck.isNotEmpty()) {
                    onUiThread { renameListener.onFail() }
                } else {
                    val videos = database.pyrumDao().getVideoByPlaylist(history.playlistName)
                    for (m in videos) {
                        m.playlistName = newName
                        database.pyrumDao().updateVideo(m)
                    }
                    onUiThread { renameListener.onSuccess() }
                }
            }
        }
    }

    private fun openPlaylist(position: Int) {
        val bottomSheet = PlaylistBottomSheetFragment()
        bottomSheet.playlistName = history[position].playlistName
        bottomSheet.isMusic = history[position].type == "music"
        bottomSheet.listener = object : PlaylistBottomSheetFragment.Listener {

        }
        bottomSheet.show(getSupportFragmentManager(), "Playlist Bottom Sheet")
    }


    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val playlistIconView: ImageView = v.findViewById(R.id.playlistIconView)
        val playlistNameView: TextView = v.findViewById(R.id.playlistNameView)
        val subTextView: TextView = v.findViewById(R.id.subTextView)
        val menuImageView: ImageView = v.findViewById(R.id.menuImageView)
        val rootView: LinearLayout = v.findViewById(R.id.rootView)
    }
}