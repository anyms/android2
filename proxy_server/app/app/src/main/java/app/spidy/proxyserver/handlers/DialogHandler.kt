package app.spidy.proxyserver.handlers

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.view.View

class DialogHandler(private val builder: AlertDialog.Builder) {
    private var positiveBtnCallback: ((DialogInterface, Int) -> Unit)? = null
    private var positiveBtnName: String? = null
    private var negativeBtnCallback: ((DialogInterface, Int) -> Unit)? = null
    private var negativeBtnName: String? = null
    private var neutralBtnCallback: ((DialogInterface, Int) -> Unit)? = null
    private var neutralBtnName: String? = null

    fun withTitle(title: String): DialogHandler {
        builder.setTitle(title)
        return this
    }

    fun withCancelable(isCancelable: Boolean): DialogHandler {
        builder.setCancelable(isCancelable)
        return this
    }

    fun withCustomView(v: View): DialogHandler {
        builder.setView(v)
        return this
    }

    fun withIcon(icon: Int): DialogHandler {
        builder.setIcon(icon)
        return this
    }

    fun withMessage(message: String): DialogHandler {
        builder.setMessage(message)
        return this
    }

    fun withPositiveButton(name: String, callback: (DialogInterface, Int) -> Unit): DialogHandler {
        positiveBtnName = name
        positiveBtnCallback = callback
        return this
    }

    fun withNegativeButton(name: String, callback: (DialogInterface, Int) -> Unit): DialogHandler {
        negativeBtnName = name
        negativeBtnCallback = callback
        return this
    }

    fun withNeutralButton(name: String, callback: (DialogInterface, Int) -> Unit): DialogHandler {
        negativeBtnName = name
        neutralBtnCallback = callback
        return this
    }

    fun show(): Dialog {
        val dialog = builder.create()

        if (positiveBtnCallback != null && positiveBtnName != null) {
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, positiveBtnName, positiveBtnCallback)
        }

        if (negativeBtnCallback != null && negativeBtnName != null) {
            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, negativeBtnName, negativeBtnCallback)
        }

        if (neutralBtnCallback != null && neutralBtnName != null) {
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, neutralBtnName, neutralBtnCallback)
        }

        dialog.show()
        return  dialog
    }
}