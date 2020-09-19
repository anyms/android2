package app.spidy.noolagam.utils

import android.content.Context
import app.spidy.noolagam.data.Book
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

object Noolagam {
    fun getBooks(context: Context): List<Book> {
        val books = ArrayList<Book>()
        val bufferedReader = BufferedReader(InputStreamReader(context.assets.open("noolagam.json"), "UTF-8"))
        var line: String?
        var jsonString = ""
        while (bufferedReader.readLine().also { line = it } != null) {
            jsonString += line
        }
        bufferedReader.close()

        val shelf = JSONArray(jsonString)
        for (i in 0 until shelf.length()) {
            val cat = shelf.getJSONObject(i)
            val category = cat.getString("c")
            val bks = cat.getJSONArray("b")
            for (j in 0 until bks.length()) {
                val bk = bks.getJSONObject(j)
                val title = bk.getString("t")
                val pageCount = bk.getInt("p")
                val label = bk.getString("l")

                books.add(Book(
                    title = title,
                    pageCount = pageCount,
                    label = label,
                    category = category
                ))
            }
        }

        return books
    }
}