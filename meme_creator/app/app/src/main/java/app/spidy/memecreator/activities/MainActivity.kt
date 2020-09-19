package app.spidy.memecreator.activities

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager.widget.ViewPager
import app.spidy.kotlinutils.ignore
import app.spidy.memecreator.BuildConfig
import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.PagerAdapter
import app.spidy.memecreator.utils.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var fab: FloatingActionButton
    private lateinit var fileChooser: FileChooser
    private lateinit var fileIO: FileIO
    private lateinit var tinyDB: TinyDB
    private lateinit var navView: NavigationView

    private var isFabOpen = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Ads.init(this)
        Ads.loadInterstitial()
        Ads.loadReward()

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        fileChooser = FileChooser(this)
        fileIO = FileIO(this)
        tinyDB = TinyDB(this)
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        fab = findViewById(R.id.fab)
        navView = findViewById(R.id.navView)

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_menu)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START, true)
        }

        tabLayout.setupWithViewPager(viewPager)
        tabLayout.addTab(tabLayout.newTab())
        tabLayout.addTab(tabLayout.newTab())
        tabLayout.addTab(tabLayout.newTab())
        val pagerAdapter = PagerAdapter(supportFragmentManager, tabLayout.tabCount, titles = listOf(
            getString(R.string.popular),
            getString(R.string.gifs),
            getString(R.string.packs)
        ))
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 3

        FirebaseMessaging.getInstance().subscribeToTopic("MemeCreatorUpdate")

        val fabOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_open)
        val fabCloseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_close)
        val fabRClockWiseAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_clockwise)
        val fabRAntiClockWiseAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_anticlockwise)
        val fabGifCreator: FloatingActionButton = findViewById(R.id.fabGifCreator)
        val fabMemeCreator: FloatingActionButton = findViewById(R.id.fabMemeCreator)
        val fabCollageCreator: FloatingActionButton = findViewById(R.id.fabCollageCreator)
        fab.setOnClickListener {
            isFabOpen = if (isFabOpen) {
                fabMemeCreator.startAnimation(fabCloseAnimation)
                fabGifCreator.startAnimation(fabCloseAnimation)
                fabCollageCreator.startAnimation(fabCloseAnimation)
                fab.startAnimation(fabRClockWiseAnimation)
                false
            } else {
                fabMemeCreator.startAnimation(fabOpenAnimation)
                fabGifCreator.startAnimation(fabOpenAnimation)
                fabCollageCreator.startAnimation(fabOpenAnimation)
                fab.startAnimation(fabRAntiClockWiseAnimation)
                true
            }
        }

        fabCollageCreator.setOnClickListener {
            showNewMemeDialog()
        }
        fabMemeCreator.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            startActivity(intent)
        }
        fabGifCreator.setOnClickListener {
            fileChooser.choose(FileChooser.RESULT_GIF_CHOOSER)
        }
        navView.setNavigationItemSelectedListener(this)
        onNavigationItemSelected(navView.menu.getItem(0).setChecked(true))
    }

    private fun showNewMemeDialog(): AlertDialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        val viewGroup: ViewGroup? = null
        val view = LayoutInflater.from(this).inflate(R.layout.layout_new_meme_dialog, viewGroup, false)

        val collage1: ImageView = view.findViewById(R.id.collage1)
        val collage2: ImageView = view.findViewById(R.id.collage2)
        val collage3: ImageView = view.findViewById(R.id.collage3)
        val collage4: ImageView = view.findViewById(R.id.collage4)
        val collage5: ImageView = view.findViewById(R.id.collage5)

        builder.setView(view)
        val dialog = builder.create()
        dialog.show()

        collage1.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, CollageActivity::class.java)
            intent.putExtra("layout", 1)
            startActivity(intent)
        }
        collage2.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, CollageActivity::class.java)
            intent.putExtra("layout", 2)
            startActivity(intent)
        }
        collage3.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, CollageActivity::class.java)
            intent.putExtra("layout", 3)
            startActivity(intent)
        }
        collage4.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, CollageActivity::class.java)
            intent.putExtra("layout", 4)
            startActivity(intent)
        }
        collage5.setOnClickListener {
            val intent = Intent(this, CollageActivity::class.java)
            intent.putExtra("layout", 5)
            startActivity(intent)
        }

        return dialog
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuStore -> {
                startActivity(Intent(this, StoreActivity::class.java))
                true
            }
            R.id.menuFeedback -> {
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
                true
            }
            R.id.menuSearch -> {
                startActivity(Intent(this, SearchActivity::class.java))
                return true
            }
            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == FileChooser.RESULT_GIF_CHOOSER) {
                data?.data?.also {
                    val dialog = showLoading(getString(R.string.decoding_your_meme))
                    val bytes = fileChooser.read(it)
                    fileIO.deleteFile("__data__", "giffy.gif")
                    fileIO.saveBytes("__data__", "giffy.gif", bytes)
                    val uri = fileIO.getUri("__data__", "giffy.gif")

                    fileIO.saveGif(uri) { frames ->
                        dialog.dismiss()
                        tinyDB.putListString("frame_names", frames)
                        val intent = Intent(this, GifEditorActivity::class.java)
                        intent.putExtra("isLocal", true)
                        startActivity(intent)
                    }
                }
            }
        }
        return super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuHome -> {}
            R.id.menuYourGifs -> {
                startActivity(Intent(this, YourGifActivity::class.java))
            }
            R.id.menuYourMemes -> {
                startActivity(Intent(this, YourMemeActivity::class.java))
            }
            R.id.menuFeedback -> {
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
            R.id.menuShare -> {
                ignore {
                    val shareIntent = Intent(Intent.ACTION_SEND);
                    shareIntent.type = "text/plain";
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "GIF and image meme creator with templates.");
                    var shareMessage = "\nShow your humor with images and GIFs\n\n";
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID +"\n\n";
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "Share with"));
                }
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
