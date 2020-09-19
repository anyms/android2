package app.spidy.memecreator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savedmemes")
data class SavedMeme(
    val uri: String,
    val type: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}