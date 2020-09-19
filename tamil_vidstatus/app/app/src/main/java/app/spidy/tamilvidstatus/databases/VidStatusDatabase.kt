package app.spidy.tamilvidstatus.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.tamilvidstatus.data.Video
import app.spidy.tamilvidstatus.interfaces.VidStatusDao

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