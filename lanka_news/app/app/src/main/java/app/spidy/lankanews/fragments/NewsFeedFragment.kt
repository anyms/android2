package app.spidy.lankanews.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.spidy.kotlinutils.*
import app.spidy.lankanews.R
import app.spidy.lankanews.adapters.NewsAdapter
import app.spidy.lankanews.data.News
import app.spidy.lankanews.utils.API
import app.spidy.lankanews.utils.isTablet
import org.jsoup.Jsoup
import java.lang.Exception

class NewsFeedFragment : Fragment() {
    private val newses = ArrayList<News>()
    private var pageNum = 1
    private var isRecyclerViewWaitingToLoadData = false
    private var isNextPageExists = true

    private lateinit var newsAdapter: NewsAdapter
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var url: String
    private lateinit var title: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_news_feed, container, false)
        title = requireArguments().getString("title")!!
        url = requireArguments().getString("url")!!
        recyclerView = v.findViewById(R.id.newsRecyclerView)
        progressBar = v.findViewById(R.id.progressBar)
        nestedScrollView = v.findViewById(R.id.nestedScrollView)

        newsAdapter = NewsAdapter(requireContext(), newses)
        recyclerView.adapter = newsAdapter
        if (requireActivity().isTablet()) {
            recyclerView.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
        } else {
            recyclerView.layoutManager = StaggeredGridLayoutManager(1, LinearLayoutManager.VERTICAL)
        }

        try {
            fetch()
        } catch (e: Exception) {
            debug(url)
            requireContext().toast("Network failed!")
        }

        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                if (!isRecyclerViewWaitingToLoadData && isNextPageExists) {
                    fetch()
                }
            }
        })

        return v
    }


    private fun fetch() {
        progressBar.visibility = View.VISIBLE
        isRecyclerViewWaitingToLoadData = true

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://www.newsfirst.lk${url}"
        }

        API.get("$url/page/$pageNum").then {
            val newses = parseNews(it.text!!)
            if (newses.isEmpty()) {
                onUiThread { requireContext().toast("No more pages") }
                isNextPageExists = false
            }
            onUiThread {
                progressBar.visibility = View.GONE
                val currentPos = this@NewsFeedFragment.newses.size
                for (news in newses) {
                    this@NewsFeedFragment.newses.add(news)
                }
                newsAdapter.notifyItemRangeInserted(currentPos, newses.size)
                pageNum += 1
            }
            isRecyclerViewWaitingToLoadData = false
        }.catch {
            onUiThread {
                progressBar.visibility = View.GONE
                isRecyclerViewWaitingToLoadData = false

                requireContext().newDialog().withTitle("Network Error!")
                    .withMessage("A network error occurred! please check your internet connection.")
                    .withCancelable(false)
                    .withPositiveButton(getString(R.string.understood)) { dialog ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }


    private fun parseNews(text: String): List<News> {
        val newses = ArrayList<News>()
        val doc = Jsoup.parse(text)
        doc.select(".panel").remove()
        doc.select(".catogery-lable").remove()
        val dateEls = doc.select(".news-lf-section .news-set-date")


        for (el in dateEls) {
            try {
                val parent = el.parent()
                val heading = parent.select("div[class*='heading']")[0].text().trim()
                var url: String = parent.select("div[class*='heading']")[0].select("a").attr("href")
                if (url.trim() == "") url = parent.select("div[class*='heading']")[0].parent().attr("href")
                val date = el.text().trim()
                val image = try {
                    parent.select("img.desk-image").attr("src")
                } catch (e: Exception) {
                    null
                }
                var isContainVideo = false
                for (img in parent.select("img")) {
                    if (img.attr("src").contains("play.png")) {
                        isContainVideo = true
                        break
                    }
                }
                newses.add(News(
                    title = heading,
                    image = image,
                    date = date,
                    isContainVideo = isContainVideo,
                    url = url
                ))
            } catch (e: Exception) {
                continue
            }
        }

        return newses
    }
}