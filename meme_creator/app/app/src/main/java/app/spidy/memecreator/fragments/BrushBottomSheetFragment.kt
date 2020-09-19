package app.spidy.memecreator.fragments

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import app.spidy.kotlinutils.toast
import app.spidy.memecreator.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import hearsilent.discreteslider.DiscreteSlider
import yuku.ambilwarna.AmbilWarnaDialog


class BrushBottomSheetFragment : BottomSheetDialogFragment() {
    var listener: Listener? = null

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.1f)

        val viewGroup: ViewGroup? = null
        val v = LayoutInflater.from(context).inflate(R.layout.fragment_brush_bottom_sheet, viewGroup)
        dialog.setContentView(v)
        (v.parent as View).setBackgroundColor(ContextCompat.getColor(context!!, android.R.color.transparent))

        val applyBtn: TextView = v.findViewById(R.id.applyBtn)
        val brushSizeSlider: DiscreteSlider = v.findViewById(R.id.brushSizeSlider)
        val brushColorView: View = v.findViewById(R.id.colorView)
        val applyCheckbox: CheckBox = v.findViewById(R.id.applyCheckbox)
        val rootView: ConstraintLayout = v.findViewById(R.id.rootView)

        var brushSize = 0
        var brushColor = 0

        arguments?.also {
            brushSize = it.getInt("brush_size")
            brushColor = it.getInt("brush_color")
            brushColorView.setBackgroundColor(brushColor)
            brushSizeSlider.progress = brushSize
        }

        applyBtn.setOnClickListener {
            listener?.onApply(brushSize, brushColor)
            dismiss()

            context?.apply {
                toast(getString(R.string.brush_settings_applied))
            }
        }

        brushSizeSlider.setOnValueChangedListener(object : DiscreteSlider.OnValueChangedListener() {
            override fun onValueChanged(progress: Int, fromUser: Boolean) {
                brushSize = progress

                if (applyCheckbox.isChecked) {
                    listener?.onApply(brushSize, brushColor)
                }
            }
        })

        brushColorView.setOnClickListener {
            val colorPicker = AmbilWarnaDialog(context, brushColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    brushColor = color
                    brushColorView.setBackgroundColor(color)

                    if (applyCheckbox.isChecked) {
                        listener?.onApply(brushSize, brushColor)
                    }
                }
            })
            colorPicker.show()
        }

//        transparentCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
//            if (isChecked) {
//                rootView.setBackgroundResource(R.drawable.rounded_corner_trans_white)
//            } else {
//                rootView.setBackgroundResource(R.drawable.rounded_corner_white)
//            }
//        }
    }

    interface Listener {
        fun onApply(brushSize: Int, brushColor: Int)
    }

    companion object {
        fun newInstance(): BrushBottomSheetFragment {
            return BrushBottomSheetFragment()
        }
    }
}