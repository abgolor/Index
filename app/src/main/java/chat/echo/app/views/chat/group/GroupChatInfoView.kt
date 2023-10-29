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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.*
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.bottomsheet.*
import chat.echo.app.views.chat.*
import chat.echo.app.views.chat.getBurnerTimerText
import chat.echo.app.views.chatlist.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.usersettings.ProfileHeader
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.charlex.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GroupInfoLayout(
  chat: Chat,
  chatModel: ChatModel,
  bottomSheetModalState: ModalBottomSheetState,
  groupInfo: GroupInfo,
  members: List<GroupMember>,
  developerTools: Boolean,
  addMembers: () -> Unit,
  showMemberInfo: (GroupMember) -> Unit,
  toggleGroupChatNotification: () -> Unit,
  removeMember: (GroupMember) -> Unit,
  editGroupProfile: () -> Unit,
  deleteGroup: () -> Unit,
  clearChat: () -> Unit,
  leaveGroup: () -> Unit,
  manageGroupLink: () -> Unit,
  close: () -> Unit
) {
  val scaffoldState = rememberScaffoldState()
  val coroutineScope = rememberCoroutineScope()
  var currentBottomSheet: GroupChatBottomSheetType? by remember {
    mutableStateOf(null)
  }
  val showChatSettingsBottomView = remember {
    mutableStateOf(true)
  }
  val openSheet: (GroupChatBottomSheetType) -> Unit = {
    currentBottomSheet = it
    coroutineScope.launch {
      delay(100)
      bottomSheetModalState.show()
    }
  }
  val closeSheet: () -> Unit = {
    coroutineScope.launch {
      bottomSheetModalState.hide()
    }
  }
  val editGroupProfile = {
    ModalManager.shared.showCustomModal { close ->
      EditGroupView(chatModel = chatModel, groupInfo = groupInfo, groupProfile = groupInfo.groupProfile, isZeroKnowledge = groupInfo.displayName.startsWith("*"), close = close)
    }
  }

  val openGroupChatSettings: () -> Unit = {
    openSheet(GroupChatBottomSheetType.ShowChatSettings)
  }

  val openBurnerTimerSettings: () -> Unit = {
    openSheet(GroupChatBottomSheetType.ShowBurnerTimerSettings)
  }

  if (!bottomSheetModalState.isVisible) {
    currentBottomSheet = null
  }


  ModalBottomSheetLayout(
    scrimColor = Color.Black.copy(alpha = 0.12F),
    modifier = Modifier.navigationBarsWithImePadding(),
    sheetContent = {
      if (currentBottomSheet != null) {
        GroupChatSheetLayout(
          chatModel = chatModel,
          chat = chat,
          groupInfo = groupInfo,
          toggleGroupChatNotification = toggleGroupChatNotification,
          deleteGroup = deleteGroup,
          clearChat = clearChat,
          leaveGroup = leaveGroup,
          bottomSheetType = currentBottomSheet!!,
          closeSheet = closeSheet,
          close = close
        )
      }
    },
    sheetState = bottomSheetModalState,
    sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    content = {
      Scaffold(
        topBar = {
          Column() {
            GroupChatInfoToolBar(groupInfo = groupInfo, centered = true, editGroupProfile = editGroupProfile, openGroupChatSettings = openGroupChatSettings, close = close)
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
              .fillMaxHeight(),
            //.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
          ) {
            GroupInfoProfileImage(cInfo = chat.chatInfo)
            GroupNameAction(
              groupInfo = groupInfo,
              groupProfile = groupInfo.groupProfile,
              chat = {
                directChatAction(chat.chatInfo, chatModel)
                close()
              },
              isZeroKnowledge = groupInfo.displayName.startsWith("*"),
              createGroupLink = manageGroupLink,
              changeBurnerTimer = {
                openBurnerTimerSettings()
              }
            )
            GroupParticipantsView(members = members, groupInfo = groupInfo, isZeroKnowledge = groupInfo.displayName.startsWith("*"), addMembers = addMembers, removeMember = removeMember, leaveGroup = leaveGroup, showMemberInfo = showMemberInfo)
          }
        }
      }
    })
}

