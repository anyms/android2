package app.spidy.spidy.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import app.spidy.kotlinutils.newDialog
import app.spidy.spidy.R
import app.spidy.spidy.services.HeadlessService

class DialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)

        val v = LayoutInflater.from(this).inflate(R.layout.layout_edittext, null)
        val input: EditText = v.findViewById(R.id.editText)
        newDialog().withTitle(intent?.getStringExtra("title") ?: "")
            .withCustomView(input)
            .withCancelable(false)
            .withNegativeButton(this.getString(R.string.cancel)) {
                HeadlessService.askValue = ""
                it.dismiss()
                finish()
            }
            .withPositiveButton(getString(R.string.ok)) {
                HeadlessService.askValue = input.text.toString().replace("\n", "\\n")
                it.dismiss()
                finish()
            }
            .create().show()
    }
}