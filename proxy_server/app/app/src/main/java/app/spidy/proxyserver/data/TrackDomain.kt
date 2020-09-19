package app.spidy.proxyserver.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_domain")
data class TrackDomain(
    val name: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}