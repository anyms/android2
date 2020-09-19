package app.spidy.lankanews.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import app.spidy.lankanews.data.Category
import app.spidy.lankanews.fragments.NewsFeedFragment

class TabAdapter(
    fragmentManager: FragmentManager,
    private val cats: List<Category>
): FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putString("title", cats[position].title)
        bundle.putString("url", cats[position].url)
        val fragment = NewsFeedFragment()
        fragment.arguments = bundle
        return fragment
    }

    override fun getCount(): Int = cats.size
}