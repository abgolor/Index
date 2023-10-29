package chat.echo.app.views.chat.group

import InfoRow
import SectionDivider
import SectionItemView
import SectionSpacer
import SectionView
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chat.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.usersettings.ProfileHeader
import chat.echo.app.views.usersettings.SettingsActionItem
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun GroupMemberInfoView(
  groupInfo: GroupInfo,
  member: GroupMember,
  connStats: ConnectionStats?,
  chatModel: ChatModel,
  close: () -> Unit,
  closeAll: () -> Unit,
) {
  BackHandler(onBack = close)
  val chat = chatModel.chats.firstOrNull { it.id == chatModel.chatId.value }
  val developerTools = chatModel.controller.appPrefs.developerTools.get()
  if (chat != null) {
    chatModel.localContactViewModel?.getLocalContact(chat.chatInfo.id)
  }
  val roles = member.canChangeRoleTo(groupInfo)
  val newRole = remember { mutableStateOf(member.memberRole) }
  if (chat != null) {
    GroupChatMemberInfoLayout(
      groupInfo = groupInfo,
      member = member,
      connStats = connStats,
      openDirectChat = {
        withApi {
          val c = chatModel.controller.apiGetChat(ChatType.Direct, it)
          if (c != null) {
            if (chatModel.getContactChat(it) == null) {
              chatModel.addChat(c)
            }
            chatModel.chatItems.clear()
            chatModel.chatItems.addAll(c.chatItems)
            chatModel.chatId.value = c.id
            closeAll()
          }
        }
        /*withApi {
          val oldChat = chatModel.getContactChat(member.memberContactId ?: return@withApi)
          if (oldChat != null) {
            openChat(oldChat.chatInfo, chatModel)
          } else {
            var newChat = chatModel.controller.apiGetChat(ChatType.Direct, member.memberContactId) ?: return@withApi
            // TODO it's not correct to blindly set network status to connected - we should manage network status in model / backend
            newChat = newChat.copy(serverInfo = Chat.ServerInfo(networkStatus = Chat.NetworkStatus.Connected()))
            chatModel.addChat(newChat)
            chatModel.chatItems.clear()
            chatModel.chatId.value = newChat.id
          }
          closeAll()
        }*/
      },
      removeMember = {
        removeMemberDialog(groupInfo, member, chatModel, close)
      },
      roles = roles,
      newRole = newRole,
      changeRole = {
        ModalManager.shared.showCustomModal { close ->
          roles?.let {groupMembers ->
            ChangeRoleView(chatModel = chatModel, roles = roles, member = member, groupInfo = groupInfo, selectedRole = newRole, close = close, closeAll = closeAll)
         /*   ChangeRoleView(roles = groupMembers, selectedRole = newRole,
              onSelected = {
              *//*  if (it == newRole.value) return@ChangeRoleView
                val prevValue = newRole.value
                newRole.value = it
                updateMemberRoleDialog(it, member, onDismiss = {
                  newRole.value = prevValue
                }) {
                  withApi {
                    kotlin.runCatching {
                      val mem = chatModel.controller.apiMemberRole(groupInfo.groupId, member.groupMemberId, it)
                      chatModel.upsertGroupMember(groupInfo, mem)
                      close.invoke()
                    }.onFailure {
                      newRole.value = prevValue
                      close.invoke()
                    }
                  }
                }*//*
              }
              *//*onSelected = {
              if (it == newRole.value) return@ChangeRoleView
              val prevValue = newRole.value
              newRole.value = it
              updateMemberRoleDialog(it, member, onDismiss = {
                newRole.value = prevValue
              }) {
                withApi {
                  kotlin.runCatching {
                    val mem = chatModel.controller.apiMemberRole(groupInfo.groupId, member.groupMemberId, it)
                    chatModel.upsertGroupMember(groupInfo, mem)
                    closeAll()
                  }.onFailure {
                    newRole.value = prevValue
                    closeAll()
                  }
                }
              }
            }*//*, close = close)*/
          }
        }
      },
      close = close
    )
  }
}

