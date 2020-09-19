package app.spidy.pirum.activities

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.toast
import app.spidy.pirum.R
import app.spidy.pirum.adapters.PagerAdapter
import app.spidy.pirum.services.Server
import app.spidy.pirum.utils.Ads
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, BillingProcessor.IBillingHandler {
    companion object {
        const val OVERLAY_PERMISSION_CODE = 121
    }

    private lateinit var billingProcessor: BillingProcessor
    private lateinit var tinyDB: TinyDB

    fun purchase() {
        billingProcessor.purchase(this, "app.spidy.pirum")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tinyDB = TinyDB(this)
        tinyDB.putBoolean(IntroActivity.IS_SHOWN, true)

        val adView: AdView = findViewById(R.id.adView)
        if (!tinyDB.getBoolean("isPro")) {
            Ads.initInterstitial(this)
            Ads.initReward(this)
            Ads.loadInterstitial()
            Ads.loadReward()
            adView.loadAd(AdRequest.Builder().build())
        } else {
            adView.visibility = View.GONE
        }

//        billingProcessor = BillingProcessor(this, null, this)
//        billingProcessor.initialize()

        billingProcessor = BillingProcessor(this,
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkqz95QM+5Yd4Vwd/kjQGJJjp4Molpa3DJ9eLd4heQIORZBsFTC5yCdDYbhgD3dQzzxUuWguEWTrUbHRdSSVIqEXMQkez51WcCLOONHcK1PLjF5euELIJOTe4hWHCg9cFpeb17Nu5L0Bub2T+5lAaZh8zvTUVdMT31YyDFtWdKoW90ab1uJcc7eBXiZpsHxksU4MQHpDnpCoSG5it1kVrKdNd7W6QjIDO4LnLxsDctI2d8ebmgR1UgpwqWaWLUoE5YWmcVgLhYh67yW3wFAdloQSjlHhmc79JaBCbJrxgZhps5iPVulAPpx7lpZhK22eAodib4mFrHTeHECaP0gUTbwIDAQAB",
            this)
        billingProcessor.initialize()
//        billingProcessor.purchase(this, "app.spidy.pirum")

        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorWhite))
        val drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)
        toolbar.setNavigationIcon(R.drawable.ic_menu)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
//            purchase()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Require Permission")
                builder.setCancelable(false)

                builder.setMessage("We require draw over other app permission to run in the background")
                val dialog = builder.create()
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Grant") { d, _ ->
                    d.dismiss()
                    if (!Settings.canDrawOverlays(this)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivityForResult(intent, OVERLAY_PERMISSION_CODE)
                    }
                }
                dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Not now") { d, _ ->
                    d.dismiss()
                }
                dialog.show()
            }
        }

        serverSwitch.isChecked = Server.isRunning
        if (!Server.isRunning) {
            serverSwitch.isChecked = true
            startService(Intent(this, Server::class.java))
        }

        serverSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                startService(Intent(this, Server::class.java))
            } else {
                Server.kill?.invoke()
            }
        }

        tabLayout.setupWithViewPager(viewPager)
        tabLayout.addTab(tabLayout.newTab())
        tabLayout.addTab(tabLayout.newTab())
        tabLayout.addTab(tabLayout.newTab())
        tabLayout.addTab(tabLayout.newTab())

        val pagerAdapter = PagerAdapter(supportFragmentManager, tabLayout.tabCount, titles = listOf(
            getString(R.string.explorer),
            getString(R.string.music),
            getString(R.string.video),
            getString(R.string.others)
        ))
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 5
        tabLayout.getTabAt(0)?.setIcon(R.drawable.ic_explorer)
        tabLayout.getTabAt(1)?.setIcon(R.drawable.img_music)
        tabLayout.getTabAt(2)?.setIcon(R.drawable.img_video)
        tabLayout.getTabAt(3)?.setIcon(R.drawable.ic_others)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        when(item.itemId) {
            R.id.menuDownloads -> {
                val intent = Intent(this, DownloadsActivity::class.java)
                startActivity(intent)
            }
            R.id.menuShare -> {
                ignore {
                    val shareIntent = Intent(Intent.ACTION_SEND);
                    shareIntent.type = "text/plain";
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Do more with your smartphone");
                    var shareMessage = "\nSend videos, audios, images and links from your desktop browser to your smartphone\n\n";
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + packageName +"\n\n";
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "Share with"));
                }
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
            R.id.menuRequestFeature -> {
                try {
                    val emailBuilder = StringBuilder("mailto:" + Uri.encode("mytellee@gmail.com"))
                    val operator = '?'
                    emailBuilder.append(operator + "subject=" + Uri.encode("Request Feature"));
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(emailBuilder.toString()))
                    startActivity(intent)
                } catch (e: Exception) {
                    toast("Unable to find an email client.")
                }
            }
            R.id.menuReportBugs -> {
                try {
                    val emailBuilder = StringBuilder("mailto:" + Uri.encode("mytellee@gmail.com"))
                    val operator = '?'
                    emailBuilder.append(operator + "subject=" + Uri.encode("Bug Report"));
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(emailBuilder.toString()))
                    startActivity(intent)
                } catch (e: Exception) {
                    toast("Unable to find an email client.")
                }
            }
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.browser -> {
                val intent = Intent(this, BrowserActivity::class.java)
                startActivity(intent)
            }
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        billingProcessor.release()
        super.onDestroy()
    }

    override fun onBillingInitialized() {

    }

    override fun onPurchaseHistoryRestored() {

    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        tinyDB.putBoolean("isPro", true)
        toast("Great! you've purchased pro version, restart the app to enable the changes.")
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        toast("billing failed")
    }
}
