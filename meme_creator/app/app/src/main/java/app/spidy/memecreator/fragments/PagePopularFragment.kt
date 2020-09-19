package app.spidy.memecreator.fragments

import android.os.Bundle
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
import app.spidy.memecreator.adapters.TemplateAdapter
import app.spidy.memecreator.data.Template
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PagePopularFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TemplateAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var loadMoreBtn: TextView
    private lateinit var loadingBar: ProgressBar

    private val templates = ArrayList<Template>()
    private val limit = 26L
    private var isFetched = false
    private var lastDocument: DocumentSnapshot? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_page_popular, container, false)

        recyclerView = v.findViewById(R.id.recyclerView)
        progressBar = v.findViewById(R.id.progressBar)
        loadMoreBtn = v.findViewById(R.id.loadMoreBtn)
        loadingBar = v.findViewById(R.id.loadingBar)
        adapter = TemplateAdapter(context, templates)
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayout.VERTICAL)

        loadMoreBtn.visibility = View.GONE

        firestore = FirebaseFirestore.getInstance()

        val first = firestore.collection("memes").document("packs").collection("popular")
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
            val next = firestore.collection("memes").document("packs").collection("popular")
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
                loadMoreBtn.visibility = View.GONE
                isFetched = true
                return@addOnSuccessListener
            }
            val startPosition = templates.size - 1
            val tmpTemplates = ArrayList<Template>()
            lastDocument = snapshot.documents[snapshot.documents.size - 1]
            snapshot.documents.forEach {  document ->
                val data = document.data
                tmpTemplates.add(Template(
                    filename = data!!["filename"]!!.toString(),
                    caption = data["caption"]!!.toString(),
                    url = data["url"]!!.toString()
                ))
            }
            if (tmpTemplates.isEmpty()) {
                loadMore()
                return@addOnSuccessListener
            }
            tmpTemplates.forEach { templates.add(it) }
            adapter.notifyItemRangeChanged(startPosition, tmpTemplates.size)

            if (tmpTemplates.size < limit) {
                loadMoreBtn.visibility = View.GONE
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
