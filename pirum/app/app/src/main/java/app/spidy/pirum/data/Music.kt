package app.spidy.pirum.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music")
data class Music(
    val uId: String,
    var status: Int,
    var playlistName: String,
    var title: String,
    val type: String,
    val src: String,
    var progress: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}