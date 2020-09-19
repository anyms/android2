package app.spidy.memecreator.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.room.Room
import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.SavedMemeAdapter
import app.spidy.memecreator.data.SavedMeme
import app.spidy.memecreator.databases.MemeDatabase
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlin.concurrent.thread

class YourMemeActivity : AppCompatActivity() {
    private lateinit var database: MemeDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SavedMemeAdapter
    private lateinit var noGifsView: TextView

    private val savedMemes = ArrayList<SavedMeme>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_your_meme)

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        recyclerView = findViewById(R.id.recyclerView)
        adapter = SavedMemeAdapter(this, savedMemes)
        noGifsView = findViewById(R.id.noGifsView)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayout.VERTICAL)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        title = getString(R.string.your_memes)

        database = Room.databaseBuilder(this, MemeDatabase::class.java, "MemeDatabase")
            .fallbackToDestructiveMigration().build()
    }

    override fun onResume() {
        savedMemes.clear()
        thread {
            val saved = database.memeDao().getSavedMemes()

            runOnUiThread {
                saved.forEach {
                    savedMemes.add(it)
                }
                saved.reversed()
                adapter.notifyDataSetChanged()

                if (saved.isEmpty()) {
                    noGifsView.visibility = View.VISIBLE
                } else {
                    noGifsView.visibility = View.GONE
                }
            }
        }

        super.onResume()
    }
}
