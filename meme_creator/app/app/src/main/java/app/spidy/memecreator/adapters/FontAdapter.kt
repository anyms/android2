package app.spidy.memecreator.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.spidy.memecreator.R
import app.spidy.memecreator.data.Font

class FontAdapter(
    private val context: Context,
    private val fonts: List<Font>,
    private val isTamil: Boolean,
    private val onFontSelect: (Typeface) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var usedView: TextView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_font_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int {
        return fonts.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        mainHolder.textView.typeface = fonts[position].typeface
        mainHolder.textView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorWhite))

        if (isTamil) {
            mainHolder.textView.text = "அஆ\nஇஈ"
        } else {
            mainHolder.textView.text = "ABC\nabc"
        }

        mainHolder.textView.setOnClickListener {
            onFontSelect.invoke(fonts[position].typeface)
            usedView?.setBackgroundColor(ContextCompat.getColor(context, R.color.colorWhite))
            mainHolder.textView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorGray))
            usedView = mainHolder.textView
        }
    }


    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val textView: TextView = v.findViewById(R.id.textView)
    }
}