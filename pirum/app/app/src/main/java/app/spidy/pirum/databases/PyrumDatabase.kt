package app.spidy.pirum.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.pirum.data.Music
import app.spidy.pirum.data.Other
import app.spidy.pirum.data.Video
import app.spidy.pirum.interfaces.PyrumDao

@Database(entities = [Music::class, Video::class, Other::class], version = 1001, exportSchema = false)
abstract class PyrumDatabase: RoomDatabase() {
    abstract fun pyrumDao(): PyrumDao
}