package app.spidy.oli.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.oli.R
import app.spidy.oli.activities.PlayerActivity
import app.spidy.oli.data.Episode
import com.bumptech.glide.Glide

class SearchAdapter(
    private val context: Context, private val episodes: List<Episode>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_episode, parent, false)
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
    }

    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val rootView: LinearLayout = v.findViewById(R.id.rootView)
        val dateView: TextView = v.findViewById(R.id.dateView)
        val titleView: TextView = v.findViewById(R.id.titleView)
        val coverImageView: ImageView = v.findViewById(R.id.coverImageView)
    }
}