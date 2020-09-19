package app.spidy.memecreator.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.TemplateAdapter
import app.spidy.memecreator.data.Template
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.collections.ArrayList

class SearchActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loadingBar: ProgressBar
    private lateinit var searchBox: EditText
    private lateinit var notFoundVIew: TextView
    private lateinit var adapter: TemplateAdapter

    private val memes = ArrayList<Template>()
    private val loadedMemes = ArrayList<Template>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        firestore = FirebaseFirestore.getInstance()
        toolbar = findViewById(R.id.toolbar)
        loadingBar = findViewById(R.id.loadingBar)
        recyclerView = findViewById(R.id.recyclerView)
        searchBox = findViewById(R.id.searchBox)
        notFoundVIew = findViewById(R.id.notFoundVIew)
        adapter = TemplateAdapter(this, memes)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayout.VERTICAL)

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        searchBox.setOnEditorActionListener { _, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE || event.action == KeyEvent.ACTION_DOWN
                || event.action == KeyEvent.KEYCODE_ENTER) {
                val query = searchBox.text.toString().toLowerCase(Locale.ROOT).split(" ")
                search(query)
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        showNotFound()
    }

    private fun showNotFound() {
        notFoundVIew.visibility = View.VISIBLE
        loadingBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
    }

    private fun showLoadingBar() {
        notFoundVIew.visibility = View.GONE
        loadingBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showRecyclerView() {
        notFoundVIew.visibility = View.GONE
        loadingBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun search(query: List<String>) {
        memes.clear()
        showLoadingBar()
        firestore.collection("memes").document("packs").collection("popular")
            .whereArrayContainsAny("keywords", query)
            .get().addOnSuccessListener {
                for (document in it.documents) {
                    val data = document.data!!
                    memes.add(Template(
                        filename = data["filename"].toString(),
                        caption = data["caption"].toString(),
                        url = data["url"].toString()
                    ))
                }
                searchGifs(query)
            }
    }

    private fun searchGifs(query: List<String>) {
        firestore.collection("gifs").document("packs").collection("popular")
            .whereArrayContainsAny("keywords", query)
            .get().addOnSuccessListener {
                for (document in it.documents) {
                    val data = document.data!!
                    Log.d("hell2", data.toString())
                    memes.add(Template(
                        filename = data["filename"].toString(),
                        caption = data["caption"].toString(),
                        url = data["url"].toString(),
                        thumb = data["thumb"].toString()
                    ))
                }
                if (memes.isEmpty()) {
                    showNotFound()
                } else {
                    adapter.notifyDataSetChanged()
                    showRecyclerView()
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_store, menu)
        menu?.getItem(1)?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuSearch -> {
                search(searchBox.text.toString().toLowerCase(Locale.ROOT).split(" "))
            }
        }
        return true
    }
}