suspend fun clearChatDialog(
  chatInfo: ChatInfo, chatModel: ChatModel,
  bottomSheetModalState: ModalBottomSheetState? = null, close: (() -> Unit)? = null
) {
  bottomSheetModalState?.hide()
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.clear_chat_question),
    text = generalGetString(R.string.clear_chat_warning),
    confirmText = generalGetString(R.string.clear_verb),
    onConfirm = {
      withApi {
        val updatedChatInfo = chatModel.controller.apiClearChat(chatInfo.chatType, chatInfo.apiId)
        if (updatedChatInfo != null) {
          chatModel.clearChat(updatedChatInfo)
          chatModel.controller.ntfManager.cancelNotificationsForChat(chatInfo.id)
          close?.invoke()
        }
      }
    }
  )
}

@Composable
fun GroupChatMemberInfoLayout(
  groupInfo: GroupInfo,
  member: GroupMember,
  connStats: ConnectionStats?,
  newRole: MutableState<GroupMemberRole>,
  roles: List<GroupMemberRole>? = null,
  openDirectChat: (Long) -> Unit,
  removeMember: () -> Unit,
  changeRole: () -> Unit,
  close: () -> Unit
) {
  val scaffoldState = rememberScaffoldState()

  Scaffold(
    topBar = {
      Column() {
        GroupMemberInfoToolBar(close = close, centered = true)
        Divider(color = PreviewTextColor)
      }
    },
    scaffoldState = scaffoldState,
    drawerGesturesEnabled = false,
    backgroundColor = Color.White
  ) {
    Box(modifier = Modifier.padding(it)) {
      Column(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
      ) {
        GroupMemberInfoProfileImage(groupMember = member, isZeroKnowledge = groupInfo.displayName.startsWith("*"))
        GroupMemberNameAction(groupInfo = groupInfo, groupMember = member, isZeroKnowledge = groupInfo.displayName.startsWith("*"), chat = openDirectChat, removeMember = removeMember)
        GroupInformationHeader(headerTitle = generalGetString(R.string.role_in_group), actionText = generalGetString(R.string.change_role), color = Color.Black, isClickable = roles != null) {
          changeRole()
        }
        Text(
          newRole.value.text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = Color.Black,
          modifier = Modifier.padding(start = 10.dp, end = 10.dp)
        )
        GroupInformationHeader(headerTitle = generalGetString(R.string.connection_level), color = Color.Black, isClickable = false) {
        }
        val conn = member.activeConn
        if (conn != null) {
          val connLevelDesc =
            if (conn.connLevel == 0) stringResource(R.string.conn_level_desc_direct)
            else String.format(generalGetString(R.string.conn_level_desc_indirect), conn.connLevel)
          Text(
            if(groupInfo.displayName.startsWith("*")) generalGetString(R.string.zero_knowledge) else connLevelDesc.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp)
          )
        }
        GroupInformationHeader(headerTitle = generalGetString(R.string.shared_group), color = Color.Black, isClickable = false) {
        }
        Text(
          if (groupInfo.fullName != "") groupInfo.fullName else
            if (groupInfo.displayName.startsWith("*")) groupInfo.displayName.removePrefix("*")
            else (groupInfo.displayName),
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = Color.Black,
          modifier = Modifier.padding(start = 10.dp, end = 10.dp)
        )
        Spacer(modifier = Modifier.padding(top = 20.dp))
        if (connStats != null) {
          ServerInformationHeader(title = generalGetString(R.string.server_information))
          Spacer(modifier = Modifier.padding(top = 5.dp, start = 10.dp, end = 10.dp, bottom = 5.dp))
          connStats.sndServers?.let { it1 -> connStats.rcvServers?.let { it2 -> ContactConnectionInfoView(senderServer = it1, receiverServer = it2) } }
        }
      }
    }
  }
}

@Composable
fun GroupMemberInfoToolBar(close: () -> Unit, centered: Boolean) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(AppBarHeight)
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color.White)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(
        Modifier
          .fillMaxHeight()
          .width(TitleInsetWithIcon - AppBarHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(close) {
          Icon(
            Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(22.dp)
          )
        }
      }
      val startPadding = TitleInsetWithIcon
      val endPadding = (2 * 50f).dp
      Box(
        Modifier
          .fillMaxWidth()
          .padding(
            start = if (centered) kotlin.math.max(startPadding.value, endPadding.value).dp else startPadding,
            end = if (centered) kotlin.math.max(startPadding.value, endPadding.value).dp else endPadding
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = generalGetString(R.string.member_info),
          fontSize = 18.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.Medium,
          color = Color.Black
        )
      }
    }
  }
}

