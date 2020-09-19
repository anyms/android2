package app.spidy.tamillovevideostatus.activities

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.spidy.kotlinutils.Permission
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.tamillovevideostatus.R
import app.spidy.tamillovevideostatus.adapters.VideoListAdapter
import app.spidy.tamillovevideostatus.data.Video
import app.spidy.tamillovevideostatus.utils.Ads
import app.spidy.tamillovevideostatus.utils.Http
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private lateinit var adapter: VideoListAdapter
    private val videos = ArrayList<Video>()
    private lateinit var permission: Permission
    private var currentPageNumber = 1
    private var isRecyclerViewWaitingtoLaadData = false
    private var isNextExists = true
    private var endPoint = "category/Love & Romance"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        setSupportActionBar(toolbar)
//        toolbar.setNavigationIcon(R.drawable.ic_menu)

        val adView: AdView = findViewById(R.id.adView)
        adView.loadAd(AdRequest.Builder().build())
        Ads.initInterstitial(this)
        Ads.loadInterstitial()

        permission = Permission(this)
        permission.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, null, object : Permission.Listener {
            override fun onGranted() {

            }
            override fun onRejected() {

            }
        })

        adapter = VideoListAdapter(this, videos)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, GridLayoutManager.VERTICAL)

        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > oldScrollY) {
//                debug("Scroll DOWN")
            }
            if (scrollY < oldScrollY) {
//                debug("Scroll UP")
            }
            if (scrollY == 0) {
//                debug("TOP SCROLL")
            }
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
//                debug("BOTTOM SCROLL")
                if (!isRecyclerViewWaitingtoLaadData && isNextExists) {
                    loadPage(currentPageNumber)
                }
            }
        })

        loadPage(currentPageNumber)
    }


    private fun loadPage(num: Int) {
        progressBar.visibility = View.VISIBLE
        isRecyclerViewWaitingtoLaadData = true
//        debug("Page: $num")
        Http.async.get("${Http.apiUrl}/${endPoint}/${num}?key=${Http.apiKey}").then {
            val data = JSONObject(it.text!!)
            isNextExists = data.getBoolean("is_next_exists")
            val startPos = videos.size
            val vids = data.getJSONArray("videos")
            for (i in 0 until vids.length()) {
                val vid = vids.getJSONObject(i)
                val video = Video(
                    uId = vid.getLong("id"),
                    downloadCount = vid.getInt("download_count"),
                    shareCount = vid.getInt("share_count"),
                    viewCount = vid.getInt("view_count"),
                    tags = vid.getString("tags"),
                    title = vid.getString("title"),
                    videoId = vid.getString("video_id"),
                    isExpired = vid.getBoolean("is_expired"),
                    category = vid.getString("category"),
                    thumb = vid.getString("thumb"),
                    expire = vid.getLong("expire")
                )
                videos.add(video)
            }
            onUiThread {
                progressBar.visibility = View.GONE
                adapter.notifyItemRangeChanged(startPos, vids.length())
                isRecyclerViewWaitingtoLaadData = false
                currentPageNumber++
            }
        }.catch {
//            debug(it)
            onUiThread {
                newDialog().withTitle("Error!")
                    .withMessage("Network error occurred! Do you want to try again?")
                    .withCancelable(false)
                    .withPositiveButton("Try again") { dialog ->
                        loadPage(num)
                        dialog.dismiss()
                    }
                    .withNegativeButton(getString(R.string.cancel)) { dialog ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSearch -> {
                startActivity(Intent(this, SearchActivity::class.java))
            }
            R.id.menuFeedback -> {
                val uri = Uri.parse("market://details?id=$packageName");
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName"))
                    )
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permission.execute(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}