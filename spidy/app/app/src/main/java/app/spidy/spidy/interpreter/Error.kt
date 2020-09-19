package app.spidy.spidy.interpreter

import android.app.AlertDialog
import android.content.Context
import app.spidy.kotlinutils.onUiThread

class Error(private val context: Context) {
    fun show(message: String) {
        onUiThread {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Error!")
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }
}