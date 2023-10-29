package chat.echo.app.views.helpers.localcontact

import android.content.Context
import androidx.room.*
import chat.echo.app.views.helpers.LocalContact

@Database(entities = [(LocalContact::class)], version = 1)
abstract class LocalContactDatabase: RoomDatabase() {

  abstract fun localContactDAO(): LocalContactDAO

  companion object {

    private var INSTANCE: LocalContactDatabase? = null

    fun getInstance(context: Context): LocalContactDatabase {
      synchronized(this) {
        var instance = INSTANCE

        if (instance == null) {
          instance = Room.databaseBuilder(
            context.applicationContext,
            LocalContactDatabase::class.java,
            "local_contact_database"
          ).fallbackToDestructiveMigration()
            .build()

          INSTANCE = instance
        }
        return instance
      }
    }
  }
}