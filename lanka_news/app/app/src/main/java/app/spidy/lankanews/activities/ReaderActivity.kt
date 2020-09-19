package app.spidy.lankanews.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.NumberPicker
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import app.spidy.kotlinutils.*
import app.spidy.lankanews.R
import app.spidy.lankanews.adapters.ArticleAdapter
import app.spidy.lankanews.data.News
import app.spidy.lankanews.utils.API
import app.spidy.lankanews.parsers.NewsPageParser
import app.spidy.lankanews.utils.Ads
import app.spidy.lankanews.utils.C
import app.spidy.oli.databases.LankaDatabase
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_reader.*


class ReaderActivity : AppCompatActivity() {
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var tinyDB: TinyDB
    private lateinit var database: LankaDatabase
    private lateinit var newsUrl: String
    private lateinit var newsDate: String
    private lateinit var newsTitle: String

    private var optionMenu: Menu? = null
    private var newsImage: String? = null
    private var isContainVideo: Boolean = false
    private var headingSize: Float = 0f
    private val paras = ArrayList<String>()
    private val textSizes = arrayOf("0.5x", "1x", "1.5x", "2x", "2.5x", "3x")
    private val textSizeValues = arrayOf(.5f, 1f, 1.5f, 2f, 2.5f, 3f)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        adView.loadAd(AdRequest.Builder().build())
        Ads.showInterstitial()
        Ads.loadInterstitial()

        tinyDB = TinyDB(applicationContext)
        headingSize = pixelsToSp(resources.getDimension(R.dimen.reader_text_size_heading))
        database = Room.databaseBuilder(this, LankaDatabase::class.java, "LankaDatabase")
            .fallbackToDestructiveMigration().build()

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        newsUrl = intent!!.getStringExtra("url")!!
        newsImage = intent!!.getStringExtra("image")
        newsDate = intent!!.getStringExtra("date")!!
        newsTitle = intent!!.getStringExtra("title")!!

        toolbar.title = newsTitle
        articleAdapter = ArticleAdapter(this, paras)
        val parser = NewsPageParser()

        val savedTextSize = tinyDB.getInt(C.TAG_READER_TEXT_SIZE, -1)
        if (savedTextSize != -1 && savedTextSize != 1) {
            headingSize = pixelsToSp(resources.getDimension(R.dimen.reader_text_size_heading)) * textSizeValues[savedTextSize]
            titleView.textSize = headingSize
            articleAdapter.updateTextSize(textSizeValues[savedTextSize])
        }

        recyclerView.adapter = articleAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        titleView.text = newsTitle
        publishedView.text = newsDate

        var isImageLoaded = false

        if (newsImage == null) {
            coverImageView.visibility = View.GONE
        } else {
            debug("Image loaded")
            isImageLoaded = true
            Glide.with(this).load(newsImage).into(coverImageView)
        }

        API.get(newsUrl).then {
            val article = parser.parse(it.text!!)

            onUiThread {
                if (article.videoId != null) {
                    isContainVideo = true
                    playImageView.visibility = View.VISIBLE
                    coverImageView.setOnClickListener {
                        watchYoutubeVideo(article.videoId)
                    }
                }

                if (!isImageLoaded) {
                    if (article.coverImage == null) {
                        coverImageView.visibility = View.GONE
                    } else {
                        coverImageView.visibility = View.VISIBLE
                        Glide.with(this).load(article.coverImage).into(coverImageView)
                    }
                }

                for (p in article.paras) paras.add(p)
                articleAdapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                nestedScrollView.visibility = View.VISIBLE
            }
        }.catch {
            onUiThread {
                progressBar.visibility = View.GONE
                nestedScrollView.visibility = View.VISIBLE
            }
        }
    }


    private fun watchYoutubeVideo(id: String) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$id"))
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("http://www.youtube.com/watch?v=$id")
        )
        try {
            startActivity(appIntent)
        } catch (ex: ActivityNotFoundException) {
            startActivity(webIntent)
        }
    }

    private fun pixelsToSp(px: Float): Float {
        val scaledDensity: Float = resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_reader, menu)
        optionMenu = menu
        async {
            val news = database.dao().getNews(newsUrl)
            if (news.isNotEmpty()) {
                onUiThread {
                    menu?.getItem(1)?.icon = ContextCompat.getDrawable(this, R.drawable.ic_bookmark_active)
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuTextSize -> {
                val v = LayoutInflater.from(this).inflate(R.layout.layout_text_size_dialog, null)
                val picker: NumberPicker = v.findViewById(R.id.picker)
                val savedTextSize = tinyDB.getInt(C.TAG_READER_TEXT_SIZE, -1)
                picker.minValue = 0
                picker.maxValue = textSizes.size - 1
                picker.value = if (savedTextSize != -1) savedTextSize else 1
                picker.displayedValues = textSizes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    picker.selectionDividerHeight = 0
                }
                newDialog().withCustomView(v)
                    .withNegativeButton(getString(R.string.cancel)) { dialog ->
                        dialog.dismiss()
                    }
                    .withPositiveButton(getString(R.string.apply)) { dialog ->
                        if (tinyDB.getInt(C.TAG_READER_TEXT_SIZE, -1) != picker.value) {
                            headingSize = pixelsToSp(resources.getDimension(R.dimen.reader_text_size_heading)) * textSizeValues[picker.value]
                            titleView.textSize = headingSize
                            articleAdapter.updateTextSize(textSizeValues[picker.value])
                            articleAdapter.notifyDataSetChanged()
                            tinyDB.putInt(C.TAG_READER_TEXT_SIZE, picker.value)
                        }
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
            R.id.menuBookmark -> {
                async {
                    val newses = database.dao().getNews(newsUrl)
                    val news = News(
                        title = newsTitle,
                        image = newsImage,
                        date = newsDate,
                        isContainVideo = isContainVideo,
                        url = newsUrl
                    )
                    if (newses.isEmpty()) {
                        database.dao().putNews(news)
                        onUiThread {
                            toast("Bookmarked")
                            optionMenu?.getItem(1)?.icon = ContextCompat.getDrawable(this, R.drawable.ic_bookmark_active)
                        }
                    } else {
                        database.dao().removeNews(news)
                        onUiThread {
                            toast("Bookmark removed")
                            optionMenu?.getItem(1)?.icon = ContextCompat.getDrawable(this, R.drawable.ic_bookmark)
                        }
                    }
                }
            }
        }

        return true
    }
}