package app.spidy.memecreator.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import app.spidy.memecreator.fragments.PagePacksFragment
import app.spidy.memecreator.fragments.PagePopularFragment
import app.spidy.memecreator.fragments.PageGifsFragment

class PagerAdapter(
    private val fragmentManager: FragmentManager,
    private val tabCount: Int,
    private val titles: List<String>
): FragmentStatePagerAdapter(fragmentManager, tabCount) {

    override fun getPageTitle(position: Int): CharSequence? {
        return titles[position]
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> PagePopularFragment()
            1 -> PageGifsFragment()
            2 -> PagePacksFragment()
            else -> PageGifsFragment()
        }
    }

    override fun getCount(): Int {
        return tabCount
    }
}