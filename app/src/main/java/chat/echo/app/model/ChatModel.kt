/*
package chat.echo.app.model

import android.app.Activity
import android.content.*
import android.net.Uri
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import chat.echo.app.*
import chat.echo.app.R
import chat.echo.app.ui.theme.*
import chat.echo.app.views.call.*
import chat.echo.app.views.chatlist.SortChat
import chat.echo.app.views.helpers.*
import chat.echo.app.views.helpers.localcontact.LocalDatabaseViewModel
import chat.echo.app.views.navbar.BottomBarView
import chat.echo.app.views.onboarding.OnboardingStage
import chat.echo.app.views.usersettings.NotificationPreviewMode
import chat.echo.app.views.usersettings.NotificationsMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.openintents.openpgp.util.OpenPgpServiceConnection
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ChatModel(val controller: ChatController) {
  val onboardingStage = mutableStateOf<OnboardingStage?>(null)
  val currentUser = mutableStateOf<User?>(null)
  val users = mutableStateListOf<UserInfo>()
  val currentPageRoute = mutableStateOf<String>(BottomBarView.Contacts.route)
  val currentPageTitle = mutableStateOf<String>(BottomBarView.Contacts.title)
  val isImport = mutableStateOf<Boolean?>(false)
  val isExport = mutableStateOf<Boolean?>(false)
  val isSetupCompleted = mutableStateOf<Boolean>(false)
  val userCreated = mutableStateOf<Boolean?>(null)
  val chatRunning = mutableStateOf<Boolean?>(null)
  val chatDbChanged = mutableStateOf<Boolean>(false)
  val chatDbEncrypted = mutableStateOf<Boolean?>(false)
  val chatDbStatus = mutableStateOf<DBMigrationResult?>(null)
  val chatDbDeleted = mutableStateOf(false)
  val isSigningCompleted = mutableStateOf(false)
  val isInternetAvailable = mutableStateOf(false)
  val chats = mutableStateListOf<Chat>()
  val isChatRefresh = mutableStateOf<Boolean>(false)
  val activeChat = mutableStateOf<Chat?>(null)
  val userAuthorized = mutableStateOf(false)
  val selectedGroupContacts = mutableStateOf(mutableStateListOf<Long>())
  val isGroupCreated = mutableStateOf(false)
  var mServiceConnection: OpenPgpServiceConnection? = null
  var activity: Activity? = null
  var localContactViewModel: LocalDatabaseViewModel? = null
    set(value) {
      field = value
    }

  //Search
  val showSearchView = mutableStateOf(false)
  val search = mutableStateOf(
    TextFieldValue(
      text = "",
      selection = TextRange(0)
    )
  )

  //Sort Chat Mode
  val sortChatMode = mutableStateOf(SortChat.SortByTime)

  // current chat
  val chatId = mutableStateOf<String?>(null)
  val chatItems = mutableStateListOf<ChatItem>()
  val groupMembers = mutableStateListOf<GroupMember>()
  val terminalItems = mutableStateListOf<TerminalItem>()
  val userAddress = mutableStateOf<String?>(null)
  val userSMPServers = mutableStateOf<(List<String>)?>(null)
  val chatItemTTL = mutableStateOf<ChatItemTTL>(ChatItemTTL.None)

  // set when app opened from external intent
  val clearOverlays = mutableStateOf<Boolean>(false)

  // set when app is opened via contact or invitation URI
  val appOpenUrl = mutableStateOf<Uri?>(null)

  // preferences
  val notificationsMode = mutableStateOf(NotificationsMode.default)
  var notificationPreviewMode = mutableStateOf(NotificationPreviewMode.default)
  val performLA = mutableStateOf(false)
  val showAdvertiseLAUnavailableAlert = mutableStateOf(false)
  var incognito = mutableStateOf(false)

  // current WebRTC call
  val callManager = CallManager(this)
  val callInvitations = mutableStateMapOf<String, RcvCallInvitation>()
  val activeCallInvitation = mutableStateOf<RcvCallInvitation?>(null)
  val activeCall = mutableStateOf<Call?>(null)
  val callCommand = mutableStateOf<WCallCommand?>(null)
  val showCallView = mutableStateOf(false)
  val switchingCall = mutableStateOf(false)
  val intentFilter = IntentFilter("android.hardware.usb.action.USB_STATE")
  val isUSBConnected = mutableStateOf(false)
  val broadcastReceiver = object: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      isUSBConnected.value = intent?.extras!!.getBoolean("connected")
    }
  }

  // currently showing QR code
  val connReqInv = mutableStateOf(null as String?)

  // working with external intents
  val sharedContent = mutableStateOf(null as SharedContent?)
  val chatInfo = mutableStateOf(null as ChatInfo?)
  val contactInfo = mutableStateOf(null as Pair<ConnectionStats, Profile?>?)

  fun updateUserProfile(profile: LocalProfile) {
    val user = currentUser.value
    if (user != null) {
      currentUser.value = user.copy(profile = profile)
    }
  }

  fun registerBroadcastReceiver(context: Context) {
    context.registerReceiver(broadcastReceiver, intentFilter)
  }

  fun unregisterBroadcastReceiver(context: Context) {
    try {
      context.unregisterReceiver(broadcastReceiver)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun getUser(userId: Long): User? = if (currentUser.value?.userId == userId) {
    currentUser.value
  } else {
    users.firstOrNull { it.user.userId == userId }?.user
  }

  private fun getUserIndex(user: User): Int =
    users.indexOfFirst { it.user.userId == user.userId }

  fun updateUser(user: User) {
    val i = getUserIndex(user)
    if (i != -1) {
      users[i] = users[i].copy(user = user)
    }
    if (currentUser.value?.userId == user.userId) {
      currentUser.value = user
    }
  }

  fun removeUser(user: User) {
    val i = getUserIndex(user)
    if (i != -1 && users[i].user.userId != currentUser.value?.userId) {
      users.removeAt(i)
    }
  }

  fun hasChat(id: String): Boolean = chats.firstOrNull { it.id == id } != null
  fun getChat(id: String): Chat? = chats.firstOrNull { it.id == id }
  fun getContactChat(contactId: Long): Chat? = chats.firstOrNull { it.chatInfo is ChatInfo.Direct && it.chatInfo.apiId == contactId }
  private fun getChatIndex(id: String): Int = chats.indexOfFirst { it.id == id }
  fun addChat(chat: Chat) = chats.add(index = 0, chat)

  fun updateChatInfo(cInfo: ChatInfo) {
    val i = getChatIndex(cInfo.id)
    if (i >= 0) chats[i] = chats[i].copy(chatInfo = cInfo)
  }

  fun updateContactConnection(contactConnection: PendingContactConnection) = updateChat(ChatInfo.ContactConnection(contactConnection))

  fun updateContact(contact: Contact) = updateChat(ChatInfo.Direct(contact), addMissing = !contact.isIndirectContact)

  fun updateGroup(groupInfo: GroupInfo) = updateChat(ChatInfo.Group(groupInfo))

  private fun updateChat(cInfo: ChatInfo, addMissing: Boolean = true) {
    if (hasChat(cInfo.id)) {
      updateChatInfo(cInfo)
    } else if (addMissing) {
      addChat(Chat(chatInfo = cInfo, chatItems = arrayListOf()))
    }
  }

  fun updateChats(newChats: List<Chat>) {
    val mergedChats = arrayListOf<Chat>()
    for (newChat in newChats) {
      val i = getChatIndex(newChat.chatInfo.id)
      if (i >= 0) {
        mergedChats.add(newChat.copy(serverInfo = chats[i].serverInfo))
      } else {
        mergedChats.add(newChat)
      }
    }
    chats.clear()
    chats.addAll(mergedChats)
    val cId = chatId.value
    // If chat is null, it was deleted in background after apiGetChats call
    if (cId != null && getChat(cId) == null) {
      chatId.value = null
    }
  }

  fun updateNetworkStatus(id: ChatId, status: Chat.NetworkStatus) {
    val i = getChatIndex(id)
    if (i >= 0) {
      val chat = chats[i]
      chats[i] = chat.copy(serverInfo = chat.serverInfo.copy(networkStatus = status))
    }
  }

  fun replaceChat(id: String, chat: Chat) {
    val i = getChatIndex(id)
    if (i >= 0) {
      chats[i] = chat
    } else {
      // invalid state, correcting
      chats.add(index = 0, chat)
    }
  }

  fun addChatItem(cInfo: ChatInfo, cItem: ChatItem) {
    // update previews
    val i = getChatIndex(cInfo.id)
    val chat: Chat
    if (i >= 0) {
      chat = chats[i]
      chats[i] = chat.copy(
        chatItems = arrayListOf(cItem),
        chatStats =
        if (cItem.meta.itemStatus is CIStatus.RcvNew) {
          val minUnreadId = if (chat.chatStats.minUnreadItemId == 0L) cItem.id else chat.chatStats.minUnreadItemId
          chat.chatStats.copy(unreadCount = chat.chatStats.unreadCount + 1, minUnreadItemId = minUnreadId)
        } else
          chat.chatStats
      )
      if (i > 0) {
        popChat_(i)
      }
    } else {
      addChat(Chat(chatInfo = cInfo, chatItems = arrayListOf(cItem)))
    }
    // add to current chat
    if (chatId.value == cInfo.id) {
      chatItems.add(cItem)
    }
  }

  fun upsertChatItem(cInfo: ChatInfo, cItem: ChatItem): Boolean {
    // update previews
    val i = getChatIndex(cInfo.id)
    val chat: Chat
    val res: Boolean
    if (i >= 0) {
      chat = chats[i]
      val pItem = chat.chatItems.lastOrNull()
      if (pItem?.id == cItem.id) {
        chats[i] = chat.copy(chatItems = arrayListOf(cItem))
        if (pItem.isRcvNew && !cItem.isRcvNew) {
          // status changed from New to Read, update counter
          decreaseCounterInChat(cInfo.id)
        }
      }
      res = false
    } else {
      addChat(Chat(chatInfo = cInfo, chatItems = arrayListOf(cItem)))
      res = true
    }
    // update current chat
    if (chatId.value == cInfo.id) {
      val itemIndex = chatItems.indexOfFirst { it.id == cItem.id }
      if (itemIndex >= 0) {
        chatItems[itemIndex] = cItem
        return false
      } else {
        chatItems.add(cItem)
        return true
      }
    } else {
      return res
    }
  }

  fun removeChatItem(cInfo: ChatInfo, cItem: ChatItem) {
    // update previews
    val i = getChatIndex(cInfo.id)
    val chat: Chat
    if (i >= 0) {
      chat = chats[i]
      val pItem = chat.chatItems.lastOrNull()
      if (pItem?.id == cItem.id) {
        chats[i] = chat.copy(chatItems = arrayListOf(cItem))
      }
    }
    // remove from current chat
    if (chatId.value == cInfo.id) {
      val itemIndex = chatItems.indexOfFirst { it.id == cItem.id }
      if (itemIndex >= 0) {
        chatItems.removeAt(itemIndex)
      }
    }
  }

  fun clearChat(cInfo: ChatInfo) {
    // clear preview
    val i = getChatIndex(cInfo.id)
    if (i >= 0) {
      chats[i] = chats[i].copy(chatItems = arrayListOf(), chatStats = Chat.ChatStats(), chatInfo = cInfo)
    }
    // clear current chat
    if (chatId.value == cInfo.id) {
      chatItems.clear()
    }
  }

  fun updateCurrentUser(newProfile: Profile, preferences: FullChatPreferences? = null) {
    val current = currentUser.value ?: return
    val updated = current.copy(
      profile = newProfile.toLocalProfile(current.profile.profileId),
      fullPreferences = preferences ?: current.fullPreferences
    )
    val indexInUsers = users.indexOfFirst { it.user.userId == current.userId }
    if (indexInUsers != -1) {
      users[indexInUsers] = UserInfo(updated, users[indexInUsers].unreadCount)
    }
    currentUser.value = updated
  }

  suspend fun addLiveDummy(chatInfo: ChatInfo): ChatItem {
    val cItem = ChatItem.liveDummy(chatInfo is ChatInfo.Direct)
    withContext(Dispatchers.Main) {
      chatItems.add(cItem)
    }
    return cItem
  }

  fun removeLiveDummy() {
    if (chatItems.lastOrNull()?.id == ChatItem.TEMP_LIVE_CHAT_ITEM_ID) {
      chatItems.removeLast()
    }
  }

  fun markChatItemsRead(cInfo: ChatInfo, range: CC.ItemRange? = null, unreadCountAfter: Int? = null) {
    val markedRead = markItemsReadInCurrentChat(cInfo, range)
    // update preview
    val chatIdx = getChatIndex(cInfo.id)
    if (chatIdx >= 0) {
      val chat = chats[chatIdx]
      val lastId = chat.chatItems.lastOrNull()?.id
      if (lastId != null) {
        val unreadCount = unreadCountAfter ?: if (range != null) chat.chatStats.unreadCount - markedRead else 0
        decreaseUnreadCounter(currentUser.value!!, chat.chatStats.unreadCount - unreadCount)
        chats[chatIdx] = chat.copy(
          chatStats = chat.chatStats.copy(
            unreadCount = unreadCount,
            // Can't use minUnreadItemId currently since chat items can have unread items between read items
            //minUnreadItemId = if (range != null) kotlin.math.max(chat.chatStats.minUnreadItemId, range.to + 1) else lastId + 1
          )
        )
      }
    }
  }

  private fun markItemsReadInCurrentChat(cInfo: ChatInfo, range: CC.ItemRange? = null): Int {
    var markedRead = 0
    if (chatId.value == cInfo.id) {
      var i = 0
      while (i < chatItems.count()) {
        val item = chatItems[i]
        if (item.meta.itemStatus is CIStatus.RcvNew && (range == null || (range.from <= item.id && item.id <= range.to))) {
          val newItem = item.withStatus(CIStatus.RcvRead())
          chatItems[i] = newItem
          if (newItem.meta.itemLive != true && newItem.meta.itemTimed?.ttl != null) {
            chatItems[i] = newItem.copy(
              meta = newItem.meta.copy(
                itemTimed = newItem.meta.itemTimed.copy(
                  deleteAt = Clock.System.now() + newItem.meta.itemTimed.ttl.toDuration(DurationUnit.SECONDS)
                )
              )
            )
          }
          markedRead++
        }
        i += 1
      }
    }
    return markedRead
  }

  private fun decreaseCounterInChat(chatId: ChatId) {
    val chatIndex = getChatIndex(chatId)
    if (chatIndex == -1) return
    val chat = chats[chatIndex]
    val unreadCount = kotlin.math.max(chat.chatStats.unreadCount - 1, 0)
    decreaseUnreadCounter(currentUser.value!!, chat.chatStats.unreadCount - unreadCount)
    chats[chatIndex] = chat.copy(
      chatStats = chat.chatStats.copy(
        unreadCount = unreadCount,
      )
    )
  }

  fun increaseUnreadCounter(user: User) {
    changeUnreadCounter(user, 1)
  }

  fun decreaseUnreadCounter(user: User, by: Int = 1) {
    changeUnreadCounter(user, -by)
  }

  private fun changeUnreadCounter(user: User, by: Int) {
    val i = users.indexOfFirst { it.user.userId == user.userId }
    if (i != -1) {
      users[i] = UserInfo(user, users[i].unreadCount + by)
    }
  }

  fun markChatItemsRead(cInfo: ChatInfo, range: CC.ItemRange? = null, unreadCountAfter: Int? = null) {
    val markedRead = markItemsReadInCurrentChat(cInfo, range)
    // update preview
    val chatIdx = getChatIndex(cInfo.id)
    if (chatIdx >= 0) {
      val chat = chats[chatIdx]
      val lastId = chat.chatItems.lastOrNull()?.id
      if (lastId != null) {
        chats[chatIdx] = chat.copy(
          chatStats = chat.chatStats.copy(
            unreadCount = unreadCountAfter ?: if (range != null) chat.chatStats.unreadCount - markedRead else 0,
            // Can't use minUnreadItemId currently since chat items can have unread items between read items
            //minUnreadItemId = if (range != null) kotlin.math.max(chat.chatStats.minUnreadItemId, range.to + 1) else lastId + 1
          )
        )
      }
    }
  }

  private fun markItemsReadInCurrentChat(cInfo: ChatInfo, range: CC.ItemRange? = null): Int {
    var markedRead = 0
    if (chatId.value == cInfo.id) {
      var i = 0
      while (i < chatItems.count()) {
        val item = chatItems[i]
        if (item.meta.itemStatus is CIStatus.RcvNew && (range == null || (range.from <= item.id && item.id <= range.to))) {
          chatItems[i] = item.withStatus(CIStatus.RcvRead())
          markedRead++
        }
        i += 1
      }
    }
    return markedRead
  }

  private fun decreaseCounterInChat(chatId: ChatId) {
    val chatIndex = getChatIndex(chatId)
    if (chatIndex == -1) return
    val chat = chats[chatIndex]
    chats[chatIndex] = chat.copy(
      chatStats = chat.chatStats.copy(
        unreadCount = kotlin.math.max(chat.chatStats.unreadCount - 1, 0),
      )
    )
  }

  //  func popChat(_ id: String) {
  //    if let i = getChatIndex(id) {
  //      popChat_(i)
  //    }
  //  }
  private fun popChat_(i: Int) {
    val chat = chats.removeAt(i)
    chats.add(index = 0, chat)
  }

  fun dismissConnReqView(id: String) {
    if (connReqInv.value == null) return
    val info = getChat(id)?.chatInfo as? ChatInfo.ContactConnection ?: return
    if (info.contactConnection.connReqInv == connReqInv.value) {
      connReqInv.value = null
      ModalManager.shared.closeModals()
    }
  }

  fun removeChat(id: String) {
    chats.removeAll { it.id == id }
  }

  fun upsertGroupMember(groupInfo: GroupInfo, member: GroupMember): Boolean {
    // update current chat
    return if (chatId.value == groupInfo.id) {
      val memberIndex = groupMembers.indexOfFirst { it.id == member.id }
      if (memberIndex >= 0) {
        groupMembers[memberIndex] = member
        false
      } else {
        groupMembers.add(member)
        true
      }
    } else {
      false
    }
  }
}

enum class ChatType(val type: String) {
  Direct("@"),
  Group("#"),
  Private("*"),
  ContactRequest("<@"),
  ContactConnection(":");
}

@Serializable
data class User(
  val userId: Long,
  val userContactId: Long,
  val localDisplayName: String,
  val profile: LocalProfile,
  val fullPreferences: FullChatPreferences,
  val activeUser: Boolean,
  val showNtfs: Boolean,
  val viewPwdHash: UserPwdHash?
): NamedChat {
  override val displayName: String get() = profile.displayName
  override val fullName: String get() = profile.fullName
  override val image: String? get() = profile.image
  override val localAlias: String = ""
  var openKeyChainID: String = ""
    get() = field
    set(value) {
      field = value
    }
  val hidden: Boolean = viewPwdHash != null
  val showNotifications: Boolean = activeUser || showNtfs

  companion object {
    val sampleData = User(
      userId = 1,
      userContactId = 1,
      localDisplayName = "alice",
      profile = LocalProfile.sampleData,
      fullPreferences = FullChatPreferences.sampleData,
      activeUser = true,
      showNtfs = true,
      viewPwdHash = null,
    )
  }
}

@Serializable
data class UserPwdHash(
  val hash: String,
  val salt: String
)

@Serializable
data class UserInfo(
  val user: User,
  val unreadCount: Int
) {
  companion object {
    val sampleData = UserInfo(
      user = User.sampleData,
      unreadCount = 1
    )
  }
}

typealias ChatId = String

interface NamedChat {
  val displayName: String
  val fullName: String
  val image: String?
  val localAlias: String
  val chatViewName: String
    get() = fullName.ifEmpty {
      displayName
    }
}

interface SomeChat {
  val chatType: ChatType
  val localDisplayName: String
  val id: ChatId
  val apiId: Long
  val ready: Boolean
  val sendMsgEnabled: Boolean
  val ntfsEnabled: Boolean
  val createdAt: Instant
  val updatedAt: Instant
}

@Serializable
data class Chat(
  val chatInfo: ChatInfo,
  val chatItems: List<ChatItem>,
  val chatStats: ChatStats = ChatStats(),
  val serverInfo: ServerInfo = ServerInfo(NetworkStatus.Unknown())
) {
  val id: String get() = chatInfo.id

  @Serializable
  data class ChatStats(val unreadCount: Int = 0, val minUnreadItemId: Long = 0)

  @Serializable
  data class ServerInfo(val networkStatus: NetworkStatus)

  @Serializable
  sealed class NetworkStatus {
    val statusString: String
      get() =
        when (this) {
          is Connected -> generalGetString(R.string.server_connected)
          is Error -> generalGetString(R.string.server_error)
          else -> generalGetString(R.string.server_connecting)
        }
    val statusExplanation: String
      get() =
        when (this) {
          is Connected -> generalGetString(R.string.connected_to_server_to_receive_messages_from_contact)
          is Error -> String.format(generalGetString(R.string.trying_to_connect_to_server_to_receive_messages_with_error), error)
          else -> generalGetString(R.string.trying_to_connect_to_server_to_receive_messages)
        }

    @Serializable @SerialName("unknown") class Unknown: NetworkStatus()
    @Serializable @SerialName("connected") class Connected: NetworkStatus()
    @Serializable @SerialName("disconnected") class Disconnected: NetworkStatus()
    @Serializable @SerialName("error") class Error(val error: String): NetworkStatus()
  }

  companion object {
    val sampleData = Chat(
      chatInfo = ChatInfo.Direct.sampleData,
      chatItems = arrayListOf(ChatItem.getSampleData())
    )
  }
}

@Serializable
sealed class ChatInfo: SomeChat, NamedChat {
  abstract val incognito: Boolean

  @Serializable @SerialName("direct")
  class Direct(val contact: Contact): ChatInfo() {
    override val chatType get() = ChatType.Direct
    override val localDisplayName get() = contact.localDisplayName
    override val id get() = contact.id
    override val apiId get() = contact.apiId
    override val ready get() = contact.ready
    override val sendMsgEnabled get() = contact.sendMsgEnabled
    override val ntfsEnabled get() = contact.chatSettings.enableNtfs
    override val incognito get() = contact.contactConnIncognito
    override val createdAt get() = contact.createdAt
    override val updatedAt get() = contact.updatedAt
    override val displayName get() = contact.displayName
    override val fullName get() = contact.fullName
    override val image get() = contact.image
    override val localAlias: String get() = contact.localAlias
    var openKeyChainID: String = contact.openKeyChainID
      set(value) {
        field = value
      }

    companion object {
      val sampleData = Direct(Contact.sampleData)
    }
  }

  @Serializable @SerialName("group")
  class Group(val groupInfo: GroupInfo): ChatInfo() {
    override val chatType get() = if (groupInfo.isZeroKnowledge) ChatType.Private else ChatType.Group
    override val localDisplayName get() = groupInfo.localDisplayName
    override val id get() = groupInfo.id
    override val apiId get() = groupInfo.apiId
    override val ready get() = groupInfo.ready
    override val sendMsgEnabled get() = groupInfo.sendMsgEnabled
    override val ntfsEnabled get() = groupInfo.chatSettings.enableNtfs
    override val incognito get() = groupInfo.membership.memberIncognito
    override val createdAt get() = groupInfo.createdAt
    override val updatedAt get() = groupInfo.updatedAt
    override val displayName get() = groupInfo.displayName
    override val fullName get() = groupInfo.fullName
    override val image get() = groupInfo.image
    override val localAlias get() = groupInfo.localAlias

    companion object {
      val sampleData = Group(GroupInfo.sampleData)
    }
  }

  @Serializable @SerialName("contactRequest")
  class ContactRequest(val contactRequest: UserContactRequest): ChatInfo() {
    override val chatType get() = ChatType.ContactRequest
    override val localDisplayName get() = contactRequest.localDisplayName
    override val id get() = contactRequest.id
    override val apiId get() = contactRequest.apiId
    override val ready get() = contactRequest.ready
    override val sendMsgEnabled get() = contactRequest.sendMsgEnabled
    override val ntfsEnabled get() = false
    override val incognito get() = false
    override val createdAt get() = contactRequest.createdAt
    override val updatedAt get() = contactRequest.updatedAt
    override val displayName get() = contactRequest.displayName
    override val fullName get() = contactRequest.fullName
    override val image get() = contactRequest.image
    override val localAlias get() = contactRequest.localAlias

    companion object {
      val sampleData = ContactRequest(UserContactRequest.sampleData)
    }
  }

  @Serializable @SerialName("contactConnection")
  class ContactConnection(val contactConnection: PendingContactConnection): ChatInfo() {
    override val chatType get() = ChatType.ContactConnection
    override val localDisplayName get() = contactConnection.localDisplayName
    override val id get() = contactConnection.id
    override val apiId get() = contactConnection.apiId
    override val ready get() = contactConnection.ready
    override val sendMsgEnabled get() = contactConnection.sendMsgEnabled
    override val ntfsEnabled get() = false
    override val incognito get() = contactConnection.incognito
    override val createdAt get() = contactConnection.createdAt
    override val updatedAt get() = contactConnection.updatedAt
    override val displayName get() = contactConnection.displayName
    override val fullName get() = contactConnection.fullName
    override val image get() = contactConnection.image
    override val localAlias get() = contactConnection.localAlias

    companion object {
      fun getSampleData(status: ConnStatus = ConnStatus.New, viaContactUri: Boolean = false): ContactConnection =
        ContactConnection(PendingContactConnection.getSampleData(status, viaContactUri))
    }
  }
}

@Serializable
data class Contact(
  val contactId: Long,
  override val localDisplayName: String,
  val profile: LocalProfile,
  val activeConn: Connection,
  val viaGroup: Long? = null,
  val chatSettings: ChatSettings,
  override val createdAt: Instant,
  override val updatedAt: Instant,
  var tag: String = "",
): SomeChat, NamedChat {
  override val chatType get() = ChatType.Direct
  override val id get() = "@$contactId"
  override val apiId get() = contactId
  override val ready get() = activeConn.connStatus == ConnStatus.Ready
  override val sendMsgEnabled get() = true
  override val ntfsEnabled get() = chatSettings.enableNtfs
  override val displayName
    get() = fullName.ifEmpty {
      localDisplayName
    }
  override val fullName get() = profile.fullName
  override val image get() = profile.image
  override val localAlias get() = profile.localAlias
  var openKeyChainID: String = ""
    set(value) {
      field = value
    }
  val isIndirectContact: Boolean
    get() =
      activeConn.connLevel > 0 || viaGroup != null
  val contactConnIncognito =
    activeConn.customUserProfileId != null

  companion object {
    val sampleData = Contact(
      contactId = 1,
      localDisplayName = "alice",
      profile = LocalProfile.sampleData,
      activeConn = Connection.sampleData,
      chatSettings = ChatSettings(true),
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now()
    )
  }
}

@Serializable
class ContactRef(
  val contactId: Long,
  var localDisplayName: String
) {
  val id: ChatId get() = "@$contactId"
}

@Serializable
class ContactSubStatus(
  val contact: Contact,
  val contactError: ChatError? = null
)

@Serializable
class Connection(val connId: Long, val connStatus: ConnStatus, val connLevel: Int, val customUserProfileId: Long? = null) {
  val id: ChatId get() = ":$connId"

  companion object {
    val sampleData = Connection(connId = 1, connStatus = ConnStatus.Ready, connLevel = 0, customUserProfileId = null)
  }
}
*/
/*@Serializable
sealed class UserConnectionLink{
  data class Configure (val connReqInv: String?, val publicKey: String): UserConnectionLink
}

object UserConnectionSerializer: KSerializer<UserConnectionLink>{
  override fun deserialize(decoder: Decoder): UserConnectionLink {
    val input = decoder as? JsonDecoder ?: throw SerializationException ("This class can be decoded only by Json format")
    val tree = input.decodeJsonElement() as? JsonObject ?: throw SerializationException("Expected JsonObject")
  }

  override val descriptor: SerialDescriptor
    get() = TODO("Not yet implemented")

  override fun serialize(encoder: Encoder, value: UserConnectionLink) {
    TODO("Not yet implemented")
  }
}*/
/*


@Serializable
data class UserConnectionLink(
  val connReqInv: String,
  val publicKey: String
) {
  val connectionLink: String
    get() {
      return connReqInv
    }

  //val userConnectionLink = json["user"]?.jsonPrimitive?.content ?: generalGetString(R.string.unknown_message_format)
  companion object {
    val sampleData = UserConnectionLink(connReqInv = "", publicKey = "")
  }
}

@Serializable
class Profile(
  override val displayName: String,
  override val fullName: String,
  override val image: String? = null,
  override val localAlias: String = ""
): NamedChat {
  val profileViewName: String
    get() {
      return if (fullName == "" || displayName == fullName) displayName else "$displayName ($fullName)"
    }

  fun toLocalProfile(profileId: Long): LocalProfile = LocalProfile(profileId, displayName, fullName, image, localAlias)

  companion object {
    val sampleData = Profile(
      displayName = "alice",
      fullName = "Alice"
    )
  }
}

@Serializable
class LocalProfile(
  val profileId: Long,
  override val displayName: String,
  override val fullName: String,
  override val image: String? = null,
  override val localAlias: String
): NamedChat {
  val profileViewName: String = localAlias.ifEmpty { if (fullName == "" || displayName == fullName) displayName else "$displayName ($fullName)" }

  fun toProfile(): Profile = Profile(displayName, fullName, image, localAlias)

  companion object {
    val sampleData = LocalProfile(
      profileId = 1L,
      displayName = "alice",
      fullName = "Alice",
      localAlias = ""
    )
  }
}

@Serializable
class Group(
  val groupInfo: GroupInfo,
  var members: List<GroupMember>
)

@Serializable
data class GroupInfo(
  val groupId: Long,
  override val localDisplayName: String,
  val groupProfile: GroupProfile,
  val membership: GroupMember,
  val hostConnCustomUserProfileId: Long? = null,
  val chatSettings: ChatSettings,
  override val createdAt: Instant,
  override val updatedAt: Instant,
  var isZeroKnowledge: Boolean = false
): SomeChat, NamedChat {
  override val chatType get() = if (isZeroKnowledge) ChatType.Group else ChatType.Private
  override val id get() = "#$groupId"
  override val apiId get() = groupId
  override val ready get() = membership.memberActive
  override val sendMsgEnabled get() = membership.memberActive
  override val ntfsEnabled get() = chatSettings.enableNtfs
  override var displayName = ""
    get() = groupProfile.displayName
  override val fullName get() = groupProfile.fullName
  override val image get() = groupProfile.image
  override val localAlias get() = ""
  val canEdit: Boolean
    get() = membership.memberRole == GroupMemberRole.Owner && membership.memberCurrent
  val canDelete: Boolean
    get() = membership.memberRole == GroupMemberRole.Owner || !membership.memberCurrent
  val canAddMembers: Boolean
    get() = membership.memberRole >= GroupMemberRole.Admin && membership.memberActive

  companion object {
    val sampleData = GroupInfo(
      groupId = 1,
      localDisplayName = "team",
      groupProfile = GroupProfile.sampleData,
      membership = GroupMember.sampleData,
      hostConnCustomUserProfileId = null,
      chatSettings = ChatSettings(true),
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now()
    )
  }
}

@Serializable
class GroupProfile(
  override val displayName: String,
  override val fullName: String,
  override val image: String? = null,
  override val localAlias: String = "",
): NamedChat {
  companion object {
    val sampleData = GroupProfile(
      displayName = "team",
      fullName = "My Team"
    )
  }
}

@Serializable
class GroupMember(
  val groupMemberId: Long,
  val groupId: Long,
  val memberId: String,
  var memberRole: GroupMemberRole,
  var memberCategory: GroupMemberCategory,
  var memberStatus: GroupMemberStatus,
  var invitedBy: InvitedBy,
  val localDisplayName: String,
  val memberProfile: LocalProfile,
  val memberContactId: Long? = null,
  val memberContactProfileId: Long,
  var activeConn: Connection? = null
) {
  val id: String get() = "#$groupId @$groupMemberId"
  var displayName: String = ""
    get() = memberProfile.localAlias.ifEmpty { memberProfile.displayName }
  val fullName: String get() = memberProfile.fullName
  val image: String? get() = memberProfile.image
  val chatViewName: String
    get() = memberProfile.localAlias.ifEmpty { displayName + (if (fullName == "" || fullName == displayName) "" else " / $fullName") }
  val memberActive: Boolean
    get() = when (this.memberStatus) {
      GroupMemberStatus.MemRemoved -> false
      GroupMemberStatus.MemLeft -> false
      GroupMemberStatus.MemGroupDeleted -> false
      GroupMemberStatus.MemInvited -> false
      GroupMemberStatus.MemIntroduced -> false
      GroupMemberStatus.MemIntroInvited -> false
      GroupMemberStatus.MemAccepted -> false
      GroupMemberStatus.MemAnnounced -> false
      GroupMemberStatus.MemConnected -> true
      GroupMemberStatus.MemComplete -> true
      GroupMemberStatus.MemCreator -> true
    }
  val memberCurrent: Boolean
    get() = when (this.memberStatus) {
      GroupMemberStatus.MemRemoved -> false
      GroupMemberStatus.MemLeft -> false
      GroupMemberStatus.MemGroupDeleted -> false
      GroupMemberStatus.MemInvited -> false
      GroupMemberStatus.MemIntroduced -> true
      GroupMemberStatus.MemIntroInvited -> true
      GroupMemberStatus.MemAccepted -> true
      GroupMemberStatus.MemAnnounced -> true
      GroupMemberStatus.MemConnected -> true
      GroupMemberStatus.MemComplete -> true
      GroupMemberStatus.MemCreator -> true
    }

  fun canBeRemoved(groupInfo: GroupInfo): Boolean {
    val userRole = groupInfo.membership.memberRole
    return memberStatus != GroupMemberStatus.MemRemoved && memberStatus != GroupMemberStatus.MemLeft
        && userRole >= GroupMemberRole.Admin && userRole >= memberRole && groupInfo.membership.memberCurrent
  }

  fun canChangeRoleTo(groupInfo: GroupInfo): List<GroupMemberRole>? =
    if (!canBeRemoved(groupInfo)) null
    else groupInfo.membership.memberRole.let { userRole ->
      GroupMemberRole.values().filter { it <= userRole }
    }

  val memberIncognito = memberProfile.profileId != memberContactProfileId

  companion object {
    val sampleData = GroupMember(
      groupMemberId = 1,
      groupId = 1,
      memberId = "abcd",
      memberRole = GroupMemberRole.Member,
      memberCategory = GroupMemberCategory.InviteeMember,
      memberStatus = GroupMemberStatus.MemComplete,
      invitedBy = InvitedBy.IBUser(),
      localDisplayName = "alice",
      memberProfile = LocalProfile.sampleData,
      memberContactId = 1,
      memberContactProfileId = 1L,
      activeConn = Connection.sampleData
    )
  }
}

@Serializable
enum class GroupMemberRole(val memberRole: String) {
  @SerialName("member") Member("member"), // order matters in comparisons
  @SerialName("admin") Admin("admin"),
  @SerialName("owner") Owner("owner");

  val text: String
    get() = when (this) {
      Member -> generalGetString(R.string.group_member_role_member)
      Admin -> generalGetString(R.string.group_member_role_admin)
      Owner -> generalGetString(R.string.group_member_role_owner)
    }
}

@Serializable
enum class GroupMemberCategory {
  @SerialName("user") UserMember,
  @SerialName("invitee") InviteeMember,
  @SerialName("host") HostMember,
  @SerialName("pre") PreMember,
  @SerialName("post") PostMember;
}

@Serializable
enum class GroupMemberStatus {
  @SerialName("removed") MemRemoved,
  @SerialName("left") MemLeft,
  @SerialName("deleted") MemGroupDeleted,
  @SerialName("invited") MemInvited,
  @SerialName("introduced") MemIntroduced,
  @SerialName("intro-inv") MemIntroInvited,
  @SerialName("accepted") MemAccepted,
  @SerialName("announced") MemAnnounced,
  @SerialName("connected") MemConnected,
  @SerialName("complete") MemComplete,
  @SerialName("creator") MemCreator;

  val text: String
    get() = when (this) {
      MemRemoved -> generalGetString(R.string.group_member_status_removed)
      MemLeft -> generalGetString(R.string.group_member_status_left)
      MemGroupDeleted -> generalGetString(R.string.group_member_status_group_deleted)
      MemInvited -> generalGetString(R.string.group_member_status_invited)
      MemIntroduced -> generalGetString(R.string.group_member_status_introduced)
      MemIntroInvited -> generalGetString(R.string.group_member_status_intro_invitation)
      MemAccepted -> generalGetString(R.string.group_member_status_accepted)
      MemAnnounced -> generalGetString(R.string.group_member_status_announced)
      MemConnected -> generalGetString(R.string.group_member_status_connected)
      MemComplete -> generalGetString(R.string.group_member_status_complete)
      MemCreator -> generalGetString(R.string.group_member_status_creator)
    }
  val shortText: String
    get() = when (this) {
      MemRemoved -> generalGetString(R.string.group_member_status_removed)
      MemLeft -> generalGetString(R.string.group_member_status_left)
      MemGroupDeleted -> generalGetString(R.string.group_member_status_group_deleted)
      MemInvited -> generalGetString(R.string.group_member_status_invited)
      MemIntroduced -> generalGetString(R.string.group_member_status_connecting)
      MemIntroInvited -> generalGetString(R.string.group_member_status_connecting)
      MemAccepted -> generalGetString(R.string.group_member_status_connecting)
      MemAnnounced -> generalGetString(R.string.group_member_status_connecting)
      MemConnected -> generalGetString(R.string.group_member_status_connected)
      MemComplete -> generalGetString(R.string.group_member_status_complete)
      MemCreator -> generalGetString(R.string.group_member_status_creator)
    }
}

@Serializable
sealed class InvitedBy {
  @Serializable @SerialName("contact") class IBContact(val byContactId: Long): InvitedBy()
  @Serializable @SerialName("user") class IBUser: InvitedBy()
  @Serializable @SerialName("unknown") class IBUnknown: InvitedBy()
}

@Serializable
class LinkPreview(
  val uri: String,
  val title: String,
  val description: String,
  val image: String
) {
  companion object {
    val sampleData = LinkPreview(
      uri = "https://www.duckduckgo.com",
      title = "Privacy, simplified.",
      description = "The Internet privacy company that empowers you to seamlessly take control of your personal information online, without any tradeoffs.",
      image = "data:image/jpg;base64,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"
    )
  }
}

@Serializable
class MemberSubError(
  val member: GroupMember,
  val memberError: ChatError
)

@Serializable
class UserContactRequest(
  val contactRequestId: Long,
  override val localDisplayName: String,
  val profile: Profile,
  override val createdAt: Instant,
  override val updatedAt: Instant
): SomeChat, NamedChat {
  override val chatType get() = ChatType.ContactRequest
  override val id get() = "<@$contactRequestId"
  override val apiId get() = contactRequestId
  override val ready get() = true
  override val sendMsgEnabled get() = false
  override val ntfsEnabled get() = false
  override val displayName get() = profile.displayName
  override val fullName get() = profile.fullName
  override val image get() = profile.image
  override val localAlias get() = ""

  companion object {
    val sampleData = UserContactRequest(
      contactRequestId = 1,
      localDisplayName = "alice",
      profile = Profile.sampleData,
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now()
    )
  }
}

@Serializable
class PendingContactConnection(
  val pccConnId: Long,
  val pccAgentConnId: String,
  val pccConnStatus: ConnStatus,
  val viaContactUri: Boolean,
  val customUserProfileId: Long? = null,
  val connReqInv: String? = null,
  override val localAlias: String,
  override val createdAt: Instant,
  override val updatedAt: Instant
): SomeChat, NamedChat {
  override val chatType get() = ChatType.ContactConnection
  override val id get() = ":$pccConnId"
  override val apiId get() = pccConnId
  override val ready get() = false
  override val sendMsgEnabled get() = false
  override val ntfsEnabled get() = false
  override val localDisplayName get() = String.format(generalGetString(R.string.connection_local_display_name), pccConnId)
  override val displayName: String
    get() {
      if (localAlias.isNotEmpty()) return localAlias
      val initiated = pccConnStatus.initiated
      return if (initiated == null) {
        // this should not be in the chat list
        generalGetString(R.string.display_name_connection_established)
      } else {
        generalGetString(
          if (initiated && !viaContactUri) R.string.display_name_invited_to_connect
          else R.string.display_name_connecting
        )
      }
    }
  override val fullName get() = ""
  override val image get() = null
  val masterKey: Long
    get() = 1L
  val publicKey: String
    get() = ""
  val initiated get() = (pccConnStatus.initiated ?: false) && !viaContactUri
  val incognito = customUserProfileId != null
  val description: String
    get() {
      val initiated = pccConnStatus.initiated
      return if (initiated == null) "" else generalGetString(
        if (initiated && !viaContactUri)
          if (incognito) R.string.description_you_shared_one_time_link_incognito else R.string.description_you_shared_one_time_link
        else if (viaContactUri)
          if (incognito) R.string.description_via_contact_address_link_incognito else R.string.description_via_contact_address_link
        else
          if (incognito) R.string.description_via_one_time_link_incognito else R.string.description_via_one_time_link
      )
    }

  companion object {
    fun getSampleData(status: ConnStatus = ConnStatus.New, viaContactUri: Boolean = false): PendingContactConnection =
      PendingContactConnection(
        pccConnId = 1,
        pccAgentConnId = "abcd",
        pccConnStatus = status,
        viaContactUri = viaContactUri,
        localAlias = "",
        customUserProfileId = null,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
      )
  }
}

@Serializable
enum class ConnStatus {
  @SerialName("new") New,
  @SerialName("joined") Joined,
  @SerialName("requested") Requested,
  @SerialName("accepted") Accepted,
  @SerialName("snd-ready") SndReady,
  @SerialName("ready") Ready,
  @SerialName("deleted") Deleted;

  val initiated: Boolean?
    get() = when (this) {
      New -> true
      Joined -> false
      Requested -> true
      Accepted -> true
      SndReady -> false
      Ready -> null
      Deleted -> null
    }
}

@Serializable
class AChatItem(
  val chatInfo: ChatInfo,
  val chatItem: ChatItem
)

@Serializable
data class ChatItem(
  val chatDir: CIDirection,
  val meta: CIMeta,
  val content: CIContent,
  val formattedText: List<FormattedText>? = null,
  val quotedItem: CIQuote? = null,
  val file: CIFile? = null,
  val chatId: Long = 0
) {
  val id: Long get() = meta.itemId
  val timestampText: String get() = meta.timestampText
  val text: String
    get() =
      when {
        content.text == "" && file != null -> file.fileName
        else -> content.text
      }
  val isRcvNew: Boolean get() = meta.itemStatus is CIStatus.RcvNew
  val memberDisplayName: String?
    get() =
      if (chatDir is CIDirection.GroupRcv) chatDir.groupMember.displayName
      else null
  val memberHiddenID: String?
    get() =
      if (chatDir is CIDirection.GroupRcv) chatDir.groupMember.memberId.substring(0, 7)
      else null
  val isDeletedContent: Boolean
    get() =
      when (content) {
        is CIContent.SndDeleted -> true
        is CIContent.RcvDeleted -> true
        else -> false
      }
  val isCall: Boolean
    get() =
      when (content) {
        is CIContent.SndCall -> true
        is CIContent.RcvCall -> true
        else -> false
      }
  val isMutedMemberEvent: Boolean
    get() =
      when (content) {
        is CIContent.RcvGroupEventContent ->
          when (content.rcvGroupEvent) {
            is RcvGroupEvent.GroupUpdated -> true
            is RcvGroupEvent.MemberConnected -> true
            is RcvGroupEvent.UserDeleted -> false
            is RcvGroupEvent.GroupDeleted -> false
            is RcvGroupEvent.MemberAdded -> false
            is RcvGroupEvent.MemberLeft -> false
            is RcvGroupEvent.MemberRole -> true
            is RcvGroupEvent.UserRole -> false
            is RcvGroupEvent.MemberDeleted -> false
          }
        is CIContent.SndGroupEventContent -> true
        else -> false
      }

  fun withStatus(status: CIStatus): ChatItem = this.copy(meta = meta.copy(itemStatus = status))

  companion object {
    fun getSampleData(
      id: Long = 1,
      dir: CIDirection = CIDirection.DirectSnd(),
      ts: Instant = Clock.System.now(),
      text: String = "hello\nthere",
      status: CIStatus = CIStatus.SndNew(),
      quotedItem: CIQuote? = null,
      file: CIFile? = null,
      itemDeleted: Boolean = false,
      itemEdited: Boolean = false,
      editable: Boolean = true
    ) =
      ChatItem(
        chatDir = dir,
        meta = CIMeta.getSample(id, ts, text, status, itemDeleted, itemEdited, editable),
        content = CIContent.SndMsgContent(msgContent = MsgContent.MCBurnerText(text)),
        quotedItem = quotedItem,
        file = file
      )

    fun getFileMsgContentSample(
      id: Long = 1,
      text: String = "",
      fileName: String = "test.txt",
      fileSize: Long = 100,
      fileStatus: CIFileStatus = CIFileStatus.RcvComplete
    ) =
      ChatItem(
        chatDir = CIDirection.DirectRcv(),
        meta = CIMeta.getSample(id, Clock.System.now(), text, CIStatus.RcvRead(), itemDeleted = false, itemEdited = false, editable = false),
        content = CIContent.RcvMsgContent(msgContent = MsgContent.MCBurnerFile(text)),
        quotedItem = null,
        file = CIFile.getSample(fileName = fileName, fileSize = fileSize, fileStatus = fileStatus)
      )

    fun getDeletedContentSampleData(
      id: Long = 1,
      dir: CIDirection = CIDirection.DirectRcv(),
      ts: Instant = Clock.System.now(),
      text: String = "this item is deleted", // sample not localized
      status: CIStatus = CIStatus.RcvRead()
    ) =
      ChatItem(
        chatDir = dir,
        meta = CIMeta.getSample(id, ts, text, status, itemDeleted = false, itemEdited = false, editable = false),
        content = CIContent.RcvDeleted(deleteMode = CIDeleteMode.cidmBroadcast),
        quotedItem = null,
        file = null
      )

    fun getGroupInvitationSample(status: CIGroupInvitationStatus = CIGroupInvitationStatus.Pending) =
      ChatItem(
        chatDir = CIDirection.DirectRcv(),
        meta = CIMeta.getSample(1, Clock.System.now(), "received invitation to join group team as admin", CIStatus.RcvRead(), itemDeleted = false, itemEdited = false, editable = false),
        content = CIContent.RcvGroupInvitation(groupInvitation = CIGroupInvitation.getSample(status = status), memberRole = GroupMemberRole.Admin),
        quotedItem = null,
        file = null
      )

    fun getGroupEventSample() =
      ChatItem(
        chatDir = CIDirection.DirectRcv(),
        meta = CIMeta.getSample(1, Clock.System.now(), "group event text", CIStatus.RcvRead(), itemDeleted = false, itemEdited = false, editable = false),
        content = CIContent.RcvGroupEventContent(rcvGroupEvent = RcvGroupEvent.MemberAdded(groupMemberId = 1, profile = Profile.sampleData)),
        quotedItem = null,
        file = null
      )
  }
}

@Serializable
sealed class CIDirection {
  @Serializable @SerialName("directSnd") class DirectSnd: CIDirection()
  @Serializable @SerialName("directRcv") class DirectRcv: CIDirection()
  @Serializable @SerialName("groupSnd") class GroupSnd: CIDirection()
  @Serializable @SerialName("groupRcv") class GroupRcv(val groupMember: GroupMember): CIDirection()

  val sent: Boolean
    get() = when (this) {
      is DirectSnd -> true
      is DirectRcv -> false
      is GroupSnd -> true
      is GroupRcv -> false
    }
}

@Serializable
data class CIMeta(
  val itemId: Long,
  val itemTs: Instant,
  val itemText: String,
  val itemStatus: CIStatus,
  val createdAt: Instant,
  val itemDeleted: Boolean,
  val itemEdited: Boolean,
  val editable: Boolean
) {
  val timestampText: String get() = getTimestampText(itemTs)

  companion object {
    fun getSample(
      id: Long, ts: Instant, text: String, status: CIStatus = CIStatus.SndNew(),
      itemDeleted: Boolean = false, itemEdited: Boolean = false, editable: Boolean = true
    ): CIMeta =
      CIMeta(
        itemId = id,
        itemTs = ts,
        itemText = text,
        itemStatus = status,
        createdAt = ts,
        itemDeleted = itemDeleted,
        itemEdited = itemEdited,
        editable = editable
      )
  }
}

fun getTimestampText(t: Instant): String {
  val tz = TimeZone.currentSystemDefault()
  val now: LocalDateTime = Clock.System.now().toLocalDateTime(tz)
  val time: LocalDateTime = t.toLocalDateTime(tz)
  val recent = now.date == time.date ||
      (now.date.minus(time.date).days == 1 && now.hour < 12 && time.hour >= 18)
  return if (recent) String.format("%02d:%02d", time.hour, time.minute)
  else String.format("%02d/%02d", time.dayOfMonth, time.monthNumber)
}

@Serializable
sealed class CIStatus {
  @Serializable @SerialName("sndNew") class SndNew: CIStatus()
  @Serializable @SerialName("sndSent") class SndSent: CIStatus()
  @Serializable @SerialName("sndErrorAuth") class SndErrorAuth: CIStatus()
  @Serializable @SerialName("sndError") class SndError(val agentError: AgentErrorType): CIStatus()
  @Serializable @SerialName("rcvViewed") class RcvViewed(val agentError: AgentErrorType): CIStatus()
  @Serializable @SerialName("rcvNew") class RcvNew: CIStatus()
  @Serializable @SerialName("rcvRead") class RcvRead: CIStatus()
}

@Serializable
enum class CIDeleteMode(val deleteMode: String) {
  @SerialName("internal") cidmInternal("internal"),
  @SerialName("broadcast") cidmBroadcast("broadcast");
}

interface ItemContent {
  val text: String
}

@Serializable
sealed class CIContent: ItemContent {
  abstract val msgContent: MsgContent?

  @Serializable @SerialName("sndMsgContent") class SndMsgContent(override val msgContent: MsgContent): CIContent()
  @Serializable @SerialName("rcvMsgContent") class RcvMsgContent(override val msgContent: MsgContent): CIContent()
  @Serializable @SerialName("sndDeleted") class SndDeleted(val deleteMode: CIDeleteMode): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvDeleted") class RcvDeleted(val deleteMode: CIDeleteMode): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndCall") class SndCall(val status: CICallStatus, val duration: Int): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvCall") class RcvCall(val status: CICallStatus, val duration: Int): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvIntegrityError") class RcvIntegrityError(val msgError: MsgErrorType): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvGroupInvitation") class RcvGroupInvitation(val groupInvitation: CIGroupInvitation, val memberRole: GroupMemberRole): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndGroupInvitation") class SndGroupInvitation(val groupInvitation: CIGroupInvitation, val memberRole: GroupMemberRole): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvGroupEvent") class RcvGroupEventContent(val rcvGroupEvent: RcvGroupEvent): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndGroupEvent") class SndGroupEventContent(val sndGroupEvent: SndGroupEvent): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  override var text: String = ""
    get() = when (this) {
      is SndMsgContent -> msgContent.text
      is RcvMsgContent -> msgContent.text
      is SndDeleted -> generalGetString(R.string.deleted_description)
      is RcvDeleted -> generalGetString(R.string.deleted_description)
      is SndCall -> status.text(duration)
      is RcvCall -> status.text(duration)
      is RcvIntegrityError -> msgError.text
      is RcvGroupInvitation -> groupInvitation.text
      is SndGroupInvitation -> groupInvitation.text
      is RcvGroupEventContent -> rcvGroupEvent.text
      is SndGroupEventContent -> sndGroupEvent.text
    }
  */
