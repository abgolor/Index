package chat.echo.app.views.chatlist

import android.content.res.Configuration
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.call.*
import chat.echo.app.views.chat.*
import chat.echo.app.views.chat.group.deleteGroupDialog
import chat.echo.app.views.chat.group.leaveGroupDialog
import chat.echo.app.views.chat.item.ItemAction
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.ContactConnectionInfoView
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

@Composable
fun ChatListNavLinkView(chat: Chat, chatModel: ChatModel) {
  val showMenu = remember { mutableStateOf(false) }
  var showMarkRead by remember { mutableStateOf(false) }
  val linkMode by remember { chatModel.controller.appPrefs.simplexLinkMode.state }
  val stopped = chatModel.chatRunning.value == false
  LaunchedEffect(chat.id, chat.chatStats.unreadCount > 0) {
    showMenu.value = false
    delay(500L)
    showMarkRead = chat.chatStats.unreadCount > 0
  }
  when (chat.chatInfo) {
    is ChatInfo.Direct ->
      ChatListNavLinkLayout(
        chatLinkPreview = { ChatPreviewView(chat, chatModel.incognito.value, chatModel.currentUser.value?.profile?.displayName, stopped, linkMode) },
        click = { directChatAction(chat.chatInfo, chatModel) },
        dropdownMenuItems = null,
        showMenu,
        stopped
      )
    is ChatInfo.Group ->
      ChatListNavLinkLayout(
        chatLinkPreview = { ChatPreviewView(chat, chatModel.incognito.value, chatModel.currentUser.value?.profile?.displayName, stopped, linkMode) },
        click = { groupChatAction(chat.chatInfo.groupInfo, chatModel) },
        dropdownMenuItems = { GroupMenuItems(chat, chat.chatInfo.groupInfo, chatModel, showMenu, showMarkRead) },
        showMenu,
        stopped
      )
    is ChatInfo.ContactRequest ->
      ChatListNavLinkLayout(
        chatLinkPreview = { ContactRequestView(chatModel.incognito.value, chat.chatInfo) },
        click = { contactRequestAlertDialog(chat.chatInfo, chatModel) },
        dropdownMenuItems = { ContactRequestMenuItems(chat.chatInfo, chatModel, showMenu) },
        showMenu,
        stopped
      )
    is ChatInfo.ContactConnection ->
      ChatListNavLinkLayout(
        chatLinkPreview = { ContactConnectionView(chat.chatInfo.contactConnection) },
        click = {
          ModalManager.shared.showModalCloseable(true) { close ->
            ContactConnectionInfoView(chatModel, chat.chatInfo.contactConnection.connReqInv, chat.chatInfo.contactConnection, false, close)
          }
        },
        dropdownMenuItems = { ContactConnectionMenuItems(chat.chatInfo, chatModel, showMenu) },
        showMenu,
        stopped
      )
    else -> {}
  }
}

