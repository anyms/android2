package app.spidy.pirum.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import app.spidy.kotlinutils.TinyDB
import app.spidy.pirum.R
import app.spidy.pirum.adapters.IntroPagerAdapter
import app.spidy.pirum.data.Tab
import app.spidy.pirum.fragments.IntroPageFragment
import kotlinx.android.synthetic.main.activity_intro.*

class IntroActivity : AppCompatActivity() {
    companion object {
        const val IS_SHOWN = "app.spidy.ghost.IS_SHOWN"
    }

    private val tabs = ArrayList<Tab>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val tinyDB = TinyDB(applicationContext)

        if (tinyDB.getBoolean(IS_SHOWN)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            tabLayout.setupWithViewPager(viewPager)
            addTab(IntroPageFragment.getInstance(1))
            addTab(IntroPageFragment.getInstance(2))
            addTab(IntroPageFragment.getInstance(3))

            val pagerAdapter = IntroPagerAdapter(supportFragmentManager, tabs)
            viewPager.adapter = pagerAdapter

            nextButton.setOnClickListener {
                if (tabLayout.selectedTabPosition > tabs.size - 2) {
                    nextButton.text = getString(R.string.start)
                } else {
                    nextButton.text = getString(R.string.next)
                }
                if (tabLayout.selectedTabPosition == tabs.size - 1) {
                    startActivity(Intent(this, MainActivity::class.java))
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