@Composable
fun GroupParticipantsView(
  groupInfo: GroupInfo, members: List<GroupMember>, isZeroKnowledge: Boolean,
  showMemberInfo: (GroupMember) -> Unit,
  addMembers: () -> Unit, removeMember: (GroupMember) -> Unit,
  leaveGroup: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  val state = rememberRevealState()
  val groupMembers = members.toMutableList()
  groupMembers.add(members.size, groupInfo.membership)
  groupMembers.reverse()
  Column() {
    Row(
      Modifier.fillMaxWidth()
        .padding(top = 10.dp, start = 10.dp, end = 10.dp)
    ) {
      Text(
        text = generalGetString(R.string.participants) + ": " + groupMembers.size,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .align(Alignment.CenterVertically),
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Black
      )
      if (groupInfo.canAddMembers) {
        IconButton(addMembers) {
          Icon(
            Icons.Outlined.GroupAdd, stringResource(R.string.chat), tint = Color.Black,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }
    LazyColumn(
      modifier = Modifier.fillMaxWidth()
        .padding(top = 5.dp, bottom = 5.dp, end = 10.dp, start = 10.dp)
    ) {
      items(groupMembers) { member ->
        val coroutineScope = rememberCoroutineScope()
        val state = rememberRevealState()
        val isUser = groupInfo.membership.id == member.id
        if (member.canBeRemoved(groupInfo) && !isUser) {
          RevealSwipe(
            directions = setOf(
              RevealDirection.EndToStart
            ),
            state = state,
            hiddenContentEnd = {
              IconButton(
                modifier = Modifier.background(DeleteRed).fillMaxHeight(1f),
                onClick = {
                  coroutineScope.launch {
                    removeMember(member)
                    state.resetFast()
                  }
                }) {
                Icon(
                  modifier = Modifier.padding(horizontal = 25.dp),
                  imageVector = Icons.Outlined.Delete,
                  tint = Color.White,
                  contentDescription = generalGetString(R.string.delete_chat)
                )
              }
            }
          ) {
            GroupMemberNavLinkView(
              groupMemberPreview = { GroupMemberPreview(groupMember = member, isZeroKnowledge = isZeroKnowledge) },
              click = { showMemberInfo(member) })
          }
        } else {
          GroupMemberNavLinkView(
            groupMemberPreview = { GroupMemberPreview(groupMember = member, isZeroKnowledge = isZeroKnowledge, isUser = isUser) },
            click = { showMemberInfo(member) })
        }
      }
    }
  }
}

@Composable
fun GroupMemberNavLinkView(
  groupMemberPreview: @Composable () -> Unit,
  click: () -> Unit
) {
  var modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).clickable { click() }
  Surface(
    modifier,
    color = Color.White
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .padding(start = 8.dp)
        .padding(end = 12.dp),
      verticalAlignment = Alignment.Top
    ) {
      groupMemberPreview()
    }
  }
  // Divider(Modifier.padding(horizontal = 8.dp), color = DividerColor)
}

@Composable
fun GroupMemberPreview(
  groupMember: GroupMember, isZeroKnowledge: Boolean,
  isUser: Boolean = false
) {
  var contactBioExtra = if (groupMember.memberProfile.localAlias != "") {
    json.decodeFromString(ContactBioInfoSerializer, groupMember.memberProfile.localAlias)
  } else {
    ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
  }
  Row(
    Modifier
      .background(Color.White)
  ) {
    Box(contentAlignment = Alignment.BottomEnd) {
      GroupMemberInfoImage(groupMember, isZeroKnowledge = isZeroKnowledge, size = 48.dp, iconColor = Color.Black)
    }
    Column(
      modifier = Modifier
        .padding(horizontal = 8.dp)
        .weight(1F)
        .align(Alignment.CenterVertically)
    ) {
      Row {
        Box(Modifier.weight(1f)) {
          Text(
            text = if (isUser == true) {
              "You"
            } else {
              if (isZeroKnowledge) groupMember.memberId.substring(0, 6) else
                if (groupMember.fullName != "") groupMember.fullName else groupMember.localDisplayName
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontSize = 16.sp),
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
          )
          if (contactBioExtra.openKeyChainID != "") {
            Icon(
              imageVector = Icons.Default.Verified,
              contentDescription = generalGetString(R.string.verified_icon),
              tint = Color.Blue,
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 4.dp)
            )
          }
        }
      }
    /*![](../../../../../../../../../../../../../../../../var/folders/fm/7jv8w5zj3kdc0m_lr5ms044c0000gn/T/TemporaryItems/NSIRD_screencaptureui_ysEq5U/Screenshot 2023-02-02 at 23.30.06.png)*/
      Text(
        groupMember.memberRole.memberRole + " • " + groupMember.memberStatus.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontSize = 12.sp),
        fontWeight = FontWeight.Normal,
        color = PreviewTextColor
      )
    }
  }
}

