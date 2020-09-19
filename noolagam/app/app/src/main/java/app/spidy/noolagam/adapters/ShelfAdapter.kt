package app.spidy.noolagam.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.debug
import app.spidy.noolagam.R
import app.spidy.noolagam.activities.ReaderActivity
import app.spidy.noolagam.data.Book
import com.bumptech.glide.Glide

class ShelfAdapter(
    private val context: Context,
    private val books: List<Book>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_book_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = books.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        Glide.with(context).load("https://online.pubhtml5.com/${books[position].label}/files/shot.jpg")
            .into(mainHolder.bookCoverView)
        mainHolder.bookTitleView.text = books[position].title

        mainHolder.rootView.setOnClickListener {
            val intent = Intent(context, ReaderActivity::class.java)
            debug(books[position])
            intent.putExtra("label", books[position].label)
            context.startActivity(intent)
        }
    }


    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val bookCoverView: ImageView = v.findViewById(R.id.bookCoverView)
        val bookTitleView: TextView = v.findViewById(R.id.bookTitleView)
        val rootView: CardView = v.findViewById(R.id.rootView)
    }
}