/* override val burnerTime: Long get() = when(this){
      is SndMsgContent -> msgContent.burnerTime
      is RcvMsgContent -> msgContent.burnerTime
      else -> 432000L
    }*/
/*

}

@Serializable
class CIQuote(
  val chatDir: CIDirection? = null,
  val itemId: Long? = null,
  val sharedMsgId: String? = null,
  val sentAt: Instant,
  val content: MsgContent,
  val formattedText: List<FormattedText>? = null
): ItemContent {
  override val text: String get() = content.text

  fun sender(membership: GroupMember?, isZeroKnowledge: Boolean = false): String? = when (chatDir) {
    is CIDirection.DirectSnd -> generalGetString(R.string.sender_you_pronoun)
    is CIDirection.DirectRcv -> null
    is CIDirection.GroupSnd -> if (isZeroKnowledge) membership?.id?.substring(0, 7) else membership?.displayName
    is CIDirection.GroupRcv -> if (isZeroKnowledge) chatDir.groupMember.id.substring(0, 7) else chatDir.groupMember.displayName
    null -> null
  }

  companion object {
    fun getSample(itemId: Long?, sentAt: Instant, text: String, chatDir: CIDirection?): CIQuote =
      CIQuote(chatDir = chatDir, itemId = itemId, sentAt = sentAt, content = MsgContent.MCBurnerText(text))
  }
}

@Serializable
class CIFile(
  val fileId: Long,
  val fileName: String,
  val fileSize: Long,
  val filePath: String? = null,
  var fileStatus: CIFileStatus
) {
  val loaded: Boolean = when (fileStatus) {
    CIFileStatus.SndStored -> true
    CIFileStatus.SndTransfer -> true
    CIFileStatus.SndComplete -> true
    CIFileStatus.SndCancelled -> true
    CIFileStatus.RcvInvitation -> false
    CIFileStatus.RcvAccepted -> false
    CIFileStatus.RcvTransfer -> false
    CIFileStatus.RcvCancelled -> false
    CIFileStatus.RcvComplete -> true
  }

  companion object {
    fun getSample(
      fileId: Long = 1,
      fileName: String = "test.txt",
      fileSize: Long = 100,
      filePath: String? = "test.txt",
      fileStatus: CIFileStatus = CIFileStatus.RcvComplete
    ): CIFile =
      CIFile(fileId = fileId, fileName = fileName, fileSize = fileSize, filePath = filePath, fileStatus = fileStatus)
  }
}

@Serializable
enum class CIFileStatus {
  @SerialName("snd_stored") SndStored,
  @SerialName("snd_transfer") SndTransfer,
  @SerialName("snd_complete") SndComplete,
  @SerialName("snd_cancelled") SndCancelled,
  @SerialName("rcv_invitation") RcvInvitation,
  @SerialName("rcv_accepted") RcvAccepted,
  @SerialName("rcv_transfer") RcvTransfer,
  @SerialName("rcv_complete") RcvComplete,
  @SerialName("rcv_cancelled") RcvCancelled;
}

@Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
@Serializable(with = ContactBioInfoSerializer::class)
sealed class ContactBioInfo {
  abstract val tag: String
  abstract val notes: String
  abstract val burnerTime: Long
  abstract val publicKey: String
  abstract val openKeyChainID: String

  @Serializable(with = ContactBioInfoSerializer::class) class ContactBioExtra(override val tag: String, override val notes: String, override val burnerTime: Long = 432000L, override val publicKey: String, override val openKeyChainID: String): ContactBioInfo()

  val cmdString: String
    get() = when (this) {
      is ContactBioExtra -> "json ${json.encodeToString(this)}"
    }
}

@Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
@Serializable(with = MsgContentSerializer::class)
sealed class MsgContent {
  abstract val text: String
  abstract val burnerTime: Long

  @Serializable(with = MsgContentSerializer::class) class MCBurnerText(override val text: String, override val burnerTime: Long = 432000L): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCBurnerTimer(override val text: String, override val burnerTime: Long = 432000L): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCBurnerPublicKey(override val text: String, override val burnerTime: Long = 432000L): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCBurnerGroupPublicKey(val groupName: String, override val text: String, override val burnerTime: Long = 432000L, val neededBy: String): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCBurnerPublicKeyRequest(override val text: String, override val burnerTime: Long = 432000L, val requestedBy: String): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCBurnerLink(override val text: String, val preview: LinkPreview, override val burnerTime: Long = 432000L): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCBurnerImage(override val text: String, val image: String, override val burnerTime: Long = 432000L): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCBurnerFile(override val text: String, override val burnerTime: Long = 432000L): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCUnknown(val type: String? = null, override val text: String, val json: JsonElement, override val burnerTime: Long = 432000L): MsgContent()

  val cmdString: String
    get() = when (this) {
      is MCBurnerTimer -> "json ${json.encodeToString(this)}"
      is MCBurnerPublicKey -> "json ${json.encodeToString(this)}"
      is MCBurnerGroupPublicKey -> "json ${json.encodeToString(this)}"
      is MCBurnerPublicKeyRequest -> "json ${json.encodeToString(this)}"
      is MCBurnerText -> "json ${json.encodeToString(this)}"
      is MCBurnerLink -> "json ${json.encodeToString(this)}"
      is MCBurnerImage -> "json ${json.encodeToString(this)}"
      is MCBurnerFile -> "json ${json.encodeToString(this)} "
      is MCUnknown -> "json $json"
    }
}

@Serializable
class CIGroupInvitation(
  val groupId: Long,
  val groupMemberId: Long,
  val localDisplayName: String,
  val groupProfile: GroupProfile,
  val status: CIGroupInvitationStatus,
) {
  val text: String
    get() = String.format(
      generalGetString(R.string.group_invitation_item_description),
      groupProfile.displayName
    )

  companion object {
    fun getSample(
      groupId: Long = 1,
      groupMemberId: Long = 1,
      localDisplayName: String = "team",
      groupProfile: GroupProfile = GroupProfile.sampleData,
      status: CIGroupInvitationStatus = CIGroupInvitationStatus.Pending
    ): CIGroupInvitation =
      CIGroupInvitation(groupId = groupId, groupMemberId = groupMemberId, localDisplayName = localDisplayName, groupProfile = groupProfile, status = status)
  }
}

@Serializable
enum class CIGroupInvitationStatus {
  @SerialName("pending") Pending,
  @SerialName("accepted") Accepted,
  @SerialName("rejected") Rejected,
  @SerialName("expired") Expired;
}

object ContactBioInfoSerializer: KSerializer<ContactBioInfo> {
  override val descriptor: SerialDescriptor = buildSerialDescriptor("ContactBioInfo", PolymorphicKind.SEALED) {
    element("ContactBioExtra", buildClassSerialDescriptor("ContactBioExtra") {
      element<String>("tag")
      element<String>("notes")
      element<String>("burnerTime")
      element<String>("publicKey")
      element<String>("openKeyChainID")
    })
  }

  override fun deserialize(decoder: Decoder): ContactBioInfo {
    require(decoder is JsonDecoder)
    val json = decoder.decodeJsonElement()
    return if (json is JsonObject) {
      val tags = json["tag"]?.jsonPrimitive?.content ?: ""
      val notes = json["notes"]?.jsonPrimitive?.content ?: ""
      val burnerTime = Json.decodeFromString<Long>(json["burnerTime"].toString())
      val publicKey = json["publicKey"]?.jsonPrimitive?.content ?: ""
      val openKeyChainID = json["openKeyChainID"]?.jsonPrimitive?.content ?: ""
      ContactBioInfo.ContactBioExtra(tags, notes, burnerTime, publicKey, openKeyChainID)
    } else {
      ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
    }
  }

  override fun serialize(encoder: Encoder, value: ContactBioInfo) {
    require(encoder is JsonEncoder)
    val json = when (value) {
      is ContactBioInfo.ContactBioExtra ->
        buildJsonObject {
          put("tag", value.tag)
          put("notes", value.notes)
          put("burnerTime", json.encodeToJsonElement(value.burnerTime))
          put("publicKey", value.publicKey)
          put("openKeyChainID", value.openKeyChainID)
        }
    }
    encoder.encodeJsonElement(json)
  }
}

object MsgContentSerializer: KSerializer<MsgContent> {
  override val descriptor: SerialDescriptor = buildSerialDescriptor("MsgContent", PolymorphicKind.SEALED) {
    element("MCBurnerTime", buildClassSerialDescriptor("MCBurnerTime") {
      element<String>("text")
      element<String>("burnerTime")
    })
    element("MCBurnerPublicKey", buildClassSerialDescriptor("MCBurnerPublicKey") {
      element<String>("text")
      element<String>("burnerTime")
    })
    element("MCBurnerGroupPublicKey", buildClassSerialDescriptor("MCBurnerGroupPublicKey") {
      element<String>("groupName")
      element<String>("text")
      element<String>("neededBy")
      element<String>("burnerTime")
    })
    element("MCBurnerPublicKeyRequest", buildClassSerialDescriptor("MCBurnerPublicKeyRequest") {
      element<String>("text")
      element<String>("burnerTime")
      element<String>("requestedBy")
    })
    element("MCBurnerText", buildClassSerialDescriptor("MCBurnerText") {
      element<String>("text")
      element<String>("burnerTimer")
    })
    element("MCBurnerLink", buildClassSerialDescriptor("MCBurnerLink") {
      element<String>("text")
      element<String>("preview")
      element<String>("burnerTimer")
    })
    element("MCBurnerImage", buildClassSerialDescriptor("MCBurnerImage") {
      element<String>("text")
      element<String>("image")
      element<String>("burnerTimer")
    })
    element("MCBurnerFile", buildClassSerialDescriptor("MCBurnerFile") {
      element<String>("text")
      element<String>("burnerTimer")
    })
    element("MCUnknown", buildClassSerialDescriptor("MCUnknown"))
  }

  override fun deserialize(decoder: Decoder): MsgContent {
    require(decoder is JsonDecoder)
    val json = decoder.decodeJsonElement()
    return if (json is JsonObject) {
      if ("type" in json) {
        val t = json["type"]?.jsonPrimitive?.content ?: ""
        val groupName = json["groupName"]?.jsonPrimitive?.content ?: ""
        val requestedBy = json["requestedBy"]?.jsonPrimitive?.content ?: ""
        val neededBy = json["neededBy"]?.jsonPrimitive?.content ?: ""
        val text = json["text"]?.jsonPrimitive?.content ?: generalGetString(R.string.unknown_message_format)
        val burnerTime = Json.decodeFromString<Long>(json["burnerTime"].toString())
        when (t) {
          "burnerText" -> MsgContent.MCBurnerText(text, burnerTime)
          "burnerLink" -> {
            val preview = Json.decodeFromString<LinkPreview>(json["preview"].toString())
            MsgContent.MCBurnerLink(text, preview, burnerTime)
          }
          "burnerImage" -> {
            val image = json["image"]?.jsonPrimitive?.content ?: "unknown message format"
            MsgContent.MCBurnerImage(text, image, burnerTime)
          }
          "burnerTime" -> {
            MsgContent.MCBurnerTimer(text, burnerTime)
          }
          "burnerPublicKey" -> {
            MsgContent.MCBurnerPublicKey(text, burnerTime)
          }
          "burnerGroupPublicKey" -> {
            MsgContent.MCBurnerGroupPublicKey(groupName, text, burnerTime, requestedBy)
          }
          "burnerPublicKeyRequest" -> {
            MsgContent.MCBurnerPublicKeyRequest(text, burnerTime, neededBy)
          }
          "burnerFile" -> MsgContent.MCBurnerFile(text, burnerTime)
          else -> MsgContent.MCUnknown(t, text, json)
        }
      } else {
        MsgContent.MCUnknown(text = generalGetString(R.string.invalid_message_format), json = json)
      }
    } else {
      MsgContent.MCUnknown(text = generalGetString(R.string.invalid_message_format), json = json)
    }
  }

  override fun serialize(encoder: Encoder, value: MsgContent) {
    require(encoder is JsonEncoder)
    val json = when (value) {
      is MsgContent.MCBurnerText ->
        buildJsonObject {
          put("type", "burnerText")
          put("text", value.text)
          put("burnerTime", value.burnerTime)
        }
      is MsgContent.MCBurnerLink ->
        buildJsonObject {
          put("type", "burnerLink")
          put("text", value.text)
          put("preview", json.encodeToJsonElement(value.preview))
          put("burnerTime", json.encodeToJsonElement(value.burnerTime))
        }
      is MsgContent.MCBurnerImage ->
        buildJsonObject {
          put("type", "burnerImage")
          put("text", value.text)
          put("image", value.image)
          put("burnerTime", json.encodeToJsonElement(value.burnerTime))
        }
      is MsgContent.MCBurnerFile ->
        buildJsonObject {
          put("type", "burnerFile")
          put("text", value.text)
          put("burnerTime", json.encodeToJsonElement(value.burnerTime))
        }
      is MsgContent.MCBurnerTimer ->
        buildJsonObject {
          put("type", "burnerTime")
          put("text", value.text)
          put("burnerTime", json.encodeToJsonElement(value.burnerTime))
        }
      is MsgContent.MCBurnerPublicKey ->
        buildJsonObject {
          put("type", "burnerPublicKey")
          put("text", value.text)
          put("burnerTime", json.encodeToJsonElement(value.burnerTime))
        }
      is MsgContent.MCBurnerGroupPublicKey ->
        buildJsonObject {
          put("type", "burnerGroupPublicKey")
          put("groupName", value.groupName)
          put("text", value.text)
          put("neededBy", value.neededBy)
          put("burnerTime", json.encodeToJsonElement(value.burnerTime))
        }
      is MsgContent.MCBurnerPublicKeyRequest ->
        buildJsonObject {
          put("type", "burnerPublicKeyRequest")
          put("text", value.text)
          put("requestedBy", value.requestedBy)
          put("burnerTime", json.encodeToJsonElement(value.burnerTime))
        }
      is MsgContent.MCUnknown -> value.json
    }
    encoder.encodeJsonElement(json)
  }
}

@Serializable
class FormattedText(val text: String, val format: Format? = null) {
  val link: String? = when (format) {
    is Format.Uri -> text
    is Format.Email -> "mailto:$text"
    is Format.Phone -> "tel:$text"
    else -> null
  }
}

@Serializable
sealed class Format {
  @Serializable @SerialName("bold") class Bold: Format()
  @Serializable @SerialName("italic") class Italic: Format()
  @Serializable @SerialName("strikeThrough") class StrikeThrough: Format()
  @Serializable @SerialName("snippet") class Snippet: Format()
  @Serializable @SerialName("secret") class Secret: Format()
  @Serializable @SerialName("colored") class Colored(val color: FormatColor): Format()
  @Serializable @SerialName("uri") class Uri: Format()
  @Serializable @SerialName("email") class Email: Format()
  @Serializable @SerialName("phone") class Phone: Format()

  val style: SpanStyle
    @Composable get() = when (this) {
      is Bold -> SpanStyle(fontWeight = FontWeight.Bold)
      is Italic -> SpanStyle(fontStyle = FontStyle.Italic)
      is StrikeThrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
      is Snippet -> SpanStyle(fontFamily = FontFamily.Monospace)
      is Secret -> SpanStyle(color = Color.Transparent, background = SecretColor)
      is Colored -> SpanStyle(color = this.color.uiColor)
      is Uri -> linkStyle
      is Email -> linkStyle
      is Phone -> linkStyle
    }

  companion object {
    val linkStyle @Composable get() = SpanStyle(color = MaterialTheme.colors.primary, textDecoration = TextDecoration.Underline)
  }
}

@Serializable
enum class FormatColor(val color: String) {
  red("red"),
  green("green"),
  blue("blue"),
  yellow("yellow"),
  cyan("cyan"),
  magenta("magenta"),
  black("black"),
  white("white");

  val uiColor: Color
    @Composable get() = when (this) {
      red -> Color.Red
      green -> SimplexGreen
      blue -> SimplexBlue
      yellow -> Color.Yellow
      cyan -> Color.Cyan
      magenta -> Color.Magenta
      black -> MaterialTheme.colors.onBackground
      white -> MaterialTheme.colors.onBackground
    }
}

@Serializable
class SndFileTransfer() {}

@Serializable
class RcvFileTransfer() {}

@Serializable
class FileTransferMeta() {}

@Serializable
enum class CICallStatus {
  @SerialName("pending") Pending,
  @SerialName("missed") Missed,
  @SerialName("rejected") Rejected,
  @SerialName("accepted") Accepted,
  @SerialName("negotiated") Negotiated,
  @SerialName("progress") Progress,
  @SerialName("ended") Ended,
  @SerialName("error") Error;

  fun text(sec: Int): String = when (this) {
    Pending -> generalGetString(R.string.callstatus_calling)
    Missed -> generalGetString(R.string.callstatus_missed)
    Rejected -> generalGetString(R.string.callstatus_rejected)
    Accepted -> generalGetString(R.string.callstatus_accepted)
    Negotiated -> generalGetString(R.string.callstatus_connecting)
    Progress -> generalGetString(R.string.callstatus_in_progress)
    Ended -> String.format(generalGetString(R.string.callstatus_ended), duration(sec))
    Error -> generalGetString(R.string.callstatus_error)
  }

  fun duration(sec: Int): String = "%02d:%02d".format(sec / 60, sec % 60)
}

@Serializable
sealed class MsgErrorType() {
  @Serializable @SerialName("msgSkipped") class MsgSkipped(val fromMsgId: Long, val toMsgId: Long): MsgErrorType()
  @Serializable @SerialName("msgBadId") class MsgBadId(val msgId: Long): MsgErrorType()
  @Serializable @SerialName("msgBadHash") class MsgBadHash(): MsgErrorType()
  @Serializable @SerialName("msgDuplicate") class MsgDuplicate(): MsgErrorType()

  val text: String
    get() = when (this) {
      is MsgSkipped -> String.format(generalGetString(R.string.integrity_msg_skipped), toMsgId - fromMsgId + 1)
      is MsgBadHash -> generalGetString(R.string.integrity_msg_bad_hash) // not used now
      is MsgBadId -> generalGetString(R.string.integrity_msg_bad_id) // not used now
      is MsgDuplicate -> generalGetString(R.string.integrity_msg_duplicate) // not used now
    }
}

@Serializable
sealed class RcvGroupEvent() {
  @Serializable @SerialName("memberAdded") class MemberAdded(val groupMemberId: Long, val profile: Profile): RcvGroupEvent()
  @Serializable @SerialName("memberConnected") class MemberConnected(): RcvGroupEvent()
  @Serializable @SerialName("memberLeft") class MemberLeft(): RcvGroupEvent()
  @Serializable @SerialName("memberRole") class MemberRole(val groupMemberId: Long, val profile: Profile, val role: GroupMemberRole): RcvGroupEvent()
  @Serializable @SerialName("userRole") class UserRole(val role: GroupMemberRole): RcvGroupEvent()
  @Serializable @SerialName("memberDeleted") class MemberDeleted(val groupMemberId: Long, val profile: Profile): RcvGroupEvent()
  @Serializable @SerialName("userDeleted") class UserDeleted(): RcvGroupEvent()
  @Serializable @SerialName("groupDeleted") class GroupDeleted(): RcvGroupEvent()
  @Serializable @SerialName("groupUpdated") class GroupUpdated(val groupProfile: GroupProfile): RcvGroupEvent()

  val text: String
    get() = when (this) {
      is MemberAdded -> String.format(generalGetString(R.string.rcv_group_event_member_added), if (profile.fullName != "") profile.fullName else profile.displayName)
      is MemberConnected -> generalGetString(R.string.rcv_group_event_member_connected)
      is MemberLeft -> generalGetString(R.string.rcv_group_event_member_left)
      is UserRole -> String.format(generalGetString(R.string.your_member_role), role.text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() })
      is MemberDeleted -> String.format(generalGetString(R.string.rcv_group_event_member_deleted), if (profile.fullName != "") profile.fullName else profile.displayName)
      is UserDeleted -> generalGetString(R.string.rcv_group_event_user_deleted)
      is GroupDeleted -> generalGetString(R.string.rcv_group_event_group_deleted)
      is GroupUpdated -> generalGetString(R.string.rcv_group_event_updated_group_profile)
      is MemberRole -> ""
    }
}

@Serializable
sealed class SndGroupEvent() {
  @Serializable @SerialName("memberRole") class MemberRole(val groupMemberId: Long, val profile: Profile, val role: GroupMemberRole): SndGroupEvent()
  @Serializable @SerialName("userRole") class UserRole(val role: GroupMemberRole): SndGroupEvent()
  @Serializable @SerialName("memberDeleted") class MemberDeleted(val groupMemberId: Long, val profile: Profile): SndGroupEvent()
  @Serializable @SerialName("userLeft") class UserLeft(): SndGroupEvent()
  @Serializable @SerialName("groupUpdated") class GroupUpdated(val groupProfile: GroupProfile): SndGroupEvent()

  val text: String
    get() = when (this) {
      is MemberRole -> ""
      is UserRole -> String.format(generalGetString(R.string.your_member_role), role.text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() })
      is MemberDeleted -> String.format(generalGetString(R.string.snd_group_event_member_deleted), if (profile.fullName != "") profile.fullName else profile.displayName)
      is UserLeft -> generalGetString(R.string.snd_group_event_user_left)
      is GroupUpdated -> generalGetString(R.string.snd_group_event_group_profile_updated)
    }
}

sealed class ChatItemTTL: Comparable<ChatItemTTL?> {
  object Day: ChatItemTTL()
  object Week: ChatItemTTL()
  object Month: ChatItemTTL()
  data class Seconds(val secs: Long): ChatItemTTL()
  object None: ChatItemTTL()

  override fun compareTo(other: ChatItemTTL?): Int = (seconds ?: Long.MAX_VALUE).compareTo(other?.seconds ?: Long.MAX_VALUE)

  val seconds: Long?
    get() =
      when (this) {
        is None -> null
        is Day -> 86400L
        is Week -> 7 * 86400L
        is Month -> 30 * 86400L
        is Seconds -> secs
      }

  companion object {
    fun fromSeconds(seconds: Long?): ChatItemTTL =
      when (seconds) {
        null -> None
        86400L -> Day
        7 * 86400L -> Week
        30 * 86400L -> Month
        else -> Seconds(seconds)
      }
  }
}
*/


