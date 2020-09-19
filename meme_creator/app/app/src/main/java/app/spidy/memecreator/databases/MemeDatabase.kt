package app.spidy.memecreator.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.memecreator.data.Pack
import app.spidy.memecreator.data.SavedMeme
import app.spidy.memecreator.data.Template
import app.spidy.memecreator.interfaces.MemeDao

@Database(entities = [Template::class, Pack::class, SavedMeme::class], version = 1004, exportSchema = false)
abstract class MemeDatabase: RoomDatabase() {
    abstract fun memeDao(): MemeDao
}