@Composable
fun ContactListNavLinkView(chat: Chat, chatModel: ChatModel) {
  val showMenu = remember { mutableStateOf(false) }
  val linkMode by remember { chatModel.controller.appPrefs.simplexLinkMode.state }
  var showMarkRead by remember { mutableStateOf(false) }
  val stopped = chatModel.chatRunning.value == false
  LaunchedEffect(chat.id, chat.chatStats.unreadCount > 0) {
    showMenu.value = false
    delay(500L)
    showMarkRead = chat.chatStats.unreadCount > 0
  }
  when (chat.chatInfo) {
    is ChatInfo.Direct ->
      ContactsListNavLinkLayout(
        chatLinkPreview = { ContactPreviewView(chatModel, chat, chatModel.incognito.value, chatModel.currentUser.value?.profile?.displayName, stopped) },
        click = {},
        dropdownMenuItems = null,
        showMenu,
        stopped
      )
    is ChatInfo.Group ->
      ChatListNavLinkLayout(
        chatLinkPreview = { ChatPreviewView(chat, chatModel.incognito.value, chatModel.currentUser.value?.profile?.displayName, stopped, linkMode) },
        click = { groupChatAction(chat.chatInfo.groupInfo, chatModel) },
        dropdownMenuItems = { GroupMenuItems(chat, chat.chatInfo.groupInfo, chatModel, showMenu, showMarkRead) },
        showMenu,
        stopped
      )
    is ChatInfo.ContactRequest ->
      ContactsListNavLinkLayout(
        chatLinkPreview = {
          ContactRequestView(
            chatModel.incognito.value,
            chat.chatInfo,
            accept = { contactAcceptRequestAlertDialog(chat.chatInfo, chatModel) },
            reject = { contactRejectRequestAlertDialog(chat.chatInfo, chatModel) })
        },
        click = { },
        dropdownMenuItems = { ContactRequestMenuItems(chat.chatInfo, chatModel, showMenu) },
        showMenu,
        stopped
      )
    is ChatInfo.ContactConnection -> {}
    /* ChatListNavLinkLayout(
       chatLinkPreview = { ContactConnectionView(chat.chatInfo.contactConnection) },
       click = {
         ModalManager.shared.showModalCloseable(true) { close ->
           ContactConnectionInfoView(chatModel, chat.chatInfo.contactConnection.connReqInv, chat.chatInfo.contactConnection, false, close)
         }
       },
       dropdownMenuItems = { ContactConnectionMenuItems(chat.chatInfo, chatModel, showMenu) },
       showMenu,
       stopped
     )*/
    else -> {}
  }
}

fun directChatInfoAction(cInfo: ChatInfo, chat: Chat, chatModel: ChatModel) {
  withApi {
    if (cInfo is ChatInfo.Direct) {
      val contactInfo = chatModel.controller.apiContactInfo(cInfo.apiId)
      ModalManager.shared.showModalCloseable(true) { close ->
        ChatInfoView(chatModel, cInfo.contact, contactInfo?.first, contactInfo?.second, chat.chatInfo.localAlias, close) {
          chatModel.updateChatInfo(it.chatInfo)
        }
      }
    }
  }
}

fun directCallAction(chatModel: ChatModel, chat: Chat) {
  val cInfo = chat.chatInfo
  if (cInfo is ChatInfo.Direct) {
    chatModel.activeCall.value = Call(contact = cInfo.contact, callState = CallState.WaitCapabilities, localMedia = CallMediaType.Audio)
    chatModel.showCallView.value = true
    chatModel.callCommand.value = WCallCommand.Capabilities
  }
}

fun directChatAction(chatInfo: ChatInfo, chatModel: ChatModel) {
  if (chatInfo.ready) {
    withApi { openChat(chatInfo, chatModel) }
  } else {
    pendingContactAlertDialog(chatInfo, chatModel)
  }
}

fun groupChatAction(groupInfo: GroupInfo, chatModel: ChatModel) {
  when (groupInfo.membership.memberStatus) {
    GroupMemberStatus.MemInvited -> acceptGroupInvitationAlertDialog(groupInfo, chatModel)
    GroupMemberStatus.MemAccepted -> groupInvitationAcceptedAlert()
    else -> withApi {
      openChat(ChatInfo.Group(groupInfo), chatModel)
    }
  }
}

suspend fun openChat(chatInfo: ChatInfo, chatModel: ChatModel) {
  val chat = chatModel.controller.apiGetChat(chatInfo.chatType, chatInfo.apiId)
  if (chat != null) {
    chatModel.chatItems.clear()
    chatModel.chatItems.addAll(chat.chatItems)
    chatModel.chatId.value = chatInfo.id
  }
}

