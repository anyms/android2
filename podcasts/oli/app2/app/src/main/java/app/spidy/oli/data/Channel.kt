package app.spidy.oli.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channel")
data class Channel(
    @PrimaryKey
    val uId: Int,
    val author: String,
    val category: String,
    val channelId: String,
    val description: String,
    val image: String,
    val rss: String,
    val title: String,
    val viewCount: Int,
    val website: String
)