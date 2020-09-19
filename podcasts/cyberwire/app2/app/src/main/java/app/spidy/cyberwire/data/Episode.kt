package app.spidy.cyberwire.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episode")
data class Episode(
    @PrimaryKey
    val uId: Int,
    val title: String,
    val audio: String,
    val channelId: String,
    val date: String,
    val timestamp: Long,
    val viewCount: Int,
    var downloadedLocation: String,
    var coverImage: String = ""
)