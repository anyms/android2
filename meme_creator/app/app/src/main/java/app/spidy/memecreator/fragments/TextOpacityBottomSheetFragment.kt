package app.spidy.memecreator.fragments

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import app.spidy.memecreator.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import hearsilent.discreteslider.DiscreteSlider


class TextOpacityBottomSheetFragment : BottomSheetDialogFragment() {
    var listener: Listener? = null

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.1f)

        val viewGroup: ViewGroup? = null
        val v = LayoutInflater.from(context).inflate(R.layout.fragment_text_opacity_bottom_sheet, viewGroup)
        dialog.setContentView(v)
        (v.parent as View).setBackgroundColor(ContextCompat.getColor(context!!, android.R.color.transparent))

        val applyBtn: TextView = v.findViewById(R.id.applyBtn)
        val textOpacitySlider: DiscreteSlider = v.findViewById(R.id.textOpacitySlider)
        val applyCheckbox: CheckBox = v.findViewById(R.id.applyCheckbox)
        val rootView: ConstraintLayout = v.findViewById(R.id.rootView)

        var textOpacity = 0

        arguments?.also {
            textOpacity = (it.getFloat("opacity") * 10f).toInt()
            textOpacitySlider.progress = textOpacity
        }

        applyBtn.setOnClickListener {
            listener?.onApply(textOpacity)
            dismiss()
        }

        textOpacitySlider.setOnValueChangedListener(object : DiscreteSlider.OnValueChangedListener() {
            override fun onValueChanged(progress: Int, fromUser: Boolean) {
                textOpacity = progress
                if (applyCheckbox.isChecked) listener?.onApply(textOpacity)
            }
        })
    }

    interface Listener {
        fun onApply(textOpacity: Int)
    }

    companion object {
        fun newInstance(): TextOpacityBottomSheetFragment {
            return TextOpacityBottomSheetFragment()
        }
    }
}