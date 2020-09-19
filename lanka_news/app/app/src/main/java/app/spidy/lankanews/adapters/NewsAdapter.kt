package app.spidy.lankanews.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.lankanews.R
import app.spidy.lankanews.activities.ReaderActivity
import app.spidy.lankanews.data.News
import com.bumptech.glide.Glide

class NewsAdapter(
    private val context: Context,
    private val newses: List<News>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_news, parent, false)
        return NewsHolder(v)
    }

    override fun getItemCount(): Int = newses.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val newsHolder = holder as NewsHolder

        newsHolder.titleView.text = newses[position].title
        newsHolder.publishedView.text = newses[position].date

        if (newses[position].image == null) {
            newsHolder.coverImageView.visibility = View.GONE
        } else {
            newsHolder.coverImageView.visibility = View.VISIBLE
            Glide.with(context).load(newses[position].image).into(newsHolder.coverImageView)
        }

        if (newses[position].isContainVideo) {
            newsHolder.playImageView.visibility = View.VISIBLE
        } else {
            newsHolder.playImageView.visibility = View.GONE
        }

        newsHolder.rootView.setOnClickListener {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra("url", newses[position].url)
            intent.putExtra("image", newses[position].image)
            intent.putExtra("date", newses[position].date)
            intent.putExtra("title", newses[position].title)
            context.startActivity(intent)
        }
    }


    inner class NewsHolder(v: View): RecyclerView.ViewHolder(v) {
        val coverImageView: ImageView = v.findViewById(R.id.coverImageView)
        val titleView: TextView = v.findViewById(R.id.titleView)
        val publishedView: TextView = v.findViewById(R.id.publishedView)
        val playImageView: ImageView = v.findViewById(R.id.playImageView)
        val rootView: CardView = v.findViewById(R.id.rootView)
    }
}