suspend fun apiLoadPrevMessages(chatInfo: ChatInfo, chatModel: ChatModel, beforeChatItemId: Long, search: String) {
  val pagination = ChatPagination.Before(beforeChatItemId, ChatPagination.PRELOAD_COUNT)
  val chat = chatModel.controller.apiGetChat(chatInfo.chatType, chatInfo.apiId, pagination, search) ?: return
  chatModel.chatItems.addAll(0, chat.chatItems)
}

suspend fun apiFindMessages(chatInfo: ChatInfo, chatModel: ChatModel, search: String) {
  val chat = chatModel.controller.apiGetChat(chatInfo.chatType, chatInfo.apiId, search = search) ?: return
  chatModel.chatItems.clear()
  chatModel.chatItems.addAll(0, chat.chatItems)
}

suspend fun setGroupMembers(groupInfo: GroupInfo, chatModel: ChatModel) {
  val groupMembers = chatModel.controller.apiListMembers(groupInfo.groupId)
  chatModel.groupMembers.clear()
  chatModel.groupMembers.addAll(groupMembers)
}

@Composable
fun ContactMenuItems(chat: Chat, chatModel: ChatModel, showMenu: MutableState<Boolean>, showMarkRead: Boolean) {
  if (showMarkRead) {
    MarkReadChatAction(chat, chatModel, showMenu)
  }
  ToggleNotificationsChatAction(chat, chatModel, chat.chatInfo.ntfsEnabled)
  ClearChatAction(chat, chatModel)
  DeleteContactAction(chat, chatModel, showMenu)
}

@Composable
fun GroupMenuItems(chat: Chat, groupInfo: GroupInfo, chatModel: ChatModel, showMenu: MutableState<Boolean>, showMarkRead: Boolean) {
  when (groupInfo.membership.memberStatus) {
    GroupMemberStatus.MemInvited -> {
      JoinGroupAction(chat, groupInfo, chatModel, showMenu)
      if (groupInfo.canDelete) {
        DeleteGroupAction(chat, groupInfo, chatModel, showMenu)
      }
    }
    else -> {
      if (showMarkRead) {
        MarkReadChatAction(chat, chatModel, showMenu)
      }
      ToggleNotificationsChatAction(chat, chatModel, chat.chatInfo.ntfsEnabled)
      ClearChatAction(chat, chatModel)
      if (groupInfo.membership.memberCurrent) {
        LeaveGroupAction(groupInfo, chatModel, showMenu)
      }
      if (groupInfo.canDelete) {
        DeleteGroupAction(chat, groupInfo, chatModel, showMenu)
      }
    }
  }
}

@Composable
fun MarkReadChatAction(chat: Chat, chatModel: ChatModel, showMenu: MutableState<Boolean>) {
  ItemAction(
    stringResource(R.string.mark_read),
    Icons.Outlined.Check,
    onClick = {
      markChatRead(chat, chatModel)
      chatModel.controller.ntfManager.cancelNotificationsForChat(chat.id)
      showMenu.value = false
    }
  )
}

@Composable
fun ToggleNotificationsChatAction(chat: Chat, chatModel: ChatModel, ntfsEnabled: Boolean) {
  ItemAction(
    if (ntfsEnabled) stringResource(R.string.mute_chat) else stringResource(R.string.unmute_chat),
    if (ntfsEnabled) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
    onClick = {
      changeNtfsStatePerChat(!ntfsEnabled, mutableStateOf(ntfsEnabled), chat, chatModel)
    }
  )
}

@Composable
fun ClearChatAction(chat: Chat, chatModel: ChatModel) {
  ItemAction(
    stringResource(R.string.clear_chat_menu_action),
    Icons.Outlined.Restore,
    onClick = {
      withApi { clearChatDialog(chat.chatInfo, chatModel) }
    },
    color = WarningOrange
  )
}

@Composable
fun DeleteContactAction(chat: Chat, chatModel: ChatModel, showMenu: MutableState<Boolean>) {
  ItemAction(
    stringResource(R.string.delete_contact_menu_action),
    Icons.Outlined.Delete,
    onClick = {
      withApi { deleteContactDialog(chat.chatInfo, chatModel) }
      showMenu.value = false
    },
    color = Color.Red
  )
}

