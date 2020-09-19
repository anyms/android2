package app.spidy.lankanews.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.lankanews.R
import app.spidy.lankanews.activities.ReaderActivity
import app.spidy.lankanews.data.SearchNews

class SearchAdapter(
    private val context: Context,
    private val newses: List<SearchNews>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_search_news, parent, false)
        return MainHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        mainHolder.descView.text = newses[position].desc
        mainHolder.titleView.text = newses[position].title
        mainHolder.dateView.text = newses[position].date

        mainHolder.rootView.setOnClickListener {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra("url", newses[position].url)
            intent.putExtra("date", newses[position].date)
            intent.putExtra("title", newses[position].title)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = newses.size


    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val dateView: TextView = v.findViewById(R.id.dateView)
        val titleView: TextView = v.findViewById(R.id.titleView)
        val descView: TextView = v.findViewById(R.id.descView)
        val rootView: LinearLayout = v.findViewById(R.id.rootView)
    }
}