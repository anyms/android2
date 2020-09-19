package app.spidy.memecreator.adapters

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.TinyDB
import app.spidy.memecreator.R
import app.spidy.memecreator.activities.ImageSliderActivity
import app.spidy.memecreator.data.Template
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

class GifsAdapter(
    private val context: Context?,
    private val gifs: List<Template>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val captions = ArrayList<String>()
    private val urls = ArrayList<String>()
    private val tinyDB = TinyDB(context!!)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_meme_item, parent, false)
        urls.clear()
        captions.clear()
        for (gif in gifs) {
            captions.add(gif.caption)
            urls.add(gif.url)
        }
        return MainHolder(v)
    }

    override fun getItemCount(): Int = gifs.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        mainHolder.captionView.text = gifs[position].caption
        Handler().postDelayed({
            loadGif(mainHolder.memeImageView, gifs[position])
        }, 100)

        mainHolder.rootView.setOnClickListener {
            tinyDB.putListString(ImageSliderActivity.URLS, urls)
            tinyDB.putListString(ImageSliderActivity.CAPTIONS, captions)
            tinyDB.putInt(ImageSliderActivity.INDEX, position)
            tinyDB.putString(ImageSliderActivity.TYPE, "gif")
            val intent = Intent(context, ImageSliderActivity::class.java)
            context?.startActivity(intent)
        }
    }

    private fun loadGif(imageView: ImageView, gif: Template) {
        val options = RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565)
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(context!!)
            .load(gif.url)
            .placeholder(R.drawable.loading)
            .thumbnail(Glide.with(context).load(gif.thumb).thumbnail(Glide.with(context).load(R.drawable.loading)))
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .apply(options)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Handler().postDelayed({ loadGif(imageView, gif) }, 1)
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(imageView)
    }


    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val memeImageView: ImageView = v.findViewById(R.id.memeImageView)
        val captionView: TextView = v.findViewById(R.id.captionView)
        val rootView: FrameLayout = v.findViewById(R.id.rootView)
    }
}