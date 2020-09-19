package app.spidy.browser.adapters

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.spidy.browser.R
import app.spidy.browser.controllers.Browser
import app.spidy.browser.data.Tab
import app.spidy.browser.fragments.BrowserFragment

class TabAdapter(
    private val context: Context,
    private val tabs: List<Tab>,
    private val browser: Browser,
    private val dismissDialog: () -> Unit
) : RecyclerView.Adapter<TabAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_layout_tab_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return tabs.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder.adapterPosition) {
            0 -> {
                val params = holder.tabRootLayout.layoutParams as RecyclerView.LayoutParams
                params.topMargin = 40
                holder.tabRootLayout.layoutParams = params
            }
            tabs.size - 1 -> {
                val params = holder.tabRootLayout.layoutParams as RecyclerView.LayoutParams
                params.bottomMargin = 40
                holder.tabRootLayout.layoutParams = params
            }
            else -> {
                val params = holder.tabRootLayout.layoutParams as RecyclerView.LayoutParams
                params.bottomMargin = 20
                params.topMargin = 0
                holder.tabRootLayout.layoutParams = params
            }
        }

        if (holder.adapterPosition == browser.currentTabIndex) {
            holder.tabRootLayout.setBackgroundResource(R.drawable.browser_tab_background_active)
        } else {
            holder.tabRootLayout.setBackgroundResource(R.drawable.browser_tab_background)
        }

        holder.tabTitleView.text = tabs[position].title
        holder.tabUrlView.text = tabs[position].url

        if (tabs[position].favIcon != null) {
            holder.tabFavIcon.setImageBitmap(tabs[position].favIcon!!)
        } else {
            holder.tabFavIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_page))
        }

        holder.tabRootLayout.setOnClickListener {
            browser.switchTab(position)
            dismissDialog()
        }

        holder.tabCloseImage.setOnClickListener {
            browser.closeTab(position)
//            dismissDialog()
        }
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tabRootLayout: ConstraintLayout = v.findViewById(R.id.tab_root_layout)
        val tabTitleView: TextView = v.findViewById(R.id.tab_title_view)
        val tabUrlView: TextView = v.findViewById(R.id.tab_url_view)
        val tabFavIcon: ImageView = v.findViewById(R.id.fav_icon_image)
        val tabCloseImage: ImageView = v.findViewById(R.id.tab_close_icon)
    }
}