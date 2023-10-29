package chat.echo.app.views.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.FileProvider
import chat.echo.app.*
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.PreviewTextColor
import chat.echo.app.views.bottomsheet.ChooseAttachmentBottomSheetView
import chat.echo.app.views.call.*
import chat.echo.app.views.chat.group.GroupChatInfoView
import chat.echo.app.views.chat.group.GroupMemberInfoView
import chat.echo.app.views.chat.item.*
import chat.echo.app.views.chatlist.*
import chat.echo.app.views.helpers.*
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.math.sign

var keyLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null

@Composable
fun ChatView(chatId: String, chatModel: ChatModel) {
  //var activeChat by remember { mutableStateOf(chatModel.chats.firstOrNull { chat -> chat.chatInfo.id == chatModel.chatId.value }) }
  val activeChat = remember { mutableStateOf(chatModel.chats.firstOrNull { chat -> chat.chatInfo.id == chatId }) }
  val searchText = rememberSaveable { mutableStateOf("") }
  val user = chatModel.currentUser.value
  val useLinkPreviews = chatModel.controller.appPrefs.privacyLinkPreviews.get()
  val composeState = rememberSaveable(saver = ComposeState.saver()) {
    mutableStateOf(ComposeState(useLinkPreviews = useLinkPreviews))
  }
  val attachmentOption = rememberSaveable { mutableStateOf<AttachmentOption?>(null) }
  val attachmentBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val scope = rememberCoroutineScope()
  val OPEN_KEY_CHAIN_AVAILABLE_PUBLIC_KEYS = "open_key_available_public_keys"
  val gson = Gson()
  var isPublicKeyRefresh by remember { mutableStateOf(false) }
  val encryptedMember = remember { mutableStateOf("") }

  fun saveUserPublicKey(chatModel: ChatModel, ct: Contact, featuresAllowed: ContactFeaturesAllowed) {
    withApi {
      val prefs = contactFeaturesAllowedToPrefs(featuresAllowed)
      val toContact = chatModel.controller.apiSetContactPrefs(ct.contactId, prefs)
      if (toContact != null) {
        chatModel.updateContact(toContact)
      }
    }
  }

  fun getEncryptedUsers(): List<GroupMember> {
    return chatModel.groupMembers
      .filter { it.memberStatus != GroupMemberStatus.MemLeft && it.memberStatus != GroupMemberStatus.MemRemoved }
      .filter { it.memberProfile.localAlias != "" && json.decodeFromString(ContactBioInfoSerializer, it.memberProfile.localAlias).openKeyChainID != "" }
      .sortedBy { it.displayName.lowercase() }
  }

  LaunchedEffect(Unit) {
    // snapshotFlow here is because it reacts much faster on changes in chatModel.chatId.value.
    // With LaunchedEffect(chatModel.chatId.value) there is a noticeable delay before reconstruction of the view
    launch {
      snapshotFlow { chatModel.chatId.value }
        .distinctUntilChanged()
        .collect {
          if (activeChat.value?.id != chatModel.chatId.value && chatModel.chatId.value != null) {
            // Redisplay the whole hierarchy if the chat is different to make going from groups to direct chat working correctly
            // Also for situation when chatId changes after clicking in notification, etc
            activeChat.value = chatModel.getChat(chatModel.chatId.value!!)
          }
          markUnreadChatAsRead(activeChat, chatModel)
        }
    }
    launch {
      snapshotFlow {
        /**
         * It's possible that in some cases concurrent modification can happen on [ChatModel.chats] list.
         * In this case only error log will be printed here (no crash).
         * TODO: Re-write [ChatModel.chats] logic to a new list assignment instead of changing content of mutableList to prevent that
         * */
        try {
          chatModel.chats.firstOrNull { chat -> chat.chatInfo.id == chatModel.chatId.value }
        } catch (e: ConcurrentModificationException) {
          Log.e(TAG, e.stackTraceToString())
          null
        }
      }
        .distinctUntilChanged()
        // Only changed chatInfo is important thing. Other properties can be skipped for reducing recompositions
        .filter { it?.chatInfo != activeChat.value?.chatInfo && it != null }
        .collect { activeChat.value = it }
    }
    launch {
      snapshotFlow { chatModel.isPublicKeyRefreshed.value }
        .distinctUntilChanged()
        .collect {
          if (it) {
            if (activeChat.value != null) {
              val chat = activeChat.value!!
              if (chat.chatInfo is ChatInfo.Direct) {
                // chat.
                //val contact = mutableStateOf { derivedStateOf { (chatModel.getContactChat(chat.chatInfo.apiId)?.chatInfo as? ChatInfo.Direct)?.contact } }
                //  val ct = contact.value ?: null
                //  var featuresAllowed = mutableStateOf(ct, stateSaver = serializableSaver()) { mutableStateOf(contactUserPrefsToFeaturesAllowed(ct.mergedPreferences)) }

                withBGApi {
                  val contacts = mutableListOf<ContactInfo>()
                  var contactBioInfo = if (chat.chatInfo.localAlias != "") {
                    json.decodeFromString(ContactBioInfoSerializer, chat.chatInfo.localAlias)
                  } else {
                    ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
                  }

                  if (contactBioInfo.publicKey != "" && contactBioInfo.openKeyChainID == "") {
                    Log.i(TAG, "ChatView: contact is " + chat.chatInfo.contact.contactId)
                    var thisContact = ContactInfo(chat.chatInfo.contact.contactId, contactBioInfo.openKeyChainID, contactBioInfo.publicKey)
                    contacts.add(thisContact)
                    refreshUserPublicKeys(
                      chatModel,
                      contacts = contacts,
                      keyLauncher = keyLauncher!!
                    )
                  }
                }
              }
              if (chat.chatInfo is ChatInfo.Group) {
                withBGApi {
                  val contacts = mutableListOf<ContactInfo>()
                  setGroupMembers(chat.chatInfo.groupInfo, chatModel)
                  var unencryptedMembers = getUnencryptedUsers(chatModel)

                  for (member in unencryptedMembers) {
                    val memberProfile = member.memberProfile
                    var contactBioInfo = if (memberProfile.localAlias != "") {
                      json.decodeFromString(ContactBioInfoSerializer, memberProfile.localAlias)
                    } else {
                      ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
                    }

                    Log.i(TAG, "ChatView: member profile is " + memberProfile.localAlias + " member name is " + memberProfile.displayName + " " + memberProfile.fullName + " " + contactBioInfo.publicKey)

                    if (contactBioInfo.publicKey != "" && contactBioInfo.openKeyChainID == "") {
                      var thisContact = ContactInfo(memberProfile.profileId, contactBioInfo.openKeyChainID, contactBioInfo.publicKey)
                      contacts.add(thisContact)
                    }
                    //Log.i("TAG", "Called here " + (contactBioInfo.publicKey != "" && contactBioInfo.openKeyChainID == ""))
                    Log.i("TAG", "ChatView: contact are " + contacts.toString())

                    if (contacts.isNotEmpty()) {
                      refreshUserPublicKeys(
                        chatModel,
                        contacts = contacts,
                        keyLauncher = keyLauncher!!
                      )
                    }
                  }
                }
              }
            }
          }
        }
    }
    launch {
      if (activeChat.value != null) {
        val chat = activeChat.value!!
        if (chat.chatInfo is ChatInfo.Direct) {
          // chat.
          //val contact = mutableStateOf { derivedStateOf { (chatModel.getContactChat(chat.chatInfo.apiId)?.chatInfo as? ChatInfo.Direct)?.contact } }
          //  val ct = contact.value ?: null
          //  var featuresAllowed = mutableStateOf(ct, stateSaver = serializableSaver()) { mutableStateOf(contactUserPrefsToFeaturesAllowed(ct.mergedPreferences)) }
          val contacts = mutableListOf<ContactInfo>()
          var contactBioInfo = if (chat.chatInfo.localAlias != "") {
            json.decodeFromString(ContactBioInfoSerializer, chat.chatInfo.localAlias)
          } else {
            ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
          }

          if (contactBioInfo.publicKey != "" && contactBioInfo.openKeyChainID == "") {
            Log.i(TAG, "ChatView: contact is " + chat.chatInfo.contact.contactId)
            var thisContact = ContactInfo(chat.chatInfo.contact.contactId, contactBioInfo.openKeyChainID, contactBioInfo.publicKey)
            contacts.add(thisContact)
            refreshUserPublicKeys(
              chatModel,
              contacts = contacts,
              keyLauncher = keyLauncher!!
            )
          }
        }
        if (chat.chatInfo is ChatInfo.Group) {
          withApi {
            val contacts = mutableListOf<ContactInfo>()
            setGroupMembers(chat.chatInfo.groupInfo, chatModel)
            var unencryptedMembers = getUnencryptedUsers(chatModel)

            for (member in unencryptedMembers) {
              val memberProfile = member.memberProfile
              Log.i(TAG, "ChatView: member profile is " + memberProfile.localAlias)
              var contactBioInfo = if (memberProfile.localAlias != "") {
                json.decodeFromString(ContactBioInfoSerializer, memberProfile.localAlias)
              } else {
                ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
              }

              Log.i(TAG, "ChatView: member profile is " + memberProfile.localAlias + " member name is " + memberProfile.displayName + " " + memberProfile.fullName + " " + contactBioInfo.publicKey)

              if (contactBioInfo.publicKey != "" && contactBioInfo.openKeyChainID == "") {
                var thisContact = ContactInfo(memberProfile.profileId, contactBioInfo.openKeyChainID, contactBioInfo.publicKey)
                contacts.add(thisContact)
              }
              //Log.i("TAG", "Called here " + (contactBioInfo.publicKey != "" && contactBioInfo.openKeyChainID == ""))
              Log.i("TAG", "ChatView: contact are " + contacts.toString())

              if (contacts.isNotEmpty()) {
                refreshUserPublicKeys(
                  chatModel,
                  contacts = contacts,
                  keyLauncher = keyLauncher!!
                )
              }
            }
          }
        }
      }
    }
  }
  /*  */
  /**
   * DO this when the user first enters the application.
   * We need to check if we need to refresh the public key
   *
   *//*
  LaunchedEffect(Unit) {
    val chat = activeChat!!
  }

  */
  /**
   * DO this when the user is already in the chat, in the case where a contact send
   * in a public key when the user is currently engaged with him.
   * We need to check if we need to refresh the public key
   *
   *//*
  LaunchedEffect(chatModel.isPublicKeyRefreshed.value) {
    if (chatModel.isPublicKeyRefreshed.value) {
      chatModel.isPublicKeyRefreshed.value = false
      if (activeChat != null) {
        val chat = activeChat!!
        if (chat.chatInfo is ChatInfo.Direct) {
          // chat.
          //val contact = mutableStateOf { derivedStateOf { (chatModel.getContactChat(chat.chatInfo.apiId)?.chatInfo as? ChatInfo.Direct)?.contact } }
          //  val ct = contact.value ?: null
          //  var featuresAllowed = mutableStateOf(ct, stateSaver = serializableSaver()) { mutableStateOf(contactUserPrefsToFeaturesAllowed(ct.mergedPreferences)) }
          val contacts = mutableListOf<ContactInfo>()
          var contactBioInfo = if (chat.chatInfo.localAlias != "") {
            json.decodeFromString(ContactBioInfoSerializer, chat.chatInfo.localAlias)
          } else {
            ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
          }

          if (contactBioInfo.publicKey != "" && contactBioInfo.openKeyChainID == "") {
            Log.i(TAG, "ChatView: contact is " + chat.chatInfo.contact.contactId)
            var thisContact = ContactInfo(chat.chatInfo.contact.contactId, contactBioInfo.openKeyChainID, contactBioInfo.publicKey)
            contacts.add(thisContact)
            refreshUserPublicKeys(
              chatModel,
              contacts = contacts,
              keyLauncher = keyLauncher!!
            )
          }
        }
        if (chat.chatInfo is ChatInfo.Group) {
          val contacts = mutableListOf<ContactInfo>()
          var unencryptedMembers = getUnencryptedUsers(chatModel)

          for (member in unencryptedMembers) {
            val memberProfile = member.memberProfile
            var contactBioInfo = if (memberProfile.localAlias != "") {
              json.decodeFromString(ContactBioInfoSerializer, memberProfile.localAlias)
            } else {
              ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
            }

            Log.i(TAG, "ChatView: member profile is " + memberProfile.localAlias + " member name is " + memberProfile.displayName + " " + memberProfile.fullName + " " + contactBioInfo.publicKey)

            if (contactBioInfo.publicKey != "" && contactBioInfo.openKeyChainID == "") {
              var thisContact = ContactInfo(memberProfile.profileId, contactBioInfo.openKeyChainID, contactBioInfo.publicKey)
              contacts.add(thisContact)
            }
          }
          //Log.i("TAG", "Called here " + (contactBioInfo.publicKey != "" && contactBioInfo.openKeyChainID == ""))
          Log.i("TAG", "ChatView: contact are " + contacts.toString())

          if (contacts.isNotEmpty()) {
            refreshUserPublicKeys(
              chatModel,
              contacts = contacts,
              keyLauncher = keyLauncher!!
            )
          }
        }
      }
    }
  }*/

  val mainScope = MainScope();

  if (activeChat.value == null || user == null) {
    chatModel.chatId.value = null
  } else {
    val chat = activeChat.value!!
    chatModel.activeChat.value = chat

    BackHandler {
      chatModel.chatId.value = null
    }


      keyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
          mainScope.launch {
            if (result.data != null) {
              val jsonContactWithAvailablePublicKey = result.data!!.getStringExtra(OPEN_KEY_CHAIN_AVAILABLE_PUBLIC_KEYS)
              val myType = object: TypeToken<List<ContactInfo>>() {}.type
              val gson = Gson()
              val contacts = gson.fromJson<List<ContactInfo>>(jsonContactWithAvailablePublicKey, myType)
              if (chat.chatInfo is ChatInfo.Direct) {
                var contactBioExtra = if (chat.chatInfo.localAlias != "") {
                  json.decodeFromString(ContactBioInfoSerializer, chat.chatInfo.localAlias)
                } else {
                  ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
                }

                Log.i(TAG, "ChatView: refreshed contact si " + contacts[0].openKeyChainID)

                if (contacts != null && contacts.isNotEmpty()) {
                  withBGApi {
                    val contact = contacts[0]
                    contactBioExtra = ContactBioInfo.ContactBioExtra(contactBioExtra.tag, contactBioExtra.notes, contact.publicKey, contact.openKeyChainID)
                    setContactAlias(chat.chatInfo.contact.contactId, json.encodeToString(ContactBioInfoSerializer, contactBioExtra), chatModel) { chat ->
                      activeChat.value = chat
                      chatModel.isChatRefresh.value = false
                    }
                    Log.i(TAG, "ChatView: contact is " + contact.toString())
                    Log.i(TAG, "ChatView: chat info local alias is  " + chat.chatInfo.localAlias)
                    // chatModel.controller.apiSetContactAlias(contact.apiId, json.encodeToString(ContactBioInfoSerializer, contactBioExtra))
                  }
                }
              }
              if (chat.chatInfo is ChatInfo.Group) {
                var contactBioExtra = if (chat.chatInfo.localAlias != "") {
                  json.decodeFromString(ContactBioInfoSerializer, chat.chatInfo.localAlias)
                } else {
                  ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
                }

                if (contacts != null && contacts.isNotEmpty()) {
                  withBGApi {
                    val contact = contacts[0]
                    contactBioExtra = ContactBioInfo.ContactBioExtra(contactBioExtra.tag, contactBioExtra.notes, contact.publicKey, contact.openKeyChainID)
                    setContactAlias(contact.apiId, json.encodeToString(ContactBioInfoSerializer, contactBioExtra), chatModel) { chat ->
                      activeChat.value = chat
                      chatModel.isChatRefresh.value = false
                    }
                    // chatModel.controller.apiSetContactAlias(contact.apiId, json.encodeToString(ContactBioInfoSerializer, contactBioExtra))
                  }
                }
              }
            }
          }
        }
      }
    // We need to have real unreadCount value for displaying it inside top right button
    // Having activeChat reloaded on every change in it is inefficient (UI lags)
    val unreadCount = remember {
      derivedStateOf {
        chatModel.chats.firstOrNull { chat -> chat.chatInfo.id == chatId }?.chatStats?.unreadCount ?: 0
      }
    }

    ChatLayout(
      user,
      chat,
      chatModel,
      unreadCount,
      composeState,
      composeView = {
        if (chat.chatInfo.sendMsgEnabled) {
          ComposeView(
            chatModel, chat, { activeChat.value = it }, composeState, attachmentOption,
            showChooseAttachment = { scope.launch { attachmentBottomSheetState.show() } }
          )
        }
      },
      attachmentOption,
      scope,
      attachmentBottomSheetState,
      chatModel.chatItems,
      searchText,
      useLinkPreviews = useLinkPreviews,
      linkMode = chatModel.simplexLinkMode.value,
      chatModelIncognito = chatModel.incognito.value,
      back = {
        chatModel.chatId.value = null
      },
      info = {
        withApi {
          val cInfo = chat.chatInfo
          if (cInfo is ChatInfo.Direct) {
            val contactInfo = chatModel.controller.apiContactInfo(cInfo.apiId)
            ModalManager.shared.showCustomModal(true) { close ->
              ChatInfoView(chatModel, cInfo.contact, contactInfo?.first, contactInfo?.second, chat.chatInfo.localAlias, close) {
                activeChat.value = it
              }
            }
          } else if (cInfo is ChatInfo.Group) {
            // setGroupMembers(cInfo.groupInfo, chatModel)
            ModalManager.shared.showCustomModal { close ->
              GroupChatInfoView(chatModel, close)
            }
          }
        }
      },
      showMemberInfo = { groupInfo: GroupInfo, member: GroupMember ->
        withBGApi {
          val stats = chatModel.controller.apiGroupMemberInfo(groupInfo.groupId, member.groupMemberId)
          ModalManager.shared.showCustomModal { close ->
            GroupMemberInfoView(groupInfo, member, stats, chatModel, close, close)
          }
        }
      },
      loadPrevMessages = { cInfo ->
        val c = chatModel.getChat(cInfo.id)
        val firstId = chatModel.chatItems.firstOrNull()?.id
        if (c != null && firstId != null) {
          withApi {
            apiLoadPrevMessages(c.chatInfo, chatModel, firstId, searchText.value)
          }
        }
      },
      deleteMessage = { itemId, mode ->
        withApi {
          val cInfo = chat.chatInfo
          val toItem = chatModel.controller.apiDeleteChatItem(
            type = cInfo.chatType,
            id = cInfo.apiId,
            itemId = itemId,
            mode = mode
          )
          if (toItem != null) chatModel.removeChatItem(cInfo, toItem.deletedChatItem.chatItem)
        }
      },
      receiveFile = { fileId ->
        withApi { chatModel.controller.receiveFile(user, fileId) }
      },
      joinGroup = { groupId ->
        withApi { chatModel.controller.apiJoinGroup(groupId) }
      },
      startCall = { media ->
        val cInfo = chat.chatInfo
        if (cInfo is ChatInfo.Direct) {
          chatModel.activeCall.value = Call(contact = cInfo.contact, callState = CallState.WaitCapabilities, localMedia = media)
          chatModel.showCallView.value = true
          chatModel.callCommand.value = WCallCommand.Capabilities
        }
      },
      acceptCall = { contact ->
        val invitation = chatModel.callInvitations.remove(contact.id)
        if (invitation == null) {
          AlertManager.shared.showAlertMsg("Call already ended!")
        } else {
          chatModel.callManager.acceptIncomingCall(invitation = invitation)
        }
      },
      addMembers = { groupInfo ->
        withApi {
          setGroupMembers(groupInfo, chatModel)
          ModalManager.shared.showModalCloseable(true) { close ->
            //AddGroupMembersView(groupInfo, chatModel, close)
          }
        }
      },
      markRead = { range, unreadCountAfter ->
        chatModel.markChatItemsRead(chat.chatInfo, range, unreadCountAfter)
        chatModel.controller.ntfManager.cancelNotificationsForChat(chat.id)
        withApi {
          chatModel.controller.apiChatRead(
            chat.chatInfo.chatType,
            chat.chatInfo.apiId,
            range
          )
        }
      },
      changeNtfsState = { enabled, currentValue -> changeNtfsStatePerChat(enabled, currentValue, chat, chatModel) },
      onSearchValueChanged = { value ->
        if (searchText.value == value) return@ChatLayout
        val c = chatModel.getChat(chat.chatInfo.id) ?: return@ChatLayout
        withApi {
          apiFindMessages(c.chatInfo, chatModel, value)
          searchText.value = value
        }
      }
    ) {
      activeChat.value = it
    }
  }
}

