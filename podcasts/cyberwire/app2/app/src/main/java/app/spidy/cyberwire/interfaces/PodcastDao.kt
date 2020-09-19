package app.spidy.cyberwire.interfaces

import androidx.room.*
import app.spidy.cyberwire.data.Episode

@Dao
interface PodcastDao {

    /* Episode */

    @Query("SELECT * FROM episode")
    fun getEpisodes(): List<Episode>

    @Query("SELECT * FROM episode WHERE channelId = :channelId")
    fun getEpisodes(channelId: String): List<Episode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putEpisode(episode: Episode)

    @Update
    fun updateEpisode(episode: Episode)

    @Delete
    fun removeEpisode(episode: Episode)

    @Query("DELETE FROM episode")
    fun clearAllEpisodes()

    @Query("SELECT * FROM episode WHERE title LIKE '%' || :query || '%'")
    fun search(query: String): List<Episode>
}