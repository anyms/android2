package app.spidy.oli.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.onUiThread
import app.spidy.oli.R
import app.spidy.oli.activities.PlayerActivity
import app.spidy.oli.data.Episode
import app.spidy.oli.utils.API
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.json.JSONObject


class PopularAdapter(
    private val context: Context,
    private val episodes: List<Episode>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var dp20: Int = 20
    private var coverImageUrl: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_popular_episode, parent, false)
        dp20 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,20f, context.resources.displayMetrics).toInt()

        return MainHolder(v)
    }

    override fun getItemCount(): Int = episodes.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder

        mainHolder.titleView.text = episodes[position].title
        loadCoverImage(episodes[position].channelId, mainHolder.coverImage, mainHolder.titleView)

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
            intent.putExtra("coverImageUrl", coverImageUrl)
            context.startActivity(intent)
        }
    }

    private fun loadCoverImage(channelId: String, coverImage: ImageView, titleView: TextView) {
        API.async.get(API.url("/channel/${channelId}")).then {
            val channel = JSONObject(it.text!!)
            val image = channel.getString("image")
            onUiThread {
                coverImageUrl = image
                Glide.with(context).load(image).listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadCoverImage(channelId, coverImage, titleView)
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        val bitmap = resource?.toBitmap()
                        Palette.from(bitmap!!).generate().getMutedColor(Color.parseColor("#222222")).also { color ->
                            titleView.setBackgroundColor(color)
                        }
                        return false
                    }
                }).into(coverImage)
            }
        }.catch {
            loadCoverImage(channelId, coverImage, titleView)
        }
    }

    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val rootView: CardView = v.findViewById(R.id.rootView)
        val coverImage: ImageView = v.findViewById(R.id.coverImage)
        val titleView: TextView = v.findViewById(R.id.titleView)
    }
}