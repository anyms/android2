package app.spidy.memecreator.interfaces

import androidx.room.*
import app.spidy.memecreator.data.Pack
import app.spidy.memecreator.data.SavedMeme
import app.spidy.memecreator.data.Template


@Dao
interface MemeDao {

    /* Template */

    @Query("SELECT * FROM template")
    fun getTemplates(): List<Template>

//    @Query("SELECT * FROM template WHERE packName = :packName")
//    fun getTemplate(packName: String): List<Template>
//
//    @Query("SELECT * FROM template WHERE packName = :packName ORDER BY id LIMIT :limit OFFSET :start")
//    fun getTemplate(packName: String, start: Int, limit: Int): List<Template>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putTemplate(template: Template)

    @Update
    fun updateTemplate(template: Template)

    @Delete
    fun removeTemplate(template: Template)

    @Query("DELETE FROM template")
    fun clearAllTemplates()

    @Query("SELECT * FROM template WHERE caption LIKE '%' || :query || '%'")
    fun searchTemplates(query: String): List<Template>


    /* Pack */

    @Query("SELECT * FROM pack")
    fun getPacks(): List<Pack>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putPack(pack: Pack)

    @Update
    fun updatePack(pack: Pack)

    @Delete
    fun removePack(pack: Pack)

    @Query("DELETE FROM pack WHERE title = :title")
    fun removePack(title: String)

    @Query("DELETE FROM pack")
    fun clearAllPacks()

    @Query("SELECT * FROM pack WHERE title LIKE '%' || :query || '%'")
    fun searchPacks(query: String): List<Pack>

    /* SavedMeme */
    @Query("SELECT * FROM savedmemes WHERE type = :type")
    fun getSavedMemes(type: String = "meme"): List<SavedMeme>

    @Query("SELECT * FROM savedmemes WHERE type = :type")
    fun getSavedGifs(type: String = "gif"): List<SavedMeme>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putSavedMeme(savedMeme: SavedMeme)

    @Delete
    fun removeSavedMeme(savedMeme: SavedMeme)

    @Query("DELETE FROM savedmemes WHERE uri = :uri")
    fun removeSavedMeme(uri: String)

}