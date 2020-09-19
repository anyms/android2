package app.spidy.memecreator.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.kotlinutils.onUiThread

import app.spidy.memecreator.R
import app.spidy.memecreator.activities.ShowTemplateActivity
import app.spidy.memecreator.activities.StoreActivity
import app.spidy.memecreator.adapters.PackAdapter
import app.spidy.memecreator.data.Pack
import app.spidy.memecreator.databases.MemeDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.concurrent.thread


class PagePacksFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var gotoStoreBtn: Button
    private lateinit var noTemplateView: TextView
    private lateinit var adapter: PackAdapter
    private lateinit var database: MemeDatabase

    private val packs = ArrayList<Pack>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_page_packs, container, false)

        database = Room.databaseBuilder(context!!, MemeDatabase::class.java, "MemeDatabase")
            .fallbackToDestructiveMigration().build()

        recyclerView = v.findViewById(R.id.recyclerView)
        gotoStoreBtn = v.findViewById(R.id.gotoStoreBtn)
        noTemplateView = v.findViewById(R.id.noTemplateView)
        adapter = PackAdapter(context, packs) {
            val intent = Intent(context, ShowTemplateActivity::class.java)
            intent.putExtra("pack_title", it.title)
            intent.putExtra("pack_path", it.path)
            intent.putExtra("pack_type", it.type)
            startActivity(intent)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        gotoStoreBtn.setOnClickListener {
            startActivity(Intent(context, StoreActivity::class.java))
        }

        return v
    }

    private fun loadPacks() {
        packs.clear()
        thread {
            val installedPacks = database.memeDao().getPacks()
            for (pack in installedPacks) {
                packs.add(pack)
            }
            onUiThread {
                packs.reverse()
                adapter.notifyDataSetChanged()

                if (packs.isEmpty()) {
                    showNotFound()
                } else {
                    showRecyclerView()
                }
            }
        }
    }

    private fun showNotFound() {
        noTemplateView.visibility = View.VISIBLE
        gotoStoreBtn.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }
    private fun showRecyclerView() {
        noTemplateView.visibility = View.GONE
        gotoStoreBtn.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }


    override fun onResume() {
        loadPacks()
        super.onResume()
    }
}
