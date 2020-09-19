package app.spidy.noolagam.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.noolagam.data.Book
import app.spidy.noolagam.interfaces.NoolagamDao

@Database(entities = [Book::class], version = 1, exportSchema = false)
abstract class NoolagamDatabase: RoomDatabase() {
    abstract fun dao(): NoolagamDao
}
