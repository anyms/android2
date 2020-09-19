package app.spidy.proxyserver.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.chaiui.ChaiEditText
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import app.spidy.proxyserver.R
import app.spidy.proxyserver.data.BlockedDomain
import app.spidy.proxyserver.databases.ProxyDatabase
import app.spidy.proxyserver.utils.newDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_user_config.*
import java.util.*
import kotlin.concurrent.thread

class BlockedAdapter(
    private val context: Context,
    private val domains: ArrayList<BlockedDomain>,
    private val recyclerView: RecyclerView
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val database = Room.databaseBuilder(context, ProxyDatabase::class.java, "ProxyDatabase")
        .fallbackToDestructiveMigration().build()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_blocked_domain_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = domains.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder

        if (domains[position].isPattern) {
            mainHolder.typeImageView.setImageResource(R.drawable.ic_pattern)
            mainHolder.statusView.text = context.getString(R.string.scope)
        } else {
            mainHolder.typeImageView.setImageResource(R.drawable.ic_domain)
            mainHolder.statusView.text = context.getString(R.string.domain)
        }

        mainHolder.domainNameView.text = domains[position].value
        mainHolder.menuImageView.setOnClickListener {
            showOptionMenu(mainHolder.menuImageView, domains[position], position)
        }
    }

    private fun showOptionMenu(menuImageView: ImageView, blockedDomain: BlockedDomain, position: Int) {
        val popupMenu =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                PopupMenu(
                    context,
                    menuImageView,
                    Gravity.NO_GRAVITY,
                    android.R.attr.actionOverflowMenuStyle,
                    0
                )
            } else {
                PopupMenu(context, menuImageView)
            }
        popupMenu.inflate(R.menu.menu_blocked_domain_item)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCopy -> {
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("domain", blockedDomain.value)
                    clipboardManager.setPrimaryClip(clipData)
                    context.toast("Copied!")
                }
                R.id.menuEdit -> {
                    val v = LayoutInflater.from(context).inflate(R.layout.layout_add_blocked_domain_dialog, null)
                    val domainField: ChaiEditText = v.findViewById(R.id.domainField)
                    domainField.setText(blockedDomain.value)
                    context.newDialog().withCustomView(v)
                        .withTitle("Add a Domain")
                        .withPositiveButton("Edit") {_, _ ->
                            val domain = domainField.text.toString().trim().toLowerCase(Locale.ROOT)
                            if (domain != "") {
                                thread {
                                    blockedDomain.value = domain
                                    database.proxyDao().updateBlockedDomain(blockedDomain)

                                    onUiThread { notifyItemChanged(position) }
                                }
                            } else {
                                context.toast("Domain should not be empty")
                            }
                        }
                        .withNegativeButton("Cancel") {dialog, _ -> dialog.dismiss() }
                        .show()
                }
                R.id.menuRemove -> {
                    domains.removeAt(position)
                    notifyItemRemoved(position)
                    thread {
                        database.proxyDao().removeBlockedDomain(blockedDomain)

                        onUiThread {
                            Snackbar.make(recyclerView, "${blockedDomain.value} removed", Snackbar.LENGTH_LONG)
                                .setAction("Undo") {
                                    domains.add(position, blockedDomain)
                                    notifyItemInserted(position)
                                    thread { database.proxyDao().putBlockedDomain(blockedDomain) }
                                }
                                .show()
                        }
                    }
                }
            }
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
    }

    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val typeImageView: ImageView = v.findViewById(R.id.typeImageView)
        val domainNameView: TextView = v.findViewById(R.id.domainNameView)
        val statusView: TextView = v.findViewById(R.id.statusView)
        val menuImageView: ImageView = v.findViewById(R.id.menuImageView)
    }
}