package chat.echo.app.model

import android.app.Activity
import android.content.*
import android.net.Uri
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import chat.echo.app.R
import chat.echo.app.ui.theme.*
import chat.echo.app.views.call.*
import chat.echo.app.views.chat.ComposeState
import chat.echo.app.views.chatlist.SortChat
import chat.echo.app.views.helpers.*
import chat.echo.app.views.helpers.localcontact.LocalDatabaseViewModel
import chat.echo.app.views.navbar.BottomBarView
import chat.echo.app.views.onboarding.OnboardingStage
import chat.echo.app.views.usersettings.NotificationPreviewMode
import chat.echo.app.views.usersettings.NotificationsMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.openintents.openpgp.util.OpenPgpServiceConnection
import java.io.File
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/*
 * Without this annotation an animation from ChatList to ChatView has 1 frame per the whole animation. Don't delete it
 * */
@Stable
class ChatModel(val controller: ChatController) {
  val onboardingStage = mutableStateOf<OnboardingStage?>(null)
  val currentUser = mutableStateOf<User?>(null)
  val users = mutableStateListOf<UserInfo>()
  val userCreated = mutableStateOf<Boolean?>(null)
  val chatRunning = mutableStateOf<Boolean?>(null)
  val chatDbChanged = mutableStateOf<Boolean>(false)
  val chatDbEncrypted = mutableStateOf<Boolean?>(false)
  val chatDbStatus = mutableStateOf<DBMigrationResult?>(null)
  val chatDbDeleted = mutableStateOf(false)
  val chats = mutableStateListOf<Chat>()
  val currentPageRoute = mutableStateOf<String>(BottomBarView.Contacts.route)
  val currentPageTitle = mutableStateOf<String>(BottomBarView.Contacts.title)
  val isImport = mutableStateOf<Boolean?>(false)
  val isExport = mutableStateOf<Boolean?>(false)
  val isSetupCompleted = mutableStateOf<Boolean>(false)
  val isSigningCompleted = mutableStateOf(false)
  val isLoadingCompleted = mutableStateOf(false)
  val isInternetAvailable = mutableStateOf<Boolean?>(null)
  val isChatRefresh = mutableStateOf<Boolean>(false)
  val activeChat = mutableStateOf<Chat?>(null)
  val userAuthorized = mutableStateOf(false)
  val selectedGroupContacts = mutableStateOf(mutableStateListOf<Long>())
  val isGroupCreated = mutableStateOf(false)
  val isPublicKeyRefreshed = mutableStateOf(false)
  var mServiceConnection: OpenPgpServiceConnection? = null
  var activity: Activity? = null
  var localContactViewModel: LocalDatabaseViewModel? = null

