package app.spidy.lankanews.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.lankanews.R

class ArticleAdapter(
    private val context: Context,
    private val paras: List<String>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var paraSize = pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_para))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_para, parent, false)
        return ParaHolder(v)
    }

    override fun getItemCount(): Int = paras.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val paraHolder = holder as ParaHolder
        paraHolder.paraView.text = paras[position]
        paraHolder.paraView.textSize = paraSize
    }


    fun updateTextSize(num: Float) {
        paraSize = if (num == 1f) {
            pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_para))
        } else {
            pixelsToSp(context.resources.getDimension(R.dimen.reader_text_size_para)) * num
        }
    }

    private fun pixelsToSp(px: Float): Float {
        val scaledDensity: Float = context.resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }

    inner class ParaHolder(v: View): RecyclerView.ViewHolder(v) {
        val paraView: TextView = v.findViewById(R.id.paraView)
    }
}