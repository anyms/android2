package app.spidy.memecreator.activities

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.kotlinutils.toast
import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.GifsAdapter
import app.spidy.memecreator.adapters.PackAdapter
import app.spidy.memecreator.adapters.TemplateAdapter
import app.spidy.memecreator.data.Pack
import app.spidy.memecreator.data.Template
import app.spidy.memecreator.databases.MemeDatabase
import app.spidy.memecreator.utils.TinyDB
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class StoreActivity : AppCompatActivity() {
    companion object {
        const val INSTALLED_PACKS = "app.spidy.memecreator.INSTALLED_PACKS"
    }

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: MemeDatabase
    private lateinit var adapter: PackAdapter
    private lateinit var loadingBar: ProgressBar
    private lateinit var tinyDB: TinyDB
    private lateinit var searchBox: EditText
    private lateinit var notFoundVIew: TextView

    private val packs = ArrayList<Pack>()
    private val loadedPacks = ArrayList<Pack>()
    private val filters = ArrayList<String>()

    private var installDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        tinyDB = TinyDB(this)
        filters.add("All")
        filters.add("Images")
        filters.add("GIFs")

        if (tinyDB.getString("MemeType") == null) {
            tinyDB.putString("MemeType", "All")
        }

        database = Room.databaseBuilder(this, MemeDatabase::class.java, "MemeDatabase")
            .fallbackToDestructiveMigration().build()
        firestore = FirebaseFirestore.getInstance()
        toolbar = findViewById(R.id.toolbar)
        loadingBar = findViewById(R.id.loadingBar)
        recyclerView = findViewById(R.id.recyclerView)
        adapter = PackAdapter(this, packs) {
            installDialog = showInstallDialog(it)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        searchBox = findViewById(R.id.searchBox)
        notFoundVIew = findViewById(R.id.notFoundVIew)

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        searchBox.setOnEditorActionListener { _, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE || event.action == KeyEvent.ACTION_DOWN
                || event.action == KeyEvent.KEYCODE_ENTER) {
                search(searchBox.text.toString().toLowerCase(Locale.getDefault()))
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        initialFetch()
    }

    private fun initialFetch() {
        firestore.collection("metas").get().addOnSuccessListener {
            it.documents.forEach { document ->
                val data = document.data
                val pack = Pack(
                    coverImage = data!!["cover"].toString(),
                    type = data["type"].toString(),
                    path = data["path"].toString(),
                    title = data["title"].toString()
                )
                loadedPacks.add(pack)
            }
            loadedPacks.reverse()
            updatePacks("All")
        }.addOnFailureListener {
            toast(getString(R.string.network_fail), isLong = true)
        }
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

    private fun updatePacks(packType: String) {
        Log.d("hello2", packType)
        showLoadingBar()
        thread {
            Thread.sleep(2000)
            runOnUiThread {
                packs.clear()
                adapter.notifyDataSetChanged()
                for (pack in loadedPacks) {
                    if (pack.type == "gif" && packType == "GIFs") {
                        packs.add(pack)
                    } else if (pack.type == "meme" && packType == "Images") {
                        packs.add(pack)
                    } else if (packType == "All") {
                        packs.add(pack)
                    }
                }
                adapter.notifyDataSetChanged()
                if (packs.isEmpty()) {
                    showNotFound()
                } else {
                    showRecyclerView()
                }
            }
        }
    }

    private fun search(query: String) {
        showLoadingBar()
        thread {
            Thread.sleep(2000)

            runOnUiThread {
                val hashes = HashMap<String, Pack>()
                val searchQueries = ArrayList<String>()
                for (pack in loadedPacks) {
                    val packTitle = pack.title.toLowerCase(Locale.getDefault())
                    searchQueries.add(packTitle)
                    hashes[packTitle] = pack
                }
                packs.clear()
                adapter.notifyDataSetChanged()
                for (q in searchQueries) {
                    if (query in q) {
                        packs.add(hashes[q]!!)
                    }
                }
                if (packs.isEmpty()) {
                    showNotFound()
                } else {
                    showRecyclerView()
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }


    private fun showInstallDialog(pack: Pack): android.app.AlertDialog {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setCancelable(true)
        val viewGroup: ViewGroup? = null
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_install, viewGroup, false)
        builder.setView(view)

        val titleView: TextView = view.findViewById(R.id.titleView)
        val typeView: TextView = view.findViewById(R.id.typeView)
//        val coverImageView: ImageView = view.findViewById(R.id.coverImageView)
        val installBtn: Button = view.findViewById(R.id.installBtn)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        val temps = ArrayList<Template>()
        val adapter: Any = if (pack.type == "gif") {
            GifsAdapter(this, temps)
        } else {
            TemplateAdapter(this, temps)
        }
        if (pack.type == "gif") {
            recyclerView.adapter = adapter as GifsAdapter
        } else {
            recyclerView.adapter = adapter as TemplateAdapter
        }
        recyclerView.layoutManager = LinearLayoutManager(this)

        val nodes = pack.path.split("/").toMutableList()
        val db = firestore.collection(nodes[0]).document(nodes[1]).collection(nodes[2])
            .orderBy("filename").limit(4)

        db.get().addOnSuccessListener {
            for (document in it.documents) {
                val data = document.data
                temps.add(Template(
                    filename = data!!["filename"]!!.toString(),
                    caption = data["caption"]!!.toString(),
                    url = data["url"]!!.toString()
                ))
            }
            if (pack.type == "gif") {
                (adapter as GifsAdapter).notifyDataSetChanged()
            } else {
                (adapter as TemplateAdapter).notifyDataSetChanged()
            }
        }

//        val options = RequestOptions()
//            .format(DecodeFormat.PREFER_RGB_565)
//            .centerCrop()
//            .error(R.drawable.loading)
//            .fallback(R.drawable.loading)
//            .placeholder(R.drawable.loading)
//            .diskCacheStrategy(DiskCacheStrategy.RESOURCE);

//        Glide.with(this).load(pack.coverImage).thumbnail(0.5f)
//            .transition(DrawableTransitionOptions.withCrossFade(300))
//            .apply(options)
//            .into(coverImageView)

        titleView.text = pack.title
        typeView.text = pack.type

        val dialog = builder.create()

        val installedPacks = tinyDB.getListString(INSTALLED_PACKS).toMutableList()
        var shouldDelete = false
        if (pack.title in installedPacks) {
            installBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.colorRed))
            installBtn.text = getString(R.string.delete)
            progressBar.visibility = View.GONE
            shouldDelete = true
        }

        installBtn.setOnClickListener {
            if (shouldDelete) {
                installedPacks.remove(pack.title)
                thread {
                    database.memeDao().removePack(pack.title)
                    runOnUiThread {
                        tinyDB.putListString(INSTALLED_PACKS, installedPacks)
                        toast(getString(R.string.remove_pack_success))
                        dialog.dismiss()
                    }
                }
                return@setOnClickListener
            }

            thread {
                val installedPackTitles = tinyDB.getListString(INSTALLED_PACKS).toMutableList()
                installedPackTitles.add(pack.title)
                tinyDB.putListString(INSTALLED_PACKS, installedPackTitles)
                database.memeDao().putPack(pack)

                runOnUiThread {
                    dialog.dismiss()
                    toast(getString(R.string.install_success))
                }
            }
        }

        dialog.show()
        return dialog
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_store, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuSearch -> {
                search(searchBox.text.toString().toLowerCase(Locale.getDefault()))
            }
            R.id.menuFilter -> {
                val selected = filters.indexOf(tinyDB.getString("MemeType"))
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Filter")
                builder.setSingleChoiceItems(filters.toArray(arrayOfNulls<String>(filters.size)), selected) { dialog, which ->
                    dialog.dismiss()
                    tinyDB.putString("MemeType", filters[which])
                    updatePacks(filters[which])
                }
                builder.show()
            }
        }
        return true
    }

//    private fun getUserCountry(context: Context): String? {
//        try {
//            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//            val simCountry = tm.simCountryIso
//            if (simCountry != null && simCountry.length == 2) {
//                return simCountry.toLowerCase(Locale.US)
//            } else if (tm.phoneType != TelephonyManager.PHONE_TYPE_CDMA) {
//                val networkCountry = tm.networkCountryIso
//                if (networkCountry != null && networkCountry.length == 2) {
//                    return networkCountry.toLowerCase(Locale.US)
//                }
//            }
//        } catch (e: Exception) {
//        }
//        return null
//    }
}
