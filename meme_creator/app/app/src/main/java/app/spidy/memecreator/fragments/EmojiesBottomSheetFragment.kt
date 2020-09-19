package app.spidy.memecreator.fragments

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.EmojiAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class EmojiesBottomSheetFragment : BottomSheetDialogFragment() {
    var listener: Listener? = null

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.1f)

        val viewGroup: ViewGroup? = null
        val v = LayoutInflater.from(context).inflate(R.layout.fragment_emojies_bottom_sheet, viewGroup)
        dialog.setContentView(v)
        (v.parent as View).setBackgroundColor(ContextCompat.getColor(context!!, android.R.color.transparent))

        val emojiesRecyclerView: RecyclerView = v.findViewById(R.id.emojiesRecyclerView)
        emojiesRecyclerView.adapter = EmojiAdapter(context, getEmojies(), listener)
        emojiesRecyclerView.layoutManager = GridLayoutManager(context, 5)
    }

    fun getEmojies(): List<String> {
        val convertedEmojiList = ArrayList<String>()
        val emojiList = resources.getStringArray(R.array.emojies)
        emojiList.forEach { emojiUnicode ->
            convertedEmojiList.add(convertEmoji(emojiUnicode))
        }
        return convertedEmojiList
    }

    private fun convertEmoji(emoji: String): String {
        return try {
            val convertEmojiToInt = Integer.parseInt(emoji.substring(2), 16)
            String(Character.toChars(convertEmojiToInt))
        } catch (e: NumberFormatException) {
            ""
        }
    }

    interface Listener {
        fun onAdd(emoji: String)
    }

    companion object {
        fun newInstance(): EmojiesBottomSheetFragment {
            return EmojiesBottomSheetFragment()
        }
    }
}