  //Search
  val showSearchView = mutableStateOf(false)
  val search = mutableStateOf(
    TextFieldValue(
      text = "",
      selection = TextRange(0)
    )
  )

  //Sort Chat Mode
  val sortChatMode = mutableStateOf(SortChat.SortByTime)

  // map of connections network statuses, key is agent connection id
  val networkStatuses = mutableStateMapOf<String, NetworkStatus>()

  // current chat
  val chatId = mutableStateOf<String?>(null)
  val chatItems = mutableStateListOf<ChatItem>()
  val groupMembers = mutableStateListOf<GroupMember>()
  val terminalItems = mutableStateListOf<TerminalItem>()
  val userAddress = mutableStateOf<UserContactLinkRec?>(null)

  // Allows to temporary save servers that are being edited on multiple screens
  val userSMPServersUnsaved = mutableStateOf<(List<ServerCfg>)?>(null)
  val chatItemTTL = mutableStateOf<ChatItemTTL>(ChatItemTTL.None)

  // set when app opened from external intent
  val clearOverlays = mutableStateOf<Boolean>(false)

  // set when app is opened via contact or invitation URI
  val appOpenUrl = mutableStateOf<Uri?>(null)

  // preferences
  val notificationsMode = mutableStateOf(NotificationsMode.default)
  var notificationPreviewMode = mutableStateOf(NotificationPreviewMode.default)
  val performLA = mutableStateOf(false)
  val showAdvertiseLAUnavailableAlert = mutableStateOf(false)
  var incognito = mutableStateOf(false)

  // current WebRTC call
  val callManager = CallManager(this)
  val callInvitations = mutableStateMapOf<String, RcvCallInvitation>()
  val activeCallInvitation = mutableStateOf<RcvCallInvitation?>(null)
  val activeCall = mutableStateOf<Call?>(null)
  val callCommand = mutableStateOf<WCallCommand?>(null)
  val showCallView = mutableStateOf(false)
  val switchingCall = mutableStateOf(false)

  // currently showing QR code
  val connReqInv = mutableStateOf(null as String?)
  var draft = mutableStateOf(null as ComposeState?)
  var draftChatId = mutableStateOf(null as String?)

  // working with external intents
  val sharedContent = mutableStateOf(null as SharedContent?)
  val filesToDelete = mutableSetOf<File>()
  val simplexLinkMode = mutableStateOf(controller.appPrefs.simplexLinkMode.get())
  val chatInfo = mutableStateOf(null as ChatInfo?)
  val contactInfo = mutableStateOf(null as Pair<ConnectionStats, Profile?>?)

