package app.spidy.pirum.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import app.spidy.pirum.fragments.ExplorerFragment
import app.spidy.pirum.fragments.MusicFragment
import app.spidy.pirum.fragments.OtherFragment
import app.spidy.pirum.fragments.VideoFragment

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
            0 -> ExplorerFragment()
            1 -> MusicFragment()
            2 -> VideoFragment()
            3 -> OtherFragment()
            else -> ExplorerFragment()
        }
    }

    override fun getCount(): Int {
        return tabCount
    }
}