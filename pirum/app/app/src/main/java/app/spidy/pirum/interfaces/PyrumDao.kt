package app.spidy.pirum.interfaces

import androidx.room.*
import app.spidy.pirum.data.Music
import app.spidy.pirum.data.Other
import app.spidy.pirum.data.Video
import app.spidy.pirum.utils.DownloadStatus

@Dao
interface PyrumDao {

    /* Music */

    @Query("SELECT * FROM music ORDER BY id DESC")
    fun getMusic(): List<Music>

    @Query("SELECT * FROM music WHERE uId = :uId")
    fun getMusicById(uId: String): List<Music>

    @Query("SELECT * FROM music WHERE playlistName = :playlistName ORDER BY id DESC")
    fun getMusicByPlaylist(playlistName: String): List<Music>

    @Query("SELECT * FROM music WHERE playlistName = :playlistName AND status = :status ORDER BY id DESC")
    fun getMusicDownloadsByPlaylist(playlistName: String, status: Int = DownloadStatus.STATE_COMPLETED): List<Music>

    @Insert
    fun putMusic(music: Music)

    @Update
    fun updateMusic(music: Music)

    @Delete
    fun removeMusic(music: Music)

    @Query("DELETE FROM music")
    fun clearAllMusic()


    /* Video */

    @Query("SELECT * FROM video ORDER BY id DESC")
    fun getVideos(): List<Video>

    @Query("SELECT * FROM video WHERE uId = :uId")
    fun getVideoById(uId: String): List<Video>

    @Query("SELECT * FROM video WHERE playlistName = :playlistName ORDER BY id DESC")
    fun getVideoByPlaylist(playlistName: String): List<Video>

    @Query("SELECT * FROM video WHERE playlistName = :playlistName AND status = :status ORDER BY id DESC")
    fun getVideoDownloadsByPlaylist(playlistName: String, status: Int = DownloadStatus.STATE_COMPLETED): List<Video>

    @Insert
    fun putVideo(video: Video)

    @Update
    fun updateVideo(video: Video)

    @Delete
    fun removeVideo(video: Video)

    @Query("DELETE FROM video")
    fun clearAllVideo()


    /* Image */
    @Query("SELECT * FROM other ORDER BY id DESC")
    fun getOthers(): List<Other>

    @Query("SELECT * FROM other WHERE uId = :uId")
    fun getOtherById(uId: String): Other

    @Query("SELECT * FROM other WHERE title = :title")
    fun getOtherByTitle(title: String): List<Other>

    @Query("SELECT * FROM other WHERE playlistName = :playlistName ORDER BY id DESC")
    fun getOtherByPlaylist(playlistName: String): List<Other>

    @Insert
    fun putOther(other: Other)

    @Update
    fun updateOther(other: Other)

    @Delete
    fun removeOther(other: Other)

    @Query("DELETE FROM other")
    fun clearAllOther()
}