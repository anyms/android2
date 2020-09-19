package app.spidy.wikireader.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.ignore
import app.spidy.wikireader.activities.MainActivity
import app.spidy.wikireader.R
import app.spidy.wikireader.data.Language
import app.spidy.wikireader.utils.LanguageUtil

class SettingsDialogFragment: DialogFragment() {
    private var listener: Listener? = null
    private lateinit var tinyDB: TinyDB

    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val v = LayoutInflater.from(context).inflate(R.layout.layout_settings_screen, null)
        val selectLanguageView: TextView = v.findViewById(R.id.selectLanguageView)
        val upgradeBtn: TextView = v.findViewById(R.id.upgradeBtn)
        val upgradeMsgView: TextView = v.findViewById(R.id.upgradeMsgView)
        builder.setView(v)
            .setTitle(getString(R.string.settings))
            .setPositiveButton(getString(R.string.dismiss)) { dialog, which ->
                dialog.dismiss()
            }

        selectLanguageView.text = "Language: ${tinyDB.getString(LanguageUtil.TAG_CURRENT_LANGUAGE_NAME) ?: "English"}"
        selectLanguageView.setOnClickListener {
            val dialog = LanguageDialogFragment()
            dialog.onLanguageSelect = {
                selectLanguageView.text = "Language: ${it.name}"
            }
            dialog.show(requireFragmentManager(), "SELECT_LANGUAGE_DIALOG")
        }

        if (tinyDB.getBoolean("isPro")) {
            upgradeMsgView.visibility = View.GONE
            upgradeBtn.visibility = View.GONE
        }
        upgradeBtn.setOnClickListener {
            (activity as? MainActivity)?.purchase()
        }

        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        tinyDB = TinyDB(context)
        ignore { listener = context as Listener }
    }

    override fun onDestroy() {
        listener = null
        super.onDestroy()
    }

    interface Listener {
        fun onSettingsApply(language: Language, pitch: Int, speed: Int)
    }
}