@Composable
fun DeleteGroupAction(chat: Chat, groupInfo: GroupInfo, chatModel: ChatModel, showMenu: MutableState<Boolean>) {
  ItemAction(
    stringResource(R.string.delete_group_menu_action),
    Icons.Outlined.Delete,
    onClick = {
      deleteGroupDialog(chat.chatInfo, groupInfo, chatModel)
      showMenu.value = false
    },
    color = Color.Red
  )
}

@Composable
fun JoinGroupAction(chat: Chat, groupInfo: GroupInfo, chatModel: ChatModel, showMenu: MutableState<Boolean>) {
  val joinGroup: () -> Unit = { withApi { chatModel.controller.apiJoinGroup(groupInfo.groupId) } }
  ItemAction(
    if (chat.chatInfo.incognito) stringResource(R.string.join_group_incognito_button) else stringResource(R.string.join_group_button),
    if (chat.chatInfo.incognito) Icons.Filled.TheaterComedy else Icons.Outlined.Login,
    color = if (chat.chatInfo.incognito) Indigo else MaterialTheme.colors.onBackground,
    onClick = {
      joinGroup()
      showMenu.value = false
    }
  )
}

@Composable
fun LeaveGroupAction(groupInfo: GroupInfo, chatModel: ChatModel, showMenu: MutableState<Boolean>) {
  ItemAction(
    stringResource(R.string.leave_group_button),
    Icons.Outlined.Logout,
    onClick = {
      leaveGroupDialog(groupInfo, chatModel)
      showMenu.value = false
    },
    color = Color.Red
  )
}

@Composable
fun ContactRequestMenuItems(chatInfo: ChatInfo.ContactRequest, chatModel: ChatModel, showMenu: MutableState<Boolean>) {
  ItemAction(
    if (chatModel.incognito.value) stringResource(R.string.accept_contact_incognito_button) else stringResource(R.string.accept_contact_button),
    if (chatModel.incognito.value) Icons.Filled.TheaterComedy else Icons.Outlined.Check,
    color = if (chatModel.incognito.value) Indigo else MaterialTheme.colors.onBackground,
    onClick = {
      acceptContactRequest(chatInfo.apiId, chatInfo, true, chatModel)
      showMenu.value = false
    }
  )
  ItemAction(
    stringResource(R.string.reject_contact_button),
    Icons.Outlined.Close,
    onClick = {
      rejectContactRequest(chatInfo, chatModel)
      showMenu.value = false
    },
    color = Color.Red
  )
}

@Composable
fun ContactConnectionMenuItems(chatInfo: ChatInfo.ContactConnection, chatModel: ChatModel, showMenu: MutableState<Boolean>) {
  ItemAction(
    stringResource(R.string.set_contact_name),
    Icons.Outlined.Edit,
    onClick = {
      ModalManager.shared.showModalCloseable(true) { close ->
        ContactConnectionInfoView(chatModel, chatInfo.contactConnection.connReqInv, chatInfo.contactConnection, true, close)
      }
      showMenu.value = false
    },
  )
  ItemAction(
    stringResource(R.string.delete_verb),
    Icons.Outlined.Delete,
    onClick = {
      deleteContactConnectionAlert(chatInfo.contactConnection, chatModel) {}
      showMenu.value = false
    },
    color = Color.Red
  )
}

fun markChatRead(chat: Chat, chatModel: ChatModel) {
  // Just to be sure
  if (chat.chatStats.unreadCount == 0) return
  val minUnreadItemId = chat.chatStats.minUnreadItemId
  chatModel.markChatItemsRead(chat.chatInfo)
  withApi {
    chatModel.controller.apiChatRead(
      chat.chatInfo.chatType,
      chat.chatInfo.apiId,
      CC.ItemRange(minUnreadItemId, chat.chatItems.last().id)
    )
  }
}

