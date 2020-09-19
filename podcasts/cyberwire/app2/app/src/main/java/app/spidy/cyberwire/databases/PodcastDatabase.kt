package app.spidy.cyberwire.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.cyberwire.data.Episode
import app.spidy.cyberwire.interfaces.PodcastDao


@Database(entities = [Episode::class], version = 1002, exportSchema = false)
abstract class PodcastDatabase: RoomDatabase() {
    abstract fun dao(): PodcastDao
}