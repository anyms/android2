package app.spidy.spidy.interfaces

import androidx.room.*
import app.spidy.spidy.data.Script

@Dao
interface SpidyDao {
    /* Script */
    @Query("SELECT * FROM script ORDER BY id DESC")
    fun getScripts(): List<Script>

    @Insert
    fun putScript(script: Script)

    @Insert
    fun putScripts(scripts: List<Script>)

    @Update
    fun updateScript(script: Script)

    @Delete
    fun removeScript(script: Script)

    @Query("DELETE FROM script")
    fun clearAllScripts()
}