@Composable
fun GroupMemberInfoProfileImage(groupMember: GroupMember, isZeroKnowledge: Boolean) {
  GroupMemberInfoProfileImage(groupMember, isZeroKnowledge = isZeroKnowledge, size = 200.dp, iconColor = if (isInDarkTheme()) Color.Black else SettingsSecondaryLight, icon = Icons.Filled.AccountCircle)
}

@Composable
fun GroupMemberNameAction(groupInfo: GroupInfo, groupMember: GroupMember, isZeroKnowledge: Boolean, chat: (Long) -> Unit, removeMember: () -> Unit) {
  Box(
    modifier = Modifier.fillMaxWidth()
      .background(Color.Black),
  ) {
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(
        Modifier
          .fillMaxHeight()
          .fillMaxWidth()
      ) {
        Box(
          Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(15.dp)
            .align(Alignment.CenterVertically)
        ) {
          Text(
            text = if (isZeroKnowledge) groupMember.memberId.substring(0, 6) else if (groupMember.fullName != "") groupMember.fullName else groupMember.displayName,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            color = Color.White
          )
        }
        Row(
          Modifier.fillMaxWidth().weight(1f).align(Alignment.CenterVertically),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if(!isZeroKnowledge) {
            IconButton(onClick = {
              chat
            }) {
              Icon(
                Icons.Outlined.Comment, stringResource(R.string.chat), tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
          }
          if (groupMember.canBeRemoved(groupInfo)) {
            IconButton(removeMember) {
              Icon(
                Icons.Outlined.Delete, stringResource(R.string.remove_member), tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ContactConnectionInfoView(senderServer: List<String>, receiverServer: List<String>) {
  val clipboardManager: ClipboardManager = LocalClipboardManager.current
  val copySenderServerEnabled = remember {
    mutableStateOf(true)
  }
  val copyReceiverServerEnabled = remember {
    mutableStateOf(true)
  }
  val senderServerInfo = senderServer.joinToString(separator = ", ") { it.substringAfter("@") }
  val receiverServerInfo = receiverServer.joinToString(separator = ", ") { it.substringAfter("@") }


  if (receiverServer != null && receiverServer.isNotEmpty()) {
    ContactInformationHeader(headerTitle = generalGetString(R.string.receiving_via), color = Color.Black, copyReceiverServerEnabled) {
      withApi {
        clipboardManager.setText(AnnotatedString(receiverServer.joinToString(separator = ",")))
        copyReceiverServerEnabled.value = false
        delay(10000)
        copyReceiverServerEnabled.value = true
      }
    }
    Text(
      receiverServerInfo,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      color = Color.Black,
      modifier = Modifier.padding(start = 10.dp, end = 10.dp)
    )
  }

  if (senderServer != null && senderServer.isNotEmpty()) {
    ContactInformationHeader(headerTitle = generalGetString(R.string.sending_via), color = Color.Black, enabled = copySenderServerEnabled) {
      withApi {
        clipboardManager.setText(AnnotatedString(senderServer.joinToString(separator = ",")))
        copySenderServerEnabled.value = false
        delay(10000)
        copySenderServerEnabled.value = true
      }
    }

    Text(
      senderServerInfo,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      color = Color.Black,
      modifier = Modifier.padding(start = 10.dp, end = 10.dp)
    )
  }
}

@Composable
fun GroupInformationHeader(headerTitle: String, actionText: String = "", color: Color = TextHeader, isClickable: Boolean, click: () -> Unit) {
  Row(Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 5.dp)) {
    Text(
      text = headerTitle,
      fontSize = 14.sp,
      modifier = Modifier.align(Alignment.CenterVertically).alpha(0.5f),
      fontWeight = FontWeight.Normal,
      color = color
    )
    Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
    if (isClickable) {
      Text(
        text = actionText,
        fontSize = 14.sp,
        modifier = Modifier
          .clickable {
            click()
          }
          .align(Alignment.CenterVertically)
          .alpha(0.5f),
        fontWeight = FontWeight.Medium,
        color = color
      )
    }
  }
}

@Composable
fun ChatBioExtraView(initialValue: String, updateValue: (String) -> Unit) {
  Log.i("TAG", "ChatBioExtraView: intial value is " + initialValue)
  val focusRequester = remember { FocusRequester() }
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  var chatBioInfo by remember {
    mutableStateOf(
      if (initialValue != "") {
        json.decodeFromString(ContactBioInfoSerializer, initialValue)
      } else {
        ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
      }
    )
  }
  val tag = remember {
    mutableStateOf(
      TextFieldValue(
        text = chatBioInfo.tag,
        selection = TextRange(chatBioInfo.tag.length)
      )
    )
  }
  val notes = remember {
    mutableStateOf(
      TextFieldValue(
        text = chatBioInfo.notes,
        selection = TextRange(chatBioInfo.notes.length)
      )
    )
  }
  val editBio = remember {
    mutableStateOf(false)
  }
  ProfileHeader(headerTitle = generalGetString(R.string.tags), color = Color.Black, editProfile = editBio, isEdit = true) {
    if (editBio.value) {
      chatBioInfo =
        ContactBioInfo.ContactBioExtra(
          if (tag.value.text.startsWith("#"))
            tag.value.text.lowercase().replace(" ", "")
          else "#" + tag.value.text.lowercase().replace(" ", ""), notes.value.text, chatBioInfo.publicKey,
          chatBioInfo.openKeyChainID
        )
      updateValue(json.encodeToString(ContactBioInfoSerializer, chatBioInfo))
      editBio.value = false
      focusManager.clearFocus()
    } else {
      editBio.value = true
    }
  }
  if (editBio.value) {
    BasicTextField(
      value = tag.value,
      onValueChange = {
        tag.value = it
      },
      modifier = if (editBio.value) Modifier
        .padding(start = 10.dp, end = 10.dp)
        .fillMaxWidth()
        .focusRequester(focusRequester) else Modifier.padding(start = 10.dp, end = 10.dp),
      decorationBox = { innerTextField ->
        Box(
          contentAlignment = Alignment.CenterStart
        ) {
          if (tag.value.text.isEmpty()) {
            Text(
              text = generalGetString(R.string.add_tag_for_this_contact),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              color = TextHeader,
            )
          }
          innerTextField()
        }
      },
      textStyle = MaterialTheme.typography.body1.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black
      ),
      keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrect = false
      ),
      singleLine = true,
      cursorBrush = SolidColor(HighOrLowlight)
    )
  } else {
    Text(
      if (chatBioInfo.tag == "") generalGetString(R.string.add_tag_for_this_contact) else chatBioInfo.tag,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      color = Color.Black,
      modifier = Modifier.padding(start = 10.dp, end = 10.dp)
    )
  }
  ProfileHeader(headerTitle = generalGetString(R.string.notes), color = Color.Black, editProfile = editBio) {
  }
  if (editBio.value) {
    BasicTextField(
      value = notes.value,
      onValueChange = {
        notes.value = it
      },
      modifier = Modifier.padding(start = 10.dp, end = 10.dp)
        .fillMaxWidth(),
      decorationBox = { innerTextField ->
        Box(
          contentAlignment = Alignment.CenterStart
        ) {
          if (notes.value.text.isEmpty()) {
            Text(
              text = generalGetString(R.string.add_a_note_visible_for_you),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              color = TextHeader,
            )
          }
          innerTextField()
        }
      },
      textStyle = MaterialTheme.typography.body1.copy(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black
      ),
      keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrect = false
      ),
      cursorBrush = SolidColor(HighOrLowlight)
    )
  } else {
    Text(
      if (chatBioInfo.tag == "") generalGetString(R.string.add_a_note_visible_for_you) else chatBioInfo.notes,
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium,
      color = Color.Black,
      modifier = Modifier.padding(start = 10.dp, end = 10.dp)
    )
  }
}

@Composable
fun GroupMemberInfoViewx(
  groupInfo: GroupInfo,
  member: GroupMember,
  connStats: ConnectionStats?,
  chatModel: ChatModel,
  close: () -> Unit,
  closeAll: () -> Unit, // Close all open windows up to ChatView
) {
  BackHandler(onBack = close)
  val chat = chatModel.chats.firstOrNull { it.id == chatModel.chatId.value }
  val developerTools = chatModel.controller.appPrefs.developerTools.get()
  if (chat != null) {
    val newRole = remember { mutableStateOf(member.memberRole) }
    GroupMemberInfoLayout(
      groupInfo,
      member,
      connStats,
      newRole,
      developerTools,
      openDirectChat = {
        withApi {
          val c = chatModel.controller.apiGetChat(ChatType.Direct, it)
          if (c != null) {
            if (chatModel.getContactChat(it) == null) {
              chatModel.addChat(c)
            }
            chatModel.chatItems.clear()
            chatModel.chatItems.addAll(c.chatItems)
            chatModel.chatId.value = c.id
            closeAll()
          }
        }
       /* withApi {
          val oldChat = chatModel.getContactChat(member.memberContactId ?: return@withApi)
          if (oldChat != null) {
            openChat(oldChat.chatInfo, chatModel)
          } else {
            var newChat = chatModel.controller.apiGetChat(ChatType.Direct, member.memberContactId) ?: return@withApi
            // TODO it's not correct to blindly set network status to connected - we should manage network status in model / backend
            newChat = newChat.copy(serverInfo = Chat.ServerInfo(networkStatus = Chat.NetworkStatus.Connected()))
            chatModel.addChat(newChat)
            chatModel.chatItems.clear()
            chatModel.chatId.value = newChat.id
          }
          closeAll()
        }*/
      },
      removeMember = { removeMemberDialog(groupInfo, member, chatModel, close) },
      onRoleSelected = {
        if (it == newRole.value) return@GroupMemberInfoLayout
        val prevValue = newRole.value
        newRole.value = it
        updateMemberRoleDialog(it, member, onDismiss = {
          newRole.value = prevValue
        }) {
          withApi {
            kotlin.runCatching {
              val mem = chatModel.controller.apiMemberRole(groupInfo.groupId, member.groupMemberId, it)
              chatModel.upsertGroupMember(groupInfo, mem)
            }.onFailure {
              newRole.value = prevValue
            }
          }
        }
      }
    )
  }
}

fun removeMemberDialog(groupInfo: GroupInfo, member: GroupMember, chatModel: ChatModel, close: (() -> Unit)? = null) {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.button_remove_member),
    text = generalGetString(R.string.member_will_be_removed_from_group_cannot_be_undone),
    confirmText = generalGetString(R.string.remove_member_confirmation),
    onConfirm = {
      withApi {
        val removedMember = chatModel.controller.apiRemoveMember(member.groupId, member.groupMemberId)
        if (removedMember != null) {
          chatModel.upsertGroupMember(groupInfo, removedMember)
        }
        close?.invoke()
      }
    }
  )
}

@Composable
fun GroupMemberInfoLayout(
  groupInfo: GroupInfo,
  member: GroupMember,
  connStats: ConnectionStats?,
  newRole: MutableState<GroupMemberRole>,
  developerTools: Boolean,
  openDirectChat: (Long) -> Unit,
  removeMember: () -> Unit,
  onRoleSelected: (GroupMemberRole) -> Unit,
) {
  Column(
    Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.Start
  ) {
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center
    ) {
      GroupMemberInfoHeader(groupInfo.displayName[0].equals('*', true), member)
    }
    SectionSpacer()

    SectionView {
      OpenChatButton({
        openDirectChat
      })
    }
    SectionSpacer()

    SectionView(title = stringResource(R.string.member_info_section_title_member)) {
      InfoRow(stringResource(R.string.info_row_group), groupInfo.displayName.removePrefix("*"))
      SectionDivider()
      val roles = remember { member.canChangeRoleTo(groupInfo) }
      if (roles != null) {
        SectionItemView {
          RoleSelectionRow(roles, newRole, onRoleSelected)
        }
      } else {
        InfoRow(stringResource(R.string.role_in_group), member.memberRole.text)
      }
      val conn = member.activeConn
      if (conn != null) {
        SectionDivider()
        val connLevelDesc =
          if (conn.connLevel == 0) stringResource(R.string.conn_level_desc_direct)
          else String.format(generalGetString(R.string.conn_level_desc_indirect), conn.connLevel)
        InfoRow(stringResource(R.string.info_row_connection), connLevelDesc)
      }
    }
    SectionSpacer()

    if (connStats != null) {
      val rcvServers = connStats.rcvServers
      val sndServers = connStats.sndServers
      if ((rcvServers != null && rcvServers.isNotEmpty()) || (sndServers != null && sndServers.isNotEmpty())) {
        SectionView(title = stringResource(R.string.conn_stats_section_title_servers)) {
          if (rcvServers != null && rcvServers.isNotEmpty()) {
            SimplexServers(stringResource(R.string.receiving_via), rcvServers)
            if (sndServers != null && sndServers.isNotEmpty()) {
              SectionDivider()
              SimplexServers(stringResource(R.string.sending_via), sndServers)
            }
          } else if (sndServers != null && sndServers.isNotEmpty()) {
            SimplexServers(stringResource(R.string.sending_via), sndServers)
          }
        }
        SectionSpacer()
      }
    }

    if (member.canBeRemoved(groupInfo)) {
      SectionView {
        RemoveMemberButton(removeMember)
      }
      SectionSpacer()
    }

    if (developerTools) {
      SectionView(title = stringResource(R.string.section_title_for_console)) {
        InfoRow(stringResource(R.string.info_row_local_name), member.localDisplayName)
        SectionDivider()
        InfoRow(stringResource(R.string.info_row_database_id), member.groupMemberId.toString())
      }
      SectionSpacer()
    }
  }
}

@Composable
fun GroupMemberInfoHeader(isZeroKnowledge: Boolean, member: GroupMember) {
  Column(
    Modifier.padding(horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    ProfileImage(size = 192.dp, member.image, color = if (isInDarkTheme()) GroupDark else SettingsSecondaryLight)
    Text(
      if (isZeroKnowledge) member.memberId.substring(0, 7) else member.displayName, style = MaterialTheme.typography.h1.copy(fontWeight = FontWeight.Normal),
      color = MaterialTheme.colors.onBackground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    if (member.fullName != "" && member.fullName != member.displayName) {
      Text(
        member.fullName, style = MaterialTheme.typography.h2,
        color = MaterialTheme.colors.onBackground,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun RemoveMemberButton(onClick: () -> Unit) {
  SettingsActionItem(
    Icons.Outlined.Delete,
    stringResource(R.string.button_remove_member),
    click = onClick,
    textColor = Color.Red,
    iconColor = Color.Red,
  )
}

@Composable
fun OpenChatButton(onClick: () -> Unit) {
  SettingsActionItem(
    Icons.Outlined.Message,
    stringResource(R.string.button_send_direct_message),
    click = onClick,
    textColor = MaterialTheme.colors.primary,
    iconColor = MaterialTheme.colors.primary,
  )
}

@Composable
private fun RoleSelectionRow(
  roles: List<GroupMemberRole>,
  selectedRole: MutableState<GroupMemberRole>,
  onSelected: (GroupMemberRole) -> Unit
) {
  Row(
    Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    val values = remember { roles.map { it to it.text } }
    ExposedDropDownSettingRow(
      generalGetString(R.string.change_role),
      values,
      selectedRole,
      icon = null,
      enabled = remember { mutableStateOf(true) },
      onSelected = onSelected
    )
  }
}

private fun updateMemberRoleDialog(
  newRole: GroupMemberRole,
  member: GroupMember,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(R.string.change_member_role_question),
    text = if (member.memberCurrent)
      String.format(generalGetString(R.string.member_role_will_be_changed_with_notification), newRole.text)
    else
      String.format(generalGetString(R.string.member_role_will_be_changed_with_invitation), newRole.text),
    confirmText = generalGetString(R.string.change_verb),
    onDismiss = onDismiss,
    onConfirm = onConfirm,
    onDismissRequest = onDismiss
  )
}

@Preview
@Composable
fun PreviewGroupMemberInfoLayout() {
  SimpleXTheme {
    GroupMemberInfoLayout(
      groupInfo = GroupInfo.sampleData,
      member = GroupMember.sampleData,
      connStats = null,
      newRole = remember { mutableStateOf(GroupMemberRole.Member) },
      developerTools = false,
      openDirectChat = {},
      removeMember = {},
      onRoleSelected = {}
    )
  }
}
