package app.spidy.tamilmemecreator.fragments

import android.app.Dialog
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.spidy.tamilmemecreator.R
import app.spidy.tamilmemecreator.adapters.FontAdapter
import app.spidy.tamilmemecreator.data.Font
import app.spidy.tamilmemecreator.utils.FontFace
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class TextFontBottomSheetFragment : BottomSheetDialogFragment() {
    var listener: Listener? = null

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.1f)

        val viewGroup: ViewGroup? = null
        val v = LayoutInflater.from(context).inflate(R.layout.fragment_text_font_bottom_sheet, viewGroup)
        dialog.setContentView(v)
        (v.parent as View).setBackgroundColor(ContextCompat.getColor(context!!, android.R.color.transparent))

        var typeface: Typeface? = null

        val applyBtn: TextView = v.findViewById(R.id.applyBtn)
        val applyCheckbox: CheckBox = v.findViewById(R.id.applyCheckbox)
        val rootView: ConstraintLayout = v.findViewById(R.id.rootView)
        val fontRecyclerView: RecyclerView = v.findViewById(R.id.fontRecyclerView)
        val uniFontView: TextView = v.findViewById(R.id.uniFontView)
        val tamilFontView: TextView = v.findViewById(R.id.tamilFontView)

        uniFontView.setPadding(20, 10, 20, 10)
        tamilFontView.setPadding(20, 10, 20, 10)

        val fonts = ArrayList<Font>()
        val fontFace = FontFace(requireContext())
        fontFace.getFontNames().forEach {
            Log.d("hello", it)
            fonts.add(fontFace.generateFont(it)!!)
        }
        fontRecyclerView.adapter = FontAdapter(context!!, fonts, false) {
            typeface = it
            if (applyCheckbox.isChecked) listener?.onApply(typeface!!)
        }
        fontRecyclerView.layoutManager = GridLayoutManager(context, 4)


        applyBtn.setOnClickListener {
            if (typeface != null) listener?.onApply(typeface!!)
            dismiss()
        }

        tamilFontView.setOnClickListener {
            fonts.clear()
            fontFace.getTamilFontNames().forEach {
                fonts.add(fontFace.generateTamilFont(it)!!)
            }
            fontRecyclerView.adapter = FontAdapter(context!!, fonts, true) {
                typeface = it
                if (applyCheckbox.isChecked) listener?.onApply(typeface!!)
            }
            uniFontView.setBackgroundResource(0)
            tamilFontView.setBackgroundResource(R.drawable.rounded_corner_accent)
            uniFontView.setPadding(20, 10, 20, 10)
            tamilFontView.setPadding(20, 10, 20, 10)
            uniFontView.setTextColor(ContextCompat.getColor(context!!, R.color.colorToolbar))
            tamilFontView.setTextColor(ContextCompat.getColor(context!!, R.color.colorWhite))
        }

        uniFontView.setOnClickListener {
            fonts.clear()
            fontFace.getFontNames().forEach {
                fonts.add(fontFace.generateFont(it)!!)
            }
            fontRecyclerView.adapter = FontAdapter(context!!, fonts, false) {
                typeface = it
                if (applyCheckbox.isChecked) listener?.onApply(typeface!!)
            }
            tamilFontView.setBackgroundResource(0)
            uniFontView.setBackgroundResource(R.drawable.rounded_corner_accent)
            uniFontView.setPadding(20, 10, 20, 10)
            tamilFontView.setPadding(20, 10, 20, 10)
            tamilFontView.setTextColor(ContextCompat.getColor(context!!, R.color.colorToolbar))
            uniFontView.setTextColor(ContextCompat.getColor(context!!, R.color.colorWhite))
        }
    }

    interface Listener {
        fun onApply(typeface: Typeface)
    }

    companion object {
        fun newInstance(): TextFontBottomSheetFragment {
            return TextFontBottomSheetFragment()
        }
    }
}