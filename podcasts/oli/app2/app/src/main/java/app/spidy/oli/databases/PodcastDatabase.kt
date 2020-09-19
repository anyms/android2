package app.spidy.oli.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.oli.data.Episode
import app.spidy.oli.interfaces.PodcastDao


@Database(entities = [Episode::class], version = 1002, exportSchema = false)
abstract class PodcastDatabase: RoomDatabase() {
    abstract fun dao(): PodcastDao
}