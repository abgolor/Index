package chat.echo.app.views.helpers.localcontact

import androidx.lifecycle.LiveData
import androidx.room.*
import chat.echo.app.views.helpers.LocalContact

@Dao
interface LocalContactDAO {

  @Insert
  fun createDirectLocalContact(directLocalContact: LocalContact)

  @Update
   fun updateLocalContact(directLocalContact: LocalContact)

  @Query("SELECT * FROM LocalContact WHERE id = :userId OR id = :apiId")
  fun findLocalContact(userId: String, apiId: String): List<LocalContact>

  @Query("SELECT * FROM LocalContact WHERE id = :apiID ")
  fun getLocalContact(apiID: String): List<LocalContact>

  @Query("DELETE FROM LocalContact WHERE id = :apiId")
  fun deleteLocalContact(apiId: String)
}