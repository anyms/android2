package app.spidy.freeproxylist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "country")
data class Country(
    val countryName: String,
    val countryCode: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}