package app.spidy.tamilmemecreator.fragments

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import app.spidy.tamilmemecreator.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import hearsilent.discreteslider.DiscreteSlider
import yuku.ambilwarna.AmbilWarnaDialog


class TextOutlineBottomSheetFragment : BottomSheetDialogFragment() {
    var listener: Listener? = null

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.1f)

        val viewGroup: ViewGroup? = null
        val v = LayoutInflater.from(context).inflate(R.layout.fragment_text_outline_bottom_sheet, viewGroup)
        dialog.setContentView(v)
        (v.parent as View).setBackgroundColor(ContextCompat.getColor(context!!, android.R.color.transparent))

        val applyBtn: TextView = v.findViewById(R.id.applyBtn)
        val textOutlineSlider: DiscreteSlider = v.findViewById(R.id.textOutlineSlider)
        val applyCheckbox: CheckBox = v.findViewById(R.id.applyCheckbox)
        val rootView: ConstraintLayout = v.findViewById(R.id.rootView)
        val colorView: View = v.findViewById(R.id.colorView)

        var outlineSize = 0
        var outlineColor = 0

        arguments?.also {
            outlineSize = it.getInt("outline_size")
            outlineColor = it.getInt("outline_color")
            textOutlineSlider.progress = outlineSize
            colorView.setBackgroundColor(outlineColor)
        }

        applyBtn.setOnClickListener {
            listener?.onApply(outlineColor, outlineSize)
            dismiss()
        }

        textOutlineSlider.setOnValueChangedListener(object : DiscreteSlider.OnValueChangedListener() {
            override fun onValueChanged(progress: Int, fromUser: Boolean) {
                outlineSize = progress
                if (applyCheckbox.isChecked) listener?.onApply(outlineColor, outlineSize)
            }
        })

        colorView.setOnClickListener {
            val colorPicker = AmbilWarnaDialog(context, outlineColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
                override fun onOk(dialog: AmbilWarnaDialog?, c: Int) {
                    outlineColor = c
                    colorView.setBackgroundColor(outlineColor)
                    if (applyCheckbox.isChecked) listener?.onApply(outlineColor, outlineSize)
                }
            })
            colorPicker.show()
        }
    }

    interface Listener {
        fun onApply(outlineColor: Int, outlineSize: Int)
    }

    companion object {
        fun newInstance(): TextOutlineBottomSheetFragment {
            return TextOutlineBottomSheetFragment()
        }
    }
}