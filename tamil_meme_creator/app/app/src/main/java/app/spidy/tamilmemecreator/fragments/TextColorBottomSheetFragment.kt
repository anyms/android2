package app.spidy.tamilmemecreator.fragments

import android.app.Dialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import app.spidy.tamilmemecreator.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import yuku.ambilwarna.AmbilWarnaDialog


class TextColorBottomSheetFragment : BottomSheetDialogFragment() {
    var listener: Listener? = null

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.1f)

        val viewGroup: ViewGroup? = null
        val v = LayoutInflater.from(context).inflate(R.layout.fragment_text_color_bottom_sheet, viewGroup)
        dialog.setContentView(v)
        (v.parent as View).setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))

        val applyBtn: TextView = v.findViewById(R.id.applyBtn)
        val rootView: ConstraintLayout = v.findViewById(R.id.rootView)
        val applyCheckbox: CheckBox = v.findViewById(R.id.applyCheckbox)

        val fgColorView: View = v.findViewById(R.id.fgColorView)
        val bgColorView: View = v.findViewById(R.id.bgColorView)
        val removeBgBtn: Button = v.findViewById(R.id.removeBgBtn)

        var fgColor: Int = 0
        var bgColor: Int = 0
        arguments?.also {
            fgColor = it.getInt("fgColor")
            bgColor = it.getInt("bgColor")
            fgColorView.setBackgroundColor(fgColor)
            bgColorView.setBackgroundColor(bgColor)
        }

        applyBtn.setOnClickListener {
            listener?.onApply(fgColor, bgColor)
            dismiss()
        }

        fgColorView.setOnClickListener {
            val colorPicker = AmbilWarnaDialog(context, fgColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
                override fun onOk(dialog: AmbilWarnaDialog?, c: Int) {
                    fgColor = c
                    fgColorView.setBackgroundColor(fgColor)
                    if (applyCheckbox.isChecked) listener?.onApply(fgColor, bgColor)
                }
            })
            colorPicker.show()
        }

        bgColorView.setOnClickListener {
            val colorPicker = AmbilWarnaDialog(context, bgColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
                override fun onOk(dialog: AmbilWarnaDialog?, c: Int) {
                    bgColor = c
                    bgColorView.setBackgroundColor(bgColor)
                    if (applyCheckbox.isChecked) listener?.onApply(fgColor, bgColor)
                }
            })
            colorPicker.show()
        }

        removeBgBtn.setOnClickListener {
            bgColor = Color.TRANSPARENT
            listener?.onApply(fgColor, bgColor)
            dismiss()
        }
    }

    interface Listener {
        fun onApply(fgColor: Int, bgColor: Int)
    }

    companion object {
        fun newInstance(): TextColorBottomSheetFragment {
            return TextColorBottomSheetFragment()
        }
    }
}