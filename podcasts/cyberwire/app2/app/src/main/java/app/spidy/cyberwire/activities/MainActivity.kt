package app.spidy.cyberwire.activities

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.cyberwire.BuildConfig
import app.spidy.cyberwire.R
import app.spidy.cyberwire.data.Episode
import app.spidy.cyberwire.fragments.DownloadFragment
import app.spidy.cyberwire.fragments.HomeFragment
import app.spidy.cyberwire.fragments.RecentFragment
import app.spidy.cyberwire.services.PlayerService
import app.spidy.cyberwire.utils.Ads
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var playerService: PlayerService? = null
    private var isBound = false
    private var currentPage = "home"

    private val playerListener = object : PlayerService.Listener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            loadingBar.visibility = if (playbackState == Player.STATE_BUFFERING) { View.VISIBLE } else { View.GONE }
//            playBtn.visibility = if (playbackState == Player.STATE_BUFFERING) { View.GONE } else { View.VISIBLE }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            newDialog().withTitle("Playback Error!")
                .withMessage("An error occurred while playing the episode.")
                .withCancelable(false)
                .withPositiveButton("Got it!") {dialog ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray,
            episode: Episode,
            coverImageUrl: String
        ) {
            updateQuickView(episode, coverImageUrl)
        }

        override fun onQueueEpisode(episode: Episode) {

        }
    }

    private fun updateQuickView(episode: Episode, coverImageUrl: String) {
        Glide.with(this@MainActivity).load(coverImageUrl).into(channelImageView)
        episodeTitleView.text = episode.title
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playerService = (service as? PlayerService.PlayerBinder)?.playerService
            isBound = true
            playerService?.apply {
                clientTerminator = {
                    unbind()
                }
                listener = playerListener
                updateQuickView(music[currentIndex], coverImageUrl)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            playerService = null
        }
    }

    private fun unbind() {
        if (isBound) {
            isBound = false
            unbindService(serviceConnection)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())
        Ads.init(this)
        Ads.loadInterstitial()

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_menu)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START, true)
        }

        navView.setNavigationItemSelectedListener(this)
        onNavigationItemSelected(navView.menu.getItem(0).setChecked(true))

        closeBtn.setOnClickListener {
            playerService?.terminate()
            quickControlView.visibility = View.GONE
        }

        quickControlView.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        FirebaseMessaging.getInstance().subscribeToTopic("podcast_cyberwire").addOnSuccessListener {
            debug("subscribed")
        }.addOnFailureListener {
            debug(it)
        }

        if (intent.extras?.getBoolean("is_daily_digest") == true) {
            title = "Daily Digest"
            currentPage = "recent"
            supportFragmentManager.beginTransaction()
                .replace(R.id.pageContainer, RecentFragment())
                .commit()
        } else {
            currentPage = "home"
            supportFragmentManager.beginTransaction()
                .add(R.id.pageContainer, HomeFragment())
                .commit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.nav_share -> {
                ignore {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Oli Podcast")
                    var shareMessage = "\nTry the number one tamil podcast app on Googleplay\n\n"
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID +"\n\n"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                    startActivity(Intent.createChooser(shareIntent, "Share with"))
                }
            }
            R.id.nav_feedback -> {
                val uri = Uri.parse("market://details?id=$packageName");
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    startActivity(goToMarket);
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName"))
                    )
                }
            }
            R.id.nav_home -> {
                if (currentPage != "home") {
                    title = "Cyberwire"
                    currentPage = "home"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.pageContainer, HomeFragment())
                        .commit()
                }
            }
            R.id.nav_downloads -> {
                if (currentPage != "download") {
                    title = "Downloads"
                    currentPage = "download"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.pageContainer, DownloadFragment())
                        .commit()
                }
            }

            R.id.nav_recent -> {
                if (currentPage != "recent") {
                    title = "Daily Digest"
                    currentPage = "recent"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.pageContainer, RecentFragment())
                        .commit()
                }
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START, true)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_feedback -> {
                val uri = Uri.parse("market://details?id=$packageName");
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    startActivity(goToMarket);
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName"))
                    )
                }
            }
            R.id.menu_search -> {
                startActivity(Intent(this, SearchActivity::class.java))
            }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        unbind()
    }

    override fun onResume() {
        super.onResume()

        thread {
            Thread.sleep(1000)
            onUiThread {
                if (PlayerService.isRunning) {
                    loadingBar.visibility = View.GONE
                    quickControlView.visibility = View.VISIBLE
                    bindService(Intent(this, PlayerService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
                } else {
                    quickControlView.visibility = View.GONE
                }
            }
        }
    }
}