package app.spidy.tamilmemecreator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pack")
data class Pack(
    val coverImage: String,
    val type: String,
    val path: String,
    val title: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}