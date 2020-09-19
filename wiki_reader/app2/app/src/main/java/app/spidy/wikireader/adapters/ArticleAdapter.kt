package app.spidy.wikireader.adapters

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.wikireader.R
import app.spidy.wikireader.data.Element
import com.bumptech.glide.Glide
import kotlin.concurrent.thread

class ArticleAdapter(
    private val context: Context,
    private val elements: List<Element>,
    private val switchSpeak: (Int) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var paraSize = pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_para))
    private var headingSize = pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_heading))
    private var highlightPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> HeadingHolder(LayoutInflater.from(context).inflate(R.layout.line_heading, parent, false))
            1 -> ImageHolder(LayoutInflater.from(context).inflate(R.layout.line_image, parent, false))
            else -> ParaHolder(LayoutInflater.from(context).inflate(R.layout.line_para, parent, false))
        }
    }

    override fun getItemCount(): Int = elements.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            elements[position].tagName.startsWith("h") -> renderHeading(holder as HeadingHolder, position)
            elements[position].tagName == "img" -> renderImage(holder as ImageHolder, position)
            else -> renderPara(holder as ParaHolder, position)
        }
    }

    private fun renderPara(holder: ParaHolder, position: Int) {
        holder.paraView.textSize = paraSize

        if (highlightPosition == position) {
            val spannableString = SpannableString(elements[position].text)
            spannableString.setSpan(BackgroundColorSpan(Color.YELLOW), 0, elements[position].text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.paraView.text = spannableString
        } else {
            val spannableString = SpannableString(elements[position].text)
            spannableString.setSpan(BackgroundColorSpan(Color.WHITE), 0, elements[position].text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.paraView.text = spannableString
        }

        holder.paraView.setOnClickListener {
            switchSpeak(position)
        }
    }

    private fun renderHeading(holder: HeadingHolder, position: Int) {
        holder.headingView.textSize = headingSize

        if (highlightPosition == position) {
            val spannableString = SpannableString(elements[position].text)
            spannableString.setSpan(BackgroundColorSpan(Color.YELLOW), 0, elements[position].text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.headingView.text = spannableString
        } else {
            val spannableString = SpannableString(elements[position].text)
            spannableString.setSpan(BackgroundColorSpan(Color.WHITE), 0, elements[position].text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.headingView.text = spannableString
        }

        holder.headingView.setOnClickListener {
            switchSpeak(position)
        }
    }

    private fun renderImage(holder: ImageHolder, position: Int) {
        Glide.with(context).load(elements[position].text).into(holder.imageView)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            elements[position].tagName.startsWith("h") -> 0
            elements[position].tagName == "img" -> 1
            else -> 2
        }
    }

    fun updateTextSize(num: Float) {
        if (num == 1f) {
            paraSize = pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_para))
            headingSize = pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_heading))
        } else {
            paraSize = pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_para)) * num
            headingSize = pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_heading)) * num
        }
    }

    fun highlight(position: Int) {
        highlightPosition = position
    }

    private fun pixelsToSp(px: Float): Float {
        val scaledDensity: Float = context.resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }


    inner class ParaHolder(v: View): RecyclerView.ViewHolder(v) {
        val paraView: TextView = v.findViewById(R.id.paraView)
    }
    inner class HeadingHolder(v: View): RecyclerView.ViewHolder(v) {
        val headingView: TextView = v.findViewById(R.id.headingView)
    }
    inner class ImageHolder(v: View): RecyclerView.ViewHolder(v) {
        val imageView: ImageView = v.findViewById(R.id.imageView)
    }
}