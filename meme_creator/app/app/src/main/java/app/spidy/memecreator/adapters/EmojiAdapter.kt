package app.spidy.memecreator.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.memecreator.R
import app.spidy.memecreator.fragments.EmojiesBottomSheetFragment

class EmojiAdapter(
    private val context: Context?,
    private val emojies: List<String>,
    private val listener: EmojiesBottomSheetFragment.Listener?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_emoji_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int {
        return emojies.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        mainHolder.emojiView.text = emojies[position]

        mainHolder.emojiView.setOnClickListener {
            listener?.onAdd(emojies[position])
        }
    }

    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val emojiView: TextView = v.findViewById(R.id.emojiView)
    }
}