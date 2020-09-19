package app.spidy.freeproxylist.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import app.spidy.freeproxylist.data.Country
import app.spidy.freeproxylist.data.Proxy
import app.spidy.kookaburra.interfaces.ProxyDao


@Database(entities = [Proxy::class, Country::class], version = 1000, exportSchema = false)
abstract class ProxyDatabase: RoomDatabase() {
    abstract fun proxyDao(): ProxyDao
}