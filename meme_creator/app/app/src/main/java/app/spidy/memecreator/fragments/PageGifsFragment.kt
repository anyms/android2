package app.spidy.memecreator.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.spidy.kotlinutils.toast

import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.GifsAdapter
import app.spidy.memecreator.data.Template
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PageGifsFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GifsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var loadMoreBtn: TextView
    private lateinit var loadingBar: ProgressBar

    private val memes = ArrayList<Template>()
    private val limit = 10L
    private var isFetched = false
    private var lastDocument: DocumentSnapshot? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_page_gifs, container, false)

        recyclerView = v.findViewById(R.id.recyclerView)
        progressBar = v.findViewById(R.id.progressBar)
        loadMoreBtn = v.findViewById(R.id.loadMoreBtn)
        loadingBar = v.findViewById(R.id.loadingBar)
        adapter = GifsAdapter(context, memes)
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayout.VERTICAL)

        loadMoreBtn.visibility = View.GONE

        firestore = FirebaseFirestore.getInstance()

        val first = firestore.collection("gifs").document("packs").collection("popular")
            .orderBy("filename", Query.Direction.DESCENDING).limit(limit)
        fetch(first)

        loadMoreBtn.setOnClickListener {
            loadMoreBtn.visibility = View.GONE
            loadingBar.visibility = View.VISIBLE
            loadMore()
        }

        return v
    }

    private fun loadMore() {
        if (!isFetched) {
            val next = firestore.collection("gifs").document("packs").collection("popular")
                .orderBy("filename", Query.Direction.DESCENDING)
                .startAfter(lastDocument!!["filename"]).limit(limit)
            fetch(next)
        } else {
            loadMoreBtn.visibility = View.GONE
            loadingBar.visibility = View.GONE
        }
    }


    private fun fetch(query: Query) {
        query.get().addOnSuccessListener { snapshot ->
            if (snapshot.documents.isEmpty()) {
                loadingBar.visibility = View.GONE
                isFetched = true
                return@addOnSuccessListener
            }
            val startPosition = memes.size - 1
            val tmpMemes = ArrayList<Template>()
            lastDocument = snapshot.documents[snapshot.documents.size - 1]
            snapshot.documents.forEach {  document ->
                val data = document.data
                tmpMemes.add(Template(
                    caption = data!!["caption"].toString(),
                    url = data["url"].toString(),
                    filename = data["filename"].toString(),
                    thumb = data["thumb"].toString()
                ))
            }
            if (tmpMemes.isEmpty()) {
                loadMore()
                return@addOnSuccessListener
            }
            tmpMemes.forEach { memes.add(it) }
            progressBar.visibility = View.GONE
            adapter.notifyItemRangeChanged(startPosition, tmpMemes.size)

            if (tmpMemes.size < limit) {
                loadingBar.visibility = View.GONE
                isFetched = true
            } else {
                loadMoreBtn.visibility = View.VISIBLE
                loadingBar.visibility = View.GONE
            }
        }.addOnFailureListener {
            context?.toast(context?.getString(R.string.network_fail))
            loadMoreBtn.visibility = View.VISIBLE
            loadingBar.visibility = View.GONE
        }
    }
}
