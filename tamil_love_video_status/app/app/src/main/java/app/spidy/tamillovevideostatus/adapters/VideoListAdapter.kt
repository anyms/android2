package app.spidy.tamillovevideostatus.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import app.spidy.tamillovevideostatus.R
import app.spidy.tamillovevideostatus.activities.PlayerActivity
import app.spidy.tamillovevideostatus.data.Video
import com.bumptech.glide.Glide

class VideoListAdapter(
    private val context: Context,
    private val videos: List<Video>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_video_item, parent, false)

        return MainHolder(v)
    }

    override fun getItemCount(): Int = videos.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder

        mainHolder.titleView.text = videos[position].title
        Glide.with(context).load(videos[position].thumb).into(mainHolder.imageView)

        mainHolder.rootView.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("is_intent", true)
            intent.putExtra("uId", videos[position].uId)
            intent.putExtra("title", videos[position].title)
            intent.putExtra("videoId", videos[position].videoId)
            intent.putExtra("tags", videos[position].tags)
            intent.putExtra("viewCount", videos[position].viewCount)
            intent.putExtra("downloadCount", videos[position].downloadCount)
            intent.putExtra("shareCount", videos[position].shareCount)
            intent.putExtra("isExpired", videos[position].isExpired)
            intent.putExtra("category", videos[position].category)
            intent.putExtra("thumb", videos[position].thumb)
            intent.putExtra("data", videos[position].data)
            intent.putExtra("expire", videos[position].expire)
            context.startActivity(intent)
        }
    }


    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val imageView: ImageView = v.findViewById(R.id.imageView)
        val titleView: TextView = v.findViewById(R.id.titleView)
        val rootView: ConstraintLayout = v.findViewById(R.id.rootView)
    }
}