  //receiver
  val intentFilter = IntentFilter("android.hardware.usb.action.USB_STATE")
  val isUSBConnected = mutableStateOf(false)
  val broadcastReceiver = object: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      isUSBConnected.value = intent?.extras!!.getBoolean("connected")
    }
  }

  fun registerBroadcastReceiver(context: Context) {
    context.registerReceiver(broadcastReceiver, intentFilter)
  }

  fun unregisterBroadcastReceiver(context: Context) {
    try {
      context.unregisterReceiver(broadcastReceiver)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun getUser(userId: Long): User? = if (currentUser.value?.userId == userId) {
    currentUser.value
  } else {
    users.firstOrNull { it.user.userId == userId }?.user
  }

  private fun getUserIndex(user: User): Int =
    users.indexOfFirst { it.user.userId == user.userId }

  fun updateUser(user: User) {
    val i = getUserIndex(user)
    if (i != -1) {
      users[i] = users[i].copy(user = user)
    }
    if (currentUser.value?.userId == user.userId) {
      currentUser.value = user
    }
  }

  fun removeUser(user: User) {
    val i = getUserIndex(user)
    if (i != -1 && users[i].user.userId != currentUser.value?.userId) {
      users.removeAt(i)
    }
  }

  fun hasChat(id: String): Boolean = chats.firstOrNull { it.id == id } != null
  fun getChat(id: String): Chat? = chats.firstOrNull { it.id == id }
  fun getContactChat(contactId: Long): Chat? = chats.firstOrNull { it.chatInfo is ChatInfo.Direct && it.chatInfo.apiId == contactId }
  private fun getChatIndex(id: String): Int = chats.indexOfFirst { it.id == id }
  fun addChat(chat: Chat) = chats.add(index = 0, chat)

  fun updateChatInfo(cInfo: ChatInfo) {
    val i = getChatIndex(cInfo.id)
    if (i >= 0) chats[i] = chats[i].copy(chatInfo = cInfo)
  }

  fun updateContactConnection(contactConnection: PendingContactConnection) = updateChat(ChatInfo.ContactConnection(contactConnection))

  fun updateContact(contact: Contact) = updateChat(ChatInfo.Direct(contact), addMissing = contact.directOrUsed)

  fun updateGroup(groupInfo: GroupInfo) = updateChat(ChatInfo.Group(groupInfo))

  private fun updateChat(cInfo: ChatInfo, addMissing: Boolean = true) {
    if (hasChat(cInfo.id)) {
      updateChatInfo(cInfo)
    } else if (addMissing) {
      addChat(Chat(chatInfo = cInfo, chatItems = arrayListOf()))
    }
  }

  fun updateChats(newChats: List<Chat>) {
    chats.clear()
    chats.addAll(newChats)
    val cId = chatId.value
    // If chat is null, it was deleted in background after apiGetChats call
    if (cId != null && getChat(cId) == null) {
      chatId.value = null
    }
  }

  fun replaceChat(id: String, chat: Chat) {
    val i = getChatIndex(id)
    if (i >= 0) {
      chats[i] = chat
    } else {
      // invalid state, correcting
      chats.add(index = 0, chat)
    }
  }

  suspend fun addChatItem(cInfo: ChatInfo, cItem: ChatItem) {
    // update previews
    val i = getChatIndex(cInfo.id)
    val chat: Chat
    if (i >= 0) {
      chat = chats[i]
      chats[i] = chat.copy(
        chatItems = arrayListOf(cItem),
        chatStats =
        if (cItem.meta.itemStatus is CIStatus.RcvNew) {
          val minUnreadId = if (chat.chatStats.minUnreadItemId == 0L) cItem.id else chat.chatStats.minUnreadItemId
          increaseUnreadCounter(currentUser.value!!)
          chat.chatStats.copy(unreadCount = chat.chatStats.unreadCount + 1, minUnreadItemId = minUnreadId)
        } else
          chat.chatStats
      )
      if (i > 0) {
        popChat_(i)
      }
    } else {
      addChat(Chat(chatInfo = cInfo, chatItems = arrayListOf(cItem)))
    }
    // add to current chat
    if (chatId.value == cInfo.id) {
      withContext(Dispatchers.Main) {
        // Prevent situation when chat item already in the list received from backend
        if (chatItems.none { it.id == cItem.id }) {
          if (chatItems.lastOrNull()?.id == ChatItem.TEMP_LIVE_CHAT_ITEM_ID) {
            chatItems.add(kotlin.math.max(0, chatItems.lastIndex), cItem)
          } else {
            chatItems.add(cItem)
          }
        }
      }
    }
  }

  suspend fun upsertChatItem(cInfo: ChatInfo, cItem: ChatItem): Boolean {
    // update previews
    val i = getChatIndex(cInfo.id)
    val chat: Chat
    val res: Boolean
    if (i >= 0) {
      chat = chats[i]
      val pItem = chat.chatItems.lastOrNull()
      if (pItem?.id == cItem.id) {
        chats[i] = chat.copy(chatItems = arrayListOf(cItem))
        if (pItem.isRcvNew && !cItem.isRcvNew) {
          // status changed from New to Read, update counter
          decreaseCounterInChat(cInfo.id)
        }
      }
      res = false
    } else {
      addChat(Chat(chatInfo = cInfo, chatItems = arrayListOf(cItem)))
      res = true
    }
    // update current chat
    return if (chatId.value == cInfo.id) {
      withContext(Dispatchers.Main) {
        val itemIndex = chatItems.indexOfFirst { it.id == cItem.id }
        if (itemIndex >= 0) {
          chatItems[itemIndex] = cItem
          false
        } else {
          chatItems.add(cItem)
          true
        }
      }
    } else {
      res
    }
  }

  fun removeChatItem(cInfo: ChatInfo, cItem: ChatItem) {
    if (cItem.isRcvNew) {
      decreaseCounterInChat(cInfo.id)
    }
    // update previews
    val i = getChatIndex(cInfo.id)
    val chat: Chat
    if (i >= 0) {
      chat = chats[i]
      val pItem = chat.chatItems.lastOrNull()
      if (pItem?.id == cItem.id) {
        chats[i] = chat.copy(chatItems = arrayListOf(ChatItem.deletedItemDummy))
      }
    }
    // remove from current chat
    if (chatId.value == cInfo.id) {
      val itemIndex = chatItems.indexOfFirst { it.id == cItem.id }
      if (itemIndex >= 0) {
        AudioPlayer.stop(chatItems[itemIndex])
        chatItems.removeAt(itemIndex)
      }
    }
  }

  fun clearChat(cInfo: ChatInfo) {
    // clear preview
    val i = getChatIndex(cInfo.id)
    if (i >= 0) {
      decreaseUnreadCounter(currentUser.value!!, chats[i].chatStats.unreadCount)
      chats[i] = chats[i].copy(chatItems = arrayListOf(), chatStats = Chat.ChatStats(), chatInfo = cInfo)
    }
    // clear current chat
    if (chatId.value == cInfo.id) {
      chatItems.clear()
    }
  }

  fun updateCurrentUser(newProfile: Profile, preferences: FullChatPreferences? = null) {
    val current = currentUser.value ?: return
    val updated = current.copy(
      profile = newProfile.toLocalProfile(current.profile.profileId),
      fullPreferences = preferences ?: current.fullPreferences
    )
    val indexInUsers = users.indexOfFirst { it.user.userId == current.userId }
    if (indexInUsers != -1) {
      users[indexInUsers] = UserInfo(updated, users[indexInUsers].unreadCount)
    }
    currentUser.value = updated
  }

  suspend fun addLiveDummy(chatInfo: ChatInfo): ChatItem {
    val cItem = ChatItem.liveDummy(chatInfo is ChatInfo.Direct)
    withContext(Dispatchers.Main) {
      chatItems.add(cItem)
    }
    return cItem
  }

  fun removeLiveDummy() {
    if (chatItems.lastOrNull()?.id == ChatItem.TEMP_LIVE_CHAT_ITEM_ID) {
      chatItems.removeLast()
    }
  }

  fun markChatItemsRead(cInfo: ChatInfo, range: CC.ItemRange? = null, unreadCountAfter: Int? = null) {
    val markedRead = markItemsReadInCurrentChat(cInfo, range)
    // update preview
    val chatIdx = getChatIndex(cInfo.id)
    if (chatIdx >= 0) {
      val chat = chats[chatIdx]
      val lastId = chat.chatItems.lastOrNull()?.id
      if (lastId != null) {
        val unreadCount = unreadCountAfter ?: if (range != null) chat.chatStats.unreadCount - markedRead else 0
        decreaseUnreadCounter(currentUser.value!!, chat.chatStats.unreadCount - unreadCount)
        chats[chatIdx] = chat.copy(
          chatStats = chat.chatStats.copy(
            unreadCount = unreadCount,
            // Can't use minUnreadItemId currently since chat items can have unread items between read items
            //minUnreadItemId = if (range != null) kotlin.math.max(chat.chatStats.minUnreadItemId, range.to + 1) else lastId + 1
          )
        )
      }
    }
  }

  private fun markItemsReadInCurrentChat(cInfo: ChatInfo, range: CC.ItemRange? = null): Int {
    var markedRead = 0
    if (chatId.value == cInfo.id) {
      var i = 0
      while (i < chatItems.count()) {
        val item = chatItems[i]
        if (item.meta.itemStatus is CIStatus.RcvNew && (range == null || (range.from <= item.id && item.id <= range.to))) {
          val newItem = item.withStatus(CIStatus.RcvRead())
          chatItems[i] = newItem
          if (newItem.meta.itemLive != true && newItem.meta.itemTimed?.ttl != null) {
            chatItems[i] = newItem.copy(
              meta = newItem.meta.copy(
                itemTimed = newItem.meta.itemTimed.copy(
                  deleteAt = Clock.System.now() + newItem.meta.itemTimed.ttl.toDuration(DurationUnit.SECONDS)
                )
              )
            )
          }
          markedRead++
        }
        i += 1
      }
    }
    return markedRead
  }

  private fun decreaseCounterInChat(chatId: ChatId) {
    val chatIndex = getChatIndex(chatId)
    if (chatIndex == -1) return
    val chat = chats[chatIndex]
    val unreadCount = kotlin.math.max(chat.chatStats.unreadCount - 1, 0)
    decreaseUnreadCounter(currentUser.value!!, chat.chatStats.unreadCount - unreadCount)
    chats[chatIndex] = chat.copy(
      chatStats = chat.chatStats.copy(
        unreadCount = unreadCount,
      )
    )
  }

  fun increaseUnreadCounter(user: User) {
    changeUnreadCounter(user, 1)
  }

  fun decreaseUnreadCounter(user: User, by: Int = 1) {
    changeUnreadCounter(user, -by)
  }

  private fun changeUnreadCounter(user: User, by: Int) {
    val i = users.indexOfFirst { it.user.userId == user.userId }
    if (i != -1) {
      users[i] = UserInfo(user, users[i].unreadCount + by)
    }
  }

  //  func popChat(_ id: String) {
  //    if let i = getChatIndex(id) {
  //      popChat_(i)
  //    }
  //  }
  private fun popChat_(i: Int) {
    val chat = chats.removeAt(i)
    chats.add(index = 0, chat)
  }

  fun dismissConnReqView(id: String) {
    if (connReqInv.value == null) return
    val info = getChat(id)?.chatInfo as? ChatInfo.ContactConnection ?: return
    if (info.contactConnection.connReqInv == connReqInv.value) {
      connReqInv.value = null
      ModalManager.shared.closeModals()
    }
  }

  fun removeChat(id: String) {
    chats.removeAll { it.id == id }
  }

  fun upsertGroupMember(groupInfo: GroupInfo, member: GroupMember): Boolean {
    // user member was updated
    if (groupInfo.membership.groupMemberId == member.groupMemberId) {
      updateGroup(groupInfo)
      return false
    }
    // update current chat
    return if (chatId.value == groupInfo.id) {
      val memberIndex = groupMembers.indexOfFirst { it.id == member.id }
      if (memberIndex >= 0) {
        groupMembers[memberIndex] = member
        false
      } else {
        groupMembers.add(member)
        true
      }
    } else {
      false
    }
  }

  fun setContactNetworkStatus(contact: Contact, status: NetworkStatus) {
    networkStatuses[contact.activeConn.agentConnId] = status
  }

  fun contactNetworkStatus(contact: Contact): NetworkStatus =
    networkStatuses[contact.activeConn.agentConnId] ?: NetworkStatus.Unknown()

  fun addTerminalItem(item: TerminalItem) {
    if (terminalItems.size >= 500) {
      terminalItems.removeAt(0)
    }
    terminalItems.add(item)
  }
}

enum class ChatType(val type: String) {
  Direct("@"),
  Group("#"),
  ContactRequest("<@"),
  ContactConnection(":");
}

@Serializable
data class User(
  val userId: Long,
  val userContactId: Long,
  val localDisplayName: String,
  val profile: LocalProfile,
  val fullPreferences: FullChatPreferences,
  val activeUser: Boolean,
  val showNtfs: Boolean,
  val viewPwdHash: UserPwdHash?
): NamedChat {
  override val displayName: String get() = profile.displayName
  override val fullName: String get() = profile.fullName
  override val image: String? get() = profile.image
  override val localAlias: String = ""
  val hidden: Boolean = viewPwdHash != null
  val showNotifications: Boolean = activeUser || showNtfs
  var openKeyChainID: String = ""
    get() = field
    set(value) {
      field = value
    }

  companion object {
    val sampleData = User(
      userId = 1,
      userContactId = 1,
      localDisplayName = "alice",
      profile = LocalProfile.sampleData,
      fullPreferences = FullChatPreferences.sampleData,
      activeUser = true,
      showNtfs = true,
      viewPwdHash = null,
    )
  }
}

@Serializable
data class UserPwdHash(
  val hash: String,
  val salt: String
)

@Serializable
data class UserInfo(
  val user: User,
  val unreadCount: Int
) {
  companion object {
    val sampleData = UserInfo(
      user = User.sampleData,
      unreadCount = 1
    )
  }
}

typealias ChatId = String

interface NamedChat {
  val displayName: String
  val fullName: String
  val image: String?
  val localAlias: String
  val chatViewName: String
    get() = localAlias.ifEmpty { displayName + (if (fullName == "" || fullName == displayName) "" else " / $fullName") }
}

interface SomeChat {
  val chatType: ChatType
  val localDisplayName: String
  val id: ChatId
  val apiId: Long
  val ready: Boolean
  val sendMsgEnabled: Boolean
  val ntfsEnabled: Boolean
  val incognito: Boolean
  fun featureEnabled(feature: ChatFeature): Boolean
  val timedMessagesTTL: Int?
  val createdAt: Instant
  val updatedAt: Instant
}

@Serializable @Stable
data class Chat(
  val chatInfo: ChatInfo,
  val chatItems: List<ChatItem>,
  val chatStats: ChatStats = ChatStats(),
) {
  val userCanSend: Boolean
    get() = when (chatInfo) {
      is ChatInfo.Direct -> true
      is ChatInfo.Group -> {
        val m = chatInfo.groupInfo.membership
        m.memberActive && m.memberRole >= GroupMemberRole.Member
      }
      else -> false
    }
  val userIsObserver: Boolean
    get() = when (chatInfo) {
      is ChatInfo.Group -> {
        val m = chatInfo.groupInfo.membership
        m.memberActive && m.memberRole == GroupMemberRole.Observer
      }
      else -> false
    }
  val id: String get() = chatInfo.id

  @Serializable
  data class ChatStats(val unreadCount: Int = 0, val minUnreadItemId: Long = 0, val unreadChat: Boolean = false)

  companion object {
    val sampleData = Chat(
      chatInfo = ChatInfo.Direct.sampleData,
      chatItems = arrayListOf(ChatItem.getSampleData())
    )
  }
}

@Serializable
sealed class ChatInfo: SomeChat, NamedChat {
  @Serializable @SerialName("direct")
  data class Direct(val contact: Contact): ChatInfo() {
    override val chatType get() = ChatType.Direct
    override val localDisplayName get() = contact.localDisplayName
    override val id get() = contact.id
    override val apiId get() = contact.apiId
    override val ready get() = contact.ready
    override val sendMsgEnabled get() = contact.sendMsgEnabled
    override val ntfsEnabled get() = contact.ntfsEnabled
    override val incognito get() = contact.incognito
    override fun featureEnabled(feature: ChatFeature) = contact.featureEnabled(feature)
    override val timedMessagesTTL: Int? get() = contact.timedMessagesTTL
    override val createdAt get() = contact.createdAt
    override val updatedAt get() = contact.updatedAt
    override val displayName get() = contact.displayName
    override val fullName get() = contact.fullName
    override val image get() = contact.image
    override val localAlias: String get() = contact.localAlias

    companion object {
      val sampleData = Direct(Contact.sampleData)
    }
  }

  @Serializable @SerialName("group")
  data class Group(val groupInfo: GroupInfo): ChatInfo() {
    override val chatType get() = ChatType.Group
    override val localDisplayName get() = groupInfo.localDisplayName
    override val id get() = groupInfo.id
    override val apiId get() = groupInfo.apiId
    override val ready get() = groupInfo.ready
    override val sendMsgEnabled get() = groupInfo.sendMsgEnabled
    override val ntfsEnabled get() = groupInfo.ntfsEnabled
    override val incognito get() = groupInfo.incognito
    override fun featureEnabled(feature: ChatFeature) = groupInfo.featureEnabled(feature)
    override val timedMessagesTTL: Int? get() = groupInfo.timedMessagesTTL
    override val createdAt get() = groupInfo.createdAt
    override val updatedAt get() = groupInfo.updatedAt
    override val displayName get() = groupInfo.displayName
    override val fullName get() = groupInfo.fullName
    override val image get() = groupInfo.image
    override val localAlias get() = groupInfo.localAlias

    companion object {
      val sampleData = Group(GroupInfo.sampleData)
    }
  }

  @Serializable @SerialName("contactRequest")
  class ContactRequest(val contactRequest: UserContactRequest): ChatInfo() {
    override val chatType get() = ChatType.ContactRequest
    override val localDisplayName get() = contactRequest.localDisplayName
    override val id get() = contactRequest.id
    override val apiId get() = contactRequest.apiId
    override val ready get() = contactRequest.ready
    override val sendMsgEnabled get() = contactRequest.sendMsgEnabled
    override val ntfsEnabled get() = contactRequest.ntfsEnabled
    override val incognito get() = contactRequest.incognito
    override fun featureEnabled(feature: ChatFeature) = contactRequest.featureEnabled(feature)
    override val timedMessagesTTL: Int? get() = contactRequest.timedMessagesTTL
    override val createdAt get() = contactRequest.createdAt
    override val updatedAt get() = contactRequest.updatedAt
    override val displayName get() = contactRequest.displayName
    override val fullName get() = contactRequest.fullName
    override val image get() = contactRequest.image
    override val localAlias get() = contactRequest.localAlias

    companion object {
      val sampleData = ContactRequest(UserContactRequest.sampleData)
    }
  }

  @Serializable @SerialName("contactConnection")
  class ContactConnection(val contactConnection: PendingContactConnection): ChatInfo() {
    override val chatType get() = ChatType.ContactConnection
    override val localDisplayName get() = contactConnection.localDisplayName
    override val id get() = contactConnection.id
    override val apiId get() = contactConnection.apiId
    override val ready get() = contactConnection.ready
    override val sendMsgEnabled get() = contactConnection.sendMsgEnabled
    override val ntfsEnabled get() = contactConnection.incognito
    override val incognito get() = contactConnection.incognito
    override fun featureEnabled(feature: ChatFeature) = contactConnection.featureEnabled(feature)
    override val timedMessagesTTL: Int? get() = contactConnection.timedMessagesTTL
    override val createdAt get() = contactConnection.createdAt
    override val updatedAt get() = contactConnection.updatedAt
    override val displayName get() = contactConnection.displayName
    override val fullName get() = contactConnection.fullName
    override val image get() = contactConnection.image
    override val localAlias get() = contactConnection.localAlias

    companion object {
      fun getSampleData(status: ConnStatus = ConnStatus.New, viaContactUri: Boolean = false): ContactConnection =
        ContactConnection(PendingContactConnection.getSampleData(status, viaContactUri))
    }
  }

  @Serializable @SerialName("invalidJSON")
  class InvalidJSON(val json: String): ChatInfo() {
    override val chatType get() = ChatType.Direct
    override val localDisplayName get() = invalidChatName
    override val id get() = ""
    override val apiId get() = 0L
    override val ready get() = false
    override val sendMsgEnabled get() = false
    override val ntfsEnabled get() = false
    override val incognito get() = false
    override fun featureEnabled(feature: ChatFeature) = false
    override val timedMessagesTTL: Int? get() = null
    override val createdAt get() = Clock.System.now()
    override val updatedAt get() = Clock.System.now()
    override val displayName get() = invalidChatName
    override val fullName get() = invalidChatName
    override val image get() = null
    override val localAlias get() = ""

    companion object {
      private val invalidChatName = generalGetString(R.string.invalid_chat)
    }
  }
}

@Serializable
sealed class NetworkStatus {
  val statusString: String
    get() =
      when (this) {
        is Connected -> generalGetString(R.string.server_connected)
        is Error -> generalGetString(R.string.server_error)
        else -> generalGetString(R.string.server_connecting)
      }
  val statusExplanation: String
    get() =
      when (this) {
        is Connected -> generalGetString(R.string.connected_to_server_to_receive_messages_from_contact)
        is Error -> String.format(generalGetString(R.string.trying_to_connect_to_server_to_receive_messages_with_error), error)
        else -> generalGetString(R.string.trying_to_connect_to_server_to_receive_messages)
      }

  @Serializable @SerialName("unknown") class Unknown: NetworkStatus()
  @Serializable @SerialName("connected") class Connected: NetworkStatus()
  @Serializable @SerialName("disconnected") class Disconnected: NetworkStatus()
  @Serializable @SerialName("error") class Error(val error: String): NetworkStatus()
}

@Serializable
data class Contact(
  val contactId: Long,
  override val localDisplayName: String,
  val profile: LocalProfile,
  val activeConn: Connection,
  val viaGroup: Long? = null,
  val contactUsed: Boolean,
  val chatSettings: ChatSettings,
  val userPreferences: ChatPreferences,
  val mergedPreferences: ContactUserPreferences,
  override val createdAt: Instant,
  override val updatedAt: Instant
): SomeChat, NamedChat {
  override val chatType get() = ChatType.Direct
  override val id get() = "@$contactId"
  override val apiId get() = contactId
  override val ready get() = activeConn.connStatus == ConnStatus.Ready
  override val sendMsgEnabled get() = true
  override val ntfsEnabled get() = chatSettings.enableNtfs
  override val incognito get() = contactConnIncognito
  override fun featureEnabled(feature: ChatFeature): Boolean = when (feature) {
    ChatFeature.TimedMessages -> mergedPreferences.timedMessages.enabled.forUser
    ChatFeature.FullDelete -> mergedPreferences.fullDelete.enabled.forUser
    ChatFeature.Voice -> mergedPreferences.voice.enabled.forUser
    ChatFeature.Calls -> mergedPreferences.calls.enabled.forUser
  }

  override val timedMessagesTTL: Int? get() = with(mergedPreferences.timedMessages) { if (enabled.forUser) userPreference.pref.ttl else null }
  override val displayName get() = localAlias.ifEmpty { profile.displayName }
  override val fullName get() = profile.fullName
  override val image get() = profile.image
  override val localAlias get() = profile.localAlias
  val verified get() = activeConn.connectionCode != null
  val directOrUsed: Boolean
    get() =
      (activeConn.connLevel == 0 && !activeConn.viaGroupLink) || contactUsed
  val contactConnIncognito =
    activeConn.customUserProfileId != null

  fun allowsFeature(feature: ChatFeature): Boolean = when (feature) {
    ChatFeature.TimedMessages -> mergedPreferences.timedMessages.contactPreference.allow != FeatureAllowed.NO
    ChatFeature.FullDelete -> mergedPreferences.fullDelete.contactPreference.allow != FeatureAllowed.NO
    ChatFeature.Voice -> mergedPreferences.voice.contactPreference.allow != FeatureAllowed.NO
    ChatFeature.Calls -> mergedPreferences.calls.contactPreference.allow != FeatureAllowed.NO
    else -> true
  }

  fun userAllowsFeature(feature: ChatFeature): Boolean = when (feature) {
    ChatFeature.TimedMessages -> mergedPreferences.timedMessages.userPreference.pref.allow != FeatureAllowed.NO
    ChatFeature.FullDelete -> mergedPreferences.fullDelete.userPreference.pref.allow != FeatureAllowed.NO
    ChatFeature.Voice -> mergedPreferences.voice.userPreference.pref.allow != FeatureAllowed.NO
    ChatFeature.Calls -> mergedPreferences.calls.userPreference.pref.allow != FeatureAllowed.NO
    else -> true
  }

  companion object {
    val sampleData = Contact(
      contactId = 1,
      localDisplayName = "alice",
      profile = LocalProfile.sampleData,
      activeConn = Connection.sampleData,
      contactUsed = true,
      chatSettings = ChatSettings(true),
      userPreferences = ChatPreferences.sampleData,
      mergedPreferences = ContactUserPreferences.sampleData,
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now()
    )
  }
}

@Serializable
class ContactRef(
  val contactId: Long,
  val agentConnId: String,
  val connId: Long,
  var localDisplayName: String
) {
  val id: ChatId get() = "@$contactId"
}

@Serializable
class ContactSubStatus(
  val contact: Contact,
  val contactError: ChatError? = null
)

@Serializable
data class Connection(
  val connId: Long,
  val agentConnId: String,
  val connStatus: ConnStatus,
  val connLevel: Int,
  val viaGroupLink: Boolean,
  val customUserProfileId: Long? = null,
  val connectionCode: SecurityCode? = null
) {
  val id: ChatId get() = ":$connId"

  companion object {
    val sampleData = Connection(connId = 1, agentConnId = "abc", connStatus = ConnStatus.Ready, connLevel = 0, viaGroupLink = false, customUserProfileId = null)
  }
}

@Serializable
data class SecurityCode(val securityCode: String, val verifiedAt: Instant)

@Serializable
data class Profile(
  override val displayName: String,
  override val fullName: String,
  override val image: String? = null,
  override val localAlias: String = "",
  val preferences: ChatPreferences? = null
): NamedChat {
  val profileViewName: String
    get() {
      return if (fullName == "" || displayName == fullName) displayName else "$displayName ($fullName)"
    }

  fun toLocalProfile(profileId: Long): LocalProfile = LocalProfile(profileId, displayName, fullName, image, localAlias, preferences)

  companion object {
    val sampleData = Profile(
      displayName = "alice",
      fullName = "Alice"
    )
  }
}

@Serializable
class LocalProfile(
  val profileId: Long,
  override val displayName: String,
  override val fullName: String,
  override val image: String? = null,
  override val localAlias: String,
  val preferences: ChatPreferences? = null
): NamedChat {
  val profileViewName: String = localAlias.ifEmpty { if (fullName == "" || displayName == fullName) displayName else "$displayName ($fullName)" }

  fun toProfile(): Profile = Profile(displayName, fullName, image, localAlias, preferences)

  companion object {
    val sampleData = LocalProfile(
      profileId = 1L,
      displayName = "alice",
      fullName = "Alice",
      preferences = ChatPreferences.sampleData,
      localAlias = ""
    )
  }
}

@Serializable
class Group(
  val groupInfo: GroupInfo,
  var members: List<GroupMember>
)

@Serializable
data class GroupInfo(
  val groupId: Long,
  override val localDisplayName: String,
  val groupProfile: GroupProfile,
  val fullGroupPreferences: FullGroupPreferences,
  val membership: GroupMember,
  val hostConnCustomUserProfileId: Long? = null,
  val chatSettings: ChatSettings,
  override val createdAt: Instant,
  override val updatedAt: Instant,
  var isZeroKnowledge: Boolean = false,
): SomeChat, NamedChat {
  override val chatType get() = ChatType.Group
  override val id get() = "#$groupId"
  override val apiId get() = groupId
  override val ready get() = membership.memberActive
  override val sendMsgEnabled get() = membership.memberActive
  override val ntfsEnabled get() = chatSettings.enableNtfs
  override val incognito get() = membership.memberIncognito
  override fun featureEnabled(feature: ChatFeature) = when (feature) {
    ChatFeature.TimedMessages -> fullGroupPreferences.timedMessages.on
    ChatFeature.FullDelete -> fullGroupPreferences.fullDelete.on
    ChatFeature.Voice -> fullGroupPreferences.voice.on
    ChatFeature.Calls -> false
    else -> false
  }

  override val timedMessagesTTL: Int? get() = with(fullGroupPreferences.timedMessages) { if (on) ttl else null }
  override val displayName get() = groupProfile.displayName
  override val fullName get() = groupProfile.fullName
  override val image get() = groupProfile.image
  override val localAlias get() = ""
  val canEdit: Boolean
    get() = membership.memberRole == GroupMemberRole.Owner && membership.memberCurrent
  val canDelete: Boolean
    get() = membership.memberRole == GroupMemberRole.Owner || !membership.memberCurrent
  val canAddMembers: Boolean
    get() = membership.memberRole >= GroupMemberRole.Admin && membership.memberActive

  companion object {
    val sampleData = GroupInfo(
      groupId = 1,
      localDisplayName = "team",
      groupProfile = GroupProfile.sampleData,
      fullGroupPreferences = FullGroupPreferences.sampleData,
      membership = GroupMember.sampleData,
      hostConnCustomUserProfileId = null,
      chatSettings = ChatSettings(true),
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now()
    )
  }
}

@Serializable
data class GroupProfile(
  override val displayName: String,
  override val fullName: String,
  val description: String? = null,
  override val image: String? = null,
  override val localAlias: String = "",
  val groupPreferences: GroupPreferences? = null
): NamedChat {
  companion object {
    val sampleData = GroupProfile(
      displayName = "team",
      fullName = "My Team"
    )
  }
}

@Serializable
data class GroupMember(
  val groupMemberId: Long,
  val groupId: Long,
  val memberId: String,
  var memberRole: GroupMemberRole,
  var memberCategory: GroupMemberCategory,
  var memberStatus: GroupMemberStatus,
  var invitedBy: InvitedBy,
  val localDisplayName: String,
  val memberProfile: LocalProfile,
  val memberContactId: Long? = null,
  val memberContactProfileId: Long,
  var activeConn: Connection? = null
) {
  val id: String get() = "#$groupId @$groupMemberId"
  val displayName: String get() = memberProfile.localAlias.ifEmpty { memberProfile.displayName }
  val fullName: String get() = memberProfile.fullName
  val image: String? get() = memberProfile.image
  val verified get() = activeConn?.connectionCode != null
  val chatViewName: String
    get() = memberProfile.localAlias.ifEmpty { displayName + (if (fullName == "" || fullName == displayName) "" else " / $fullName") }
  val memberActive: Boolean
    get() = when (this.memberStatus) {
      GroupMemberStatus.MemRemoved -> false
      GroupMemberStatus.MemLeft -> false
      GroupMemberStatus.MemGroupDeleted -> false
      GroupMemberStatus.MemInvited -> false
      GroupMemberStatus.MemIntroduced -> false
      GroupMemberStatus.MemIntroInvited -> false
      GroupMemberStatus.MemAccepted -> false
      GroupMemberStatus.MemAnnounced -> false
      GroupMemberStatus.MemConnected -> true
      GroupMemberStatus.MemComplete -> true
      GroupMemberStatus.MemCreator -> true
    }
  val memberCurrent: Boolean
    get() = when (this.memberStatus) {
      GroupMemberStatus.MemRemoved -> false
      GroupMemberStatus.MemLeft -> false
      GroupMemberStatus.MemGroupDeleted -> false
      GroupMemberStatus.MemInvited -> false
      GroupMemberStatus.MemIntroduced -> true
      GroupMemberStatus.MemIntroInvited -> true
      GroupMemberStatus.MemAccepted -> true
      GroupMemberStatus.MemAnnounced -> true
      GroupMemberStatus.MemConnected -> true
      GroupMemberStatus.MemComplete -> true
      GroupMemberStatus.MemCreator -> true
    }

  fun canBeRemoved(groupInfo: GroupInfo): Boolean {
    val userRole = groupInfo.membership.memberRole
    return memberStatus != GroupMemberStatus.MemRemoved && memberStatus != GroupMemberStatus.MemLeft
        && userRole >= GroupMemberRole.Admin && userRole >= memberRole && groupInfo.membership.memberCurrent
  }

  fun canChangeRoleTo(groupInfo: GroupInfo): List<GroupMemberRole>? =
    if (!canBeRemoved(groupInfo)) null
    else groupInfo.membership.memberRole.let { userRole ->
      GroupMemberRole.values().filter { it <= userRole }
    }

  val memberIncognito = memberProfile.profileId != memberContactProfileId

  companion object {
    val sampleData = GroupMember(
      groupMemberId = 1,
      groupId = 1,
      memberId = "abcd",
      memberRole = GroupMemberRole.Member,
      memberCategory = GroupMemberCategory.InviteeMember,
      memberStatus = GroupMemberStatus.MemComplete,
      invitedBy = InvitedBy.IBUser(),
      localDisplayName = "alice",
      memberProfile = LocalProfile.sampleData,
      memberContactId = 1,
      memberContactProfileId = 1L,
      activeConn = Connection.sampleData
    )
  }
}

@Serializable
class GroupMemberRef(
  val groupMemberId: Long,
  val profile: Profile
)

@Serializable
enum class GroupMemberRole(val memberRole: String) {
  @SerialName("observer") Observer("observer"), // order matters in comparisons
  @SerialName("member") Member("member"),
  @SerialName("admin") Admin("admin"),
  @SerialName("owner") Owner("owner");

  val text: String
    get() = when (this) {
      Observer -> generalGetString(R.string.group_member_role_observer)
      Member -> generalGetString(R.string.group_member_role_member)
      Admin -> generalGetString(R.string.group_member_role_admin)
      Owner -> generalGetString(R.string.group_member_role_owner)
    }
}

@Serializable
enum class GroupMemberCategory {
  @SerialName("user") UserMember,
  @SerialName("invitee") InviteeMember,
  @SerialName("host") HostMember,
  @SerialName("pre") PreMember,
  @SerialName("post") PostMember;
}

@Serializable
enum class GroupMemberStatus {
  @SerialName("removed") MemRemoved,
  @SerialName("left") MemLeft,
  @SerialName("deleted") MemGroupDeleted,
  @SerialName("invited") MemInvited,
  @SerialName("introduced") MemIntroduced,
  @SerialName("intro-inv") MemIntroInvited,
  @SerialName("accepted") MemAccepted,
  @SerialName("announced") MemAnnounced,
  @SerialName("connected") MemConnected,
  @SerialName("complete") MemComplete,
  @SerialName("creator") MemCreator;

  val text: String
    get() = when (this) {
      MemRemoved -> generalGetString(R.string.group_member_status_removed)
      MemLeft -> generalGetString(R.string.group_member_status_left)
      MemGroupDeleted -> generalGetString(R.string.group_member_status_group_deleted)
      MemInvited -> generalGetString(R.string.group_member_status_invited)
      MemIntroduced -> generalGetString(R.string.group_member_status_introduced)
      MemIntroInvited -> generalGetString(R.string.group_member_status_intro_invitation)
      MemAccepted -> generalGetString(R.string.group_member_status_accepted)
      MemAnnounced -> generalGetString(R.string.group_member_status_announced)
      MemConnected -> generalGetString(R.string.group_member_status_connected)
      MemComplete -> generalGetString(R.string.group_member_status_complete)
      MemCreator -> generalGetString(R.string.group_member_status_creator)
    }
  val shortText: String
    get() = when (this) {
      MemRemoved -> generalGetString(R.string.group_member_status_removed)
      MemLeft -> generalGetString(R.string.group_member_status_left)
      MemGroupDeleted -> generalGetString(R.string.group_member_status_group_deleted)
      MemInvited -> generalGetString(R.string.group_member_status_invited)
      MemIntroduced -> generalGetString(R.string.group_member_status_connecting)
      MemIntroInvited -> generalGetString(R.string.group_member_status_connecting)
      MemAccepted -> generalGetString(R.string.group_member_status_connecting)
      MemAnnounced -> generalGetString(R.string.group_member_status_connecting)
      MemConnected -> generalGetString(R.string.group_member_status_connected)
      MemComplete -> generalGetString(R.string.group_member_status_complete)
      MemCreator -> generalGetString(R.string.group_member_status_creator)
    }
}

@Serializable
sealed class InvitedBy {
  @Serializable @SerialName("contact") class IBContact(val byContactId: Long): InvitedBy()
  @Serializable @SerialName("user") class IBUser: InvitedBy()
  @Serializable @SerialName("unknown") class IBUnknown: InvitedBy()
}

@Serializable
class LinkPreview(
  val uri: String,
  val title: String,
  val description: String,
  val image: String
) {
  companion object {
    val sampleData = LinkPreview(
      uri = "https://www.duckduckgo.com",
      title = "Privacy, simplified.",
      description = "The Internet privacy company that empowers you to seamlessly take control of your personal information online, without any tradeoffs.",
      image = "data:image/jpg;base64,/9j/4AAQSkZJRgABAQAASABIAAD/4QBYRXhpZgAATU0AKgAAAAgAAgESAAMAAAABAAEAAIdpAAQAAAABAAAAJgAAAAAAA6ABAAMAAAABAAEAAKACAAQAAAABAAAAuKADAAQAAAABAAAAYAAAAAD/7QA4UGhvdG9zaG9wIDMuMAA4QklNBAQAAAAAAAA4QklNBCUAAAAAABDUHYzZjwCyBOmACZjs+EJ+/8AAEQgAYAC4AwEiAAIRAQMRAf/EAB8AAAEFAQEBAQEBAAAAAAAAAAABAgMEBQYHCAkKC//EALUQAAIBAwMCBAMFBQQEAAABfQECAwAEEQUSITFBBhNRYQcicRQygZGhCCNCscEVUtHwJDNicoIJChYXGBkaJSYnKCkqNDU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5+v/EAB8BAAMBAQEBAQEBAQEAAAAAAAABAgMEBQYHCAkKC//EALURAAIBAgQEAwQHBQQEAAECdwABAgMRBAUhMQYSQVEHYXETIjKBCBRCkaGxwQkjM1LwFWJy0QoWJDThJfEXGBkaJicoKSo1Njc4OTpDREVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoKDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uLj5OXm5+jp6vLz9PX29/j5+v/bAEMAAQEBAQEBAgEBAgMCAgIDBAMDAwMEBgQEBAQEBgcGBgYGBgYHBwcHBwcHBwgICAgICAkJCQkJCwsLCwsLCwsLC//bAEMBAgICAwMDBQMDBQsIBggLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLC//dAAQADP/aAAwDAQACEQMRAD8A/v4ooooAKKKKACiiigAooooAKK+CP2vP+ChXwZ/ZPibw7dMfEHi2VAYdGs3G9N33TO/IiU9hgu3ZSOa/NzXNL/4KJ/td6JJ49+NXiq2+Cvw7kG/ZNKbDMLcjKblmfI/57SRqewrwMdxBRo1HQoRdWqt1HaP+KT0j838j7XKOCMXiqEcbjKkcPh5bSne8/wDr3BXlN+is+5+43jb45/Bf4bs0fj/xZpGjSL1jvL2KF/8AvlmDfpXjH/DfH7GQuPsv/CydD35x/wAfIx+fT9a/AO58D/8ABJj4UzvF4v8AFfif4l6mp/evpkfkWzP3w2Isg+omb61X/wCF0/8ABJr/AI9f+FQeJPL6ed9vbzPrj7ZivnavFuIT+KhHyc5Sf3wjY+7w/hlgZQv7PF1P70aUKa+SqTUvwP6afBXx2+CnxIZYvAHi3R9ZkfpHZ3sUz/8AfKsW/SvVq/lItvBf/BJX4rTLF4V8UeJ/hpqTH91JqUfn2yv2y2JcD3MqfUV9OaFon/BRH9krQ4vH3wI8XW3xq+HkY3+XDKb/ABCvJxHuaZMDr5Ergd1ruwvFNVrmq0VOK3lSkp29Y6SS+R5GY+HGGi1DD4qVKo9oYmm6XN5RqK9Nvsro/obor4A/ZC/4KH/Bv9qxV8MLnw54vjU+bo9443SFPvG3k4EoHdcB17rjmvv+vqcHjaGKpKth5qUX1X9aPyZ+b5rlOMy3ESwmOpOFRdH+aezT6NXTCiiiuo84KKKKACiiigCC6/49pP8AdP8AKuOrsbr/AI9pP90/yrjqAP/Q/v4ooooAKKKKACiiigAr8tf+ChP7cWs/BEWfwD+A8R1P4k+JQkUCQr5rWUc52o+zndNIf9Up4H324wD9x/tDfGjw/wDs9fBnX/i/4jAeHRrZpI4c4M87YWKIe7yFV9gc9q/n6+B3iOb4GfCLxL/wU1+Oypq3jzxndT2nhK2uBwZptyvcBeoQBSq4xthjwPvivluIs0lSthKM+WUk5Sl/JBbtebekfM/R+BOHaeIcszxVL2kISUKdP/n7WlrGL/uxXvT8u6uizc6b8I/+CbmmRePPi9HD8Q/j7rifbktLmTz7bSGm582ZzktITyX++5+5tX5z5L8LPgv+0X/wVH12+8ZfEbxneW/2SRxB9o02eTSosdY4XRlgjYZGV++e5Jr8xvF3i7xN4+8UX/jXxney6jquqTNcXVzMcvJI5ySfQdgBwBgDgV+sP/BPX9jj9oL9oXw9H4tuvG2s+DfAVlM8VsthcyJLdSBsyCBNwREDZ3SEHLcBTgkfmuX4j+0MXHB06LdBXagna/8AenK6u+7el9Ej9+zvA/2Jls81r4uMcY7J1px5lHf93ShaVo9FFJNq8pMyPil/wRs/aj8D6dLq3gq70vxdHECxgtZGtrogf3UmAQn2EmT2r8rPEPh3xB4R1u58M+KrGfTdRsnMdxa3MbRTROOzKwBBr+674VfCnTfhNoI0DTtX1jWFAGZtYvpL2U4934X/AICAK8V/aW/Yf/Z9/areHUvibpkkerWsRhg1KxkMFyqHkBiMrIAeQJFYDJxjJr6bNPD+nOkqmAfLP+WTuvk7XX4/I/PeHvG6tSxDo5zH2lLpUhHll6uN7NelmvPY/iir2T4KftA/GD9njxMvir4Q65caTPkGWFTutrgD+GaE/I4+oyOxB5r2n9tb9jTxj+x18RYvD+pTtqmgaqrS6VqezZ5qpjfHIBwsseRuA4IIYdcD4yr80q0sRgcQ4SvCpB+jT8mvzP6Bw2JwOcYGNany1aFRdVdNdmn22aauno9T9tLO0+D/APwUr02Txd8NI4Ph38ftGT7b5NtIYLXWGh58yJwQVkBGd/8ArEP3i6fMP0R/4J7ftw6/8YZ7z9nb9oGJtN+JPhoPFIJ18p75IPlclegnj/5aKOGHzrxnH8rPhXxT4j8D+JbHxj4QvZdO1TTJkuLW5hba8UqHIIP8x0I4PFfsZ8bPEdx+0N8FvDv/AAUl+CgXSfiJ4EuYLXxZBbDALw4CXO0clMEZznMLlSf3Zr7PJM+nzyxUF+9ir1IrRVILeVtlOO+lrr5n5RxfwbRdKGXVXfDzfLRm9ZUKr+GDlq3RqP3UnfllZfy2/ptorw/9m/43aF+0X8FNA+L+gARpq1uGnhByYLlCUmiP+44IHqMHvXuFfsNGtCrTjVpu8ZJNPyZ/LWKwtXDVp4evG04Nxa7NOzX3hRRRWhzhRRRQBBdf8e0n+6f5Vx1djdf8e0n+6f5Vx1AH/9H+/iiiigAooooAKKKKAPw9/wCCvXiPWviH4q+F/wCyN4XlKT+K9TS6uQvoXFvAT7AvI3/AQe1fnF/wVO+IOnXfxx034AeDj5Xhv4ZaXb6TawKfkE7Ro0rY6bgvlofdT61+h3xNj/4Tv/gtd4Q0W/8Anh8P6THLGp6Ax21xOD/324Nfg3+0T4kufGH7QHjjxRdtukvte1GXJ9PPcKPwAAr8a4pxUpLEz6zq8n/btOK0+cpX9Uf1d4c5bCDy+lbSlh3W/wC38RNq/qoQcV5M8fjiaeRYEOGchR9TxX9svw9+GHijSvgB4I+Gnwr1ceGbGztYY728gijluhbohLLAJVeJZJJCN0jo+0Zwu4gj+JgO8REsf3l+YfUV/bf8DNVm+Mv7KtkNF1CTTZ9Z0d4Ir2D/AFls9zF8sidPmj3hhz1Fel4YyhGtiHpzWjur6e9f9Dw/H9VXQwFvgvUv62hb8Oa3zPoDwfp6aPoiaONXuNaa1Zo3ubp43nLDqrmJEXI/3QfWukmjMsTRBihYEbl6jPcZ7ivxk/4JMf8ABOv9ob9hBvFdr8ZvGOma9Yak22wttLiYGV2kMkl1dzSIkkkzcKisX8tSwDYNfs/X7Bj6NOlXlCjUU4/zJWv8j+ZsNUnOmpThyvtufj/+1Z8Hf2bPi58PviF8Avh/4wl1j4iaBZjXG0m71qfU7i3u4FMqt5VxLL5LzR70Kx7AVfJXAXH8sysGUMOh5r+vzwl+wD+y78KP2wPEX7bGn6xqFv4g8QmWa70+fUFGlrdTRmGS4EGATIY2dRvdlXe+0DPH83Nh+x58bPFev3kljpSaVYPcymGS+kEX7oudp2DL/dx/DX4Z4xZxkmCxGHxdTGRTlG0ueUU7q3S93a7S69Oh/SngTnNSjgcZhMc1CnCSlC70966dr/4U7Lq79T5Kr9MP+CWfxHsNH+P138EPF2JvDfxL0640a9gc/I0vls0Rx6kb4x/v1x3iz9hmHwV4KuPFHiLxlaWkltGzt5sBSAsBkIHL7iT0GFJJ7V8qfAnxLc+D/jd4N8V2bFJdP1vT5wR/szoT+YyK/NeD+Lcvx+Ijisuq88ackpPlklruveSvdX2ufsmavC5zlWKw9CV7xaTs1aSV4tXS1Ukmrdj9/P8Agkfrus/DD4ifFP8AY/8AEkrPJ4Z1F7y1DeiSG3mI9m2wv/wI1+5Ffhd4Ki/4Qf8A4Lb+INM0/wCSHxDpDySqOhL2cMx/8fizX7o1/RnC7ccLPDP/AJdTnBeid1+DP5M8RkqmZUselZ4ijSqv1lG0vvcWwooor6Q+BCiiigCC6/49pP8AdP8AKuOrsbr/AI9pP90/yrjqAP/S/v4ooooAKKKKACiiigD8LfiNIfBP/BbLwpq9/wDJDr2kJHGTwCZLS4gH/j0eK/Bj9oPw7c+Evj3428M3ilZLHXtRiIPoJ3x+Ywa/fL/grnoWsfDPx98K/wBrzw5EzyeGNSS0uSvokguYQfZtsy/8CFfnB/wVP+HNho/7QFp8bvCeJvDnxK0231mznQfI0vlqsoz6kbJD/v1+M8U4WUViYW1hV5/+3akVr/4FG3qz+r/DnMYTeX1b6VcP7L/t/Dzenq4Tcl5I/M2v6yP+CR3j4eLP2XbLRZZN0uku9sRnp5bMB/45sr+Tev3u/wCCJXj7yNW8T/DyZ+C6XUak9pUw36xD865uAcV7LNFTf24tfd736Hd405d9Y4cddLWlOMvk7wf/AKUvuP6Kq/P/APaa+InjJfF8vge3lez06KONgIyVM+8ZJYjkgHIx045r9AK/Gr/gsB8UPHXwg8N+AvFfgV4oWmv7u3uTJEsiyL5SsiNkZxkMeCDmvU8bsgzPN+Fa+FyrEujUUot6tKcdnBtapO6fny2ejZ/OnAOFWJzqjheVOU+ZK+yaTlfr2t8z85td/b18H6D4n1DQLrw5fSLY3Elv5okRWcxsVJKMAVyR0yTivEPHf7f3jjVFe18BaXb6PGeBPcH7RN9QMBAfqGrFP7UPwj8c3f2/4y/DuzvbxgA93ZNtd8dyGwT+Lmuvh/aP/ZT8IxC58EfD0y3Y5UzwxKAf99mlP5Cv49wvCeBwUoc3D9Sday3qRlTb73c7Wf8Aej8j+rKWVUKLV8vlKf8AiTj/AOlW+9Hw74w8ceNvHl8NX8bajc6jK2SjTsSo/wBxeFUf7orovgf4dufF3xp8H+F7NS0uoa3p8Cgf7c6A/pW98avjx4q+NmoW0mswW9jY2G/7LaWy4WPfjJLHlicD0HoBX13/AMEtPhrZeI/2jH+L3inEPh34cWE+t31w/wBxJFRliBPqPmkH/XOv3fhXCVa/1ahUoRoybV4RacYq/dKK0jq7Ky1s3uezm+PeByeviqkFBxhK0U767RirJattLTqz9H/CMg8af8Futd1DT/ni8P6OySsOxSyiiP8A49Niv3Qr8NP+CS+j6t8V/iv8V/2wdfiZD4i1B7K0LDtLJ9olUf7imFfwr9y6/oLhe88LUxPSrUnNejdl+CP5G8RWqeY0cAnd4ejSpP8AxRjd/c5NBRRRX0h8CFFFFAEF1/x7Sf7p/lXHV2N1/wAe0n+6f5Vx1AH/0/7+KKKKACiiigAooooA8M/aT+B+iftGfBLxB8INcIjGrWxFvORnyLmMh4ZB/uSAE46jI71+AfwU8N3H7SXwL8Qf8E5fjFt0r4kfD65nuvCstycbmhz5ltuPVcE4x1idWHEdf031+UX/AAUL/Yj8T/FG/sv2mP2c5H074keGtkoFufLe+jg5Taennx9Ezw6/Ie2PleI8slUtjKUOZpOM4/zwe6X96L1j5/cfpPAXEMKF8rxNX2cZSU6VR7Uq0dE3/cmvcn5dldn8r/iXw3r/AIN8Q3vhPxXZy6fqemzPb3VtMNskUsZwysPY/n1HFfe3/BL3x/8A8IP+1bptvK+2HVbeSBvdoyso/RWH419SX8fwg/4Kc6QmleIpLfwB8f8ASI/ssiXCGC11kwfLtZSNwkGMbceZH0w6Dj88tM+HvxW/ZK/aO8OQ/FvR7nQ7uw1OElpV/czQs+x2ilGUkUqTypPvivy3DYWWX46hjaT56HOrSXa+ql/LK26fy0P6LzDMYZ3lGMynEx9ni/ZyvTfV2bjKD+3BtJqS9HZn9gnxB/aM+Cvwp8XWXgj4ja/Bo+o6hB9ogW5DrG0ZYoCZNvlr8wI+Zh0r48/4KkfDey+NP7GOqeIPDUsV7L4elh1u0khYOskcOVl2MCQcwu5GDyRXwx/wVBnbVPH3gjxGeVvPDwUt2LxzOW/9Cr87tO8PfFXVdPisbDS9avNImbzLNILa4mtXfo5j2KULZwDjmvqs+4srKvi8rqYfnjays2nqlq9JX3v0P4FwfiDisjzqNanQU3RnGUbNq9rOz0ej207nxZovhrV9enMNhHwpwztwq/U+vt1qrrWlT6JqUumXBDNHj5l6EEZr7U+IHhHxF8JvEUHhL4j2Umiald2sV/Hb3Q8t2hnztbB75BDKfmVgQQCK8e0f4N/E349/FRvBvwh0a41y+YRq/kD91ECPvSyHCRqPVmFfl8aNZ1vYcj59rWd79rbn9T+HPjFnnEPE1WhmmEWEwKw8qkVJNbSppTdSSimmpO1ko2a3aueH+H/D+ueLNds/DHhi0lv9R1CZLe2toV3SSyyHCqoHUk1+yfxl8N3X7Ln7P+h/8E9/hOF1X4nfEm4gufFDWp3FBMR5dqGHRTgLzx5au5wJKtaZZ/B7/gmFpBhsJLbx78fdVi+zwQWyma00UzjbgAfMZDnGMCSToAiElvv/AP4J7fsS+LPh5q15+1H+0q76h8R/Em+ZUuSHksI5/vFj0E8g4YDiNPkH8VfeZJkVTnlhYfxpK02tqUHur7c8trdFfzt9dxdxjQ9lDMKi/wBlpvmpRejxFVfDK26o03713bmla2yv90/sw/ArRv2bvgboHwh0crK2mQZup1GPPu5Tvmk9fmcnGei4HavfKKK/YaFGFGnGlTVoxSSXkj+WMXi6uKr1MTXlec25N923dsKKKK1OcKKKKAILr/j2k/3T/KuOrsbr/j2k/wB0/wAq46gD/9T+/iiiigAooooAKKKKACiiigD87P2wf+Ccnwm/ahmbxvosh8K+NY8NHq1onyzOn3ftEYK7yMcSKVkX1IAFfnT4m8f/ALdv7L+gyfDn9rjwFb/GLwFD8q3ssf2srGOjfaAjspA6GeMMOzV/RTRXz+N4eo1akq+Hm6VR7uNrS/xRekvzPuMo45xOGoQweOpRxFCPwqd1KH/XuorSh8m0uiPwz0L/AIKEf8E3vi6miH4saHd6Xc6B5gs4tWs3vYIPNILAGFpA65UcSLxjgCvtS1/4KT/sLWVlHFZePrCGCJAqRJa3K7VHQBRFxj0xXv8A48/Zc/Zx+J0z3Xj3wPoupzyHLTS2cfnE+8iqH/WvGP8Ah23+w953n/8ACu9PznOPMn2/98+bj9K5oYTOqMpSpyoyb3k4yjJ2015Xqac/BNSbrPD4mlKW6hKlJf8AgUkpP5n5zfta/tof8Ex/jPq+k+IPHelan491HQlljtI7KGWyikWUqSkryNCzJlcgc4JPHNcZ4V+Iv7c37TGgJ8N/2Ovh7bfB7wHN8pvoo/shMZ4LfaSiMxx1MERf/ar9sPAn7LH7N3wxmS68B+BtF02eM5WaOzjMwI9JGBf9a98AAGBWSyDF16kquKrqPN8Xso8rfrN3lY9SXG+WYPDww2W4SdRQ+B4io5xjre6pRtTvfW+up+cv7H//AATg+FX7MdynjzxHMfFnjeTLvqt2vyQO/wB77OjFtpOeZGLSH1AOK/Rqiivo8FgaGEpKjh4KMV/V33fmz4LNs5xuZ4h4rHVXOb6vouyWyS6JJIKKKK6zzAooooAKKKKAILr/AI9pP90/yrjq7G6/49pP90/yrjqAP//Z"
    )
  }
}

@Serializable
class MemberSubError(
  val member: GroupMember,
  val memberError: ChatError
)

@Serializable
class UserContactRequest(
  val contactRequestId: Long,
  override val localDisplayName: String,
  val profile: Profile,
  override val createdAt: Instant,
  override val updatedAt: Instant
): SomeChat, NamedChat {
  override val chatType get() = ChatType.ContactRequest
  override val id get() = "<@$contactRequestId"
  override val apiId get() = contactRequestId
  override val ready get() = true
  override val sendMsgEnabled get() = false
  override val ntfsEnabled get() = false
  override val incognito get() = false
  override fun featureEnabled(feature: ChatFeature) = false
  override val timedMessagesTTL: Int? get() = null
  override val displayName get() = profile.displayName
  override val fullName get() = profile.fullName
  override val image get() = profile.image
  override val localAlias get() = ""

  companion object {
    val sampleData = UserContactRequest(
      contactRequestId = 1,
      localDisplayName = "alice",
      profile = Profile.sampleData,
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now()
    )
  }
}

@Serializable
class PendingContactConnection(
  val pccConnId: Long,
  val pccAgentConnId: String,
  val pccConnStatus: ConnStatus,
  val viaContactUri: Boolean,
  val groupLinkId: String? = null,
  val customUserProfileId: Long? = null,
  val connReqInv: String? = null,
  override val localAlias: String,
  override val createdAt: Instant,
  override val updatedAt: Instant
): SomeChat, NamedChat {
  override val chatType get() = ChatType.ContactConnection
  override val id get() = ":$pccConnId"
  override val apiId get() = pccConnId
  override val ready get() = false
  override val sendMsgEnabled get() = false
  override val ntfsEnabled get() = false
  override val incognito get() = customUserProfileId != null
  override fun featureEnabled(feature: ChatFeature) = false
  override val timedMessagesTTL: Int? get() = null
  override val localDisplayName get() = String.format(generalGetString(R.string.connection_local_display_name), pccConnId)
  override val displayName: String
    get() {
      if (localAlias.isNotEmpty()) return localAlias
      val initiated = pccConnStatus.initiated
      return if (initiated == null) {
        // this should not be in the chat list
        generalGetString(R.string.display_name_connection_established)
      } else {
        generalGetString(
          if (initiated && !viaContactUri) R.string.display_name_invited_to_connect
          else R.string.display_name_connecting
        )
      }
    }
  override val fullName get() = ""
  override val image get() = null
  val initiated get() = (pccConnStatus.initiated ?: false) && !viaContactUri
  val description: String
    get() {
      val initiated = pccConnStatus.initiated
      return if (initiated == null) "" else generalGetString(
        if (initiated && !viaContactUri)
          if (incognito) R.string.description_you_shared_one_time_link_incognito else R.string.description_you_shared_one_time_link
        else if (viaContactUri)
          if (groupLinkId != null)
            if (incognito) R.string.description_via_group_link_incognito else R.string.description_via_group_link
          else
            if (incognito) R.string.description_via_contact_address_link_incognito else R.string.description_via_contact_address_link
        else
          if (incognito) R.string.description_via_one_time_link_incognito else R.string.description_via_one_time_link
      )
    }

  companion object {
    fun getSampleData(status: ConnStatus = ConnStatus.New, viaContactUri: Boolean = false): PendingContactConnection =
      PendingContactConnection(
        pccConnId = 1,
        pccAgentConnId = "abcd",
        pccConnStatus = status,
        viaContactUri = viaContactUri,
        localAlias = "",
        customUserProfileId = null,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
      )
  }
}

@Serializable
enum class ConnStatus {
  @SerialName("new") New,
  @SerialName("joined") Joined,
  @SerialName("requested") Requested,
  @SerialName("accepted") Accepted,
  @SerialName("snd-ready") SndReady,
  @SerialName("ready") Ready,
  @SerialName("deleted") Deleted;

  val initiated: Boolean?
    get() = when (this) {
      New -> true
      Joined -> false
      Requested -> true
      Accepted -> true
      SndReady -> false
      Ready -> null
      Deleted -> null
    }
}

@Serializable
class AChatItem(
  val chatInfo: ChatInfo,
  val chatItem: ChatItem
)

@Serializable @Stable
data class ChatItem(
  val chatDir: CIDirection,
  val meta: CIMeta,
  val content: CIContent,
  val formattedText: List<FormattedText>? = null,
  val quotedItem: CIQuote? = null,
  val file: CIFile? = null
) {
  val id: Long get() = meta.itemId
  val timestampText: String get() = meta.timestampText
  val text: String
    get() {
      val mc = content.msgContent
      return when {
        content.text == "" && file != null && mc is MsgContent.MCVoice -> String.format(generalGetString(R.string.voice_message_with_duration), durationText(mc.duration))
        content.text == "" && file != null -> file.fileName
        else -> content.text
      }
    }
  val isRcvNew: Boolean get() = meta.isRcvNew
  val memberDisplayName: String?
    get() =
      if (chatDir is CIDirection.GroupRcv) chatDir.groupMember.displayName
      else null
  val memberHiddenID: String?
    get() =
      if (chatDir is CIDirection.GroupRcv) chatDir.groupMember.memberId.substring(0, 7)
      else null
  val isDeletedContent: Boolean
    get() =
      when (content) {
        is CIContent.SndDeleted -> true
        is CIContent.RcvDeleted -> true
        is CIContent.SndModerated -> true
        is CIContent.RcvModerated -> true
        else -> false
      }

  fun memberToModerate(chatInfo: ChatInfo): Pair<GroupInfo, GroupMember>? {
    return if (chatInfo is ChatInfo.Group && chatDir is CIDirection.GroupRcv) {
      val m = chatInfo.groupInfo.membership
      if (m.memberRole >= GroupMemberRole.Admin && m.memberRole >= chatDir.groupMember.memberRole && meta.itemDeleted == null) {
        chatInfo.groupInfo to chatDir.groupMember
      } else {
        null
      }
    } else {
      null
    }
  }

  private val showNtfDir: Boolean get() = !chatDir.sent
  val showNotification: Boolean
    get() =
      when (content) {
        is CIContent.SndMsgContent -> showNtfDir
        is CIContent.RcvMsgContent -> showNtfDir
        is CIContent.SndDeleted -> showNtfDir
        is CIContent.RcvDeleted -> showNtfDir
        is CIContent.SndCall -> showNtfDir
        is CIContent.RcvCall -> false // notification is shown on CallInvitation instead
        is CIContent.RcvIntegrityError -> showNtfDir
        is CIContent.RcvDecryptionError -> showNtfDir
        is CIContent.RcvGroupInvitation -> showNtfDir
        is CIContent.SndGroupInvitation -> showNtfDir
        is CIContent.RcvGroupEventContent -> when (content.rcvGroupEvent) {
          is RcvGroupEvent.MemberAdded -> false
          is RcvGroupEvent.MemberConnected -> false
          is RcvGroupEvent.MemberLeft -> false
          is RcvGroupEvent.MemberRole -> false
          is RcvGroupEvent.UserRole -> showNtfDir
          is RcvGroupEvent.MemberDeleted -> false
          is RcvGroupEvent.UserDeleted -> showNtfDir
          is RcvGroupEvent.GroupDeleted -> showNtfDir
          is RcvGroupEvent.GroupUpdated -> false
          is RcvGroupEvent.InvitedViaGroupLink -> false
        }
        is CIContent.SndGroupEventContent -> showNtfDir
        is CIContent.RcvConnEventContent -> false
        is CIContent.SndConnEventContent -> showNtfDir
        is CIContent.RcvChatFeature -> false
        is CIContent.SndChatFeature -> showNtfDir
        is CIContent.RcvChatPreference -> false
        is CIContent.SndChatPreference -> showNtfDir
        is CIContent.RcvGroupFeature -> false
        is CIContent.SndGroupFeature -> showNtfDir
        is CIContent.RcvChatFeatureRejected -> showNtfDir
        is CIContent.RcvGroupFeatureRejected -> showNtfDir
        is CIContent.SndModerated -> true
        is CIContent.RcvModerated -> true
        is CIContent.InvalidJSON -> false
      }

  fun withStatus(status: CIStatus): ChatItem = this.copy(meta = meta.copy(itemStatus = status))

  companion object {
    fun getSampleData(
      id: Long = 1,
      dir: CIDirection = CIDirection.DirectSnd(),
      ts: Instant = Clock.System.now(),
      text: String = "hello\nthere",
      status: CIStatus = CIStatus.SndNew(),
      quotedItem: CIQuote? = null,
      file: CIFile? = null,
      itemDeleted: CIDeleted? = null,
      itemEdited: Boolean = false,
      itemTimed: CITimed? = null,
      editable: Boolean = true
    ) =
      ChatItem(
        chatDir = dir,
        meta = CIMeta.getSample(id, ts, text, status, itemDeleted, itemEdited, itemTimed, editable),
        content = CIContent.SndMsgContent(msgContent = MsgContent.MCText(text)),
        quotedItem = quotedItem,
        file = file
      )

    fun getFileMsgContentSample(
      id: Long = 1,
      text: String = "",
      fileName: String = "test.txt",
      fileSize: Long = 100,
      fileStatus: CIFileStatus = CIFileStatus.RcvComplete
    ) =
      ChatItem(
        chatDir = CIDirection.DirectRcv(),
        meta = CIMeta.getSample(id, Clock.System.now(), text, CIStatus.RcvRead()),
        content = CIContent.RcvMsgContent(msgContent = MsgContent.MCFile(text)),
        quotedItem = null,
        file = CIFile.getSample(fileName = fileName, fileSize = fileSize, fileStatus = fileStatus)
      )

    fun getDeletedContentSampleData(
      id: Long = 1,
      dir: CIDirection = CIDirection.DirectRcv(),
      ts: Instant = Clock.System.now(),
      text: String = "this item is deleted", // sample not localized
      status: CIStatus = CIStatus.RcvRead()
    ) =
      ChatItem(
        chatDir = dir,
        meta = CIMeta.getSample(id, ts, text, status),
        content = CIContent.RcvDeleted(deleteMode = CIDeleteMode.cidmBroadcast),
        quotedItem = null,
        file = null
      )

    fun getGroupInvitationSample(status: CIGroupInvitationStatus = CIGroupInvitationStatus.Pending) =
      ChatItem(
        chatDir = CIDirection.DirectRcv(),
        meta = CIMeta.getSample(1, Clock.System.now(), "received invitation to join group team as admin", CIStatus.RcvRead()),
        content = CIContent.RcvGroupInvitation(groupInvitation = CIGroupInvitation.getSample(status = status), memberRole = GroupMemberRole.Admin),
        quotedItem = null,
        file = null
      )

    fun getGroupEventSample() =
      ChatItem(
        chatDir = CIDirection.DirectRcv(),
        meta = CIMeta.getSample(1, Clock.System.now(), "group event text", CIStatus.RcvRead()),
        content = CIContent.RcvGroupEventContent(rcvGroupEvent = RcvGroupEvent.MemberAdded(groupMemberId = 1, profile = Profile.sampleData)),
        quotedItem = null,
        file = null
      )

    fun getChatFeatureSample(feature: ChatFeature, enabled: FeatureEnabled): ChatItem {
      val content = CIContent.RcvChatFeature(feature = feature, enabled = enabled, param = null)
      return ChatItem(
        chatDir = CIDirection.DirectRcv(),
        meta = CIMeta.getSample(1, Clock.System.now(), content.text, CIStatus.RcvRead()),
        content = content,
        quotedItem = null,
        file = null
      )
    }

    private const val TEMP_DELETED_CHAT_ITEM_ID = -1L
    const val TEMP_LIVE_CHAT_ITEM_ID = -2L
    val deletedItemDummy: ChatItem
      get() = ChatItem(
        chatDir = CIDirection.DirectRcv(),
        meta = CIMeta(
          itemId = TEMP_DELETED_CHAT_ITEM_ID,
          itemTs = Clock.System.now(),
          itemText = generalGetString(R.string.deleted_description),
          itemStatus = CIStatus.RcvRead(),
          createdAt = Clock.System.now(),
          updatedAt = Clock.System.now(),
          itemDeleted = null,
          itemEdited = false,
          itemTimed = null,
          itemLive = false,
          editable = false
        ),
        content = CIContent.RcvDeleted(deleteMode = CIDeleteMode.cidmBroadcast),
        quotedItem = null,
        file = null
      )

    fun liveDummy(direct: Boolean): ChatItem = ChatItem(
      chatDir = if (direct) CIDirection.DirectSnd() else CIDirection.GroupSnd(),
      meta = CIMeta(
        itemId = TEMP_LIVE_CHAT_ITEM_ID,
        itemTs = Clock.System.now(),
        itemText = "",
        itemStatus = CIStatus.RcvRead(),
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        itemDeleted = null,
        itemEdited = false,
        itemTimed = null,
        itemLive = true,
        editable = false
      ),
      content = CIContent.SndMsgContent(MsgContent.MCText("")),
      quotedItem = null,
      file = null
    )

    fun invalidJSON(chatDir: CIDirection?, meta: CIMeta?, json: String): ChatItem =
      ChatItem(
        chatDir = chatDir ?: CIDirection.DirectSnd(),
        meta = meta ?: CIMeta.invalidJSON(),
        content = CIContent.InvalidJSON(json),
        quotedItem = null,
        file = null
      )
  }
}

@Serializable
sealed class CIDirection {
  @Serializable @SerialName("directSnd") class DirectSnd: CIDirection()
  @Serializable @SerialName("directRcv") class DirectRcv: CIDirection()
  @Serializable @SerialName("groupSnd") class GroupSnd: CIDirection()
  @Serializable @SerialName("groupRcv") class GroupRcv(val groupMember: GroupMember): CIDirection()

  val sent: Boolean
    get() = when (this) {
      is DirectSnd -> true
      is DirectRcv -> false
      is GroupSnd -> true
      is GroupRcv -> false
    }
}

@Serializable
data class CIMeta(
  val itemId: Long,
  val itemTs: Instant,
  val itemText: String,
  val itemStatus: CIStatus,
  val createdAt: Instant,
  val updatedAt: Instant,
  val itemDeleted: CIDeleted?,
  val itemEdited: Boolean,
  val itemTimed: CITimed?,
  val itemLive: Boolean?,
  val editable: Boolean
) {
  val timestampText: String get() = getTimestampText(itemTs)
  val recent: Boolean get() = updatedAt + 10.toDuration(DurationUnit.SECONDS) > Clock.System.now()
  val isLive: Boolean get() = itemLive == true
  val disappearing: Boolean get() = !isRcvNew && itemTimed?.deleteAt != null
  val isRcvNew: Boolean get() = itemStatus is CIStatus.RcvNew

  fun statusIcon(primaryColor: Color, metaColor: Color = HighOrLowlight): Pair<ImageVector, Color>? =
    when (itemStatus) {
      is CIStatus.SndSent -> Icons.Filled.Check to metaColor
      is CIStatus.SndErrorAuth -> Icons.Filled.Close to Color.Red
      is CIStatus.SndError -> Icons.Filled.WarningAmber to WarningYellow
      is CIStatus.RcvNew -> Icons.Filled.Circle to primaryColor
      else -> null
    }

  companion object {
    fun getSample(
      id: Long, ts: Instant, text: String, status: CIStatus = CIStatus.SndNew(),
      itemDeleted: CIDeleted? = null, itemEdited: Boolean = false, itemTimed: CITimed? = null, itemLive: Boolean = false, editable: Boolean = true
    ): CIMeta =
      CIMeta(
        itemId = id,
        itemTs = ts,
        itemText = text,
        itemStatus = status,
        createdAt = ts,
        updatedAt = ts,
        itemDeleted = itemDeleted,
        itemEdited = itemEdited,
        itemTimed = itemTimed,
        itemLive = itemLive,
        editable = editable
      )

    fun invalidJSON(): CIMeta =
      CIMeta(
        // itemId can not be the same for different items, otherwise ChatView will crash
        itemId = Random.nextLong(-1000000L, -1000L),
        itemTs = Clock.System.now(),
        itemText = "invalid JSON",
        itemStatus = CIStatus.SndNew(),
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        itemDeleted = null,
        itemEdited = false,
        itemTimed = null,
        itemLive = false,
        editable = false
      )
  }
}

@Serializable
data class CITimed(
  val ttl: Int,
  val deleteAt: Instant?
)

fun getTimestampText(t: Instant): String {
  val tz = TimeZone.currentSystemDefault()
  val now: LocalDateTime = Clock.System.now().toLocalDateTime(tz)
  val time: LocalDateTime = t.toLocalDateTime(tz)
  val recent = now.date == time.date ||
      (now.date.minus(time.date).days == 1 && now.hour < 12 && time.hour >= 18)
  return if (recent) String.format("%02d:%02d", time.hour, time.minute)
  else String.format("%02d/%02d", time.dayOfMonth, time.monthNumber)
}

@Serializable
sealed class CIStatus {
  @Serializable @SerialName("sndNew") class SndNew: CIStatus()
  @Serializable @SerialName("sndSent") class SndSent: CIStatus()
  @Serializable @SerialName("sndErrorAuth") class SndErrorAuth: CIStatus()
  @Serializable @SerialName("sndError") class SndError(val agentError: String): CIStatus()
  @Serializable @SerialName("rcvNew") class RcvNew: CIStatus()
  @Serializable @SerialName("rcvRead") class RcvRead: CIStatus()
}

@Serializable
sealed class CIDeleted {
  @Serializable @SerialName("deleted") class Deleted: CIDeleted()
  @Serializable @SerialName("moderated") class Moderated(val byGroupMember: GroupMember): CIDeleted()
}

@Serializable
enum class CIDeleteMode(val deleteMode: String) {
  @SerialName("internal") cidmInternal("internal"),
  @SerialName("broadcast") cidmBroadcast("broadcast");
}

interface ItemContent {
  val text: String
}

@Serializable
sealed class CIContent: ItemContent {
  abstract val msgContent: MsgContent?

  @Serializable @SerialName("sndMsgContent") class SndMsgContent(override val msgContent: MsgContent): CIContent()
  @Serializable @SerialName("rcvMsgContent") class RcvMsgContent(override val msgContent: MsgContent): CIContent()
  @Serializable @SerialName("sndDeleted") class SndDeleted(val deleteMode: CIDeleteMode): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvDeleted") class RcvDeleted(val deleteMode: CIDeleteMode): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndCall") class SndCall(val status: CICallStatus, val duration: Int): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvCall") class RcvCall(val status: CICallStatus, val duration: Int): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvIntegrityError") class RcvIntegrityError(val msgError: MsgErrorType): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvDecryptionError") class RcvDecryptionError(val msgDecryptError: MsgDecryptError, val msgCount: UInt): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvGroupInvitation") class RcvGroupInvitation(val groupInvitation: CIGroupInvitation, val memberRole: GroupMemberRole): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndGroupInvitation") class SndGroupInvitation(val groupInvitation: CIGroupInvitation, val memberRole: GroupMemberRole): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvGroupEvent") class RcvGroupEventContent(val rcvGroupEvent: RcvGroupEvent): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndGroupEvent") class SndGroupEventContent(val sndGroupEvent: SndGroupEvent): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvConnEvent") class RcvConnEventContent(val rcvConnEvent: RcvConnEvent): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndConnEvent") class SndConnEventContent(val sndConnEvent: SndConnEvent): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvChatFeature") class RcvChatFeature(val feature: ChatFeature, val enabled: FeatureEnabled, val param: Int? = null): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndChatFeature") class SndChatFeature(val feature: ChatFeature, val enabled: FeatureEnabled, val param: Int? = null): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvChatPreference") class RcvChatPreference(val feature: ChatFeature, val allowed: FeatureAllowed, val param: Int? = null): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndChatPreference") class SndChatPreference(val feature: ChatFeature, val allowed: FeatureAllowed, val param: Int? = null): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvGroupFeature") class RcvGroupFeature(val groupFeature: GroupFeature, val preference: GroupPreference, val param: Int? = null): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndGroupFeature") class SndGroupFeature(val groupFeature: GroupFeature, val preference: GroupPreference, val param: Int? = null): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvChatFeatureRejected") class RcvChatFeatureRejected(val feature: ChatFeature): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvGroupFeatureRejected") class RcvGroupFeatureRejected(val groupFeature: GroupFeature): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("sndModerated") object SndModerated: CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("rcvModerated") object RcvModerated: CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  @Serializable @SerialName("invalidJSON") data class InvalidJSON(val json: String): CIContent() {
    override val msgContent: MsgContent? get() = null
  }

  override val text: String
    get() = when (this) {
      is SndMsgContent -> msgContent.text
      is RcvMsgContent -> msgContent.text
      is SndDeleted -> generalGetString(R.string.deleted_description)
      is RcvDeleted -> generalGetString(R.string.deleted_description)
      is SndCall -> status.text(duration)
      is RcvCall -> status.text(duration)
      is RcvIntegrityError -> msgError.text
      is RcvDecryptionError -> msgDecryptError.text
      is RcvGroupInvitation -> groupInvitation.text
      is SndGroupInvitation -> groupInvitation.text
      is RcvGroupEventContent -> rcvGroupEvent.text
      is SndGroupEventContent -> sndGroupEvent.text
      is RcvConnEventContent -> rcvConnEvent.text
      is SndConnEventContent -> sndConnEvent.text
      is RcvChatFeature -> featureText(feature, enabled.text, param)
      is SndChatFeature -> featureText(feature, enabled.text, param)
      is RcvChatPreference -> preferenceText(feature, allowed, param)
      is SndChatPreference -> preferenceText(feature, allowed, param)
      is RcvGroupFeature -> featureText(groupFeature, preference.enable.text, param)
      is SndGroupFeature -> featureText(groupFeature, preference.enable.text, param)
      is RcvChatFeatureRejected -> "${feature.text}: ${generalGetString(R.string.feature_received_prohibited)}"
      is RcvGroupFeatureRejected -> "${groupFeature.text}: ${generalGetString(R.string.feature_received_prohibited)}"
      is SndModerated -> generalGetString(R.string.moderated_description)
      is RcvModerated -> generalGetString(R.string.moderated_description)
      is InvalidJSON -> "invalid data"
    }

  companion object {
    fun featureText(feature: Feature, enabled: String, param: Int?): String =
      if (feature.hasParam) {
        "${feature.text} ${TimedMessagesPreference.ttlText(param)} after they're sent."
      } else {
        "${feature.text}: $enabled"
      }

    fun preferenceText(feature: Feature, allowed: FeatureAllowed, param: Int?): String = when {
      allowed != FeatureAllowed.NO && feature.hasParam && param != null ->
        String.format(generalGetString(R.string.feature_offered_item_with_param), feature.text, TimedMessagesPreference.ttlText(param))
      allowed != FeatureAllowed.NO ->
        String.format(generalGetString(R.string.feature_offered_item), feature.text, TimedMessagesPreference.ttlText(param))
      else ->
        String.format(generalGetString(R.string.feature_cancelled_item), feature.text, TimedMessagesPreference.ttlText(param))
    }
  }
}

