package app.spidy.cyberwire.adapters

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import app.spidy.cyberwire.R
import app.spidy.cyberwire.activities.PlayerActivity
import app.spidy.cyberwire.data.Episode
import app.spidy.cyberwire.databases.PodcastDatabase
import com.bumptech.glide.Glide
import java.io.File
import kotlin.concurrent.thread

class DownloadAdapter(
    private val context: Context, private val episodes: ArrayList<Episode>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val database = Room.databaseBuilder(context, PodcastDatabase::class.java, "PodcastDatabase")
        .fallbackToDestructiveMigration().build()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_episode_downloaded, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = episodes.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder

        mainHolder.titleView.text = episodes[position].title
        mainHolder.dateView.text = episodes[position].date
        Glide.with(context).load(episodes[position].coverImage).into(mainHolder.coverImageView)
        mainHolder.rootView.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("uId", episodes[position].uId)
            intent.putExtra("title", episodes[position].title)
            intent.putExtra("audio", episodes[position].audio)
            intent.putExtra("channelId", episodes[position].channelId)
            intent.putExtra("date", episodes[position].date)
            intent.putExtra("timestamp", episodes[position].timestamp)
            intent.putExtra("viewCount", episodes[position].viewCount)
            intent.putExtra("downloadedLocation", episodes[position].downloadedLocation)
            intent.putExtra("coverImageUrl", episodes[position].coverImage)
            context.startActivity(intent)
        }

        mainHolder.menuImageView.setOnClickListener {
            val popupMenu =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    PopupMenu(
                        context,
                        mainHolder.menuImageView,
                        Gravity.NO_GRAVITY,
                        android.R.attr.actionOverflowMenuStyle,
                        0
                    )
                } else {
                    PopupMenu(context, mainHolder.menuImageView)
                }
            popupMenu.inflate(R.menu.menu_downloaded_item)
            popupMenu.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.menuDelete -> {
                        context.newDialog().withTitle("Are you sure?")
                            .withMessage("You can not undo this action. Do you really want to delete this file?")
                            .withCancelable(false)
                            .withPositiveButton("No, keep it") { dialog ->
                                dialog.dismiss()
                            }
                            .withNegativeButton("Yes") { dialog ->
                                thread {
                                    database.dao().removeEpisode(episodes[position])
                                    File(episodes[position].downloadedLocation).delete()
                                    onUiThread {
                                        episodes.removeAt(position)
                                        notifyItemRemoved(position)
                                        context.toast("Deleted.")
                                    }
                                }
                            }
                            .create()
                            .show()
                    }
                }
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }
    }

    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val rootView: LinearLayout = v.findViewById(R.id.rootView)
        val dateView: TextView = v.findViewById(R.id.dateView)
        val titleView: TextView = v.findViewById(R.id.titleView)
        val coverImageView: ImageView = v.findViewById(R.id.coverImageView)
        val menuImageView: ImageView = v.findViewById(R.id.menuImageView)
    }
}