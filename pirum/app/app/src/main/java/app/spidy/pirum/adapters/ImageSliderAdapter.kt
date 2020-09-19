package app.spidy.pirum.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import app.spidy.pirum.R
import app.spidy.pirum.data.Other
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView

class ImageSliderAdapter(
    private val context: Context,
    private val others: ArrayList<Other>,
    private val isCenterCrop: () -> Boolean
): PagerAdapter() {
    val imageViews = HashMap<Int, ImageView>()

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int = others.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imageView = PhotoView(context)
        Glide.with(context).load(others[position].src)
            .error(R.drawable.broken_image)
            .into(imageView)
        container.addView(imageView)
        if (isCenterCrop()) {
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        }
        imageViews.put(position, imageView)
        return imageView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        imageViews.remove(position)
        container.removeView(`object` as View)
    }
}