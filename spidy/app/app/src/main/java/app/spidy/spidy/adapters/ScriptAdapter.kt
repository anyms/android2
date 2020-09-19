package app.spidy.spidy.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.InputType
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.kotlinutils.*
import app.spidy.spidy.R
import app.spidy.spidy.activities.BrowserActivity
import app.spidy.spidy.activities.DebugConsoleActivity
import app.spidy.spidy.activities.EditorActivity
import app.spidy.spidy.activities.MainActivity
import app.spidy.spidy.data.Script
import app.spidy.spidy.databases.SpidyDatabase
import app.spidy.spidy.utils.Ads
import app.spidy.spidy.utils.C
import app.spidy.spidy.viewmodels.MainActivityViewModel
import kotlin.concurrent.thread

class ScriptAdapter(
    private val context: Context,
    private val scripts: List<Script>,
    private val viewModel: MainActivityViewModel,
    private val runHeadless: (id: Int, code: String) -> Unit,
    private val terminateScript: (script: Script) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val database = Room.databaseBuilder(context, SpidyDatabase::class.java, "SpidyDatabase")
        .addMigrations(SpidyDatabase.migration).build()
    private val tinyDB = TinyDB(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_script_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = scripts.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder

        mainHolder.titleView.text = scripts[position].name
        mainHolder.editBtn.setOnClickListener {
            val intent = Intent(context, EditorActivity::class.java)
            intent.putExtra("scriptName", scripts[position].name)
            intent.putExtra("code", scripts[position].code)
            context.startActivity(intent)
        }

        if (scripts[position].isBackgroundRunning) {
            mainHolder.consoleBtn.visibility = View.VISIBLE
            mainHolder.runBtn.setImageResource(R.drawable.ic_stop)
        } else {
            mainHolder.consoleBtn.visibility = View.GONE
            mainHolder.runBtn.setImageResource(R.drawable.ic_play)
        }

        mainHolder.consoleBtn.setOnClickListener {
            val intent = Intent(context, DebugConsoleActivity::class.java)
            intent.putExtra("script_id", scripts[position].id)
            context.startActivity(intent)
        }

        mainHolder.runBtn.setOnClickListener {
            if (scripts[position].isBackgroundRunning) {
                terminateScript(scripts[position])
                return@setOnClickListener
            }
            val codeResult = "==CODE_START==\n(.*)\n==CODE_END==".toRegex().find(scripts[position].code)
            if (codeResult != null) {
                val code = Base64.decode(codeResult.groupValues[1], Base64.DEFAULT).toString(charset("UTF-8"))
                WebStorage.getInstance().deleteAllData()
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()

                val popupMenu =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        PopupMenu(
                            context,
                            mainHolder.runBtn,
                            Gravity.NO_GRAVITY,
                            android.R.attr.actionOverflowMenuStyle,
                            0
                        )
                    } else {
                        PopupMenu(context, mainHolder.runBtn)
                    }
                popupMenu.inflate(R.menu.run_menu)
                popupMenu.setOnMenuItemClickListener {
                    when(it.itemId) {
                        R.id.menuRun -> {
                            val intent = Intent(context, BrowserActivity::class.java)
                            intent.putExtra("isElementSelector", false)
                            intent.putExtra("code", code)
                            context.startActivity(intent)
                        }
                        R.id.menuRunHeadless -> {
                            var canExec = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (!Settings.canDrawOverlays(context)) {
                                    val builder = AlertDialog.Builder(context)
                                    builder.setTitle(context.getString(R.string.require_permission))
                                    builder.setCancelable(false)

                                    builder.setMessage(context.getString(R.string.draw_over_permission_message))
                                    val dialog = builder.create()
                                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.grant)) { d, _ ->
                                        d.dismiss()
                                        if (!Settings.canDrawOverlays(context)) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            (context as MainActivity).startActivityForResult(intent, C.OVERLAY_PERMISSION_CODE)
                                        }
                                    }
                                    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel)) { d, _ ->
                                        d.dismiss()
                                    }
                                    dialog.show()
                                } else {
                                    canExec = true
                                }
                            } else {
                                canExec = true
                            }

                            if (canExec) {
                                if (!tinyDB.getBoolean("isPro")) {
                                    context.newDialog()
                                        .withMessage(context.getString(R.string.headless_running_ad_message))
                                        .withPositiveButton(context.getString(R.string.watch_now)) {
                                            Ads.showReward {
                                                runHeadless(scripts[position].id, code)
                                                context.toast("Running headless")
                                            }
                                        }
                                        .withNegativeButton(context.getString(R.string.cancel)) { dialog ->
                                            dialog.dismiss()
                                        }
                                        .create().show()
                                } else {
                                    runHeadless(scripts[position].id, code)
                                    context.toast("Running headless")
                                }
                            }
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
                popupMenu.show()
            }
        }

        mainHolder.menuView.setOnClickListener {
            val popupMenu =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    PopupMenu(
                        context,
                        mainHolder.menuView,
                        Gravity.NO_GRAVITY,
                        android.R.attr.actionOverflowMenuStyle,
                        0
                    )
                } else {
                    PopupMenu(context, mainHolder.menuView)
                }
            popupMenu.inflate(R.menu.script_item_menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.menuRename -> {
                        val input = EditText(context)
                        input.inputType = InputType.TYPE_CLASS_TEXT
                        input.setText(scripts[position].name)
                        context.newDialog().withTitle(context.getString(R.string.rename))
                            .withCustomView(input)
                            .withPositiveButton(context.getString(R.string.rename)) {
                                val name = input.text.toString().trim()
                                thread {
                                    val tmp = database.spidyDao().getScripts()
                                    var isExist = false
                                    for (t in tmp) {
                                        if (t.name == name) {
                                            onUiThread {
                                                context.toast("A script already exist with that name")
                                            }
                                            isExist = true
                                            break
                                        }
                                    }

                                    if (!isExist) {
                                        scripts[position].name = name
                                        database.spidyDao().updateScript(scripts[position])
                                        onUiThread {
                                            viewModel.updateView()
                                            context.toast("Script renamed")
                                        }
                                    }
                                }
                            }
                            .withNegativeButton(context.getString(R.string.cancel)) {
                                it.dismiss()
                            }
                            .create().show()
                    }
                    R.id.menuDelete -> {
                        context.newDialog().withMessage("Do you really want to delete the script '${scripts[position].name}'?")
                            .withPositiveButton(context.getString(R.string.cancel)) {
                                it.dismiss()
                            }
                            .withNegativeButton(context.getString(R.string.delete)) {
                                thread {
                                    database.spidyDao().removeScript(scripts[position])
                                    onUiThread {
                                        viewModel.updateView()
                                        context.toast("Script deleted")
                                    }
                                }
                            }
                            .create().show()
                    }
                }
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }
    }

    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val titleView: TextView = v.findViewById(R.id.titleView)
        val runBtn: ImageView = v.findViewById(R.id.runBtn)
        val editBtn: ImageView = v.findViewById(R.id.editBtn)
        val menuView: ImageView = v.findViewById(R.id.menuView)
        val consoleBtn: ImageView = v.findViewById(R.id.consoleBtn)
    }
}