@Serializable
enum class MsgDecryptError {
  @SerialName("ratchetHeader") RatchetHeader,
  @SerialName("tooManySkipped") TooManySkipped;

  val text: String
    get() = when (this) {
      RatchetHeader -> generalGetString(R.string.decryption_error)
      TooManySkipped -> generalGetString(R.string.decryption_error)
    }
}

@Serializable
class CIQuote(
  val chatDir: CIDirection? = null,
  val itemId: Long? = null,
  val sharedMsgId: String? = null,
  val sentAt: Instant,
  val content: MsgContent,
  val formattedText: List<FormattedText>? = null
): ItemContent {
  override val text: String by lazy {
    if (content.text == "" && content is MsgContent.MCVoice)
      durationText(content.duration)
    else
      content.text
  }

  fun sender(membership: GroupMember?, isZeroKnowledge: Boolean = false): String? = when (chatDir) {
    is CIDirection.DirectSnd -> generalGetString(R.string.sender_you_pronoun)
    is CIDirection.DirectRcv -> null
    is CIDirection.GroupSnd -> if (isZeroKnowledge) membership?.id?.substring(0, 7) else membership?.displayName
    is CIDirection.GroupRcv -> if (isZeroKnowledge) chatDir.groupMember.id.substring(0, 7) else chatDir.groupMember.displayName
    null -> null
  }

  companion object {
    fun getSample(itemId: Long?, sentAt: Instant, text: String, chatDir: CIDirection?): CIQuote =
      CIQuote(chatDir = chatDir, itemId = itemId, sentAt = sentAt, content = MsgContent.MCText(text))
  }
}

