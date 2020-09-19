package app.spidy.proxyserver.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_domain")
data class BlockedDomain(
    var value: String,
    val isPattern: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}