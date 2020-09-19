package app.spidy.wikireader.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.toast
import app.spidy.wikireader.R
import app.spidy.wikireader.data.Language
import app.spidy.wikireader.utils.LanguageUtil

class LanguageDialogFragment : DialogFragment() {
    private lateinit var tinyDB: TinyDB
    private var listener: Listener? = null

    var onLanguageSelect: ((Language) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        val languageNames = LanguageUtil.getNames()
        val languages = LanguageUtil.getLanguages()
        var currentLanguageName = tinyDB.getString(LanguageUtil.TAG_CURRENT_LANGUAGE_NAME) ?: "English"

        builder.setTitle(getString(R.string.select_a_language))
            .setSingleChoiceItems(languageNames.toTypedArray(), languageNames.indexOf(currentLanguageName)) { dialog, which ->
                currentLanguageName = languageNames[which]
            }
            .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                tinyDB.putString(LanguageUtil.TAG_CURRENT_LANGUAGE_NAME, currentLanguageName)
                languages.find { it.name == currentLanguageName }?.let {
                    tinyDB.putString(LanguageUtil.TAG_CURRENT_LANGUAGE_CODE, it.code)
                    listener?.onLanguageChange(it)
                    onLanguageSelect?.invoke(it)
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                dialog.dismiss()
            }
        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        tinyDB = TinyDB(context)

        ignore { listener = context as Listener }
    }

    override fun onDestroy() {
        onLanguageSelect = null
        listener = null
        super.onDestroy()
    }

    interface Listener {
        fun onLanguageChange(language: Language)
    }
}