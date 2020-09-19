package app.spidy.pirum.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video")
data class Video(
    val uId: String,
    var status: Int,
    var playlistName: String,
    var title: String,
    val type: String,
    var src: String = "",
    var vSrc: String = "",
    var aSrc: String = "",
    var progress: Int = 0,
    var isSepAudioDownloaded: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}