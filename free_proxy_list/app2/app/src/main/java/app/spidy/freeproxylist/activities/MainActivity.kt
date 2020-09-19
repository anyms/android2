package app.spidy.freeproxylist.activities

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import app.spidy.freeproxylist.R
import app.spidy.freeproxylist.adapters.CountryAdapter
import app.spidy.freeproxylist.adapters.ProxyAdapter
import app.spidy.freeproxylist.data.Country
import app.spidy.freeproxylist.data.Proxy
import app.spidy.freeproxylist.databases.ProxyDatabase
import app.spidy.freeproxylist.utils.Ads
import app.spidy.freeproxylist.utils.ProxyFetcher
import app.spidy.kotlinutils.TimeMachine
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var database: ProxyDatabase
    private lateinit var proxyAdapter: ProxyAdapter

    private var country: Country? = null
    private val proxies = ArrayList<Proxy>()
    private var isInitialLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        isInitialLoading = true

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())
        Ads.initInterstitial(this)
        Ads.loadInterstitial()

        database = Room.databaseBuilder(this, ProxyDatabase::class.java, "ProxyDatabase")
            .fallbackToDestructiveMigration().build()

        proxyAdapter = ProxyAdapter(this, proxies)
        proxyRecyclerView.adapter = proxyAdapter
        proxyRecyclerView.layoutManager = LinearLayoutManager(this)

        val timeMachine = TimeMachine(this)
        timeMachine.schedule("proxy_cleaner").after(TimeMachine.HOUR).run {
            thread {
                database.proxyDao().clearAllCountries()
                database.proxyDao().clearAllProxies()
            }
        }


        ProxyFetcher.getCountries(object : ProxyFetcher.CountryListener {
            override fun onFail() {
                toast("Network error occurred")
            }
            override fun onSuccess(countries: List<Country>) {
                init(countries.sortedBy { it.countryName })
            }
        })
    }

    private fun showLoading() {
        proxyRecyclerView.visibility = View.GONE
        if (isInitialLoading) {
            overlayView.visibility = View.VISIBLE
            isInitialLoading = false
        } else {
            progressBar.visibility = View.VISIBLE
        }

    }

    private fun hideLoading() {
        proxyRecyclerView.visibility = View.VISIBLE
        overlayView.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun loadView() {
        proxyAdapter.notifyDataSetChanged()
        hideLoading()
    }

    private fun init(countries: List<Country>) {
        val adapter = CountryAdapter(this, countries)
        countriesSpinner.adapter = adapter
        countriesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (parent?.getItemAtPosition(position) as? Country)?.also { country ->
                    showLoading()
                    this@MainActivity.country = country
                    Ads.showInterstitial()
                    Ads.loadInterstitial()

                    this@MainActivity.proxies.clear()
                    thread {
                        val proxies = database.proxyDao().getProxies(country.countryName)

                        onUiThread {
                            if (proxies.isEmpty()) {
                                ProxyFetcher.getProxies(country.countryName, object : ProxyFetcher.ProxyListener {
                                    override fun onFail() {
                                        toast("Network error!")
                                        loadView()
                                    }
                                    override fun onSuccess(proxies: List<Proxy>) {
                                        for (proxy in proxies) {
                                            this@MainActivity.proxies.add(proxy)

                                            thread {
                                                database.proxyDao().putProxy(proxy)
                                            }
                                        }
                                        loadView()
                                    }
                                })
                            } else {
                                for (proxy in proxies) {
                                    this@MainActivity.proxies.add(proxy)
                                }
                                loadView()
                            }
                        }
                    }
                }
            }
        }

        countriesSpinner.setSelection(0)
    }
}