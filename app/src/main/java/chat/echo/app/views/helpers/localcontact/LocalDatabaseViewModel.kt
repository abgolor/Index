package chat.echo.app.views.helpers.localcontact

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import chat.echo.app.views.helpers.LocalContact

class LocalDatabaseViewModel (application: Application) : ViewModel(){

  val searchResults: MutableLiveData<List<LocalContact>>
  val localContact: MutableLiveData<List<LocalContact>>
  private val repository: LocalContactRepository


  init {
    val contactDB = LocalContactDatabase.getInstance(application)
    val contactDAO = contactDB.localContactDAO()
    repository = LocalContactRepository(contactDAO)

    searchResults = repository.searchResults
    localContact = repository.localContact
  }

  fun createLocalContact(contact: LocalContact){
    repository.createLocalContact(contact)
  }

  fun updateLocalContact(contact: LocalContact){
    repository.updateLocalContact(contact)
  }

  fun findLocalContact(userId: String, contactId: String){
    repository.findLocalContact(userId, contactId)
  }

  fun processLocalContact(userId: String, contactId: String, action: (localContacts: List<LocalContact>) -> Unit){
    repository.processLocalContactData(userId, contactId, action)
  }

  fun getLocalContact(contactId: String){
    repository.getLocalContact(contactId)
  }

  fun deleteContact(id: String){
    repository.deleteLocalContact(id)
  }
}