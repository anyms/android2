package app.spidy.browser.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import app.spidy.browser.data.Tab
import app.spidy.browser.fragments.WebViewFragment

class PagerStateAdapter(
    private val tabs: MutableList<Tab>,
    val context: FragmentActivity
) : FragmentStateAdapter(context) {
    override fun getItemCount(): Int = tabs.size
    override fun createFragment(position: Int): Fragment = tabs[position].fragment

    fun removeFragment(position: Int) {
        tabs.removeAt(position)
        notifyItemRangeChanged(position, tabs.size)
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return tabs[position].tabId // make sure notifyDataSetChanged() works
    }

    override fun containsItem(itemId: Long): Boolean {
        return tabs.map { it.tabId }.contains(itemId)
    }
}