@Composable
fun GroupProfileDetails(groupInfo: GroupInfo, groupProfile: GroupProfile, saveProfile: (GroupProfile) -> Unit) {
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val isZeroKnowledge = groupInfo.displayName.startsWith("*")
  val displayName = remember {
    mutableStateOf(
      TextFieldValue(
        text = if (isZeroKnowledge) groupProfile.displayName.removePrefix("*") else groupProfile.displayName,
        selection = TextRange(if (isZeroKnowledge) groupProfile.displayName.length - 1 else groupProfile.displayName.length)
      )
    )
  }
  val fullName = remember {
    mutableStateOf(
      TextFieldValue(
        text = groupProfile.fullName,
        selection = TextRange(groupProfile.fullName.length)
      )
    )
  }
  val editGroupProfile = remember {
    mutableStateOf(false)
  }
  ProfileHeader(headerTitle = generalGetString(R.string.display_name), color = Color.Black, editProfile = editGroupProfile, isEdit = groupInfo.canEdit) {
    if (editGroupProfile.value) {
      if (displayName.value.text.isEmpty()) {
        saveProfile(GroupProfile(groupProfile.displayName, fullName.value.text, groupProfile.image, groupProfile.localAlias))
      } else {
        saveProfile(GroupProfile(if (isZeroKnowledge) "*" + displayName.value.text.replace(" ", "") else displayName.value.text.replace(" ", ""), fullName.value.text, groupProfile.image, groupProfile.localAlias))
      }
      editGroupProfile.value = false
      focusManager.clearFocus()
    } else {
      editGroupProfile.value = true
    }
  }
  if (editGroupProfile.value) {
    BasicTextField(
      value = displayName.value,
      onValueChange = {
        displayName.value = it
      },
      modifier = if (editGroupProfile.value) Modifier
        .padding(start = 10.dp, end = 10.dp)
        .fillMaxWidth()
        .focusRequester(focusRequester) else Modifier.padding(start = 10.dp, end = 10.dp),
      decorationBox = { innerTextField ->
        Box(
          contentAlignment = Alignment.CenterStart
        ) {
          if (displayName.value.text.isEmpty()) {
            Text(
              text = generalGetString(R.string.enter_group_display_name),
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
      if (groupProfile.displayName == "") generalGetString(R.string.enter_group_display_name) else groupProfile.displayName,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      color = Color.Black,
      modifier = Modifier.padding(start = 10.dp, end = 10.dp)
    )
  }
  ProfileHeader(headerTitle = generalGetString(R.string.full_name), color = Color.Black, editProfile = editGroupProfile) {
  }
  if (editGroupProfile.value) {
    BasicTextField(
      value = fullName.value,
      onValueChange = {
        fullName.value = it
      },
      modifier = Modifier.padding(start = 10.dp, end = 10.dp)
        .fillMaxWidth(),
      decorationBox = { innerTextField ->
        Box(
          contentAlignment = Alignment.CenterStart
        ) {
          if (fullName.value.text.isEmpty()) {
            Text(
              text = generalGetString(R.string.enter_group_full_name),
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
      if (groupProfile.fullName == "") generalGetString(R.string.enter_group_full_name) else groupProfile.fullName,
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium,
      color = Color.Black,
      modifier = Modifier.padding(start = 10.dp, end = 10.dp)
    )
  }
}

@Composable
fun GroupNameAction(
  groupInfo: GroupInfo, groupProfile: GroupProfile, isZeroKnowledge: Boolean, chat: () -> Unit, createGroupLink: () -> Unit,
  changeBurnerTimer: () -> Unit
) {
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
            text = if (groupProfile.fullName.isNotEmpty()) groupProfile.fullName else
              if (isZeroKnowledge) groupProfile.displayName.removePrefix("*") else groupProfile.displayName,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            color = Color.White
          )
        }
        Row(
          Modifier.align(Alignment.CenterVertically),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(chat) {
            Icon(
              Icons.Outlined.Comment, stringResource(R.string.chat), tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          IconButton(changeBurnerTimer) {
            Icon(
              Icons.Outlined.Timer, stringResource(R.string.change_group_burner_timer), tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          if (groupInfo.canAddMembers) {
            IconButton(createGroupLink) {
              Icon(
                Icons.Outlined.Link, stringResource(R.string.button_create_group_link), tint = Color.White,
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
fun GroupChatInfoToolBar(
  groupInfo: GroupInfo,
  centered: Boolean,
  openGroupChatSettings: () -> Unit,
  editGroupProfile: () -> Unit,
  close: () -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()
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
      //end padding is gotten from the number of icon * 50f in dp
      val endPadding = (1 * 50f).dp
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
          text = generalGetString(R.string.group_info),
          fontSize = 18.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.Medium,
          color = Color.Black
        )
      }
      Row(
        Modifier
          .fillMaxHeight()
          .fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (groupInfo.canEdit) {
          IconButton(editGroupProfile) {
            Icon(
              Icons.Outlined.Edit, stringResource(R.string.edit_group), tint = Color.Black,
              modifier = Modifier.size(22.dp)
            )
          }
        }
        IconButton({
          coroutineScope.launch {
            openGroupChatSettings()
          }
        }) {
          Icon(
            Icons.Outlined.MoreHoriz, stringResource(R.string.more_options), tint = Color.Black,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }
  }
}

@Composable
fun GroupInfoProfileImage(cInfo: ChatInfo) {
  ChatInfoImage(cInfo, size = 200.dp, iconColor = if (isInDarkTheme()) Color.Black else SettingsSecondaryLight)
}

@Composable
fun GroupChatInfoView(chatModel: ChatModel, close: () -> Unit) {
  BackHandler(onBack = close)
  val chat = chatModel.chats.firstOrNull { it.id == chatModel.chatId.value }
  val developerTools = chatModel.controller.appPrefs.developerTools.get()
  val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val coroutineScope = rememberCoroutineScope()
  if (chat != null && chat.chatInfo is ChatInfo.Group) {
    val groupInfo = chat.chatInfo.groupInfo
    GroupInfoLayout(
      chat,
      chatModel,
      bottomSheetModalState,
      groupInfo,
      members = chatModel.groupMembers
        .filter { it.memberStatus != GroupMemberStatus.MemLeft && it.memberStatus != GroupMemberStatus.MemRemoved }
        .sortedBy { it.displayName.lowercase() },
      developerTools,
      addMembers = {
        withApi {
          setGroupMembers(groupInfo, chatModel)
          ModalManager.shared.showCustomModal { close ->
            AddGroupMembersView(chatModel, isZeroKnowledge = false, groupInfo = groupInfo, close = close)
          }
        }
      },
      removeMember = { groupMember ->
        removeMemberDialog(groupInfo, groupMember, chatModel)
      },
      showMemberInfo = { member ->
        withApi {
          val stats = chatModel.controller.apiGroupMemberInfo(groupInfo.groupId, member.groupMemberId)
          ModalManager.shared.showCustomModal { closeCurrent ->
            GroupMemberInfoView(groupInfo, member, stats, chatModel, closeCurrent) { closeCurrent(); close() }
          }
        }
      },
      editGroupProfile = {
        ModalManager.shared.showCustomModal { close -> GroupProfileView(groupInfo, chatModel, close) }
      },
      deleteGroup = {
        coroutineScope.launch {
          bottomSheetModalState.hide()
          deleteGroupDialog(chat.chatInfo, groupInfo, chatModel, close)
        }
      },
      clearChat = {
        coroutineScope.launch {
          bottomSheetModalState.hide()
          clearChatDialog(chat.chatInfo, chatModel, close = close)
        }
      },
      leaveGroup = {
        coroutineScope.launch {
          bottomSheetModalState.hide()
          leaveGroupDialog(groupInfo, chatModel, close)
        }
      },
      manageGroupLink = {
        withApi {
          //showGroupLinkComingSoonDialog()
          val link = chatModel.controller.apiGetGroupLink(chat.chatInfo.groupInfo.groupId)
          var groupLink = link?.first
          var groupLinkMemberRole = link?.second
          ModalManager.shared.showCustomModal { close -> GroupLinkViewNew(chatModel, groupInfo, groupLink, groupLinkMemberRole, {}, close) }
          // ModalManager.shared.showModal { GroupLinkViewNew(chatModel, groupInfo, groupLink) }
        }
      },
      toggleGroupChatNotification = {
        coroutineScope.launch {
          bottomSheetModalState.hide()
          changeNtfsStatePerChat(!chat.chatInfo.ntfsEnabled, mutableStateOf(chat.chatInfo.ntfsEnabled), chat, chatModel)
        }
      },
      close = close
    )
  }
}

fun deleteGroupDialog(chatInfo: ChatInfo, groupInfo: GroupInfo, chatModel: ChatModel, close: (() -> Unit)? = null) {
  val alertTextKey =
    if (groupInfo.membership.memberCurrent) R.string.delete_group_for_all_members_cannot_undo_warning
    else R.string.delete_group_for_self_cannot_undo_warning
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.delete_group_question),
    text = generalGetString(alertTextKey),
    confirmText = generalGetString(R.string.delete_verb),
    onConfirm = {
      withApi {
        val r = chatModel.controller.apiDeleteChat(chatInfo.chatType, chatInfo.apiId)
        if (r) {
          chatModel.removeChat(chatInfo.id)
          chatModel.chatId.value = null
          chatModel.controller.ntfManager.cancelNotificationsForChat(chatInfo.id)
          close?.invoke()
        }
      }
    }
  )
}

fun showGroupLinkComingSoonDialog() {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.coming_soon),
    text = generalGetString(R.string.group_link_coming_soon),
    confirmText = generalGetString(R.string.ok),
    onConfirm = {
      AlertManager.shared.hideAlert()
    }
  )
}

fun showBurnerTimerComingSoonDialog() {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.coming_soon),
    text = generalGetString(R.string.in_the_nearest_future_you_would_be_able_to_send_burner_timed_messages_but_for_now_messages_are_deleted_after_five_days),
    confirmText = generalGetString(R.string.ok),
    onConfirm = {
      AlertManager.shared.hideAlert()
    }
  )
}

fun leaveGroupDialog(groupInfo: GroupInfo, chatModel: ChatModel, close: (() -> Unit)? = null) {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.leave_group_question),
    text = generalGetString(R.string.you_will_stop_receiving_messages_from_this_group_chat_history_will_be_preserved),
    confirmText = generalGetString(R.string.leave_group_button),
    onConfirm = {
      withApi {
        chatModel.controller.leaveGroup(groupInfo.groupId)
        close?.invoke()
      }
    }
  )
}

@Composable
fun GroupChatInfoLayout(
  chat: Chat,
  chatModel: ChatModel,
  groupInfo: GroupInfo,
  members: List<GroupMember>,
  developerTools: Boolean,
  addMembers: () -> Unit,
  showMemberInfo: (GroupMember) -> Unit,
  editGroupProfile: () -> Unit,
  deleteGroup: () -> Unit,
  clearChat: () -> Unit,
  leaveGroup: () -> Unit,
  manageGroupLink: () -> Unit,
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
      GroupChatInfoHeader(chat.chatInfo)
    }
    SectionSpacer()

    SectionView(title = String.format(generalGetString(R.string.group_info_section_title_num_members), members.count() + 1)) {
      if (groupInfo.canAddMembers) {
        SectionItemView(manageGroupLink) { GroupLinkButton() }
        SectionDivider()
        val onAddMembersClick = if (chat.chatInfo.incognito) ::cantInviteIncognitoAlert else addMembers
        SectionItemView(onAddMembersClick) {
          val tint = if (chat.chatInfo.incognito) HighOrLowlight else MaterialTheme.colors.primary
          AddMembersButton(tint)
        }
        SectionDivider()
      }
      SectionItemView(minHeight = 50.dp) {
        MemberRow(chatModel, groupInfo.id, groupInfo.membership, groupInfo.isZeroKnowledge, user = true)
      }
      if (members.isNotEmpty()) {
        SectionDivider()
      }
      Log.i(TAG, ("GroupChatInfoLayout: is zero today is " + groupInfo.displayName.get(0)))
      MembersList(chatModel, groupInfo.id, members, groupInfo.displayName.get(0).equals('*', true), showMemberInfo)
    }
    SectionSpacer()
    SectionView {
      SharePublicKeyButton {
        withApi {
          val content = MsgContent.MCGroupPublicKey((groupInfo.groupId).toString(), chatModel.controller.appPrefs.publicKey.get()!!, "all")
          val command = chatModel.controller.apiSendMessage(chat.chatInfo.chatType, chat.chatInfo.apiId, null, null, content)
          if (command != null) {
            chatModel.addChatItem(chat.chatInfo, command.chatItem)
            ModalManager.shared.closeModal()
          }
        }
      }
      SectionDivider()
      SectionItemView() {
        GroupBurnerTimerRow(chat, chatModel)
      }
    }
    SectionDivider()
    SectionView {
      if (groupInfo.canEdit) {
        SectionItemView(editGroupProfile) { EditGroupProfileButton() }
        SectionDivider()
      }
      ClearChatButton(clearChat)
      if (groupInfo.canDelete) {
        SectionDivider()
        SectionItemView(deleteGroup) { DeleteGroupButton() }
      }
      if (groupInfo.membership.memberCurrent) {
        SectionDivider()
        SectionItemView(leaveGroup) { LeaveGroupButton() }
      }
    }
    SectionSpacer()

    if (developerTools) {
      SectionView(title = stringResource(R.string.section_title_for_console)) {
        InfoRow(stringResource(R.string.info_row_local_name), groupInfo.localDisplayName)
        SectionDivider()
        InfoRow(stringResource(R.string.info_row_database_id), groupInfo.apiId.toString())
      }
      SectionSpacer()
    }
  }
}

@Composable
fun GroupBurnerTimerRow(chat: Chat, model: ChatModel) {
  var expanded by remember {
    mutableStateOf(false)
  }
  val burnerTimerList = arrayOf(30L, 300L, 3600L, 28800L, 86400L, 432000L)
  var burnerTime = remember {
    mutableStateOf(0L)
  }
  //For display only...It needs to show the accurate burner timer
  model.localContactViewModel?.processLocalContact(
    model.currentUser.value?.userId.toString(),
    chat.chatInfo.id
  ) {
    val currentGroup = it[0]
    burnerTime.value = currentGroup.burnerTime
  }

  Row(
    Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      Modifier.clickable(
        onClick = {
          AlertManager.shared.showAlertMsg(
            "Burner Timer",
            "All messages that have been sent within ${getBurnerTimerText(burnerTime.value!!)} will be deleted automatically."
          )
        }),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        Icons.Outlined.Timer,
        "Burner Timer",
        tint = MaterialTheme.colors.primary,
      )
      Text("Burner Timer")
    }
    Row(
      Modifier.clickable(
        onClick = {
          expanded = true
        }
      ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Column() {
        Text(
          getBurnerTimerText(burnerTime.value!!),
          color = HighOrLowlight
        )
        DropdownMenu(
          expanded = expanded,
          onDismissRequest = {
            expanded = false
          }
        ) {
          // adding items
          burnerTimerList.forEachIndexed { itemIndex, itemValue ->
            DropdownMenuItem(
              onClick = {
                showBurnerTimerComingSoonDialog()
              }
            ) {
              Text(text = getBurnerTimerText(itemValue))
            }
          }
        }
      }
    }
  }
}

fun processLocalViewModel(model: ChatModel, userID: String, groupID: String, burnerTime: MutableState<Long>, action: (localGroup: LocalContact) -> Unit) {
  model.localContactViewModel?.processLocalContact(
    userID,
    groupID
  ) {
    val currentGroup = it[0]
    burnerTime.value = currentGroup.burnerTime
    action(currentGroup)
  }
}

@Composable
fun GroupChatInfoHeader(cInfo: ChatInfo) {
  Column(
    Modifier.padding(horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    ChatInfoImage(cInfo, size = 192.dp, iconColor = if (isInDarkTheme()) GroupDark else SettingsSecondaryLight)
    Text(
      cInfo.displayName.removePrefix("*"), style = MaterialTheme.typography.h1.copy(fontWeight = FontWeight.Normal),
      color = MaterialTheme.colors.onBackground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    if (cInfo.fullName != "" && cInfo.fullName != cInfo.displayName) {
      Text(
        cInfo.fullName, style = MaterialTheme.typography.h2,
        color = MaterialTheme.colors.onBackground,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun AddMembersButton(tint: Color = MaterialTheme.colors.primary) {
  Row(
    Modifier.fillMaxSize(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      Icons.Outlined.Add,
      stringResource(R.string.button_add_members),
      tint = tint
    )
    Spacer(Modifier.size(8.dp))
    Text(stringResource(R.string.button_add_members), color = tint)
  }
}

@Composable
fun MembersList(chatModel: ChatModel, groupID: String, members: List<GroupMember>, isZeroKnowledge: Boolean, showMemberInfo: (GroupMember) -> Unit) {
  Column {
    members.forEachIndexed { index, member ->
      SectionItemView({ showMemberInfo(member) }, minHeight = 50.dp) {
        MemberRow(chatModel, groupID, member, isZeroKnowledge = isZeroKnowledge)
      }
      if (index < members.lastIndex) {
        SectionDivider()
      }
    }
  }
}

@Composable
fun MemberRow(chatModel: ChatModel, groupID: String, member: GroupMember, isZeroKnowledge: Boolean, user: Boolean = false) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    chatModel.localContactViewModel?.processLocalContact(
      chatModel.currentUser.value?.userId.toString(),
      groupID
    ) {}
    val localViewModel = chatModel.localContactViewModel
    localViewModel?.getLocalContact(groupID)
    val keyColor = remember { mutableStateOf(Color.White) }
    val keyIcon = remember { mutableStateOf(Icons.Outlined.LockClock) }
    val keyDescription = remember { mutableStateOf("Pending") }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      ProfileImage(size = 46.dp, member.image)
      Column {
        Row() {
          Text(
            if (isZeroKnowledge) member.memberId.substring(0, 7) else member.chatViewName, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (member.memberIncognito) Indigo else Color.Unspecified
          )
          if (user) {
            Icon(
              Icons.Outlined.Lock, "Secured", tint = WarningOrange,
              modifier = Modifier.size(10.dp)
            )
          } else {
            chatModel.localContactViewModel?.processLocalContact(
              chatModel.currentUser.value?.userId.toString(),
              groupID
            ) {
              val localGroup = it[0]
              val gson = Gson()
              if (localGroup.encryptedMembers != "") {
                val myType = object: TypeToken<MutableList<LocalContact>>() {}.type
                val encryptedLocalContacts = gson.fromJson<MutableList<LocalContact>>(localGroup.encryptedMembers, myType)
                for (localContact in encryptedLocalContacts) {
                  if (member.memberId == localContact.apiID) {
                    keyIcon.value = Icons.Outlined.Lock
                    keyDescription.value = "Secured"
                    keyColor.value = Color.Green
                  } else {
                    keyIcon.value = Icons.Outlined.LockOpen
                    keyDescription.value = "Not Secured"
                    keyColor.value = Color.Red
                  }
                }
              }
            }
            Icon(
              keyIcon.value, keyDescription.value, tint = keyColor.value,
              modifier = Modifier.size(10.dp)
            )
          }
        }
        val s = member.memberStatus.shortText
        val statusDescr = if (user) String.format(generalGetString(R.string.group_info_member_you), s) else s
        Text(
          statusDescr,
          color = HighOrLowlight,
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    val role = member.memberRole
    if (role == GroupMemberRole.Owner || role == GroupMemberRole.Admin) {
      Text(role.text, color = HighOrLowlight)
    }
  }
}

@Composable
fun GroupLinkButton() {
  Row(
    Modifier
      .fillMaxSize(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      Icons.Outlined.Link,
      stringResource(R.string.group_link),
      tint = MaterialTheme.colors.primary
    )
    Spacer(Modifier.size(8.dp))
    Text(stringResource(R.string.group_link), color = MaterialTheme.colors.primary)
  }
}

@Composable
fun EditGroupProfileButton() {
  Row(
    Modifier
      .fillMaxSize(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      Icons.Outlined.Edit,
      stringResource(R.string.button_edit_group_profile),
      tint = MaterialTheme.colors.primary
    )
    Spacer(Modifier.size(8.dp))
    Text(stringResource(R.string.button_edit_group_profile), color = MaterialTheme.colors.primary)
  }
}

@Composable
fun LeaveGroupButton() {
  Row(
    Modifier.fillMaxSize(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      Icons.Outlined.Logout,
      stringResource(R.string.button_leave_group),
      tint = Color.Red
    )
    Spacer(Modifier.size(8.dp))
    Text(stringResource(R.string.button_leave_group), color = Color.Red)
  }
}

@Composable
fun DeleteGroupButton() {
  Row(
    Modifier.fillMaxSize(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      Icons.Outlined.Delete,
      stringResource(R.string.button_delete_group),
      tint = Color.Red
    )
    Spacer(Modifier.size(8.dp))
    Text(stringResource(R.string.button_delete_group), color = Color.Red)
  }
}
/*@Preview
@Composable
fun PreviewGroupChatInfoLayout() {
  SimpleXTheme {
    GroupChatInfoLayout(
      chat = Chat(
        chatInfo = ChatInfo.Direct.sampleData,
        chatItems = arrayListOf(),
        serverInfo = Chat.ServerInfo(Chat.NetworkStatus.Error("agent BROKER TIMEOUT"))
      ),
      groupInfo = GroupInfo.sampleData,
      members = listOf(GroupMember.sampleData, GroupMember.sampleData, GroupMember.sampleData),
      developerTools = false,
      addMembers = {}, showMemberInfo = {}, editGroupProfile = {}, deleteGroup = {}, clearChat = {}, leaveGroup = {}, manageGroupLink = {},
    )
  }
}*/
