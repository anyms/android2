package app.spidy.pirum.adapters

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import app.spidy.pirum.R
import app.spidy.pirum.activities.ImageSliderActivity
import app.spidy.pirum.data.Other
import app.spidy.pirum.data.OtherHistory
import app.spidy.pirum.databases.PyrumDatabase
import app.spidy.pirum.interfaces.RenameListener
import kotlin.concurrent.thread


class OtherAdapter(
    private val context: Context,
    private val otherHistory: ArrayList<OtherHistory>,
    private val updateOtherHistory: () -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val database = Room.databaseBuilder(context, PyrumDatabase::class.java, "PyrumDatabase")
        .fallbackToDestructiveMigration().build()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_history_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = otherHistory.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as OtherAdapter.MainHolder

        mainHolder.playlistNameView.text = if (otherHistory[position].type == Other.TYPE_IMAGE) {
            otherHistory[position].playlistName
        } else {
            otherHistory[position].title
        }

        mainHolder.subTextView.text = if (otherHistory[position].type == Other.TYPE_IMAGE) {
            mainHolder.playlistIconView.setImageResource(R.drawable.img_image_list)
            "Images • ${otherHistory[position].itemCount}"
        } else {
            mainHolder.playlistIconView.setImageResource(R.drawable.ic_others)
            mainHolder.playlistIconView.setColorFilter(ContextCompat.getColor(context, R.color.colorWhite))
            otherHistory[position].src
        }

        mainHolder.menuImageView.setOnClickListener {
            showOptionMenu(it, position)
        }

        mainHolder.rootView.setOnClickListener {
            open(position)
        }
    }

    private fun showOptionMenu(v: View, position: Int) {
        val popupMenu =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                PopupMenu(
                    context,
                    v,
                    Gravity.NO_GRAVITY,
                    android.R.attr.actionOverflowMenuStyle,
                    0
                )
            } else {
                PopupMenu(context, v)
            }
        popupMenu.inflate(R.menu.menu_other_history)
        popupMenu.menu.getItem(3)?.isVisible = otherHistory[position].type == Other.TYPE_PAGE
        popupMenu.menu.getItem(4)?.isVisible = otherHistory[position].type == Other.TYPE_PAGE
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.rename -> {
                    val title = if (otherHistory[position].type == Other.TYPE_IMAGE) otherHistory[position].playlistName else otherHistory[position].title
                    showRenameDialog(title) { name ->
                        if (name.trim() != "") {
                            renamePlaylist(title, name, otherHistory[position].type, object : RenameListener {
                                override fun onFail() {
                                    context.toast("Playlist name already exist", true)
                                }

                                override fun onSuccess() {
                                    if (otherHistory[position].type == Other.TYPE_IMAGE) {
                                        otherHistory[position].playlistName = name
                                    } else {
                                        otherHistory[position].title = name
                                    }
                                    notifyItemChanged(position)
                                }
                            })
                        }
                    }
                }
                R.id.open -> open(position)
                R.id.delete -> {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Are you sure?")
                    builder.setMessage("This action can not be undone, do you really want to delete?")
                    builder.setIcon(android.R.drawable.stat_sys_warning)
                    val dialog = builder.create()
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "No, keep it") {d, _ ->
                        d.dismiss()
                    }

                    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Yes") {d, _ ->
                        d.dismiss()
                        deletePlaylist(if (otherHistory[position].type == Other.TYPE_IMAGE) otherHistory[position].playlistName else otherHistory[position].title, otherHistory[position].type)
                    }
                    dialog.show()
                }
                R.id.share -> {
                    val share = Intent(Intent.ACTION_SEND)
                    share.type = "text/plain"
                    share.putExtra(Intent.EXTRA_SUBJECT, otherHistory[position].title)
                    share.putExtra(Intent.EXTRA_TEXT, otherHistory[position].src)
                    context.startActivity(Intent.createChooser(share, "Share via"))
                }
                R.id.copy_url -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("url", otherHistory[position].src)
                    clipboard.setPrimaryClip(clip)
                    context.toast("URL Copied!")
                }
            }
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
    }

    private fun deletePlaylist(title: String, type: String) {
        if (type == Other.TYPE_IMAGE) {
            thread {
                val images = database.pyrumDao().getOtherByPlaylist(title)
                for (img in images) database.pyrumDao().removeOther(img)

                onUiThread { updateOtherHistory() }
            }
        } else {
            thread {
                val images = database.pyrumDao().getOtherByTitle(title)
                for (img in images) database.pyrumDao().removeOther(img)

                onUiThread { updateOtherHistory() }
            }
        }
    }

    private fun showRenameDialog(oldName: String, onSuccess: (name: String) -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Rename")
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(oldName)
        input.setSelectAllOnFocus(true)
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ -> onSuccess(input.text.toString()) }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun renamePlaylist(oldName: String, newName: String, type: String, renameListener: RenameListener) {
        thread {
            if (type == Other.TYPE_IMAGE) {
                val imagesCheck = database.pyrumDao().getOtherByPlaylist(oldName)
                if (imagesCheck.isNotEmpty()) {
                    onUiThread { renameListener.onFail() }
                } else {
                    val others = database.pyrumDao().getOtherByPlaylist(oldName)
                    for (m in others) {
                        m.playlistName = newName
                        database.pyrumDao().updateOther(m)
                    }
                    onUiThread { renameListener.onSuccess() }
                }
            } else {
                val others = database.pyrumDao().getOtherByTitle(oldName)
                for (m in others) {
                    m.title = newName
                    database.pyrumDao().updateOther(m)
                }
                onUiThread { renameListener.onSuccess() }
            }
        }
    }

    private fun open(position: Int) {
        if (otherHistory[position].type == Other.TYPE_IMAGE) {
            val intent = Intent(context, ImageSliderActivity::class.java)
            intent.putExtra("playlistName", otherHistory[position].playlistName)
            intent.putExtra("isFromLocal", true)
            context.startActivity(intent)
        } else {
            if (otherHistory[position].isToRead) {
                openToRead(otherHistory[position].src)
            } else {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(otherHistory[position].src))
                appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(appIntent)
            }
        }
    }

    private fun openToRead(url: String) {
        val builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(ContextCompat.getColor(context, android.R.color.white))
        builder.setExitAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
        builder.setShowTitle(true)
        val tabIntent = builder.build()
        tabIntent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        tabIntent.launchUrl(context, Uri.parse(url))
    }

    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val playlistIconView: ImageView = v.findViewById(R.id.playlistIconView)
        val playlistNameView: TextView = v.findViewById(R.id.playlistNameView)
        val subTextView: TextView = v.findViewById(R.id.subTextView)
        val menuImageView: ImageView = v.findViewById(R.id.menuImageView)
        val rootView: LinearLayout = v.findViewById(R.id.rootView)
    }
}