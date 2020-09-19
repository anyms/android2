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
import app.spidy.memecreator.data.SavedMeme
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


class SavedMemeAdapter(
    private val context: Context?,
    private val savedMemes: List<SavedMeme>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val tinyDB = TinyDB(context!!)
    private val urls = ArrayList<String>()
    private val captions = ArrayList<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_saved_meme, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = savedMemes.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        loadImage(mainHolder.memeImageView, savedMemes[position].uri)

        mainHolder.memeImageView.setOnClickListener {
            urls.clear()
            captions.clear()
            for (temp in savedMemes) {
                captions.add("")
                urls.add(temp.uri)
            }

            tinyDB.putListString(ImageSliderActivity.URLS, urls)
            tinyDB.putListString(ImageSliderActivity.CAPTIONS, captions)
            tinyDB.putInt(ImageSliderActivity.INDEX, position)
            tinyDB.putString(ImageSliderActivity.TYPE, "gif")
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

    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val memeImageView: ImageView = v.findViewById(R.id.memeImageView)
    }
}