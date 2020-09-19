package app.spidy.tamilmemecreator.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.tamilmemecreator.data.Pack
import app.spidy.tamilmemecreator.data.SavedMeme
import app.spidy.tamilmemecreator.data.Template
import app.spidy.tamilmemecreator.interfaces.MemeDao

@Database(entities = [Template::class, Pack::class, SavedMeme::class], version = 1, exportSchema = false)
abstract class MemeDatabase: RoomDatabase() {
    abstract fun memeDao(): MemeDao
}