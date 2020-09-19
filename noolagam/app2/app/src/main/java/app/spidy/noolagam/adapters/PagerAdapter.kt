package app.spidy.noolagam.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import app.spidy.noolagam.data.Page
import app.spidy.noolagam.fragments.PageFragment

class PagerAdapter(
    private val bookId: String,
    private val pageNums: List<Int>,
    fragmentManager: FragmentManager
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        val fragment = PageFragment()
        val bundle = Bundle()
        bundle.putInt("page_num", pageNums[position])
        bundle.putString("book_id", bookId)
        fragment.arguments = bundle
        return fragment
    }

    override fun getCount(): Int = pageNums.size

}