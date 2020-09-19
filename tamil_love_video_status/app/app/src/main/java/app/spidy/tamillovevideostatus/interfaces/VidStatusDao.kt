package app.spidy.tamillovevideostatus.interfaces

import androidx.room.*
import app.spidy.tamillovevideostatus.data.Video

@Dao
interface VidStatusDao {
    /* Video */
    @Query("SELECT * FROM video ORDER BY uId DESC")
    fun getVideos(): List<Video>

    @Query("SELECT * FROM video WHERE uId = :id")
    fun getVideo(id: Long): List<Video>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putVideo(video: Video)

    @Insert
    fun putVideos(scripts: List<Video>)

    @Update
    fun updateVideo(video: Video)

    @Delete
    fun removeVideo(video: Video)

    @Query("DELETE FROM video")
    fun clearAllVideos()
}