data class CIListState(
  val scrolled: Boolean,
  val itemCount: Int,
  val keyboardState: KeyboardState
)

val CIListStateSaver = run {
  val scrolledKey = "scrolled"
  val countKey = "itemCount"
  val keyboardKey = "keyboardState"
  mapSaver(
    save = { mapOf(scrolledKey to it.scrolled, countKey to it.itemCount, keyboardKey to it.keyboardState) },
    restore = { CIListState(it[scrolledKey] as Boolean, it[countKey] as Int, it[keyboardKey] as KeyboardState) }
  )
}

@Composable
fun BoxWithConstraintsScope.ChatItemsList(
  user: User,
  chat: Chat,
  chatModel: ChatModel,
  unreadCount: State<Int>,
  composeState: MutableState<ComposeState>,
  chatItems: List<ChatItem>,
  searchValue: State<String>,
  useLinkPreviews: Boolean,
  linkMode: SimplexLinkMode,
  chatModelIncognito: Boolean,
  showMemberInfo: (GroupInfo, GroupMember) -> Unit,
  loadPrevMessages: (ChatInfo) -> Unit,
  deleteMessage: (Long, CIDeleteMode) -> Unit,
  receiveFile: (Long) -> Unit,
  joinGroup: (Long) -> Unit,
  acceptCall: (Contact) -> Unit,
  markRead: (CC.ItemRange, unreadCountAfter: Int?) -> Unit,
  setFloatingButton: (@Composable () -> Unit) -> Unit,
  onChatUpdated: (Chat) -> Unit
) {
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val uriHandler = LocalUriHandler.current
  val cxt = LocalContext.current
  chatModel.localContactViewModel?.getLocalContact(chat.chatInfo.id)
  // Helps to scroll to bottom after moving from Group to Direct chat
  // and prevents scrolling to bottom on orientation change
  var shouldAutoScroll by rememberSaveable { mutableStateOf(true) }
  LaunchedEffect(chat.chatInfo.apiId, chat.chatInfo.chatType, shouldAutoScroll) {
    if (shouldAutoScroll && listState.firstVisibleItemIndex != 0) {
      scope.launch { listState.scrollToItem(0) }
    }
    // Don't autoscroll next time until it will be needed
    shouldAutoScroll = false
  }
  var prevSearchEmptiness by rememberSaveable { mutableStateOf(searchValue.value.isEmpty()) }
  // Scroll to bottom when search value changes from something to nothing and back
  LaunchedEffect(searchValue.value.isEmpty()) {
    // They are equal when orientation was changed, don't need to scroll.
    // LaunchedEffect unaware of this event since it uses remember, not rememberSaveable
    if (prevSearchEmptiness == searchValue.value.isEmpty()) return@LaunchedEffect
    prevSearchEmptiness = searchValue.value.isEmpty()

    if (listState.firstVisibleItemIndex != 0) {
      scope.launch { listState.scrollToItem(0) }
    }
  }

  PreloadItems(listState, ChatPagination.UNTIL_PRELOAD_COUNT, chat, chatItems) { c ->
    Log.i(TAG, "ChatItemsList: preloaded")
    loadPrevMessages(c.chatInfo)
  }

  Spacer(Modifier.size(8.dp))
  val reversedChatItems by remember { derivedStateOf { chatItems.reversed() } }
  val maxHeightRounded = with(LocalDensity.current) { maxHeight.roundToPx() }
  val scrollToItem: (Long) -> Unit = { itemId: Long ->
    val index = reversedChatItems.indexOfFirst { it.id == itemId }
    if (index != -1) {
      scope.launch { listState.animateScrollToItem(kotlin.math.min(reversedChatItems.lastIndex, index + 1), -maxHeightRounded) }
    }
  }
  if (reversedChatItems.isNotEmpty()) {
    LazyColumn(Modifier.align(Alignment.BottomCenter), state = listState, reverseLayout = true) {
      itemsIndexed(reversedChatItems) { i, cItem ->
        CompositionLocalProvider(
          // Makes horizontal and vertical scrolling to coexist nicely.
          // With default touchSlop when you scroll LazyColumn, you can unintentionally open reply view
          LocalViewConfiguration provides LocalViewConfiguration.current.bigTouchSlop()
        ) {
          val dismissState = rememberDismissState(initialValue = DismissValue.Default) { false }
          val directions = setOf(DismissDirection.EndToStart)
          val swipeableModifier = SwipeToDismissModifier(
            state = dismissState,
            directions = directions,
            swipeDistance = with(LocalDensity.current) { 30.dp.toPx() },
          )
          val swipedToEnd = (dismissState.overflow.value > 0f && directions.contains(DismissDirection.StartToEnd))
          val swipedToStart = (dismissState.overflow.value < 0f && directions.contains(DismissDirection.EndToStart))
          if (dismissState.isAnimationRunning && (swipedToStart || swipedToEnd)) {
            LaunchedEffect(Unit) {
              scope.launch {
                if (composeState.value.editing) {
                  composeState.value = ComposeState(contextItem = ComposeContextItem.QuotedItem(cItem), useLinkPreviews = useLinkPreviews)
                } else {
                  composeState.value = composeState.value.copy(contextItem = ComposeContextItem.QuotedItem(cItem))
                }
              }
            }
          }
          val provider = {
            providerForGallery(i, chatItems, cItem.id) { indexInReversed ->
              scope.launch {
                listState.scrollToItem(
                  kotlin.math.min(reversedChatItems.lastIndex, indexInReversed + 1),
                  -maxHeightRounded
                )
              }
            }
          }
          if (chat.chatInfo is ChatInfo.Group) {
            when (val mc = cItem.content.msgContent) {
              is MsgContent.MCGroupPublicKey -> {
                Box(
                  Modifier
                    .padding(
                      start = 50.dp,
                      end = 50.dp
                    )
                    .fillMaxWidth(),
                  contentAlignment = Alignment.Center
                ) {
                  GroupPublicKeyChatItemView(cModel = chatModel, cInfo = chat.chatInfo, groupInfo = chat.chatInfo.groupInfo, cItem = cItem)
                }
              }

              is MsgContent.MCPublicKeyRequest -> {
                Box(
                  Modifier
                    .padding(
                      start = 50.dp,
                      end = 50.dp
                    )
                    .fillMaxWidth(),
                  contentAlignment = Alignment.Center
                ) {
                  PublicKeyRequestChatItemView(messageContent = mc, cModel = chatModel, cInfo = chat.chatInfo, cItem = cItem, chat = chat)
                }
              }

              else -> {
                val prevItem = if (i < reversedChatItems.lastIndex) reversedChatItems[i + 1] else null
                if (cItem.chatDir is CIDirection.GroupRcv) {
                  val showMember = if (cItem.chatDir is CIDirection.GroupRcv) {
                    val member = cItem.chatDir.groupMember
                    showMemberImage(member, prevItem)
                  } else {
                    showSenderImage(prevItem)
                  }
                  if (!isAnnouncement(cItem)) {
                    Row(Modifier.padding(start = 8.dp, end = 66.dp).then(swipeableModifier)) {
                      /*if (showMember) {
                        val contactId = member.memberContactId
                        if (contactId == null) {
                          MemberImage(member)
                        }
                        else {
                          Box(
                            Modifier
                              .clip(CircleShape)
                              .clickable { showMemberInfo(chat.chatInfo.groupInfo, member) }
                          ) {
                            MemberImage(member)
                          }
                        }
                        Spacer(Modifier.size(4.dp))
                      } else {
                        Spacer(Modifier.size(42.dp))
                      }*/
                      ChatItemView(user, chatModel, chat.chatInfo, cItem, composeState, cxt, uriHandler, provider, showMember = showMember, linkMode = linkMode, chatModelIncognito = chatModelIncognito, useLinkPreviews = useLinkPreviews, deleteMessage = deleteMessage, receiveFile = receiveFile, joinGroup = {}, acceptCall = acceptCall, scrollToItem = scrollToItem)
                    }
                  } else {
                    ChatItemView(user, chatModel, chat.chatInfo, cItem, composeState, cxt, uriHandler, provider, chatModelIncognito = chatModelIncognito, useLinkPreviews = useLinkPreviews, linkMode = linkMode, deleteMessage = deleteMessage, receiveFile = receiveFile, joinGroup = {}, acceptCall = acceptCall, scrollToItem = scrollToItem)
                  }
                } else if (cItem.chatDir is CIDirection.GroupSnd) {
                  if (!isAnnouncement(cItem)) {
                    val showSenderImage = showSenderImage(prevItem)
                    Box(Modifier.padding(start = 86.dp, end = 12.dp).then(swipeableModifier)) {
                      ChatItemView(user, chatModel, chat.chatInfo, cItem, composeState, cxt, uriHandler, provider, chatModelIncognito = chatModelIncognito, useLinkPreviews = useLinkPreviews, linkMode = linkMode, deleteMessage = deleteMessage, showMember = showSenderImage, receiveFile = receiveFile, joinGroup = {}, acceptCall = acceptCall, scrollToItem = scrollToItem)
                    }
                  } else {
                    ChatItemView(user, chatModel, chat.chatInfo, cItem, composeState, cxt, uriHandler, provider, chatModelIncognito = chatModelIncognito, useLinkPreviews = useLinkPreviews, linkMode = linkMode, deleteMessage = deleteMessage, receiveFile = receiveFile, joinGroup = {}, acceptCall = acceptCall, scrollToItem = scrollToItem)
                  }
                } else {
                  if (!isAnnouncement(cItem)) {
                    Box(Modifier.padding(start = 86.dp, end = 12.dp).then(swipeableModifier)) {
                      ChatItemView(user, chatModel, chat.chatInfo, cItem, composeState, cxt, uriHandler, provider, chatModelIncognito = chatModelIncognito, useLinkPreviews = useLinkPreviews, linkMode = linkMode, deleteMessage = deleteMessage, receiveFile = receiveFile, joinGroup = {}, acceptCall = acceptCall, scrollToItem = scrollToItem)
                    }
                  } else {
                    val showSenderImage = showSenderImage(prevItem)
                    Log.i(TAG, "ChatItemsList: show sender image is " + showSenderImage)
                    ChatItemView(user, chatModel, chat.chatInfo, cItem, composeState, cxt, uriHandler, provider, chatModelIncognito = chatModelIncognito, useLinkPreviews = useLinkPreviews, linkMode = linkMode, deleteMessage = deleteMessage, showMember = showSenderImage, receiveFile = receiveFile, joinGroup = {}, acceptCall = acceptCall, scrollToItem = scrollToItem)
                  }
                }
              }
            }
          }
          else { // direct message
            val sent = cItem.chatDir.sent
            when (val mc = cItem.content.msgContent) {
              is MsgContent.MCPublicKey, is MsgContent.MCText -> {
                if ((mc is MsgContent.MCPublicKey
                      || (mc is MsgContent.MCText && mc.text.startsWith("-----BEGIN PGP PUBLIC KEY BLOCK-----", true)))
                ) {
                  Box(
                    Modifier
                      .padding(
                        start = 50.dp,
                        end = 50.dp
                      )
                      .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                  ) {
                    BurnerPublicKeyChatItemView(cModel = chatModel, cInfo = chat.chatInfo, cItem = cItem, onChatUpdated = onChatUpdated)
                  }
                } else {
                  if (!isAnnouncement(cItem)) {
                    Box(
                      Modifier
                        .padding(
                          start = if (sent) 76.dp else 12.dp,
                          end = if (sent) 12.dp else 76.dp,
                        ).then(swipeableModifier)
                    ) {
                      val prevItem = if (i < reversedChatItems.lastIndex) reversedChatItems[i + 1] else null
                      val senderImage = showSenderImage(prevItem = prevItem)
                      ChatItemView(user, chatModel, chat.chatInfo, cItem, composeState, cxt, uriHandler, provider, chatModelIncognito = chatModelIncognito, useLinkPreviews = useLinkPreviews, linkMode = linkMode, deleteMessage = deleteMessage, showMember = senderImage, receiveFile = receiveFile, joinGroup = joinGroup, acceptCall = acceptCall, scrollToItem = scrollToItem)
                    }
                  } else {
                    ChatItemView(user, chatModel, chat.chatInfo, cItem, composeState, cxt, uriHandler, provider, chatModelIncognito = chatModelIncognito, useLinkPreviews = useLinkPreviews, linkMode = linkMode, deleteMessage = deleteMessage, receiveFile = receiveFile, joinGroup = joinGroup, acceptCall = acceptCall, scrollToItem = scrollToItem)
                  }
                }
              }

              is MsgContent.MCPublicKeyRequest -> {
                Box(
                  Modifier
                    .padding(
                      start = 50.dp,
                      end = 50.dp
                    )
                    .fillMaxWidth(),
                  contentAlignment = Alignment.Center
                ) {
                  PublicKeyRequestChatItemView(cModel = chatModel, cInfo = chat.chatInfo, cItem = cItem, chat = chat, messageContent = mc)
                }
              }

              else -> {
                if (!isAnnouncement(cItem)) {
                  Box(
                    Modifier
                      .padding(
                        start = if (sent) 76.dp else 12.dp,
                        end = if (sent) 12.dp else 76.dp,
                      ).then(swipeableModifier)
                  ) {
                    val prevItem = if (i < reversedChatItems.lastIndex) reversedChatItems[i + 1] else null
                    val senderImage = showSenderImage(prevItem = prevItem)
                    ChatItemView(user, chatModel, chat.chatInfo, cItem, composeState, cxt, uriHandler, provider, chatModelIncognito = chatModelIncognito, useLinkPreviews = useLinkPreviews, linkMode = linkMode, deleteMessage = deleteMessage, showMember = senderImage, receiveFile = receiveFile, joinGroup = joinGroup, acceptCall = acceptCall, scrollToItem = scrollToItem)
                  }
                } else {
                  ChatItemView(user, chatModel, chat.chatInfo, cItem, composeState, cxt, uriHandler, provider, chatModelIncognito = chatModelIncognito, useLinkPreviews = useLinkPreviews, linkMode = linkMode, deleteMessage = deleteMessage, receiveFile = receiveFile, joinGroup = joinGroup, acceptCall = acceptCall, scrollToItem = scrollToItem)
                }
              }
            }
          }

          if (cItem.isRcvNew) {
            LaunchedEffect(cItem.id) {
              scope.launch {
                delay(600)
                markRead(CC.ItemRange(cItem.id, cItem.id), null)
              }
            }
          }
        }
      }
    }
  } else {
    Box(
      Modifier.fillMaxSize()
    ) {
      Card(
        shape = RoundedCornerShape(10.dp),
        backgroundColor = Color.Black,
        modifier = Modifier
          .padding(25.dp)
          .align(Alignment.Center)
      ) {
        Column(Modifier.padding(15.dp).fillMaxWidth()) {
          Text(
            text = generalGetString(R.string.no_messages_yet),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.SemiBold,
            color = Color.White
          )
          Spacer(modifier = Modifier.size(10.dp))
          Text(
            text = generalGetString(R.string.send_a_message_or_just_reply_with_a_greeting_emoji),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Normal,
            color = Color.White
          )
          Spacer(modifier = Modifier.size(5.dp))
        }
      }
      /*if (!stopped && !newChatSheetState.collectAsState().value.isVisible()) {
       OnboardingButtons(showNewChatSheet)
     }*/
      //Text(stringResource(R.string.you_have_no_contacts), Modifier.align(Alignment.Center), color = HighOrLowlight)
    }
  }
  FloatingButtons(chatItems, unreadCount, chat.chatStats.minUnreadItemId, searchValue, markRead, setFloatingButton, listState)
}

