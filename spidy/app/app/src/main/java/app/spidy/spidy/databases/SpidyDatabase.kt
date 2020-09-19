package app.spidy.spidy.databases

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.spidy.spidy.data.Script
import app.spidy.spidy.interfaces.SpidyDao

@Database(entities = [Script::class], version = 1002, exportSchema = false)
abstract class SpidyDatabase: RoomDatabase() {
    abstract fun spidyDao(): SpidyDao

    companion object {
        val migration = object : Migration(1001, 1002) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE 'script' ADD COLUMN 'isBackgroundRunning' INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}