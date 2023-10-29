package chat.echo.app.views.helpers

import androidx.annotation.NonNull
import androidx.room.*
import chat.echo.app.model.ChatType
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "LocalContact")
class LocalContact {
  @PrimaryKey(autoGenerate = false)
  @NonNull
  @ColumnInfo(name = "id")
   var apiID: String = ""

  @ColumnInfo(name = "displayName")
   var displayName: String = ""

  @ColumnInfo(name = "fullName")
   var fullName: String = ""

  @ColumnInfo(name = "localAlias")
   var localAlias: String = ""

  @ColumnInfo(name = "okcEmail")
   var openKeyChainEmail: String = ""

  @ColumnInfo(name = "chatType")
  var chatType: ChatType = ChatType.Direct

  @ColumnInfo(name = "encryptedMembers")
  var encryptedMembers: String = ""

  @ColumnInfo(name = "isLocalContactRefresh")
  var isLocalContactRefresh: Boolean = false

  @ColumnInfo(name = "public")
  var publicKey: String = ""

  @ColumnInfo(name = "burnerTime")
  var burnerTime: Long = 432000L

  constructor()

  constructor(apiID: String, displayName: String, fullName: String, localAlias: String,  openKeyChainEmail: String, chatType: ChatType,
  encryptedMembers: String, isLocalContactRefresh: Boolean = false, publicKey:  String = "", burnerTime: Long = 432000L){
    this.apiID = apiID
    this.displayName = displayName
    this.fullName = fullName
    this.localAlias = localAlias
    this.openKeyChainEmail = openKeyChainEmail
    this.chatType = chatType
    this.encryptedMembers = encryptedMembers
    this.isLocalContactRefresh = isLocalContactRefresh
    this.publicKey = publicKey
    this.burnerTime = burnerTime
  }

  override fun toString(): String {
    return super.toString()
  }
}