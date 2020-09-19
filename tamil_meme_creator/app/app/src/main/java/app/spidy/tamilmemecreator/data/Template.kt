package app.spidy.tamilmemecreator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "template")
data class Template(
    val filename: String,
    val caption: String,
    var url: String = "",
    var thumb: String = ""
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}