@Serializable
class CIFile(
  val fileId: Long,
  val fileName: String,
  val fileSize: Long,
  val filePath: String? = null,
  val fileStatus: CIFileStatus,
  val fileProtocol: FileProtocol
) {
  val loaded: Boolean = when (fileStatus) {
    is CIFileStatus.SndStored -> true
    is CIFileStatus.SndTransfer -> true
    is CIFileStatus.SndComplete -> true
    is CIFileStatus.SndCancelled -> true
    is CIFileStatus.SndError -> true
    is CIFileStatus.RcvInvitation -> false
    is CIFileStatus.RcvAccepted -> false
    is CIFileStatus.RcvTransfer -> false
    is CIFileStatus.RcvCancelled -> false
    is CIFileStatus.RcvComplete -> true
    is CIFileStatus.RcvError -> false
  }
  val cancelAction: CancelAction? = when (fileStatus) {
    is CIFileStatus.SndStored -> sndCancelAction
    is CIFileStatus.SndTransfer -> sndCancelAction
    is CIFileStatus.SndComplete ->
      if (fileProtocol == FileProtocol.XFTP) {
        revokeCancelAction
      } else {
        null
      }
    is CIFileStatus.SndCancelled -> null
    is CIFileStatus.SndError -> null
    is CIFileStatus.RcvInvitation -> null
    is CIFileStatus.RcvAccepted -> rcvCancelAction
    is CIFileStatus.RcvTransfer -> rcvCancelAction
    is CIFileStatus.RcvCancelled -> null
    is CIFileStatus.RcvComplete -> null
    is CIFileStatus.RcvError -> null
  }

  companion object {
    fun getSample(
      fileId: Long = 1,
      fileName: String = "test.txt",
      fileSize: Long = 100,
      filePath: String? = "test.txt",
      fileStatus: CIFileStatus = CIFileStatus.RcvComplete
    ): CIFile =
      CIFile(fileId = fileId, fileName = fileName, fileSize = fileSize, filePath = filePath, fileStatus = fileStatus, fileProtocol = FileProtocol.XFTP)
  }
}

