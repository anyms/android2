package app.spidy.memecreator.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.memecreator.R
import app.spidy.memecreator.data.Pack
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

class PackAdapter(
    private val context: Context?,
    private val packs: List<Pack>,
    private val showInstallDialog: (pack: Pack) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_packs_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = packs.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        loadImage(mainHolder.packImageView, packs[position].coverImage)
        mainHolder.packTitleView.text = packs[position].title
        mainHolder.rootView.setOnClickListener {
            showInstallDialog(packs[position])
        }

        if (packs[position].type == "gif") {
            mainHolder.gifPack.visibility = View.VISIBLE
        } else {
            mainHolder.gifPack.visibility = View.GONE
        }
    }

    private fun loadImage(imageView: ImageView, image: String) {
        val options = RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(context!!)
            .load(image)
            .thumbnail(Glide.with(context).load(R.drawable.loading).override(250, 250))
            .transition(DrawableTransitionOptions.withCrossFade(300))
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
        val packImageView: ImageView = v.findViewById(R.id.packImageView)
        val packTitleView: TextView = v.findViewById(R.id.packTitleView)
        val rootView: CardView = v.findViewById(R.id.rootView)
        val gifPack: ImageView = v.findViewById(R.id.gifPack)
    }
}