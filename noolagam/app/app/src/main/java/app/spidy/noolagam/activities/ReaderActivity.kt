package app.spidy.noolagam.activities

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.viewpager.widget.ViewPager
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.toast
import app.spidy.noolagam.R
import app.spidy.noolagam.adapters.ImageSliderAdapter
import app.spidy.noolagam.data.Book
import app.spidy.noolagam.utils.Ads
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_reader.*
import kotlinx.android.synthetic.main.activity_reader.adView
import kotlinx.android.synthetic.main.activity_reader.toolbar
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class ReaderActivity : AppCompatActivity() {
    private lateinit var tinyDB: TinyDB
    private lateinit var intentLabel: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        adView.loadAd(AdRequest.Builder().build())

        Ads.showInterstitial()
        Ads.loadInterstitial()

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        tinyDB = TinyDB(this)
        intentLabel = intent?.getStringExtra("label")!!

        val bufferedReader = BufferedReader(InputStreamReader(assets.open("noolagam.json"), "UTF-8"))
        var line: String?
        var jsonString = ""
        while (bufferedReader.readLine().also { line = it } != null) {
            jsonString += line
        }
        bufferedReader.close()

        var book: Book? = null
        val shelf = JSONArray(jsonString)
        for (i in 0 until shelf.length()) {
            val cat = shelf.getJSONObject(i)
            val category = cat.getString("c")
            val bks = cat.getJSONArray("b")
            for (j in 0 until bks.length()) {
                val bk = bks.getJSONObject(j)
                val title = bk.getString("t")
                val pageCount = bk.getInt("p")
                val label = bk.getString("l")

                if (label == intentLabel) {
                    book = Book(
                        title = title,
                        pageCount = pageCount,
                        label = label,
                        category = category
                    )
                    break
                }
            }
            if (book != null) break
        }
        val images = ArrayList<String>()
        for (i in 1 .. book!!.pageCount) {
            images.add("https://online.pubhtml5.com/${book.label}/files/large/$i.jpg")
        }
        val imageSliderAdapter = ImageSliderAdapter(this, images)
        viewPager.adapter = imageSliderAdapter
        viewPager.currentItem = tinyDB.getInt(intentLabel, 0)
        bookTitleView.text = book.title
        pageCountView.text = "${tinyDB.getInt(intentLabel, 0) + 1} / ${book.pageCount}"

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                tinyDB.putInt(intentLabel, position)
                pageCountView.text = "${position + 1} / ${book.pageCount}"
            }

            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_reader, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuGotoPage -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Go to Page")
                val input = EditText(this)
                input.inputType = InputType.TYPE_CLASS_TEXT
                input.setText((tinyDB.getInt(intentLabel, 0) + 1).toString())
                input.setSelectAllOnFocus(true)
                builder.setView(input)

                builder.setPositiveButton("OK") { _, _ ->
                    try {
                        val pageNum = input.text.toString().toInt() - 1
                        viewPager.currentItem = pageNum
                    } catch (e: Exception) {
                        toast("Unable to switch to that page")
                    }
                }
                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

                builder.show()
            }
        }

        return true
    }
}