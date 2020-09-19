package app.spidy.oli.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.lankanews.data.News
import app.spidy.lankanews.interfaces.LankaDao


@Database(entities = [News::class], version = 1, exportSchema = false)
abstract class LankaDatabase: RoomDatabase() {
    abstract fun dao(): LankaDao
}