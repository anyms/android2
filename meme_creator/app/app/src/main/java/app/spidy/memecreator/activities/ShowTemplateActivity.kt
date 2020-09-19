package app.spidy.memecreator.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.room.Room
import app.spidy.kotlinutils.toast
import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.GifsAdapter
import app.spidy.memecreator.adapters.TemplateAdapter
import app.spidy.memecreator.data.Template
import app.spidy.memecreator.databases.MemeDatabase
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.lang.Exception
import kotlin.concurrent.thread

class ShowTemplateActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadMoreBtn: TextView
    private lateinit var loadingBar: ProgressBar
    private lateinit var packTitle: String
    private lateinit var packPath: String
    private lateinit var packType: String
    private lateinit var pathNodes: List<String>
    private lateinit var tempAdapter: TemplateAdapter
    private lateinit var gifsAdapter: GifsAdapter

    private val templates = ArrayList<Template>()
    private val limit = 26L
    private var isFetched = false
    private var lastDocument: DocumentSnapshot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_template)

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        packTitle = intent!!.getStringExtra("pack_title")!!
        packPath = intent!!.getStringExtra("pack_path")!!
        packType = intent!!.getStringExtra("pack_type")!!
        pathNodes = packPath.split("/").toMutableList()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        title = packTitle

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        loadMoreBtn = findViewById(R.id.loadMoreBtn)
        loadingBar = findViewById(R.id.loadingBar)
        if (packType == "gif") {
            Log.d("hell2", packType)
            gifsAdapter = GifsAdapter(this, templates)
            recyclerView.adapter = gifsAdapter
        } else {
            tempAdapter = TemplateAdapter(this, templates)
            recyclerView.adapter = tempAdapter
        }
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayout.VERTICAL)

        loadMoreBtn.visibility = View.GONE

        firestore = FirebaseFirestore.getInstance()

        val first = firestore.collection(pathNodes[0]).document(pathNodes[1]).collection(pathNodes[2])
            .orderBy("filename").limit(limit)
        fetch(first)

        loadMoreBtn.setOnClickListener {
            loadMoreBtn.visibility = View.GONE
            loadingBar.visibility = View.VISIBLE
            loadMore()
        }
    }

    private fun loadMore() {
        if (!isFetched) {
            val next = firestore.collection(pathNodes[0]).document(pathNodes[1]).collection(pathNodes[2])
                .orderBy("filename")
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
                loadMoreBtn.visibility = View.GONE
                isFetched = true
                return@addOnSuccessListener
            }
            val startPosition = templates.size - 1
            val tmpTemplates = ArrayList<Template>()
            lastDocument = snapshot.documents[snapshot.documents.size - 1]
            snapshot.documents.forEach {  document ->
                val data = document.data
                val thumb = data!!["thumb"].toString()
                val temp = Template(
                    filename = data["filename"]!!.toString(),
                    caption = data["caption"]!!.toString(),
                    url = data["url"]!!.toString(),
                    thumb = thumb
                )
                tmpTemplates.add(temp)
            }
            if (tmpTemplates.isEmpty()) {
                loadMore()
                return@addOnSuccessListener
            }
            tmpTemplates.forEach { templates.add(it) }

            if (packType == "gif") {
                gifsAdapter.notifyItemRangeChanged(startPosition, tmpTemplates.size)
            } else {
                tempAdapter.notifyItemRangeChanged(startPosition, tmpTemplates.size)
            }

            if (tmpTemplates.size < limit) {
                loadingBar.visibility = View.GONE
                loadMoreBtn.visibility = View.GONE
                isFetched = true
            } else {
                loadMoreBtn.visibility = View.VISIBLE
                loadingBar.visibility = View.GONE
            }
        }.addOnFailureListener {
            toast(getString(R.string.network_fail))
            loadMoreBtn.visibility = View.VISIBLE
            loadingBar.visibility = View.GONE
        }
    }
}
