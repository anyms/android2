package app.spidy.noolagam.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.spidy.kotlinutils.debug
import app.spidy.noolagam.R
import app.spidy.noolagam.adapters.ShelfAdapter
import app.spidy.noolagam.data.Book


class ShelfFragment : Fragment() {
    var books: List<Book>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_shelf, container, false)
        val recyclerView: RecyclerView = v.findViewById(R.id.recyclerView)
        val shelfAdapter = ShelfAdapter(requireContext(), books!!)
        recyclerView.hasFixedSize()
        recyclerView.adapter = shelfAdapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        return v
    }

    companion object {
        @JvmStatic
        fun newInstance(books: List<Book>): ShelfFragment {
            val shelfFragment = ShelfFragment()
            shelfFragment.books = books
            return shelfFragment
        }
    }
}