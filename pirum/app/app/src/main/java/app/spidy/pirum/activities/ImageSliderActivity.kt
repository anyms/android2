package app.spidy.pirum.activities

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import androidx.viewpager.widget.ViewPager
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.toast
import app.spidy.pirum.R
import app.spidy.pirum.adapters.ImageSliderAdapter
import app.spidy.pirum.data.Other
import app.spidy.pirum.databases.PyrumDatabase
import app.spidy.pirum.utils.IO
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_downloads.*
import kotlinx.android.synthetic.main.activity_image_slider.*
import kotlinx.android.synthetic.main.activity_image_slider.nameView
import kotlinx.android.synthetic.main.music_exo_player_control_view.*
import org.json.JSONArray
import java.net.URLDecoder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class ImageSliderActivity : AppCompatActivity() {
    private lateinit var database: PyrumDatabase
    private lateinit var tinyDB: TinyDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_slider)

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        tinyDB = TinyDB(this)

        val adView: AdView = findViewById(R.id.adView)
        if (!tinyDB.getBoolean("isPro")) {
            adView.loadAd(AdRequest.Builder().build())
        } else {
            adView.visibility = View.GONE
        }

        val data = intent?.getStringExtra("data")
        val isFromLocal = intent?.getBooleanExtra("isFromLocal", false)
        database = Room.databaseBuilder(this, PyrumDatabase::class.java, "PyrumDatabase")
            .fallbackToDestructiveMigration().build()
        val playlistName = intent?.getStringExtra("playlistName") ?: IO.getPlaylistName()
        val images = ArrayList<Other>()
        var currentIndex = 0
        var isCenterCrop = false
        val adapter = ImageSliderAdapter(this, images) {
            return@ImageSliderAdapter isCenterCrop
        }
        val  downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        if (data != null) {
            val imageList: JSONArray
            try {
                imageList = JSONArray(data)
            } catch (e: Exception) {
                toast("Invalid request")
                finish()
                return
            }

            for (i in 0 until imageList.length()) {
                val info = imageList.getJSONObject(i)
                val img = URLDecoder.decode(info.getString("src"), "UTF-8")
                images.add(Other(
                    uId = UUID.randomUUID().toString(),
                    playlistName = playlistName,
                    src = img,
                    title = IO.getFileNameFromURL(img) ?: "-",
                    type = Other.TYPE_IMAGE
                ))
            }

            if (isFromLocal == null || !isFromLocal) {
                thread {
                    for (m in images) {
                        database.pyrumDao().putOther(m)
                    }
                }
            }

            viewPager.adapter = adapter
            nameView.text = images[0].title
        } else if (isFromLocal == true) {
            thread {
                database.pyrumDao().getOtherByPlaylist(playlistName).forEach {
                    images.add(it)
                }

                runOnUiThread {
                    viewPager.adapter = adapter
                    nameView.text = images[0].title
                }
            }
        }

        fullscreenBtn.setOnClickListener {
            if (isCenterCrop) {
                fullscreenBtn.setImageResource(R.drawable.ic_fullscreen)
                for ((_, imageView) in adapter.imageViews) {
                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                }
                isCenterCrop = false
            } else {
                fullscreenBtn.setImageResource(R.drawable.ic_exit_fullscreen)
                for ((_, imageView) in adapter.imageViews) {
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                }
                isCenterCrop = true
            }
        }

        downloadBtn.setOnClickListener {
            val uri = Uri.parse(images[currentIndex].src)
            val request = DownloadManager.Request(uri)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, images[currentIndex].title)
            downloadManager.enqueue(request)

            toast("Downloading ${images[currentIndex].title}...")
        }

        exitBtn.setOnClickListener {
            finish()
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {}
            override fun onPageSelected(position: Int) {
                currentIndex = position
                nameView.text = images[position].title
            }
        })
    }
}