fun contactRequestAlertDialog(contactRequest: ChatInfo.ContactRequest, chatModel: ChatModel) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(R.string.accept_connection_request__question),
    text = generalGetString(R.string.if_you_choose_to_reject_the_sender_will_not_be_notified),
    confirmText = if (chatModel.incognito.value) generalGetString(R.string.accept_contact_incognito_button) else generalGetString(R.string.accept_contact_button),
    onConfirm = { acceptContactRequest(contactRequest.apiId, contactRequest, true, chatModel) },
    dismissText = generalGetString(R.string.reject_contact_button),
    onDismiss = { rejectContactRequest(contactRequest, chatModel) }
  )
}

fun contactAcceptRequestAlertDialog(contactRequest: ChatInfo.ContactRequest, chatModel: ChatModel) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(R.string.accept_connection_request__question),
    text = String.format(generalGetString(R.string.do_you_want_to_accept_request), if (contactRequest.fullName != "") contactRequest.fullName else contactRequest.displayName),
    confirmText = generalGetString(R.string.accept_contact_button),
    onConfirm = { acceptContactRequest(contactRequest.apiId, contactRequest, true, chatModel) },
    dismissText = generalGetString(R.string.cancel_verb)
  )
}

fun contactRejectRequestAlertDialog(contactRequest: ChatInfo.ContactRequest, chatModel: ChatModel) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(R.string.reject_connection_request__question),
    text = String.format(generalGetString(R.string.do_you_want_to_reject_request), if (contactRequest.fullName != "") contactRequest.fullName else contactRequest.displayName),
    confirmText = generalGetString(R.string.accept_contact_button),
    onConfirm = { rejectContactRequest(contactRequest, chatModel) },
    dismissText = generalGetString(R.string.cancel_verb)
  )
}

fun acceptContactRequest(apiId: Long, contactRequest: ChatInfo.ContactRequest?, isCurrentUser: Boolean, chatModel: ChatModel) {
  withApi {
    val contact = chatModel.controller.apiAcceptContactRequest(apiId)
    if (contact != null && isCurrentUser && contactRequest != null) {
      val chat = Chat(ChatInfo.Direct(contact), listOf())
      chatModel.replaceChat(contactRequest.id, chat)
    }
  }
}

fun rejectContactRequest(contactRequest: ChatInfo.ContactRequest, chatModel: ChatModel) {
  withApi {
    chatModel.controller.apiRejectContactRequest(contactRequest.apiId)
    chatModel.removeChat(contactRequest.id)
  }
}

fun contactConnectionAlertDialog(connection: PendingContactConnection, chatModel: ChatModel) {
  AlertManager.shared.showAlertDialogButtons(
    title = generalGetString(
      if (connection.initiated) R.string.you_invited_your_contact
      else R.string.you_accepted_connection
    ),
    text = generalGetString(
      if (connection.viaContactUri) R.string.you_will_be_connected_when_your_connection_request_is_accepted
      else R.string.you_will_be_connected_when_your_contacts_device_is_online
    ),
    buttons = {
      Row(
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(onClick = {
          AlertManager.shared.hideAlert()
          deleteContactConnectionAlert(connection, chatModel) {}
        }) {
          Text(stringResource(R.string.delete_verb))
        }
        Spacer(Modifier.padding(horizontal = 4.dp))
        TextButton(onClick = { AlertManager.shared.hideAlert() }) {
          Text(stringResource(R.string.ok))
        }
      }
    }
  )
}

