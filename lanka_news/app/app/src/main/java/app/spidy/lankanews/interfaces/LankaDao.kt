package app.spidy.lankanews.interfaces

import androidx.room.*
import app.spidy.lankanews.data.News

@Dao
interface LankaDao {

    @Query("SELECT * FROM news")
    fun getNewses(): List<News>

    @Query("SELECT * FROM news WHERE url = :url")
    fun getNews(url: String): List<News>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putNews(news: News)

    @Delete
    fun removeNews(news: News)

    @Query("DELETE FROM news")
    fun clearAllEpisodes()
}