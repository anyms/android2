package app.spidy.proxyserver.interfaces

import androidx.room.*
import app.spidy.proxyserver.data.BlockedDomain
import app.spidy.proxyserver.data.InappropDomain
import app.spidy.proxyserver.data.TrackDomain

@Dao
interface ProxyDao {

    /* InappropDomain */

    @Query("SELECT * FROM inapprop_domain ORDER BY id DESC")
    fun getInappropDomains(): List<InappropDomain>

    @Query("SELECT * FROM inapprop_domain WHERE name = :name")
    fun getInappropDomainsByName(name: String): List<InappropDomain>

    @Insert
    fun putInappropDomain(inappropDomain: InappropDomain)

    @Insert
    fun putInappropDomains(inappropDomain: List<InappropDomain>)

    @Update
    fun updateInappropDomain(inappropDomain: InappropDomain)

    @Delete
    fun removeInappropDomain(inappropDomain: InappropDomain)

    @Query("DELETE FROM inapprop_domain")
    fun clearAllInappropDomains()


    /* TrackDomain */

    @Query("SELECT * FROM track_domain ORDER BY id DESC")
    fun getTrackDomains(): List<TrackDomain>

    @Query("SELECT * FROM track_domain WHERE name = :name")
    fun getTrackDomainsByName(name: String): List<TrackDomain>

    @Insert
    fun putTrackDomain(trackDomain: TrackDomain)

    @Insert
    fun putTrackDomains(domains: List<TrackDomain>)

    @Update
    fun updateTrackDomain(trackDomain: TrackDomain)

    @Delete
    fun removeTrackDomain(trackDomain: TrackDomain)

    @Query("DELETE FROM track_domain")
    fun clearAllTrackDomains()


    /* BlockedDomain */

    @Query("SELECT * FROM blocked_domain ORDER BY id DESC")
    fun getBlockedDomains(): List<BlockedDomain>

    @Query("SELECT * FROM blocked_domain WHERE value = :value")
    fun getBlockedDomainsByName(value: String): List<BlockedDomain>

    @Insert
    fun putBlockedDomain(domain: BlockedDomain)

    @Insert
    fun putBlockedDomains(domains: List<BlockedDomain>)

    @Update
    fun updateBlockedDomain(domain: BlockedDomain)

    @Delete
    fun removeBlockedDomain(domain: BlockedDomain)

    @Query("DELETE FROM blocked_domain")
    fun clearAllBlockedDomains()

}