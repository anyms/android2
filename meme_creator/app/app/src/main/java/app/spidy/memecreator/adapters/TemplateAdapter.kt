package app.spidy.memecreator.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.onUiThread
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
import kotlin.concurrent.thread


class TemplateAdapter(
    private val context: Context?,
    private val templates: List<Template>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val captions = ArrayList<String>()
    private val urls = ArrayList<String>()
    private val tinyDB = TinyDB(context!!)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_meme_item, parent, false)
        urls.clear()
        captions.clear()
        for (temp in templates) {
            captions.add(temp.caption)
            urls.add(temp.url)
        }
        return MainHolder(v)
    }

    override fun getItemCount(): Int = templates.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        mainHolder.captionView.text = templates[position].caption

        if (templates[position].thumb == "null" || templates[position].thumb == "") {
            Handler().postDelayed({
                loadImage(mainHolder.memeImageView, templates[position].url)
            }, 100)
            mainHolder.isGifView.visibility = View.GONE
        } else {
            Handler().postDelayed({
                loadGif(mainHolder.memeImageView, templates[position])
            }, 100)
            mainHolder.isGifView.visibility = View.VISIBLE
        }

        mainHolder.rootView.setOnClickListener {
            tinyDB.putListString(ImageSliderActivity.URLS, urls)
            tinyDB.putListString(ImageSliderActivity.CAPTIONS, captions)
            tinyDB.putInt(ImageSliderActivity.INDEX, position)
            tinyDB.putString(ImageSliderActivity.TYPE, "meme")
            val intent = Intent(context, ImageSliderActivity::class.java)
            context?.startActivity(intent)
        }
    }

    private fun loadImage(imageView: ImageView, image: String) {
        val options = RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565)
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(context!!)
            .load(image)
            .thumbnail(Glide.with(context).load(R.drawable.loading))
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(options)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Handler().postDelayed({ loadImage(imageView, image) }, 1)
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
        val isGifView: ImageView = v.findViewById(R.id.isGifView)
    }
}