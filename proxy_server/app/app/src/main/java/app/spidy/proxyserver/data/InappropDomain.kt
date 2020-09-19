package app.spidy.proxyserver.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inapprop_domain")
data class InappropDomain(
    val name: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}