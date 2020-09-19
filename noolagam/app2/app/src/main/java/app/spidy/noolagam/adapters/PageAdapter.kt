package app.spidy.noolagam.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.noolagam.R
import app.spidy.noolagam.data.Line
import com.bumptech.glide.Glide

class PageAdapter(
    private val context: Context,
    private val lines: List<Line>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var paraSize = pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_para))
    private var headingSize = pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_heading))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            0 -> ParaHolder(LayoutInflater.from(context).inflate(R.layout.line_para, parent, false))
            1 -> HeadingHolder(LayoutInflater.from(context).inflate(R.layout.line_heading, parent, false))
            2 -> ImageHolder(LayoutInflater.from(context).inflate(R.layout.line_image, parent, false))
            3 -> AudioHolder(LayoutInflater.from(context).inflate(R.layout.line_audio, parent, false))
            else -> AdHolder(LayoutInflater.from(context).inflate(R.layout.line_ad, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (lines[position].type) {
            "para" -> 0
            "heading" -> 1
            "image" -> 2
            "audio" -> 3
            else -> 4
        }
    }

    override fun getItemCount(): Int = lines.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(lines[position].type) {
            "para" -> renderPara(holder as ParaHolder, position)
            "heading" -> renderHeading(holder as HeadingHolder, position)
            "image" -> renderImage(holder as ImageHolder, position)
            "audio" -> renderAudio(holder as AudioHolder, position)
            else -> renderAd(holder as AdHolder, position)
        }
    }

    private fun renderPara(holder: ParaHolder, position: Int) {
        holder.paraView.text = lines[position].value
        holder.paraView.textSize = paraSize
    }

    private fun renderHeading(holder: HeadingHolder, position: Int) {
        holder.headingView.text = lines[position].value
        holder.headingView.textSize = headingSize
    }

    private fun renderImage(holder: ImageHolder, position: Int) {
        Glide.with(context).load(lines[position].value).into(holder.imageView)
    }

    private fun renderAudio(holder: AudioHolder, position: Int) {

    }

    private fun renderAd(holder: AdHolder, position: Int) {

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

    private fun pixelsToSp(px: Float): Float {
        val scaledDensity: Float = context.resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }


    // HOLDERS

    inner class ParaHolder(v: View): RecyclerView.ViewHolder(v) {
        val paraView: TextView = v.findViewById(R.id.paraView)
    }
    inner class HeadingHolder(v: View): RecyclerView.ViewHolder(v) {
        val headingView: TextView = v.findViewById(R.id.headingView)
    }
    inner class AudioHolder(v: View): RecyclerView.ViewHolder(v) {

    }
    inner class ImageHolder(v: View): RecyclerView.ViewHolder(v) {
        val imageView: ImageView = v.findViewById(R.id.imageView)
    }

    inner class AdHolder(v: View): RecyclerView.ViewHolder(v) {

    }
}