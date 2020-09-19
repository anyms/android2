package app.spidy.cyberwire.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.cyberwire.R
import app.spidy.cyberwire.activities.PlayerActivity
import app.spidy.cyberwire.data.Episode
import com.bumptech.glide.Glide
import com.google.gson.Gson

class EpisodeAdapter(
    private val context: Context,
    private val coverImage: String,
    private val episodes: List<Episode>,
    private val getPageNum: () -> Int,
    private val isNextExists: () -> Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val gson = Gson()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_episode, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = episodes.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder

        mainHolder.titleView.text = episodes[position].title
        mainHolder.dateView.text = episodes[position].date
        Glide.with(context).load(coverImage).into(mainHolder.coverImageView)

        mainHolder.rootView.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("is_channel", true)
            intent.putExtra("coverImageUrl", coverImage)
            intent.putExtra("data", gson.toJson(episodes))
            intent.putExtra("index", position)
            intent.putExtra("current_page", getPageNum())
            intent.putExtra("is_next_exists", isNextExists())
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