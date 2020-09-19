package app.spidy.ghost.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import app.spidy.ghost.fragments.AppExplainFragment
import app.spidy.ghost.adapters.PagerAdapter
import app.spidy.ghost.R
import app.spidy.ghost.data.Tab
import app.spidy.ghost.fragments.AppRecommendFragment
import app.spidy.ghost.utils.TinyDB
import com.google.android.material.tabs.TabLayout

class IntroActivity : AppCompatActivity() {
    companion object {
        const val IS_SHOWN = "app.spidy.ghost.IS_SHOWN"
    }

    private lateinit var tabLayout: TabLayout
    private lateinit var nextButton: TextView
    private lateinit var viewPager: ViewPager

    private val tabs = ArrayList<Tab>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val tinyDB = TinyDB(applicationContext)

        if (tinyDB.getBoolean(IS_SHOWN)) {
            startActivity(Intent(this, LoadingActivity::class.java))
            finish()
        } else {
            tabLayout = findViewById(R.id.tabLayout)
            viewPager = findViewById(R.id.viewPager)
            nextButton = findViewById(R.id.nextButton)

            tabLayout.setupWithViewPager(viewPager)
            addTab(AppExplainFragment())
            addTab(AppRecommendFragment())

            val pagerAdapter =
                PagerAdapter(supportFragmentManager, tabs)
            viewPager.adapter = pagerAdapter

            nextButton.setOnClickListener {
                if (tabLayout.selectedTabPosition > tabs.size - 2) {
                    nextButton.text = getString(R.string.start)
                } else {
                    nextButton.text = getString(R.string.next)
                }
                if (tabLayout.selectedTabPosition == tabs.size - 1) {
                    startActivity(Intent(this, LoadingActivity::class.java))
                    finish()
                } else {
                    tabLayout.selectTab(tabLayout.getTabAt(tabLayout.selectedTabPosition + 1))
                }
            }

            viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    if (tabLayout.selectedTabPosition > tabs.size - 2) {
                        nextButton.text = getString(R.string.start)
                    } else {
                        nextButton.text = getString(R.string.next)
                    }
                }
            })
        }
    }

    private fun addTab(fragment: Fragment) {
        tabLayout.addTab(tabLayout.newTab())
        tabs.add(
            Tab(
                title = "",
                fragment = fragment
            )
        )
    }
}
