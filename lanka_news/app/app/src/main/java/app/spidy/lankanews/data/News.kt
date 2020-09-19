package app.spidy.lankanews.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news")
data class News(
    val title: String,
    val image: String?,
    val date: String,
    val isContainVideo: Boolean,
    @PrimaryKey
    val url: String
)