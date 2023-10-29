package chat.echo.app.views.helpers

import kotlinx.serialization.Serializable

@Serializable
class ContactInfo {
  var apiId: Long = 0L
  var openKeyChainID: String = ""
  var publicKey: String = ""
  var burnerTime: Long = 432000L

  constructor()

  constructor(apiID: Long,
  openKeyChainID: String,
  publicKey: String){
    this.apiId = apiID
    this.openKeyChainID = openKeyChainID
    this.publicKey = publicKey
  }

  override fun toString(): String {
    return "ContactInfo(apiId=$apiId, openKeyChainID='$openKeyChainID', publicKey='$publicKey', burnerTime=$burnerTime)"
  }
}