fun deleteContactConnectionAlert(connection: PendingContactConnection, chatModel: ChatModel, onSuccess: () -> Unit) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(R.string.delete_pending_connection__question),
    text = generalGetString(
      if (connection.initiated) R.string.contact_you_shared_link_with_wont_be_able_to_connect
      else R.string.connection_you_accepted_will_be_cancelled
    ),
    confirmText = generalGetString(R.string.delete_verb),
    onConfirm = {
      withApi {
        AlertManager.shared.hideAlert()
        if (chatModel.controller.apiDeleteChat(ChatType.ContactConnection, connection.apiId)) {
          chatModel.removeChat(connection.id)
          onSuccess()
        }
      }
    }
  )
}

fun pendingContactAlertDialog(chatInfo: ChatInfo, chatModel: ChatModel) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(R.string.alert_title_contact_connection_pending),
    text = generalGetString(R.string.alert_text_connection_pending_they_need_to_be_online_can_delete_and_retry),
    confirmText = generalGetString(R.string.button_delete_contact),
    onConfirm = {
      withApi {
        val r = chatModel.controller.apiDeleteChat(chatInfo.chatType, chatInfo.apiId)
        if (r) {
          chatModel.removeChat(chatInfo.id)
          chatModel.chatId.value = null
        }
      }
    },
    dismissText = generalGetString(R.string.cancel_verb),
  )
}

fun acceptGroupInvitationAlertDialog(groupInfo: GroupInfo, chatModel: ChatModel) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(R.string.join_group_question),
    text = generalGetString(R.string.you_are_invited_to_group_join_to_connect_with_group_members),
    confirmText = if (groupInfo.membership.memberIncognito) generalGetString(R.string.join_group_incognito_button) else generalGetString(R.string.join_group_button),
    onConfirm = { withApi { chatModel.controller.apiJoinGroup(groupInfo.groupId) } },
    dismissText = generalGetString(R.string.delete_verb),
    onDismiss = { deleteGroup(groupInfo, chatModel) }
  )
}

fun cantInviteIncognitoAlert() {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.alert_title_cant_invite_contacts),
    text = generalGetString(R.string.alert_title_cant_invite_contacts_descr),
    confirmText = generalGetString(R.string.ok),
  )
}

fun deleteGroup(groupInfo: GroupInfo, chatModel: ChatModel) {
  withApi {
    val r = chatModel.controller.apiDeleteChat(ChatType.Group, groupInfo.apiId)
    if (r) {
      chatModel.removeChat(groupInfo.id)
      chatModel.chatId.value = null
      chatModel.controller.ntfManager.cancelNotificationsForChat(groupInfo.id)
    }
  }
}

fun groupInvitationAcceptedAlert() {
  AlertManager.shared.showAlertMsg(
    generalGetString(R.string.joining_group),
    generalGetString(R.string.youve_accepted_group_invitation_connecting_to_inviting_group_member)
  )
}

fun changeNtfsStatePerChat(enabled: Boolean, currentState: MutableState<Boolean>, chat: Chat, chatModel: ChatModel) {
  val newChatInfo = when (chat.chatInfo) {
    is ChatInfo.Direct -> with(chat.chatInfo) {
      ChatInfo.Direct(contact.copy(chatSettings = contact.chatSettings.copy(enableNtfs = enabled)))
    }
    is ChatInfo.Group -> with(chat.chatInfo) {
      ChatInfo.Group(groupInfo.copy(chatSettings = groupInfo.chatSettings.copy(enableNtfs = enabled)))
    }
    else -> null
  }
  withApi {
    val res = when (newChatInfo) {
      is ChatInfo.Direct -> with(newChatInfo) {
        chatModel.controller.apiSetSettings(chatType, apiId, contact.chatSettings)
      }
      is ChatInfo.Group -> with(newChatInfo) {
        chatModel.controller.apiSetSettings(chatType, apiId, groupInfo.chatSettings)
      }
      else -> false
    }
    if (res && newChatInfo != null) {
      chatModel.updateChatInfo(newChatInfo)
      if (!enabled) {
        chatModel.controller.ntfManager.cancelNotificationsForChat(chat.id)
      }
      currentState.value = enabled
    }
  }
}

