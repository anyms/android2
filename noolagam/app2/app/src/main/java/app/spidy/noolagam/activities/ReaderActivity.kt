package app.spidy.noolagam.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import androidx.viewpager.widget.ViewPager
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.toast
import app.spidy.noolagam.R
import app.spidy.noolagam.adapters.PagerAdapter
import app.spidy.noolagam.data.Book
import app.spidy.noolagam.databases.NoolagamDatabase
import app.spidy.noolagam.utils.Ads
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_reader.*
import java.lang.Exception
import kotlin.concurrent.thread


class ReaderActivity : AppCompatActivity() {
    private lateinit var adapter: PagerAdapter
    private lateinit var tinyDB: TinyDB
    private var pageCount: Int = 0
    private lateinit var book: Book
    private lateinit var database: NoolagamDatabase

    private val pageNums = ArrayList<Int>()

    lateinit var pageNumView: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        adView.loadAd(AdRequest.Builder().build())
        Ads.showInterstitial()
        Ads.loadInterstitial()

        database = Room.databaseBuilder(this, NoolagamDatabase::class.java, "NoolagamDatabase")
            .fallbackToDestructiveMigration().build()
        pageNumView = findViewById(R.id.pageNumView)

        setSupportActionBar(toolbar)
        tinyDB = TinyDB(this)

        book = Book(
            bookId = intent!!.getStringExtra("book_id")!!,
            category = intent!!.getStringExtra("category")!!,
            cover = intent!!.getStringExtra("cover")!!,
            id = intent!!.getIntExtra("id", 0),
            pageCount = intent!!.getIntExtra("page_count", 0),
            published = intent!!.getStringExtra("published")!!,
            timestamp = intent!!.getLongExtra("timestamp", 0),
            title = intent!!.getStringExtra("title")!!,
            viewCount = intent!!.getIntExtra("view_count", 0)
        )
        pageCount = book.pageCount

        toolbar.title = book.title
        toolbar.setTitleTextAppearance(this, R.style.TamilFontStyle)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        for (i in 1..pageCount) {
            pageNums.add(i)
        }

        val currentPage = tinyDB.getInt(book.bookId, 0)
        adapter = PagerAdapter(book.bookId, pageNums, supportFragmentManager)
        viewPager.adapter = adapter
        viewPager.currentItem = currentPage
        pageNumView.text = "${currentPage + 1} / $pageCount"
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                pageNumView.text = "${position + 1} / $pageCount"
                pageNumView.visibility = View.VISIBLE
                tinyDB.putInt(book.bookId, position)

                Ads.showInterstitial()
                Ads.loadInterstitial()
            }
        })

        thread {
            database.dao().putBook(book)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_reader, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuGoToPage -> {
                val v = LayoutInflater.from(this).inflate(R.layout.layout_go_to_page_dialog, null)
                val numView: EditText = v.findViewById(R.id.numView)
                newDialog().withCustomView(v)
                    .withTitle(getString(R.string.go_to_page))
                    .withPositiveButton(getString(R.string.go)) { dialog ->
                        try {
                            val num = numView.text.toString().toInt()
                            if (num in 1..pageCount) {
                                viewPager.currentItem = num
                            } else {
                                toast("${getString(R.string.there_is_no_page)} $num")
                            }
                        } catch (e: Exception) {}
                        dialog.dismiss()
                    }
                    .withNegativeButton(getString(R.string.cancel)) { dialog ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }

            R.id.menuDownload -> {
                newDialog().withTitle(getString(R.string.info))
                    .withCancelable(false)
                    .withMessage(getString(R.string.download_info_message))
                    .withPositiveButton(getString(R.string.got_it)) { dialog ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }

        return false
    }
}