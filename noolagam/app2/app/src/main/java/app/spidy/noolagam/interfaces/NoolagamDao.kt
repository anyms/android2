package app.spidy.noolagam.interfaces

import androidx.room.*
import app.spidy.noolagam.data.Book

@Dao
interface NoolagamDao {

    /* Book */

    @Query("SELECT * FROM book")
    fun getBooks(): List<Book>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putBook(book: Book)

    @Update
    fun updateBook(book: Book)

    @Delete
    fun removeBook(book: Book)

    @Query("DELETE FROM book")
    fun clearAllEpisodes()
}