@Composable
fun BoxWithConstraintsScope.FloatingButtons(
  chatItems: List<ChatItem>,
  unreadCount: State<Int>,
  minUnreadItemId: Long,
  searchValue: State<String>,
  markRead: (CC.ItemRange, unreadCountAfter: Int?) -> Unit,
  setFloatingButton: (@Composable () -> Unit) -> Unit,
  listState: LazyListState
) {
  val scope = rememberCoroutineScope()
  var firstVisibleIndex by remember { mutableStateOf(listState.firstVisibleItemIndex) }
  var lastIndexOfVisibleItems by remember { mutableStateOf(listState.layoutInfo.visibleItemsInfo.lastIndex) }
  var firstItemIsVisible by remember { mutableStateOf(firstVisibleIndex == 0) }

  LaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemIndex }
      .distinctUntilChanged()
      .collect {
        firstVisibleIndex = it
        firstItemIsVisible = firstVisibleIndex == 0
      }
  }

  LaunchedEffect(listState) {
    // When both snapshotFlows located in one LaunchedEffect second block will never be called because coroutine is paused on first block
    // so separate them into two LaunchedEffects
    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastIndex }
      .distinctUntilChanged()
      .collect {
        lastIndexOfVisibleItems = it
      }
  }
  val bottomUnreadCount by remember {
    derivedStateOf {
      if (unreadCount.value == 0) return@derivedStateOf 0
      val from = chatItems.lastIndex - firstVisibleIndex - lastIndexOfVisibleItems
      if (chatItems.size <= from || from < 0) return@derivedStateOf 0

      chatItems.subList(from, chatItems.size).count { it.isRcvNew }
    }
  }
  val firstVisibleOffset = (-with(LocalDensity.current) { maxHeight.roundToPx() } * 0.8).toInt()

  LaunchedEffect(bottomUnreadCount, firstItemIsVisible) {
    val showButtonWithCounter = bottomUnreadCount > 0 && !firstItemIsVisible && searchValue.value.isEmpty()
    val showButtonWithArrow = !showButtonWithCounter && !firstItemIsVisible
    setFloatingButton(
      bottomEndFloatingButton(
        bottomUnreadCount,
        showButtonWithCounter,
        showButtonWithArrow,
        onClickArrowDown = {
          scope.launch { listState.animateScrollToItem(0) }
        },
        onClickCounter = {
          scope.launch { listState.animateScrollToItem(kotlin.math.max(0, bottomUnreadCount - 1), firstVisibleOffset) }
        }
      ))
  }
  // Don't show top FAB if is in search
  if (searchValue.value.isNotEmpty()) return
  val fabSize = 56.dp
  val topUnreadCount by remember {
    derivedStateOf { unreadCount.value - bottomUnreadCount }
  }
  val showButtonWithCounter = topUnreadCount > 0
  val height = with(LocalDensity.current) { maxHeight.toPx() }
  var showDropDown by remember { mutableStateOf(false) }

  TopEndFloatingButton(
    Modifier.padding(end = 16.dp, top = 24.dp).align(Alignment.TopEnd),
    topUnreadCount,
    showButtonWithCounter,
    onClick = { scope.launch { listState.animateScrollBy(height) } },
    onLongClick = { showDropDown = true }
  )

  DropdownMenu(
    expanded = showDropDown,
    onDismissRequest = { showDropDown = false },
    Modifier.width(220.dp),
    offset = DpOffset(maxWidth - 16.dp, 24.dp + fabSize)
  ) {
    DropdownMenuItem(
      onClick = {
        markRead(
          CC.ItemRange(minUnreadItemId, chatItems[chatItems.size - listState.layoutInfo.visibleItemsInfo.lastIndex - 1].id - 1),
          bottomUnreadCount
        )
        showDropDown = false
      }
    ) {
      Text(
        generalGetString(R.string.mark_read),
        maxLines = 1,
      )
    }
  }
}

