package app.spidy.pirum.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import app.spidy.hiper.Hiper
import app.spidy.kookaburra.fragments.BrowserFragment
import app.spidy.kotlinutils.TinyDB
import app.spidy.pirum.BrowserListener
import app.spidy.pirum.Pyson
import app.spidy.pirum.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_downloads.*
import kotlinx.android.synthetic.main.music_exo_player_control_view.*
import java.net.Inet4Address
import java.net.NetworkInterface

class BrowserActivity : AppCompatActivity() {
    private lateinit var browserFragment: BrowserFragment
    private lateinit var tinyDB: TinyDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        tinyDB = TinyDB(this)
        val adView: AdView = findViewById(R.id.adView)
        if (!tinyDB.getBoolean("isPro")) {
            adView.loadAd(AdRequest.Builder().build())
        } else {
            adView.visibility = View.GONE
        }

        val browserListener = BrowserListener(this, intent?.getStringExtra("url")) { ds ->
        }

        browserFragment = BrowserFragment()
        browserFragment.browserListener = browserListener
        browserFragment.addOptionMenu(1, "Pyrum", getDrawable(R.drawable.logo), MenuItem.SHOW_AS_ACTION_ALWAYS) {
            val builder = AlertDialog.Builder(this)
            val v =  layoutInflater.inflate(R.layout.layout_detects_dialog, null)
            val detects = browserListener.getDetects()

            val openAsVideo: LinearLayout = v.findViewById(R.id.openAsVideo)
            val openAsMusic: LinearLayout = v.findViewById(R.id.openAsMusic)
            val openLastMusic: TextView = v.findViewById(R.id.openLastMusic)
            val openLastVideo: TextView = v.findViewById(R.id.openLastVideo)
            val musicCountView: TextView = v.findViewById(R.id.musicCountView)
            val videoCountView: TextView = v.findViewById(R.id.videoCountView)

            musicCountView.text = "${detects.size}"
            videoCountView.text = "${detects.size}"

            builder.setView(v)
            val dialog = builder.create()
            dialog.setTitle("Pyrum")
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss)) { d, _ ->
                d.dismiss()
            }

            if (detects.isNotEmpty()) {
                val json = Pyson(detects).toJson()
                val lastJson = Pyson(detects.last()).toJson()

                openAsMusic.setOnClickListener {
                    dialog.dismiss()
                    val appIntent = Intent(this, MusicPlayerActivity::class.java)
                    appIntent.putExtra("data", json)
                    appIntent.putExtra("currentTime", "0")
                    appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(appIntent)
                }
                openAsVideo.setOnClickListener {
                    dialog.dismiss()
                    val appIntent = Intent(this, VideoPlayerActivity::class.java)
                    appIntent.putExtra("data", json)
                    appIntent.putExtra("currentTime", "0")
                    appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(appIntent)
                }
                openLastMusic.setOnClickListener {
                    dialog.dismiss()
                    val appIntent = Intent(this, MusicPlayerActivity::class.java)
                    appIntent.putExtra("data", "[$lastJson]")
                    appIntent.putExtra("currentTime", "0")
                    appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(appIntent)
                }
                openLastVideo.setOnClickListener {
                    dialog.dismiss()
                    val appIntent = Intent(this, VideoPlayerActivity::class.java)
                    appIntent.putExtra("data", "[$lastJson]")
                    appIntent.putExtra("currentTime", "0")
                    appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(appIntent)
                }
            }

            dialog.show()
        }
        supportFragmentManager.beginTransaction()
            .add(R.id.browserContainer, browserFragment)
            .commit()
    }

    override fun onBackPressed() {
        if (!browserFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

    private fun getIpv4HostAddress(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress }
        }
        return "127.0.0.1"
    }
}
