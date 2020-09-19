package app.spidy.noolagam.fragments

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.ProgressBar
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.noolagam.R
import app.spidy.noolagam.activities.ReaderActivity
import app.spidy.noolagam.adapters.PageAdapter
import app.spidy.noolagam.data.Line
import app.spidy.noolagam.data.Page
import app.spidy.noolagam.utils.API
import app.spidy.noolagam.utils.C
import org.json.JSONArray
import org.json.JSONObject

class PageFragment : Fragment() {
    private lateinit var pageRecyclerView: RecyclerView
    private lateinit var pageAdapter: PageAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tinyDB: TinyDB
    private lateinit var nestedScrollView: NestedScrollView

    private val lines = ArrayList<Line>()
    private val textSizes = arrayOf("0.5x", "1x", "1.5x", "2x", "2.5x", "3x")
    private val textSizeValues = arrayOf(.5f, 1f, 1.5f, 2f, 2.5f, 3f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_page, container, false)

        nestedScrollView = v.findViewById(R.id.nestedScrollView)
        pageAdapter = PageAdapter(requireContext(), lines)
        pageRecyclerView = v.findViewById(R.id.pageRecyclerView)
        pageRecyclerView.adapter = pageAdapter
        pageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        progressBar = v.findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        tinyDB = TinyDB(requireContext())
        val pageNum = requireArguments().getInt("page_num")
        val bookId = requireArguments().getString("book_id")!!

        API.get("/page/$bookId/$pageNum").then {
            val data = JSONObject(it.text!!)
            val page = Page(
                bookId = data.getString("book_id"),
                id = data.getInt("id"),
                pageNum = data.getInt("page_num"),
                content = data.getJSONArray("content").toString()
            )
            onUiThread { loadPage(page) }
        }.catch {
            val page = Page(
                bookId = bookId,
                id = 0,
                pageNum = pageNum,
                content = JSONArray("""
                    [{"type": "para", "value": "Network failed"}]
                """.trimIndent()).toString()
            )
            onUiThread { loadPage(page) }
        }

        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > oldScrollY) {
                (activity as? ReaderActivity)?.pageNumView?.visibility = View.INVISIBLE
            } else {
                (activity as? ReaderActivity)?.pageNumView?.visibility = View.VISIBLE
            }
        })

        return v
    }

    private fun loadPage(page: Page) {
        val o = JSONArray(page.content)

        for (i in 0 until o.length()) {
            val line = o.getJSONObject(i)
            lines.add(Line(type = line.getString("type"), value = line.getString("value")))
        }

        progressBar.visibility = View.GONE
        val savedTextSize = tinyDB.getInt(C.TAG_READER_TEXT_SIZE, -1)
        if (savedTextSize != -1 && savedTextSize != 1) {
            pageAdapter.updateTextSize(textSizeValues[savedTextSize])
        }
        pageAdapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        val savedTextSize = tinyDB.getInt(C.TAG_READER_TEXT_SIZE, -1)
        if (savedTextSize != -1 && savedTextSize != 1) {
            pageAdapter.updateTextSize(textSizeValues[savedTextSize])
            pageAdapter.notifyDataSetChanged()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuTextSize -> {
                val v = LayoutInflater.from(requireContext()).inflate(R.layout.layout_text_size_dialog, null)
                val picker: NumberPicker = v.findViewById(R.id.picker)
                val savedTextSize = tinyDB.getInt(C.TAG_READER_TEXT_SIZE, -1)
                picker.minValue = 0
                picker.maxValue = textSizes.size - 1
                picker.value = if (savedTextSize != -1) savedTextSize else 1
                picker.displayedValues = textSizes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    picker.selectionDividerHeight = 0
                }
                requireContext().newDialog().withCustomView(v)
                    .withNegativeButton(getString(R.string.cancel)) { dialog ->
                        dialog.dismiss()
                    }
                    .withPositiveButton(getString(R.string.apply)) { dialog ->
                        if (tinyDB.getInt(C.TAG_READER_TEXT_SIZE, -1) != picker.value) {
                            pageAdapter.updateTextSize(textSizeValues[picker.value])
                            pageAdapter.notifyDataSetChanged()
                            tinyDB.putInt(C.TAG_READER_TEXT_SIZE, picker.value)
                        }
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                return true
            }
        }

        return false
    }
}