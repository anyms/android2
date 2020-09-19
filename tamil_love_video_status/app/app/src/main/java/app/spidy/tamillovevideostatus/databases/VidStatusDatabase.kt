package app.spidy.tamillovevideostatus.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.tamillovevideostatus.data.Video
import app.spidy.tamillovevideostatus.interfaces.VidStatusDao

@Database(entities = [Video::class], version = 3, exportSchema = false)
abstract class VidStatusDatabase: RoomDatabase() {
    abstract fun dao(): VidStatusDao

//    companion object {
//        val migration = object : Migration(1001, 1002) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("ALTER TABLE 'script' ADD COLUMN 'isBackgroundRunning' INTEGER NOT NULL DEFAULT 0")
//            }
//        }
//    }
}