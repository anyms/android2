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


class TextSizeBottomSheetFragment : BottomSheetDialogFragment() {
    var listener: Listener? = null

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.1f)

        val viewGroup: ViewGroup? = null
        val v = LayoutInflater.from(context).inflate(R.layout.fragment_text_size_bottom_sheet, viewGroup)
        dialog.setContentView(v)
        (v.parent as View).setBackgroundColor(ContextCompat.getColor(context!!, android.R.color.transparent))

        val applyBtn: TextView = v.findViewById(R.id.applyBtn)
        val textSizeSlider: DiscreteSlider = v.findViewById(R.id.textSizeSlider)
        val applyCheckbox: CheckBox = v.findViewById(R.id.applyCheckbox)
        val rootView: ConstraintLayout = v.findViewById(R.id.rootView)

        var textSize = 0

        arguments?.also {
            textSize = it.getInt("text_size")
            textSizeSlider.progress = textSize
        }

        applyBtn.setOnClickListener {
            listener?.onApply(textSize)
            dismiss()
        }

        textSizeSlider.setOnValueChangedListener(object : DiscreteSlider.OnValueChangedListener() {
            override fun onValueChanged(progress: Int, fromUser: Boolean) {
                textSize = progress
                if (applyCheckbox.isChecked) listener?.onApply(textSize)
            }
        })
    }

    interface Listener {
        fun onApply(textSize: Int)
    }

    companion object {
        fun newInstance(): TextSizeBottomSheetFragment {
            return TextSizeBottomSheetFragment()
        }
    }
}