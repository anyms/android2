package app.spidy.noolagam.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.noolagam.R
import app.spidy.noolagam.activities.ReaderActivity
import app.spidy.noolagam.data.Book
import com.bumptech.glide.Glide

class BookAdapter(
    private val context: Context,
    private val books: List<Book>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_book, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = books.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder

        mainHolder.titleView.text = books[position].title
        mainHolder.publishedView.text = books[position].published
        Glide.with(context).load(books[position].cover).into(mainHolder.coverImageView)

        mainHolder.rootView.setOnClickListener {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra("book_id", books[position].bookId)
            intent.putExtra("category", books[position].category)
            intent.putExtra("cover", books[position].cover)
            intent.putExtra("id", books[position].id)
            intent.putExtra("page_count", books[position].pageCount)
            intent.putExtra("published", books[position].published)
            intent.putExtra("timestamp", books[position].timestamp)
            intent.putExtra("title", books[position].title)
            intent.putExtra("view_count", books[position].viewCount)
            context.startActivity(intent)
        }
    }

    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val coverImageView: ImageView = v.findViewById(R.id.coverImageView)
        val titleView: TextView = v.findViewById(R.id.titleView)
        val publishedView: TextView = v.findViewById(R.id.publishedView)
        val rootView: CardView = v.findViewById(R.id.rootView)
    }
}