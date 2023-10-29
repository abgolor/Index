package chat.echo.app.views.helpers.localcontact

import android.view.Display
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import chat.echo.app.views.helpers.LocalContact
import chat.echo.app.views.helpers.copyText
import kotlinx.coroutines.*

class LocalContactRepository (private val localContactDAO: LocalContactDAO){
  private val coroutineScope = CoroutineScope(Dispatchers.Main)

  var searchResults = MutableLiveData<List<LocalContact>>()
  var localContact = MutableLiveData<List<LocalContact>>()

  fun createLocalContact(newLocalContact: LocalContact) {
    coroutineScope.launch(Dispatchers.IO) {
      localContactDAO.createDirectLocalContact(newLocalContact)
    }
  }

  fun deleteLocalContact(apiId: String) {
    coroutineScope.launch(Dispatchers.IO) {
      localContactDAO.deleteLocalContact(apiId)
    }
  }

  fun updateLocalContact(localContact: LocalContact){
    coroutineScope.launch(Dispatchers.IO) {
      localContactDAO.updateLocalContact(localContact)
    }
  }

  fun processLocalContactData(userId: String, contactId: String, action: (localContacts: List<LocalContact> ) -> Unit){
    coroutineScope.launch(Dispatchers.Main) {
      val localContacts = asyncFind(userId, contactId).await()!!
      action(localContacts)
    }
  }

  fun findLocalContact(userId: String, contactId: String) {
    coroutineScope.launch(Dispatchers.Main) {
      searchResults.value = asyncFind(userId, contactId).await()!!
    }
  }

  fun getLocalContact(apiId: String){
    coroutineScope.launch(Dispatchers.Main){
      localContact.value = asyncGetLocalContact(apiId).await()!!
    }
  }

  private fun asyncGetLocalContact(apiId: String): Deferred<List<LocalContact>?> =
    coroutineScope.async(Dispatchers.IO) {
      return@async localContactDAO.getLocalContact(apiId)
    }

  private fun asyncFind(userId: String, contactId: String): Deferred<List<LocalContact>?> =
    coroutineScope.async(Dispatchers.IO) {
      return@async localContactDAO.findLocalContact(userId, contactId)
    }
}