@Composable
fun PreloadItems(
  listState: LazyListState,
  remaining: Int = 10,
  chat: Chat,
  items: List<*>,
  onLoadMore: (chat: Chat) -> Unit,
) {
  LaunchedEffect(listState, chat, items) {
    snapshotFlow { listState.layoutInfo }
      .map {
        val totalItemsNumber = it.totalItemsCount
        val lastVisibleItemIndex = (it.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
        if (lastVisibleItemIndex > (totalItemsNumber - remaining))
          totalItemsNumber
        else
          0
      }
      .distinctUntilChanged()
      .filter { it > 0 }
      .collect {
        onLoadMore(chat)
      }
  }
}

fun refreshUserPublicKeys(chatModel: ChatModel, contacts: MutableList<ContactInfo>, keyLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
  val intent = Intent(Intent.ACTION_MAIN)
  val ECHO_CHAT_OPEN_KEYCHAIN_CLONE = "org.sufficientlysecure.keychain"
  val USER_PUBLIC_KEYS = "user_public_keys"
  val gson = Gson()
  val publicKeysToImport = gson.toJson(contacts)

  intent.component = ComponentName.unflattenFromString(ECHO_CHAT_OPEN_KEYCHAIN_CLONE + ".debug" + "/" + ECHO_CHAT_OPEN_KEYCHAIN_CLONE + ".ui.AutomaticallyImportKey")
  intent.addCategory(Intent.CATEGORY_LAUNCHER)
  intent.putExtra(USER_PUBLIC_KEYS, publicKeysToImport)
  keyLauncher.launch(intent)
}

fun showMemberImage(member: GroupMember, prevItem: ChatItem?): Boolean {
  return prevItem == null || prevItem.chatDir is CIDirection.GroupSnd ||
      (prevItem.chatDir is CIDirection.GroupRcv && prevItem.chatDir.groupMember.groupMemberId != member.groupMemberId) ||
      isAnnouncement(prevItem)
}

fun showSenderImage(prevItem: ChatItem?): Boolean {
  return prevItem == null || prevItem.chatDir is CIDirection.GroupRcv || prevItem.chatDir is CIDirection.DirectRcv ||
      isAnnouncement(prevItem)
}

@Composable
fun MemberImage(member: GroupMember) {
  ProfileImage(38.dp, member.memberProfile.image)
}

@Composable
private fun TopEndFloatingButton(
  modifier: Modifier = Modifier,
  unreadCount: Int,
  showButtonWithCounter: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit
) = when {
  showButtonWithCounter -> {
    val interactionSource = interactionSourceWithDetection(onClick, onLongClick)
    FloatingActionButton(
      {}, // no action here
      modifier.size(48.dp),
      elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
      interactionSource = interactionSource,
    ) {
      Text(
        unreadCountStr(unreadCount),
        color = MaterialTheme.colors.primary,
        fontSize = 14.sp,
      )
    }
  }

  else -> {
  }
}

private fun bottomEndFloatingButton(
  unreadCount: Int,
  showButtonWithCounter: Boolean,
  showButtonWithArrow: Boolean,
  onClickArrowDown: () -> Unit,
  onClickCounter: () -> Unit
): @Composable () -> Unit = when {
  showButtonWithCounter -> {
    {
      FloatingActionButton(
        onClick = onClickCounter,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        modifier = Modifier.size(48.dp)
      ) {
        Text(
          unreadCountStr(unreadCount),
          color = MaterialTheme.colors.primary,
          fontSize = 14.sp,
        )
      }
    }
  }

  showButtonWithArrow -> {
    {
      FloatingActionButton(
        onClick = onClickArrowDown,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        modifier = Modifier.size(48.dp)
      ) {
        Icon(
          imageVector = Icons.Default.KeyboardArrowDown,
          contentDescription = null,
          tint = MaterialTheme.colors.primary
        )
      }
    }
  }

  else -> {
    {}
  }
}

private fun markUnreadChatAsRead(activeChat: MutableState<Chat?>, chatModel: ChatModel) {
  val chat = activeChat.value
  if (chat?.chatStats?.unreadChat != true) return
  withApi {
    val success = chatModel.controller.apiChatUnread(
      chat.chatInfo.chatType,
      chat.chatInfo.apiId,
      false
    )
    if (success && chat.id == activeChat.value?.id) {
      activeChat.value = chat.copy(chatStats = chat.chatStats.copy(unreadChat = false))
      chatModel.replaceChat(chat.id, activeChat.value!!)
    }
  }
}

private fun providerForGallery(
  listStateIndex: Int,
  chatItems: List<ChatItem>,
  cItemId: Long,
  scrollTo: (Int) -> Unit
): ImageGalleryProvider {
  fun canShowImage(item: ChatItem): Boolean =
    item.content.msgContent is MsgContent.MCImage && item.file?.loaded == true && getLoadedFilePath(SimplexApp.context, item.file) != null

  fun item(skipInternalIndex: Int, initialChatId: Long): Pair<Int, ChatItem>? {
    var processedInternalIndex = -skipInternalIndex.sign
    val indexOfFirst = chatItems.indexOfFirst { it.id == initialChatId }
    for (chatItemsIndex in if (skipInternalIndex >= 0) indexOfFirst downTo 0 else indexOfFirst..chatItems.lastIndex) {
      val item = chatItems[chatItemsIndex]
      if (canShowImage(item)) {
        processedInternalIndex += skipInternalIndex.sign
      }
      if (processedInternalIndex == skipInternalIndex) {
        return chatItemsIndex to item
      }
    }
    return null
  }

  var initialIndex = Int.MAX_VALUE / 2
  var initialChatId = cItemId
  return object: ImageGalleryProvider {
    override val initialIndex: Int = initialIndex
    override val totalImagesSize = mutableStateOf(Int.MAX_VALUE)
    override fun getImage(index: Int): Pair<Bitmap, Uri>? {
      val internalIndex = initialIndex - index
      val file = item(internalIndex, initialChatId)?.second?.file
      val imageBitmap: Bitmap? = getLoadedImage(SimplexApp.context, file)
      val filePath = getLoadedFilePath(SimplexApp.context, file)
      return if (imageBitmap != null && filePath != null) {
        val uri = FileProvider.getUriForFile(SimplexApp.context, "${BuildConfig.APPLICATION_ID}.provider", File(filePath))
        imageBitmap to uri
      } else null
    }

    override fun currentPageChanged(index: Int) {
      val internalIndex = initialIndex - index
      val item = item(internalIndex, initialChatId) ?: return
      initialIndex = index
      initialChatId = item.second.id
    }

    override fun scrollToStart() {
      initialIndex = 0
      ///      initialChatId = chatItems.first { canShowImage(it) }.id
    }

    override fun onDismiss(index: Int) {
      val internalIndex = initialIndex - index
      val indexInChatItems = item(internalIndex, initialChatId)?.first ?: return
      val indexInReversed = chatItems.lastIndex - indexInChatItems
      // Do not scroll to active item, just to different items
      if (indexInReversed == listStateIndex) return
      scrollTo(indexInReversed)
    }
  }
}

private fun ViewConfiguration.bigTouchSlop(slop: Float = 50f) = object: ViewConfiguration {
  override val longPressTimeoutMillis
    get() =
      this@bigTouchSlop.longPressTimeoutMillis
  override val doubleTapTimeoutMillis
    get() =
      this@bigTouchSlop.doubleTapTimeoutMillis
  override val doubleTapMinTimeMillis
    get() =
      this@bigTouchSlop.doubleTapMinTimeMillis
  override val touchSlop: Float get() = slop
}

fun setContactAlias(contactApiId: Long, contactBioExtra: String, chatModel: ChatModel, onChatUpdated: (Chat) -> Unit) = withApi {
  chatModel.controller.apiSetContactAlias(contactApiId, contactBioExtra)?.let {
    chatModel.updateContact(it)
    onChatUpdated(chatModel.getChat(chatModel.chatId.value ?: return@withApi) ?: return@withApi)
  }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ChatLayout(
  user: User,
  chat: Chat,
  chatModel: ChatModel,
  unreadCount: State<Int>,
  composeState: MutableState<ComposeState>,
  composeView: (@Composable () -> Unit),
  attachmentOption: MutableState<AttachmentOption?>,
  scope: CoroutineScope,
  attachmentBottomSheetState: ModalBottomSheetState,
  chatItems: List<ChatItem>,
  searchValue: State<String>,
  useLinkPreviews: Boolean,
  linkMode: SimplexLinkMode,
  chatModelIncognito: Boolean,
  back: () -> Unit,
  info: () -> Unit,
  showMemberInfo: (GroupInfo, GroupMember) -> Unit,
  loadPrevMessages: (ChatInfo) -> Unit,
  deleteMessage: (Long, CIDeleteMode) -> Unit,
  receiveFile: (Long) -> Unit,
  joinGroup: (Long) -> Unit,
  startCall: (CallMediaType) -> Unit,
  acceptCall: (Contact) -> Unit,
  addMembers: (GroupInfo) -> Unit,
  markRead: (CC.ItemRange, unreadCountAfter: Int?) -> Unit,
  changeNtfsState: (Boolean, currentValue: MutableState<Boolean>) -> Unit,
  onSearchValueChanged: (String) -> Unit,
  onChatUpdated: (Chat) -> Unit
) {
  Surface(
    Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colors.background)
  ) {
    ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
      ModalBottomSheetLayout(
        scrimColor = Color.Black.copy(alpha = 0.12F),
        modifier = Modifier.navigationBarsWithImePadding(),
        sheetContent = {
          ChooseAttachmentBottomSheetView(
            attachmentOption,
            hide = { scope.launch { attachmentBottomSheetState.hide() } }
          )
        },
        sheetState = attachmentBottomSheetState,
        sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
      ) {
        val floatingButton: MutableState<@Composable () -> Unit> = remember { mutableStateOf({}) }
        val setFloatingButton = { button: @Composable () -> Unit ->
          floatingButton.value = button
        }

        Scaffold(
          topBar = {
            // ChatInfoToolbar(chat, back, info, startCall, addMembers, changeNtfsState, onSearchValueChanged)
            Column(Modifier.background(Color.White)) {
              DirectChatViewToolbar(back, chat.chatInfo, info, { startCall(CallMediaType.Audio) }, { startCall(CallMediaType.Video) })
              Divider(color = PreviewTextColor)
            }
          },
          bottomBar = { composeView() },
          modifier = Modifier.navigationBarsWithImePadding(),
          floatingActionButton = { floatingButton.value() },
        ) { contentPadding ->
          BoxWithConstraints(Modifier.fillMaxHeight().padding(contentPadding).background(Color.White)) {
            ChatItemsList(
              user, chat, chatModel, unreadCount, composeState, chatItems, searchValue,
              useLinkPreviews, linkMode, chatModelIncognito, showMemberInfo, loadPrevMessages, deleteMessage,
              receiveFile, joinGroup, acceptCall, markRead, setFloatingButton, onChatUpdated
            )
          }
        }
      }
    }
  }
}