@Composable
fun ChatListNavLinkLayout(
  chatLinkPreview: @Composable () -> Unit,
  click: () -> Unit,
  dropdownMenuItems: (@Composable () -> Unit)?,
  showMenu: MutableState<Boolean>,
  stopped: Boolean
) {
  var modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
  if (!stopped) modifier = modifier.combinedClickable(onClick = click, onLongClick = { showMenu.value = true })
  Surface(
    modifier,
    color = Color.White
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 5.dp)
        .padding(end = 5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      chatLinkPreview()
    }
    if (dropdownMenuItems != null) {
      Box(Modifier.padding(horizontal = 16.dp)) {
        DropdownMenu(
          expanded = showMenu.value,
          onDismissRequest = { showMenu.value = false },
          Modifier.width(220.dp)
        ) {
          dropdownMenuItems()
        }
      }
    }
  }
  Divider(Modifier.padding(horizontal = 8.dp))
}

@Composable
fun ContactsListNavLinkLayout(
  chatLinkPreview: @Composable () -> Unit,
  click: () -> Unit,
  dropdownMenuItems: (@Composable () -> Unit)?,
  showMenu: MutableState<Boolean>,
  stopped: Boolean
) {
  var modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
  if (!stopped) modifier = modifier.combinedClickable(onClick = click, onLongClick = { showMenu.value = true })
  Surface(
    modifier,
    color = Color.White
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 5.dp)
        .padding(end = 5.dp),
      verticalAlignment = Alignment.Top
    ) {
      chatLinkPreview()
    }
    if (dropdownMenuItems != null) {
      Box(Modifier.padding(horizontal = 16.dp)) {
        DropdownMenu(
          expanded = showMenu.value,
          onDismissRequest = { showMenu.value = false },
          Modifier.width(220.dp)
        ) {
          dropdownMenuItems()
        }
      }
    }
  }
  // Divider(Modifier.padding(horizontal = 8.dp), color = DividerColor)
}

@Preview
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewChatListNavLinkDirect() {
  SimpleXTheme {
    ChatListNavLinkLayout(
      chatLinkPreview = {
        ChatPreviewView(
          chat = Chat(
            chatInfo = ChatInfo.Direct.sampleData,
            chatItems = listOf(
              ChatItem.getSampleData(
                1,
                CIDirection.DirectSnd(),
                Clock.System.now(),
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
              )
            ),
            chatStats = Chat.ChatStats()
          ),
          false,
          null,
          stopped = false,
          linkMode = SimplexLinkMode.DESCRIPTION
        )
      },
      click = {},
      dropdownMenuItems = null,
      showMenu = remember { mutableStateOf(false) },
      stopped = false
    )
  }
}

@Preview
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewChatListNavLinkGroup() {
  SimpleXTheme {
    ChatListNavLinkLayout(
      chatLinkPreview = {
        ChatPreviewView(
          Chat(
            chatInfo = ChatInfo.Group.sampleData,
            chatItems = listOf(
              ChatItem.getSampleData(
                1,
                CIDirection.DirectSnd(),
                Clock.System.now(),
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
              )
            ),
            chatStats = Chat.ChatStats()
          ),
          false,
          null,
          stopped = false,
          linkMode = SimplexLinkMode.DESCRIPTION
        )
      },
      click = {},
      dropdownMenuItems = null,
      showMenu = remember { mutableStateOf(false) },
      stopped = false
    )
  }
}

@Preview
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewChatListNavLinkContactRequest() {
  SimpleXTheme {
    ChatListNavLinkLayout(
      chatLinkPreview = {
        ContactRequestView(false, ChatInfo.ContactRequest.sampleData)
      },
      click = {},
      dropdownMenuItems = null,
      showMenu = remember { mutableStateOf(false) },
      stopped = false
    )
  }
}
