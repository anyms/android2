package app.spidy.pirum.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.TinyDB
import app.spidy.pirum.R
import app.spidy.pirum.activities.MusicPlayerActivity
import app.spidy.pirum.data.Music
import app.spidy.pirum.services.MediaDownloadService
import app.spidy.pirum.utils.DownloadStatus
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.util.Util

class MusicPlaylistAdapter(
    private val context: Context,
    private val musics: ArrayList<Music>,
    private val bindToService: () -> Unit,
    private val purchaseCallback: () -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val tinyDB: TinyDB = TinyDB(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_media_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = musics.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        var isOffline = false
        when (musics[position].status) {
            DownloadStatus.STATE_NONE,
            DownloadStatus.STATE_FAILED,
            DownloadStatus.STATE_STOPPED -> {
                mainHolder.progressContainer.visibility = View.GONE
                mainHolder.downloadBtn.visibility = View.VISIBLE
            }
            DownloadStatus.STATE_QUEUED -> {
                mainHolder.progressContainer.visibility = View.GONE
                mainHolder.downloadBtn.visibility = View.VISIBLE
                mainHolder.progressBar.isIndeterminate = true
            }
            DownloadStatus.STATE_COMPLETED -> {
                isOffline = true
                mainHolder.downloadBtn.setImageResource(R.drawable.ic_offline)
                mainHolder.progressContainer.visibility = View.GONE
                mainHolder.downloadBtn.visibility = View.VISIBLE
            }
            else -> {
                mainHolder.progressContainer.visibility = View.VISIBLE
                mainHolder.downloadBtn.visibility = View.GONE
                mainHolder.progressBar.isIndeterminate = false
            }
        }

        mainHolder.progressContainer.setOnClickListener {
            DownloadService.sendSetStopReason(
                context,
                MediaDownloadService::class.java,
                musics[position].uId,
                1,
                false
            )
        }

        mainHolder.playlistIconView.setImageResource(R.drawable.img_music)
        mainHolder.playlistNameView.text = musics[position].title
        mainHolder.subTextView.text = musics[position].src

        mainHolder.rootView.setOnClickListener {
            val appIntent = Intent(context, MusicPlayerActivity::class.java)
            appIntent.putExtra("playlistName", musics[position].playlistName)
            appIntent.putExtra("isFromLocal", true)
            appIntent.putExtra("currentIndex", position)
            appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(appIntent)
        }

        mainHolder.downloadBtn.setOnClickListener {
            if (tinyDB.getBoolean("isPro")) {
                if (!isOffline) {
                    val music = musics[position]
                    bindToService()
                    val downloadRequest = if (music.type == "stream") {
                        DownloadRequest(
                            music.uId,
                            DownloadRequest.TYPE_HLS,
                            Uri.parse(music.src),
                            listOf(),
                            null,
                            Util.toByteArray(music.title.byteInputStream())
                        )

                    } else {
                        DownloadRequest(
                            music.uId,
                            DownloadRequest.TYPE_PROGRESSIVE,
                            Uri.parse(music.src),
                            listOf(),
                            null,
                            Util.toByteArray(music.title.byteInputStream())
                        )
                    }

                    DownloadService.sendAddDownload(
                        context,
                        MediaDownloadService::class.java,
                        downloadRequest,
                        false
                    )

                }
            } else {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Upgrade")
                builder.setCancelable(false)

                builder.setMessage(context.getString(R.string.upgrade_message))
                val dialog = builder.create()
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Purchase") {d, _ ->
                    d.dismiss()
                    purchaseCallback()
                }
                dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Not now") {d, _ ->
                    d.dismiss()
                }
                dialog.show()
            }
        }
    }


    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val playlistIconView: ImageView = v.findViewById(R.id.playlistIconView)
        val playlistNameView: TextView = v.findViewById(R.id.playlistNameView)
        val subTextView: TextView = v.findViewById(R.id.subTextView)
        val rootView: LinearLayout = v.findViewById(R.id.rootView)
        val downloadBtn: ImageView = v.findViewById(R.id.downloadBtn)
        val progressBar: ProgressBar = v.findViewById(R.id.progressBar)
        val progressContainer: FrameLayout = v.findViewById(R.id.progressContainer)
    }
}