@Serializable
class CancelAction(
  val uiActionId: Int,
  val alert: AlertInfo
)

@Serializable
class AlertInfo(
  val titleId: Int,
  val messageId: Int,
  val confirmId: Int
)

private val sndCancelAction: CancelAction = CancelAction(
  uiActionId = R.string.stop_file__action,
  alert = AlertInfo(
    titleId = R.string.stop_snd_file__title,
    messageId = R.string.stop_snd_file__message,
    confirmId = R.string.stop_file__confirm
  )
)
private val revokeCancelAction: CancelAction = CancelAction(
  uiActionId = R.string.revoke_file__action,
  alert = AlertInfo(
    titleId = R.string.revoke_file__title,
    messageId = R.string.revoke_file__message,
    confirmId = R.string.revoke_file__confirm
  )
)
private val rcvCancelAction: CancelAction = CancelAction(
  uiActionId = R.string.stop_file__action,
  alert = AlertInfo(
    titleId = R.string.stop_rcv_file__title,
    messageId = R.string.stop_rcv_file__message,
    confirmId = R.string.stop_file__confirm
  )
)

@Serializable
enum class FileProtocol {
  @SerialName("smp") SMP,
  @SerialName("xftp") XFTP;
}

@Serializable
sealed class CIFileStatus {
  @Serializable @SerialName("sndStored") object SndStored: CIFileStatus()
  @Serializable @SerialName("sndTransfer") class SndTransfer(val sndProgress: Long, val sndTotal: Long): CIFileStatus()
  @Serializable @SerialName("sndComplete") object SndComplete: CIFileStatus()
  @Serializable @SerialName("sndCancelled") object SndCancelled: CIFileStatus()
  @Serializable @SerialName("sndError") object SndError: CIFileStatus()
  @Serializable @SerialName("rcvInvitation") object RcvInvitation: CIFileStatus()
  @Serializable @SerialName("rcvAccepted") object RcvAccepted: CIFileStatus()
  @Serializable @SerialName("rcvTransfer") class RcvTransfer(val rcvProgress: Long, val rcvTotal: Long): CIFileStatus()
  @Serializable @SerialName("rcvComplete") object RcvComplete: CIFileStatus()
  @Serializable @SerialName("rcvCancelled") object RcvCancelled: CIFileStatus()
  @Serializable @SerialName("rcvError") object RcvError: CIFileStatus()
}

@Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
@Serializable(with = ContactBioInfoSerializer::class)
sealed class ContactBioInfo {
  abstract val tag: String
  abstract val notes: String
  abstract val publicKey: String
  abstract val openKeyChainID: String

  @Serializable(with = ContactBioInfoSerializer::class) class ContactBioExtra(override val tag: String, override val notes: String, override val publicKey: String, override val openKeyChainID: String): ContactBioInfo()

  val cmdString: String
    get() = when (this) {
      is ContactBioExtra -> "json ${json.encodeToString(this)}"
    }
}

@Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
@Serializable(with = MsgContentSerializer::class)
sealed class MsgContent {
  abstract val text: String

  @Serializable(with = MsgContentSerializer::class) class MCText(override val text: String): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCLink(override val text: String, val preview: LinkPreview): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCImage(override val text: String, val image: String): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCVideo(override val text: String, val image: String, val duration: Int): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCVoice(override val text: String, val duration: Int): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCFile(override val text: String): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCPublicKey(override val text: String): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCGroupPublicKey(val groupName: String, override val text: String, val neededBy: String): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCPublicKeyRequest(override val text: String, val publicKey: String, val requestedBy: String): MsgContent()
  @Serializable(with = MsgContentSerializer::class) class MCUnknown(val type: String? = null, override val text: String, val json: JsonElement): MsgContent()

  val cmdString: String
    get() =
      if (this is MCUnknown) "json $json" else "json ${json.encodeToString(this)}"
}

@Serializable
class CIGroupInvitation(
  val groupId: Long,
  val groupMemberId: Long,
  val localDisplayName: String,
  val groupProfile: GroupProfile,
  val status: CIGroupInvitationStatus,
) {
  val text: String
    get() = String.format(
      generalGetString(R.string.group_invitation_item_description),
      groupProfile.displayName
    )

  companion object {
    fun getSample(
      groupId: Long = 1,
      groupMemberId: Long = 1,
      localDisplayName: String = "team",
      groupProfile: GroupProfile = GroupProfile.sampleData,
      status: CIGroupInvitationStatus = CIGroupInvitationStatus.Pending
    ): CIGroupInvitation =
      CIGroupInvitation(groupId = groupId, groupMemberId = groupMemberId, localDisplayName = localDisplayName, groupProfile = groupProfile, status = status)
  }
}

@Serializable
enum class CIGroupInvitationStatus {
  @SerialName("pending") Pending,
  @SerialName("accepted") Accepted,
  @SerialName("rejected") Rejected,
  @SerialName("expired") Expired;
}

object ContactBioInfoSerializer: KSerializer<ContactBioInfo> {
  override val descriptor: SerialDescriptor = buildSerialDescriptor("ContactBioInfo", PolymorphicKind.SEALED) {
    element("ContactBioExtra", buildClassSerialDescriptor("ContactBioExtra") {
      element<String>("tag")
      element<String>("notes")
      element<String>("publicKey")
      element<String>("openKeyChainID")
    })
  }

  override fun deserialize(decoder: Decoder): ContactBioInfo {
    require(decoder is JsonDecoder)
    val json = decoder.decodeJsonElement()
    return if (json is JsonObject) {
      val tags = json["tag"]?.jsonPrimitive?.content ?: ""
      val notes = json["notes"]?.jsonPrimitive?.content ?: ""
      val publicKey = json["publicKey"]?.jsonPrimitive?.content ?: ""
      val openKeyChainID = json["openKeyChainID"]?.jsonPrimitive?.content ?: ""
      ContactBioInfo.ContactBioExtra(tags, notes, publicKey, openKeyChainID)
    } else {
      ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
    }
  }

  override fun serialize(encoder: Encoder, value: ContactBioInfo) {
    require(encoder is JsonEncoder)
    val json = when (value) {
      is ContactBioInfo.ContactBioExtra ->
        buildJsonObject {
          put("tag", value.tag)
          put("notes", value.notes)
          put("publicKey", value.publicKey)
          put("openKeyChainID", value.openKeyChainID)
        }
    }
    encoder.encodeJsonElement(json)
  }
}

object MsgContentSerializer: KSerializer<MsgContent> {
  override val descriptor: SerialDescriptor = buildSerialDescriptor("MsgContent", PolymorphicKind.SEALED) {
    element("MCText", buildClassSerialDescriptor("MCText") {
      element<String>("text")
    })
    element("MCLink", buildClassSerialDescriptor("MCLink") {
      element<String>("text")
      element<String>("preview")
    })
    element("MCImage", buildClassSerialDescriptor("MCImage") {
      element<String>("text")
      element<String>("image")
    })
    element("MCVideo", buildClassSerialDescriptor("MCVideo") {
      element<String>("text")
      element<String>("image")
      element<Int>("duration")
    })
    element("MCFile", buildClassSerialDescriptor("MCFile") {
      element<String>("text")
    })
    element("MCPublicKey", buildClassSerialDescriptor("MCPublicKey") {
      element<String>("text")
    })
    element("MCGroupPublicKey", buildClassSerialDescriptor("MCGroupPublicKey") {
      element<String>("groupName")
      element<String>("text")
      element<String>("neededBy")
    })
    element("MCPublicKeyRequest", buildClassSerialDescriptor("MCPublicKeyRequest") {
      element<String>("text")
      element<String>("publicKey")
      element<String>("requestedBy")
    })
    element("MCUnknown", buildClassSerialDescriptor("MCUnknown"))
  }

  override fun deserialize(decoder: Decoder): MsgContent {
    require(decoder is JsonDecoder)
    val json = decoder.decodeJsonElement()
    return if (json is JsonObject) {
      if ("type" in json) {
        val t = json["type"]?.jsonPrimitive?.content ?: ""
        val text = json["text"]?.jsonPrimitive?.content ?: generalGetString(R.string.unknown_message_format)
        val groupName = json["groupName"]?.jsonPrimitive?.content ?: ""
        val publicKey = json["publicKey"]?.jsonPrimitive?.content ?: ""
        val requestedBy = json["requestedBy"]?.jsonPrimitive?.content ?: ""
        val neededBy = json["neededBy"]?.jsonPrimitive?.content ?: ""
        when (t) {
          "text" -> MsgContent.MCText(text)
          "link" -> {
            val preview = Json.decodeFromString<LinkPreview>(json["preview"].toString())
            MsgContent.MCLink(text, preview)
          }
          "image" -> {
            val image = json["image"]?.jsonPrimitive?.content ?: "unknown message format"
            MsgContent.MCImage(text, image)
          }
          "video" -> {
            val image = json["image"]?.jsonPrimitive?.content ?: "unknown message format"
            val duration = json["duration"]?.jsonPrimitive?.intOrNull ?: 0
            MsgContent.MCVideo(text, image, duration)
          }
          "voice" -> {
            val duration = json["duration"]?.jsonPrimitive?.intOrNull ?: 0
            MsgContent.MCVoice(text, duration)
          }
          "file" -> MsgContent.MCFile(text)
          "publicKey" -> {
            MsgContent.MCPublicKey(text)
          }
          "groupPublicKey" -> {
            MsgContent.MCGroupPublicKey(groupName, text, requestedBy)
          }
          "publicKeyRequest" -> {
            MsgContent.MCPublicKeyRequest(text, publicKey, neededBy)
          }
          else -> MsgContent.MCUnknown(t, text, json)
        }
      } else {
        MsgContent.MCUnknown(text = generalGetString(R.string.invalid_message_format), json = json)
      }
    } else {
      MsgContent.MCUnknown(text = generalGetString(R.string.invalid_message_format), json = json)
    }
  }

  override fun serialize(encoder: Encoder, value: MsgContent) {
    require(encoder is JsonEncoder)
    val json = when (value) {
      is MsgContent.MCText ->
        buildJsonObject {
          put("type", "text")
          put("text", value.text)
        }
      is MsgContent.MCLink ->
        buildJsonObject {
          put("type", "link")
          put("text", value.text)
          put("preview", json.encodeToJsonElement(value.preview))
        }
      is MsgContent.MCImage ->
        buildJsonObject {
          put("type", "image")
          put("text", value.text)
          put("image", value.image)
        }
      is MsgContent.MCVideo ->
        buildJsonObject {
          put("type", "video")
          put("text", value.text)
          put("image", value.image)
          put("duration", value.duration)
        }
      is MsgContent.MCVoice ->
        buildJsonObject {
          put("type", "voice")
          put("text", value.text)
          put("duration", value.duration)
        }
      is MsgContent.MCFile ->
        buildJsonObject {
          put("type", "file")
          put("text", value.text)
        }
      is MsgContent.MCPublicKey ->
        buildJsonObject {
          put("type", "publicKey")
          put("text", value.text)
        }
      is MsgContent.MCGroupPublicKey ->
        buildJsonObject {
          put("type", "groupPublicKey")
          put("groupName", value.groupName)
          put("text", value.text)
          put("neededBy", value.neededBy)
        }
      is MsgContent.MCPublicKeyRequest ->
        buildJsonObject {
          put("type", "publicKeyRequest")
          put("text", value.text)
          put("publicKey", value.publicKey)
          put("requestedBy", value.requestedBy)
        }
      is MsgContent.MCUnknown -> value.json
    }
    encoder.encodeJsonElement(json)
  }
}

@Serializable
class FormattedText(val text: String, val format: Format? = null) {
  // TODO make it dependent on simplexLinkMode preference
  fun link(mode: SimplexLinkMode): String? = when (format) {
    is Format.Uri -> text
    is Format.SimplexLink -> if (mode == SimplexLinkMode.BROWSER) text else format.simplexUri
    is Format.Email -> "mailto:$text"
    is Format.Phone -> "tel:$text"
    else -> null
  }

  // TODO make it dependent on simplexLinkMode preference
  fun viewText(mode: SimplexLinkMode): String =
    if (format is Format.SimplexLink && mode == SimplexLinkMode.DESCRIPTION) simplexLinkText(format.linkType, format.smpHosts) else text

  fun simplexLinkText(linkType: SimplexLinkType, smpHosts: List<String>): String =
    "${linkType.description} (${String.format(generalGetString(R.string.simplex_link_connection), smpHosts.firstOrNull() ?: "?")})"
}

@Serializable
sealed class Format {
  @Serializable @SerialName("bold") class Bold: Format()
  @Serializable @SerialName("italic") class Italic: Format()
  @Serializable @SerialName("strikeThrough") class StrikeThrough: Format()
  @Serializable @SerialName("snippet") class Snippet: Format()
  @Serializable @SerialName("secret") class Secret: Format()
  @Serializable @SerialName("colored") class Colored(val color: FormatColor): Format()
  @Serializable @SerialName("uri") class Uri: Format()
  @Serializable @SerialName("simplexLink") class SimplexLink(val linkType: SimplexLinkType, val simplexUri: String, val trustedUri: Boolean, val smpHosts: List<String>): Format()
  @Serializable @SerialName("email") class Email: Format()
  @Serializable @SerialName("phone") class Phone: Format()

  val style: SpanStyle
    @Composable get() = when (this) {
      is Bold -> SpanStyle(fontWeight = FontWeight.Bold)
      is Italic -> SpanStyle(fontStyle = FontStyle.Italic)
      is StrikeThrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
      is Snippet -> SpanStyle(fontFamily = FontFamily.Monospace)
      is Secret -> SpanStyle(color = Color.Transparent, background = SecretColor)
      is Colored -> SpanStyle(color = this.color.uiColor)
      is Uri -> linkStyle
      is SimplexLink -> linkStyle
      is Email -> linkStyle
      is Phone -> linkStyle
    }

  companion object {
    val linkStyle @Composable get() = SpanStyle(color = MaterialTheme.colors.primary, textDecoration = TextDecoration.Underline)
  }
}

@Serializable
enum class SimplexLinkType(val linkType: String) {
  contact("contact"),
  invitation("invitation"),
  group("group");

  val description: String
    get() = generalGetString(
      when (this) {
        contact -> R.string.simplex_link_contact
        invitation -> R.string.simplex_link_invitation
        group -> R.string.simplex_link_group
      }
    )
}

@Serializable
enum class FormatColor(val color: String) {
  red("red"),
  green("green"),
  blue("blue"),
  yellow("yellow"),
  cyan("cyan"),
  magenta("magenta"),
  black("black"),
  white("white");

  val uiColor: Color
    @Composable get() = when (this) {
      red -> Color.Red
      green -> SimplexGreen
      blue -> SimplexBlue
      yellow -> WarningYellow
      cyan -> Color.Cyan
      magenta -> Color.Magenta
      black -> MaterialTheme.colors.onBackground
      white -> MaterialTheme.colors.onBackground
    }
}

@Serializable
class SndFileTransfer

@Serializable
class RcvFileTransfer

@Serializable
class FileTransferMeta

@Serializable
enum class CICallStatus {
  @SerialName("pending") Pending,
  @SerialName("missed") Missed,
  @SerialName("rejected") Rejected,
  @SerialName("accepted") Accepted,
  @SerialName("negotiated") Negotiated,
  @SerialName("progress") Progress,
  @SerialName("ended") Ended,
  @SerialName("error") Error;

  fun text(sec: Int): String = when (this) {
    Pending -> generalGetString(R.string.callstatus_calling)
    Missed -> generalGetString(R.string.callstatus_missed)
    Rejected -> generalGetString(R.string.callstatus_rejected)
    Accepted -> generalGetString(R.string.callstatus_accepted)
    Negotiated -> generalGetString(R.string.callstatus_connecting)
    Progress -> generalGetString(R.string.callstatus_in_progress)
    Ended -> String.format(generalGetString(R.string.callstatus_ended), durationText(sec))
    Error -> generalGetString(R.string.callstatus_error)
  }
}

fun durationText(sec: Int): String {
  val s = sec % 60
  val m = sec / 60
  return if (m < 60) "%02d:%02d".format(m, s) else "%02d:%02d:%02d".format(m / 60, m % 60, s)
}

@Serializable
sealed class MsgErrorType {
  @Serializable @SerialName("msgSkipped") class MsgSkipped(val fromMsgId: Long, val toMsgId: Long): MsgErrorType()
  @Serializable @SerialName("msgBadId") class MsgBadId(val msgId: Long): MsgErrorType()
  @Serializable @SerialName("msgBadHash") class MsgBadHash: MsgErrorType()
  @Serializable @SerialName("msgDuplicate") class MsgDuplicate: MsgErrorType()

  val text: String
    get() = when (this) {
      is MsgSkipped -> String.format(generalGetString(R.string.integrity_msg_skipped), toMsgId - fromMsgId + 1)
      is MsgBadHash -> generalGetString(R.string.integrity_msg_bad_hash) // not used now
      is MsgBadId -> generalGetString(R.string.integrity_msg_bad_id) // not used now
      is MsgDuplicate -> generalGetString(R.string.integrity_msg_duplicate) // not used now
    }
}

@Serializable
sealed class RcvGroupEvent {
  @Serializable @SerialName("memberAdded") class MemberAdded(val groupMemberId: Long, val profile: Profile): RcvGroupEvent()
  @Serializable @SerialName("memberConnected") class MemberConnected: RcvGroupEvent()
  @Serializable @SerialName("memberLeft") class MemberLeft: RcvGroupEvent()
  @Serializable @SerialName("memberRole") class MemberRole(val groupMemberId: Long, val profile: Profile, val role: GroupMemberRole): RcvGroupEvent()
  @Serializable @SerialName("userRole") class UserRole(val role: GroupMemberRole): RcvGroupEvent()
  @Serializable @SerialName("memberDeleted") class MemberDeleted(val groupMemberId: Long, val profile: Profile): RcvGroupEvent()
  @Serializable @SerialName("userDeleted") class UserDeleted: RcvGroupEvent()
  @Serializable @SerialName("groupDeleted") class GroupDeleted: RcvGroupEvent()
  @Serializable @SerialName("groupUpdated") class GroupUpdated(val groupProfile: GroupProfile): RcvGroupEvent()
  @Serializable @SerialName("invitedViaGroupLink") class InvitedViaGroupLink: RcvGroupEvent()

  val text: String
    get() = when (this) {
      is MemberAdded -> String.format(generalGetString(R.string.rcv_group_event_member_added), profile.profileViewName)
      is MemberConnected -> generalGetString(R.string.rcv_group_event_member_connected)
      is MemberLeft -> generalGetString(R.string.rcv_group_event_member_left)
      is MemberRole -> String.format(generalGetString(R.string.rcv_group_event_changed_member_role), profile.profileViewName, role.text)
      is UserRole -> String.format(generalGetString(R.string.rcv_group_event_changed_your_role), role.text)
      is MemberDeleted -> String.format(generalGetString(R.string.rcv_group_event_member_deleted), profile.profileViewName)
      is UserDeleted -> generalGetString(R.string.rcv_group_event_user_deleted)
      is GroupDeleted -> generalGetString(R.string.rcv_group_event_group_deleted)
      is GroupUpdated -> generalGetString(R.string.rcv_group_event_updated_group_profile)
      is InvitedViaGroupLink -> generalGetString(R.string.rcv_group_event_invited_via_your_group_link)
    }
}

@Serializable
sealed class SndGroupEvent {
  @Serializable @SerialName("memberRole") class MemberRole(val groupMemberId: Long, val profile: Profile, val role: GroupMemberRole): SndGroupEvent()
  @Serializable @SerialName("userRole") class UserRole(val role: GroupMemberRole): SndGroupEvent()
  @Serializable @SerialName("memberDeleted") class MemberDeleted(val groupMemberId: Long, val profile: Profile): SndGroupEvent()
  @Serializable @SerialName("userLeft") class UserLeft: SndGroupEvent()
  @Serializable @SerialName("groupUpdated") class GroupUpdated(val groupProfile: GroupProfile): SndGroupEvent()

  val text: String
    get() = when (this) {
      is MemberRole -> String.format(generalGetString(R.string.snd_group_event_changed_member_role), profile.profileViewName, role.text)
      is UserRole -> String.format(generalGetString(R.string.snd_group_event_changed_role_for_yourself), role.text)
      is MemberDeleted -> String.format(generalGetString(R.string.snd_group_event_member_deleted), profile.profileViewName)
      is UserLeft -> generalGetString(R.string.snd_group_event_user_left)
      is GroupUpdated -> generalGetString(R.string.snd_group_event_group_profile_updated)
    }
}

@Serializable
sealed class RcvConnEvent {
  @Serializable @SerialName("switchQueue") class SwitchQueue(val phase: SwitchPhase): RcvConnEvent()

  val text: String
    get() = when (this) {
      is SwitchQueue -> when (phase) {
        SwitchPhase.Completed -> generalGetString(R.string.rcv_conn_event_switch_queue_phase_completed)
        else -> generalGetString(R.string.rcv_conn_event_switch_queue_phase_changing)
      }
    }
}

@Serializable
sealed class SndConnEvent {
  @Serializable @SerialName("switchQueue") class SwitchQueue(val phase: SwitchPhase, val member: GroupMemberRef? = null): SndConnEvent()

  val text: String
    get() = when (this) {
      is SwitchQueue -> {
        member?.profile?.profileViewName?.let {
          return when (phase) {
            SwitchPhase.Completed -> String.format(generalGetString(R.string.snd_conn_event_switch_queue_phase_completed_for_member), it)
            else -> String.format(generalGetString(R.string.snd_conn_event_switch_queue_phase_changing_for_member), it)
          }
        }
        when (phase) {
          SwitchPhase.Completed -> generalGetString(R.string.snd_conn_event_switch_queue_phase_completed)
          else -> generalGetString(R.string.snd_conn_event_switch_queue_phase_changing)
        }
      }
    }
}

@Serializable
enum class SwitchPhase {
  @SerialName("started") Started,
  @SerialName("confirmed") Confirmed,
  @SerialName("completed") Completed
}

sealed class ChatItemTTL: Comparable<ChatItemTTL?> {
  object Day: ChatItemTTL()
  object Week: ChatItemTTL()
  object Month: ChatItemTTL()
  data class Seconds(val secs: Long): ChatItemTTL()
  object None: ChatItemTTL()

  override fun compareTo(other: ChatItemTTL?): Int = (seconds ?: Long.MAX_VALUE).compareTo(other?.seconds ?: Long.MAX_VALUE)

  val seconds: Long?
    get() =
      when (this) {
        is None -> null
        is Day -> 86400L
        is Week -> 7 * 86400L
        is Month -> 30 * 86400L
        is Seconds -> secs
      }

  companion object {
    fun fromSeconds(seconds: Long?): ChatItemTTL =
      when (seconds) {
        null -> None
        86400L -> Day
        7 * 86400L -> Week
        30 * 86400L -> Month
        else -> Seconds(seconds)
      }
  }
}