@Composable
fun DirectChatViewToolbar(
  onBackClicked: () -> Unit, cInfo: ChatInfo,
  info: () -> Unit,
  call: () -> Unit,
  videoCall: () -> Unit,
  centered: Boolean = true
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(AppBarHeight)
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color.White)
  ) {
    Box(
      modifier = Modifier
        .clickable(onClick = info)
        .fillMaxWidth(),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(
        Modifier
          .fillMaxHeight()
          .width(TitleInsetWithIcon - AppBarHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onBackClicked) {
          Icon(
            Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(22.dp)
          )
        }
      }
      val startPadding = TitleInsetWithIcon
      val endPadding = (0 * 50f).dp
      Box(
        Modifier
          .fillMaxWidth()
          .padding(
            start = if (centered) kotlin.math.max(startPadding.value, endPadding.value).dp else startPadding,
            end = if (centered) kotlin.math.max(startPadding.value, endPadding.value).dp else endPadding
          ),
        contentAlignment = Alignment.Center
      ) {
        if (cInfo is ChatInfo.Group && cInfo.displayName.startsWith("*", true)) {
          Text(
            text = if (cInfo.fullName != "" && cInfo.fullName != cInfo.displayName) cInfo.fullName else cInfo.displayName.removePrefix("*"),
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center),
            fontWeight = FontWeight.Medium,
            color = Color.Black
          )
        } else {
          Text(
            text = if (cInfo.fullName != "" && cInfo.fullName != cInfo.localDisplayName) cInfo.fullName else cInfo.localDisplayName,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center),
            fontWeight = FontWeight.Medium,
            color = Color.Black
          )
        }
      }
      if (cInfo is ChatInfo.Direct) {
        Row(
          Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(call) {
            Icon(
              Icons.Outlined.Call, stringResource(R.string.phone_call), tint = Color.Black,
              modifier = Modifier.size(22.dp)
            )
          }
          IconButton(videoCall) {
            Icon(
              Icons.Outlined.Videocam, stringResource(R.string.video_call), tint = Color.Black,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ChatInfoToolbar(
  chat: Chat,
  back: () -> Unit,
  info: () -> Unit,
  startCall: (CallMediaType) -> Unit,
  addMembers: (GroupInfo) -> Unit,
  changeNtfsState: (Boolean, currentValue: MutableState<Boolean>) -> Unit,
  onSearchValueChanged: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  var showMenu by rememberSaveable { mutableStateOf(false) }
  var showSearch by rememberSaveable { mutableStateOf(false) }
  val onBackClicked = {
    if (!showSearch) {
      back()
    } else {
      onSearchValueChanged("")
      showSearch = false
    }
  }
  BackHandler(onBack = onBackClicked)
  val barButtons = arrayListOf<@Composable RowScope.() -> Unit>()
  val menuItems = arrayListOf<@Composable () -> Unit>()
  menuItems.add {
    ItemAction(stringResource(android.R.string.search_go).capitalize(Locale.current), Icons.Outlined.Search, onClick = {
      showMenu = false
      showSearch = true
    })
  }

  if (chat.chatInfo is ChatInfo.Direct) {
    barButtons.add {
      IconButton({
        showMenu = false
        startCall(CallMediaType.Audio)
      }) {
        Icon(Icons.Outlined.Phone, stringResource(R.string.icon_descr_more_button), tint = MaterialTheme.colors.primary)
      }
    }
    menuItems.add {
      ItemAction(stringResource(R.string.icon_descr_video_call).capitalize(Locale.current), Icons.Outlined.Videocam, onClick = {
        showMenu = false
        startCall(CallMediaType.Video)
      })
    }
  } else if (chat.chatInfo is ChatInfo.Group && chat.chatInfo.groupInfo.canAddMembers && !chat.chatInfo.incognito) {
    barButtons.add {
      IconButton({
        showMenu = false
        addMembers(chat.chatInfo.groupInfo)
      }) {
        Icon(Icons.Outlined.PersonAdd, stringResource(R.string.icon_descr_add_members), tint = MaterialTheme.colors.primary)
      }
    }
  }
  val ntfsEnabled = remember { mutableStateOf(chat.chatInfo.ntfsEnabled) }
  menuItems.add {
    ItemAction(
      if (ntfsEnabled.value) stringResource(R.string.mute_chat) else stringResource(R.string.unmute_chat),
      if (ntfsEnabled.value) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
      onClick = {
        showMenu = false
        // Just to make a delay before changing state of ntfsEnabled, otherwise it will redraw menu item with new value before closing the menu
        scope.launch {
          delay(200)
          changeNtfsState(!ntfsEnabled.value, ntfsEnabled)
        }
      }
    )
  }

  barButtons.add {
    IconButton({ showMenu = true }) {
      Icon(Icons.Default.MoreVert, stringResource(R.string.icon_descr_more_button), tint = MaterialTheme.colors.primary)
    }
  }

  DefaultTopAppBar(
    navigationButton = { NavigationButtonBack(onBackClicked) },
    title = { ChatInfoToolbarTitle(chat.chatInfo) },
    onTitleClick = info,
    showSearch = showSearch,
    onSearchValueChanged = onSearchValueChanged,
    buttons = barButtons
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color.White)
  ) {
    Column(Modifier.fillMaxWidth()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
      ) {
        IconButton(onBackClicked) {
          Icon(
            Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(22.dp)
          )
        }
        Text(
          text = generalGetString(R.string.settings),
          fontSize = 18.sp,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.Medium,
          color = Color.Black
        )
      }
      Divider(Modifier.padding(top = AppBarHeight))

      Box(Modifier.fillMaxWidth().wrapContentSize(Alignment.TopEnd).offset(y = AppBarHeight)) {
        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          Modifier.widthIn(min = 220.dp)
        ) {
          menuItems.forEach { it() }
        }
      }
      /*  SettingsContentView(
          chatModel = chatModel,
          editProfile = editProfile,
          profile = profile,
          setPerformLA = setPerformLA,
          showModal = showModal,
          showSettingsModal = showSettingsModal,
          saveProfile = saveProfile
        )*/
    }
  }
}

@Composable
fun ChatInfoToolbarTitle(cInfo: ChatInfo, imageSize: Dp = 40.dp, iconColor: Color = MaterialTheme.colors.secondary) {
  Row(
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    /*    if (cInfo.incognito) {
          IncognitoImage(size = 36.dp, Color.White)
        }
        ChatInfoImage(cInfo, size = imageSize, iconColor)*/
    Column(
      Modifier.padding(start = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (cInfo is ChatInfo.Group && cInfo.localDisplayName.startsWith("*", true)) {
        Text(
          if (cInfo.fullName != "" && cInfo.fullName != cInfo.localDisplayName) cInfo.fullName else cInfo.localDisplayName.removePrefix("*"),
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = Color.Black,
          fontSize = 18.sp
        )
      } else {
        Text(
          if (cInfo.fullName != "" && cInfo.fullName != cInfo.localDisplayName) cInfo.fullName else cInfo.localDisplayName,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = Color.Black,
          fontSize = 18.sp
        )
      }
    }
  }
}
/*
@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewChatLayout() {
  SimpleXTheme {
    val chatItems = listOf(
      ChatItem.getSampleData(
        1, CIDirection.DirectSnd(), Clock.System.now(), "hello"
      ),
      ChatItem.getSampleData(
        2, CIDirection.DirectRcv(), Clock.System.now(), "hello"
      ),
      ChatItem.getDeletedContentSampleData(3),
      ChatItem.getSampleData(
        4, CIDirection.DirectSnd(), Clock.System.now(), "hello"
      ),
      ChatItem.getSampleData(
        5, CIDirection.DirectSnd(), Clock.System.now(), "hello"
      ),
      ChatItem.getSampleData(
        6, CIDirection.DirectRcv(), Clock.System.now(), "hello"
      )
    )
    val unreadCount = remember { mutableStateOf(chatItems.count { it.isRcvNew }) }
    val searchValue = remember { mutableStateOf("") }
    ChatLayout(
      user = User.sampleData,
      chat = Chat(
        chatInfo = ChatInfo.Direct.sampleData,
        chatItems = chatItems,
        chatStats = Chat.ChatStats()
      ),
      unreadCount = unreadCount,
      composeState = remember { mutableStateOf(ComposeState(useLinkPreviews = true)) },
      composeView = {},
      attachmentOption = remember { mutableStateOf<AttachmentOption?>(null) },
      scope = rememberCoroutineScope(),
      attachmentBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
      chatItems = chatItems,
      searchValue,
      useLinkPreviews = true,
      chatModelIncognito = false,
      back = {},
      info = {},
      showMemberInfo = { _, _ -> },
      loadPrevMessages = { _ -> },
      deleteMessage = { _, _ -> },
      receiveFile = {},
      joinGroup = {},
      startCall = {},
      acceptCall = { _ -> },
      addMembers = { _ -> },
      markRead = { _, _ -> },
      changeNtfsState = { _, _ -> },
      onSearchValueChanged = {},
    )
  }
}*/
/*@Preview(showBackground = true)
@Composable
fun PreviewGroupChatLayout() {
  SimpleXTheme {
    val chatItems = listOf(
      ChatItem.getSampleData(
        1, CIDirection.GroupSnd(), Clock.System.now(), "hello"
      ),
      ChatItem.getSampleData(
        2, CIDirection.GroupRcv(GroupMember.sampleData), Clock.System.now(), "hello"
      ),
      ChatItem.getDeletedContentSampleData(3),
      ChatItem.getSampleData(
        4, CIDirection.GroupRcv(GroupMember.sampleData), Clock.System.now(), "hello"
      ),
      ChatItem.getSampleData(
        5, CIDirection.GroupSnd(), Clock.System.now(), "hello"
      ),
      ChatItem.getSampleData(
        6, CIDirection.GroupRcv(GroupMember.sampleData), Clock.System.now(), "hello"
      )
    )
    val unreadCount = remember { mutableStateOf(chatItems.count { it.isRcvNew }) }
    val searchValue = remember { mutableStateOf("") }
    ChatLayout(
      user = User.sampleData,
      chat = Chat(
        chatInfo = ChatInfo.Group.sampleData,
        chatItems = chatItems,
        chatStats = Chat.ChatStats()
      ),
      unreadCount = unreadCount,
      composeState = remember { mutableStateOf(ComposeState(useLinkPreviews = true)) },
      composeView = {},
      attachmentOption = remember { mutableStateOf<AttachmentOption?>(null) },
      scope = rememberCoroutineScope(),
      attachmentBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
      chatItems = chatItems,
      searchValue,
      useLinkPreviews = true,
      chatModelIncognito = false,
      back = {},
      info = {},
      showMemberInfo = { _, _ -> },
      loadPrevMessages = { _ -> },
      deleteMessage = { _, _ -> },
      receiveFile = {},
      joinGroup = {},
      startCall = {},
      acceptCall = { _ -> },
      addMembers = { _ -> },
      markRead = { _, _ -> },
      changeNtfsState = { _, _ -> },
      onSearchValueChanged = {},
    )
  }
}*/
