package app.spidy.spidy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "script")
data class Script(
    var name: String,
    var code: String,
    var isBackgroundRunning: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}