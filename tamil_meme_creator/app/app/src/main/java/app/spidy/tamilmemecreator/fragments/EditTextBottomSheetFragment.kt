package app.spidy.tamilmemecreator.fragments

import android.app.Dialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import app.spidy.tamilmemecreator.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class EditTextBottomSheetFragment : BottomSheetDialogFragment() {
    var listener: Listener? = null

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.1f)

        val viewGroup: ViewGroup? = null
        val v = LayoutInflater.from(context).inflate(R.layout.fragment_edit_text_bottom_sheet, viewGroup)
        dialog.setContentView(v)
        (v.parent as View).setBackgroundColor(ContextCompat.getColor(context!!, android.R.color.transparent))

        val applyBtn: TextView = v.findViewById(R.id.applyBtn)
        val rootView: ConstraintLayout = v.findViewById(R.id.rootView)
        val applyCheckbox: CheckBox = v.findViewById(R.id.applyCheckbox)

        val textView: TextView = v.findViewById(R.id.textView)

        arguments?.also {
            textView.text = it.getString("text")
        }

        applyBtn.setOnClickListener {
            listener?.onApply(textView.text.toString())
            dismiss()
        }

        textView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {
                    if (applyCheckbox.isChecked) listener?.onApply(s.toString())
                }
            }
        })
    }

    interface Listener {
        fun onApply(text: String)
    }

    companion object {
        fun newInstance(): EditTextBottomSheetFragment {
            return EditTextBottomSheetFragment()
        }
    }
}