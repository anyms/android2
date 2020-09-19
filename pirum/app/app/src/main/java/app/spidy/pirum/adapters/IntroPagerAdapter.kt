package app.spidy.pirum.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import app.spidy.pirum.data.Tab

class IntroPagerAdapter(
    private val fragmentManager: FragmentManager,
    private val tabs: List<Tab>
): FragmentStatePagerAdapter(fragmentManager, tabs.size) {
    override fun getPageTitle(position: Int): CharSequence? {
        return tabs[position].title
    }

    override fun getItem(position: Int): Fragment {
        return tabs[position].fragment
    }

    override fun getCount(): Int {
        return tabs.size
    }
}