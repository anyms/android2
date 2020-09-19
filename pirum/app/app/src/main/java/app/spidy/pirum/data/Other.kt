package app.spidy.pirum.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "other")
data class Other(
    val uId: String,
    var playlistName: String,
    val src: String,
    var title: String,
    val type: String,
    var isToRead: Boolean = false
) {
    companion object {
        const val TYPE_IMAGE = "app.spidy.pirum.other.TYPE_IMAGE"
        const val TYPE_PAGE = "app.spidy.pirum.other.TYPE_PAGE"
    }

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}