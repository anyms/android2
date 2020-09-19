package app.spidy.noolagam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book")
data class Book(
    @PrimaryKey
    val bookId: String,
    val category: String,
    val cover: String,
    val id: Int,
    val pageCount: Int,
    val published: String,
    val timestamp: Long,
    val title: String,
    val viewCount: Int
)