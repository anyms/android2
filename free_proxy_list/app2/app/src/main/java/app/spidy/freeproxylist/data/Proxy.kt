package app.spidy.freeproxylist.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "proxy")
data class Proxy(
    val ip: String,
    val port: String,
    val countryCode: String,
    val countryName: String,
    val anonymity: String,
    val googlePassed: Boolean,
    val sslSupport: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}