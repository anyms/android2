package app.spidy.tamillovevideostatus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video")
data class Video(
    @PrimaryKey
    val uId: Long,
    val title: String,
    var videoId: String,
    val tags: String,
    val viewCount: Int,
    val downloadCount: Int,
    val shareCount: Int,
    val isExpired: Boolean,
    val category: String,
    val thumb: String,
    val expire: Long,
    var data: String = ""
)