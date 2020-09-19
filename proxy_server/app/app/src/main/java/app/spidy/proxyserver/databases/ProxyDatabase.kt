package app.spidy.proxyserver.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.proxyserver.data.BlockedDomain
import app.spidy.proxyserver.data.InappropDomain
import app.spidy.proxyserver.data.TrackDomain
import app.spidy.proxyserver.interfaces.ProxyDao

@Database(entities = [InappropDomain::class, TrackDomain::class, BlockedDomain::class], version = 1001, exportSchema = false)
abstract class ProxyDatabase: RoomDatabase() {
    abstract fun proxyDao(): ProxyDao
}