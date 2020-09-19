package app.spidy.cyberwire.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import app.spidy.cyberwire.R
import app.spidy.cyberwire.activities.ChannelActivity
import app.spidy.cyberwire.data.Channel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


class ChannelAdapter(
    private val context: Context,
    private val channels: List<Channel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_channel, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = channels.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder

        mainHolder.titleView.text = channels[position].title
        loadCoverImage(channels[position].image, mainHolder.titleView, mainHolder.coverImage)
        mainHolder.rootView.setOnClickListener {
            val intent = Intent(context, ChannelActivity::class.java)
            intent.putExtra("channel_id", channels[position].channelId)
            intent.putExtra("author", channels[position].author)
            intent.putExtra("description", channels[position].description)
            intent.putExtra("title", channels[position].title)
            intent.putExtra("category", channels[position].category)
            intent.putExtra("image", channels[position].image)
            intent.putExtra("rss", channels[position].rss)
            intent.putExtra("uId", channels[position].uId)
            intent.putExtra("viewCount", channels[position].viewCount)
            intent.putExtra("website", channels[position].website)
            context.startActivity(intent)
        }
    }

    private fun loadCoverImage(image: String, titleView: TextView, coverImage: ImageView) {
        Glide.with(context).load(image).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                loadCoverImage(image, titleView, coverImage)
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                val bitmap = resource?.toBitmap()
                Palette.from(bitmap!!).generate().getMutedColor(Color.parseColor("#222222")).also {
                    titleView.setBackgroundColor(it)
                }
                return false
            }
        }).into(coverImage)
    }

    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val rootView: CardView = v.findViewById(R.id.rootView)
        val coverImage: ImageView = v.findViewById(R.id.coverImage)
        val titleView: TextView = v.findViewById(R.id.titleView)
    }
}