package app.spidy.kookaburra.interfaces

import androidx.room.*
import app.spidy.freeproxylist.data.Country
import app.spidy.freeproxylist.data.Proxy

@Dao
interface ProxyDao {

    /* Tab */

    @Query("SELECT * FROM proxy")
    fun getProxies(): List<Proxy>

    @Query("SELECT * FROM proxy WHERE countryName = :countryName")
    fun getProxies(countryName: String): List<Proxy>

    @Insert
    fun putProxy(tab: Proxy)

    @Update
    fun updateProxy(tab: Proxy)

    @Delete
    fun removeProxy(tab: Proxy)

    @Query("DELETE FROM proxy")
    fun clearAllProxies()


    /* Bookmark */

    @Query("SELECT * FROM country")
    fun getCountries(): List<Country>

    @Insert
    fun putCountry(bookmark: Country)

    @Update
    fun updateCountry(bookmark: Country)

    @Delete
    fun removeCountry(bookmark: Country)

    @Query("DELETE FROM country")
    fun clearAllCountries()
}