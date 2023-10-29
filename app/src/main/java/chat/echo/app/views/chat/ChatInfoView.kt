package chat.echo.app.views.chat

import InfoRow
import InfoRowEllipsis
import SectionSpacer
import SectionView
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.SimplexApp
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.bottomsheet.DirectChatBottomSheetType
import chat.echo.app.views.bottomsheet.DirectChatSheetLayout
import chat.echo.app.views.chatlist.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.usersettings.ProfileHeader
import chat.echo.app.views.usersettings.SettingsActionItem
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Composable
fun ChatInfoView(
  chatModel: ChatModel,
  contact: Contact,
  connStats: ConnectionStats?,
  customUserProfile: Profile?,
  localAlias: String,
  close: () -> Unit,
  onChatUpdated: (Chat) -> Unit,
) {
  BackHandler(onBack = close)
  val chat = chatModel.chats.firstOrNull { it.id == chatModel.chatId.value }
  val developerTools = chatModel.controller.appPrefs.developerTools.get()
  if (chat != null) {
    chatModel.localContactViewModel?.getLocalContact(chat.chatInfo.id)
  }
  val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val coroutineScope = rememberCoroutineScope()
  if (chat != null) {
    ChatInfoLayout(
      chat,
      chatModel,
      contact,
      connStats,
      customUserProfile,
      localAlias,
      developerTools,
      onLocalAliasChanged = {
        setContactAlias(chat.chatInfo.apiId, it, chatModel, onChatUpdated)
      },
      bottomSheetModalState = bottomSheetModalState,
      toggleChatNotification = { toggleChatNotification(chatModel, chat, chat.chatInfo) },
      sharePublicKey = { coroutineScope.launch { sharePublicKey(chatModel, chat, bottomSheetModalState, close) } },
      deleteContact = { coroutineScope.launch { deleteContactDialog(chat.chatInfo, chatModel, bottomSheetModalState, close) } },
      clearChat = { coroutineScope.launch { clearChatDialog(chat.chatInfo, chatModel, bottomSheetModalState, close) } },
      close = close,
    )
  }
}

fun toggleChatNotification(chatModel: ChatModel, chat: Chat, chatInfo: ChatInfo) {
  changeNtfsStatePerChat(!chatInfo.ntfsEnabled, mutableStateOf(chatInfo.ntfsEnabled), chat, chatModel)
}

suspend fun sharePublicKey(chatModel: ChatModel, chat: Chat, bottomSheetModalState: ModalBottomSheetState? = null, close: (() -> Unit)? = null) {
  bottomSheetModalState?.hide()
  val content = MsgContent.MCPublicKey(chatModel.controller.appPrefs.publicKey.get()!!)
  val command = chatModel.controller.apiSendMessage(chat.chatInfo.chatType, chat.chatInfo.apiId, null, null, content)
  if (command != null) {
    chatModel.addChatItem(chat.chatInfo, command.chatItem)
    close?.invoke()
  }
}

suspend fun deleteContactDialog(
  chatInfo: ChatInfo, chatModel: ChatModel,
  bottomSheetModalState: ModalBottomSheetState? = null, close: (() -> Unit)? = null
) {
  bottomSheetModalState?.hide()
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.delete_contact_question),
    text = generalGetString(R.string.delete_contact_all_messages_deleted_cannot_undo_warning),
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
fun ChatInfoLayout(
  chat: Chat,
  chatModel: ChatModel,
  contact: Contact,
  connStats: ConnectionStats?,
  customUserProfile: Profile?,
  localAlias: String,
  developerTools: Boolean,
  bottomSheetModalState: ModalBottomSheetState,
  onLocalAliasChanged: (String) -> Unit,
  sharePublicKey: () -> Unit,
  toggleChatNotification: () -> Unit,
  deleteContact: () -> Unit,
  clearChat: () -> Unit,
  close: () -> Unit
) {
  val scaffoldState = rememberScaffoldState()
  val coroutineScope = rememberCoroutineScope()
  var currentBottomSheet: DirectChatBottomSheetType? by remember {
    mutableStateOf(null)
  }
  val openSheet: (DirectChatBottomSheetType) -> Unit = {
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
  val openDirectChatSettings: () -> Unit = {
    openSheet(DirectChatBottomSheetType.ShowChatSettings)
  }
  val openBurnerTimerSettings: () -> Unit = {
    openSheet(DirectChatBottomSheetType.ShowBurnerTimerSettings)
  }

  if (!bottomSheetModalState.isVisible) {
    currentBottomSheet = null
  }


  ModalBottomSheetLayout(
    scrimColor = Color.Black.copy(alpha = 0.12F),
    modifier = Modifier.navigationBarsWithImePadding(),
    sheetContent = {
      if (currentBottomSheet != null) {
        DirectChatSheetLayout(
          chatModel = chatModel,
          chat = chat,
          sharePublicKey = sharePublicKey,
          toggleChatNotification = toggleChatNotification,
          deleteContact = deleteContact,
          clearChat = clearChat,
          bottomSheetType = currentBottomSheet!!,
          closeSheet = closeSheet,
          close = close
        )
      } else {
        Box(Modifier.size(1.dp))
      }
    },
    sheetState = bottomSheetModalState,
    sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    content = {
      Scaffold(
        topBar = {
          Column() {
            ChatInfoToolbar(close = close, centered = true, openDirectChatSettings = openDirectChatSettings)
            Divider(color = PreviewTextColor)
          }
        },
        scaffoldState = scaffoldState,
        drawerGesturesEnabled = false,
        backgroundColor = White
      ) {
        Box(modifier = Modifier.padding(it)) {
          Column(
            Modifier
              .fillMaxWidth()
              .fillMaxHeight()
              .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
          ) {
            ChatInfoProfileImage(cInfo = chat.chatInfo)
            ChatNameAction(
              cInfo = chat.chatInfo,
              chat = {
                directChatAction(chat.chatInfo, chatModel)
                close()
              },
              call = {
                directCallAction(chatModel, chat)
                close()
              },
              setBurnerTimer = {
                /*withApi {
                  showBurnerTimerComingSoonDialog()
                }*/
                openBurnerTimerSettings()
              }
            )
            ChatBioExtraView(initialValue = localAlias, updateValue = onLocalAliasChanged)
            if (customUserProfile != null) {
              SectionSpacer()
              SectionView(generalGetString(R.string.incognito).uppercase()) {
                InfoRow(generalGetString(R.string.incognito_random_profile), customUserProfile.chatViewName)
              }
            }

            Spacer(modifier = Modifier.padding(top = 20.dp))

            if (connStats != null) {
              ServerInformationHeader(title = generalGetString(R.string.server_information))
              Spacer(modifier = Modifier.padding(top = 5.dp, start = 10.dp, end = 10.dp, bottom = 5.dp))
              connStats.sndServers?.let { it1 -> connStats.rcvServers?.let { it2 -> ContactConnectionInfoView(senderServer = it1, receiverServer = it2) } }
            }
            /*  SectionSpacer()

               if (connStats != null) {
                 SectionView(title = stringResource(R.string.conn_stats_section_title_servers)) {
                   SectionItemView({
                     AlertManager.shared.showAlertMsg(
                       generalGetString(R.string.network_status),
                       chat.serverInfo.networkStatus.statusExplanation
                     )
                   }) {
                     NetworkStatusRow(chat.serverInfo.networkStatus)
                   }
                   SectionDivider()
                   SectionItemView() {
                     BurnerTimerRow(chat, chatModel)
                   }
                   val rcvServers = connStats.rcvServers
                   if (rcvServers != null && rcvServers.isNotEmpty()) {
                     SectionDivider()
                     SimplexServers(stringResource(R.string.receiving_via), rcvServers)
                   }
                   val sndServers = connStats.sndServers
                   if (sndServers != null && sndServers.isNotEmpty()) {
                     SectionDivider()
                     SimplexServers(stringResource(R.string.sending_via), sndServers)
                   }
                 }
               }*/
          }
        }
      }
    })
}
/*fun contactFeaturesAllowedToPrefs(contactFeaturesAllowed: ContactFeaturesAllowed): ChatPreferences =
  ChatPreferences(
    timedMessages = TimedMessagesPreference(if (contactFeaturesAllowed.timedMessagesAllowed) FeatureAllowed.YES else FeatureAllowed.NO, contactFeaturesAllowed.timedMessagesTTL),
    fullDelete = contactFeatureAllowedToPref(contactFeaturesAllowed.fullDelete),
    voice = contactFeatureAllowedToPref(contactFeaturesAllowed.voice)
  )*/

@Composable
fun ChatInfoToolbar(close: () -> Unit, centered: Boolean, openDirectChatSettings: () -> Unit) {
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
          text = generalGetString(R.string.profile),
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
        IconButton({
          coroutineScope.launch {
           openDirectChatSettings()
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
fun ChatInfoProfileImage(cInfo: ChatInfo) {
  ChatInfoImage(cInfo, size = 200.dp, iconColor = if (isInDarkTheme()) Color.Black else SettingsSecondaryLight)
}

@Composable
fun ChatNameAction(cInfo: ChatInfo, chat: () -> Unit, call: () -> Unit, setBurnerTimer: () -> Unit) {
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
          if (cInfo is ChatInfo.Direct) {
            Text(
              text = if (cInfo.contact.fullName != "" && cInfo.contact.fullName != cInfo.contact.localDisplayName) cInfo.fullName else cInfo.contact.localDisplayName,
              fontSize = 18.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              fontWeight = FontWeight.Medium,
              color = Color.White
            )
          }
        }
        Row(
          Modifier.fillMaxWidth().weight(1f).align(Alignment.CenterVertically),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(chat) {
            Icon(
              Icons.Outlined.Comment, stringResource(R.string.chat), tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          IconButton(call) {
            Icon(
              Icons.Outlined.Call, stringResource(R.string.voice_call), tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          IconButton(setBurnerTimer) {
            Icon(
              Icons.Outlined.Timer, stringResource(R.string.burner_timer), tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ChatInfoHeader(cInfo: ChatInfo, contact: Contact) {
  Column(
    Modifier.padding(horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    ChatInfoImage(cInfo, size = 192.dp, iconColor = if (isInDarkTheme()) GroupDark else SettingsSecondaryLight)
    Text(
      contact.profile.displayName, style = MaterialTheme.typography.h1.copy(fontWeight = FontWeight.Normal),
      color = MaterialTheme.colors.onBackground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    if (cInfo.fullName != "" && cInfo.fullName != cInfo.localDisplayName && cInfo.fullName != contact.profile.displayName) {
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
fun LocalAliasEditor(
  initialValue: String,
  center: Boolean = true,
  leadingIcon: Boolean = false,
  focus: Boolean = false,
  updateValue: (String) -> Unit
) {
  var value by rememberSaveable { mutableStateOf(initialValue) }
  val modifier = if (center)
    Modifier.padding(horizontal = if (!leadingIcon) DEFAULT_PADDING else 0.dp).widthIn(min = 100.dp)
  else
    Modifier.padding(horizontal = if (!leadingIcon) DEFAULT_PADDING else 0.dp).fillMaxWidth()
  Row(Modifier.fillMaxWidth(), horizontalArrangement = if (center) Arrangement.Center else Arrangement.Start) {
    DefaultBasicTextField(
      modifier,
      value,
      {
        Text(
          generalGetString(R.string.text_field_set_contact_placeholder),
          textAlign = if (center) TextAlign.Center else TextAlign.Start,
          color = HighOrLowlight
        )
      },
      leadingIcon = if (leadingIcon) {
        { Icon(Icons.Default.Edit, null, Modifier.padding(start = 7.dp)) }
      } else null,
      color = HighOrLowlight,
      focus = focus,
      textStyle = TextStyle.Default.copy(textAlign = if (value.isEmpty() || !center) TextAlign.Start else TextAlign.Center),
      keyboardActions = KeyboardActions(onDone = { updateValue(value) })
    ) {
      value = it
    }
  }
  LaunchedEffect(Unit) {
    snapshotFlow { value }
      .onEach { delay(500) } // wait a little after every new character, don't emit until user stops typing
      .conflate() // get the latest value
      .filter { it == value } // don't process old ones
      .collect {
        updateValue(value)
      }
  }
  DisposableEffect(Unit) {
    onDispose { updateValue(value) } // just in case snapshotFlow will be canceled when user presses Back too fast
  }
}

@Composable
fun ContactInformationHeader(headerTitle: String, color: Color = TextHeader, enabled: MutableState<Boolean>, copy: () -> Unit) {
  Row(Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 5.dp)) {
    Text(
      text = headerTitle,
      fontSize = 14.sp,
      modifier = Modifier.align(Alignment.CenterVertically),
      fontWeight = FontWeight.Normal,
      color = color
    )
    Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
    Text(
      text = if (enabled.value) generalGetString(R.string.copy_verb) else generalGetString(R.string.copied_verb),
      fontSize = 14.sp,
      modifier = Modifier
        .clickable {
          if (enabled.value) {
            copy()
          }
        }
        .align(Alignment.CenterVertically),
      fontWeight = FontWeight.Medium,
      color = TextHeader
    )
  }
}

@Composable
fun NetworkStatusRow(networkStatus: NetworkStatus) {
  Row(
    Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(stringResource(R.string.network_status))
      Icon(
        Icons.Outlined.Info,
        stringResource(R.string.network_status),
        tint = MaterialTheme.colors.primary
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        networkStatus.statusString,
        color = HighOrLowlight
      )
      ServerImage(networkStatus)
    }
  }
}
/*@Composable
fun BurnerTimerRow(chat: Chat, model: ChatModel) {
  var expanded by remember {
    mutableStateOf(false)
  }
  val context = LocalContext.current
  val burnerTimerList = arrayOf(30L, 300L, 3600L, 28800L, 86400L, 432000L)
  val searchResults = model.localContactViewModel?.localContact?.value
  val localContact = searchResults?.get(0)
  var burnerTime by rememberSaveable {
    mutableStateOf(localContact?.burnerTime)
  }
  model.localContactViewModel?.getLocalContact(chat.chatInfo.id)
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
            "All messages that have been sent within ${burnerTime} will be deleted automatically."
          )
        }),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text("Burner Timer")
      Icon(
        Icons.Outlined.Timer,
        "Burner Timer",
        tint = MaterialTheme.colors.primary,
      )
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
          getBurnerTimerText(burnerTime!!),
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
                withApi {
                  val content = MsgContent.MCBurnerTimer("cmd burner ${itemValue}", itemValue)
                  if (chat.serverInfo.networkStatus is Chat.NetworkStatus.Connected) {
                    if (searchResults != null && searchResults.isNotEmpty()) {
                      val command = model.controller.apiSendMessage(chat.chatInfo.chatType, chat.chatInfo.apiId, null, null, content)
                      val localContact = searchResults[0]
                      localContact.burnerTime = itemValue
                      if (command != null) {
                        burnerTime = itemValue
                        model.localContactViewModel?.updateLocalContact(localContact)
                        model.addChatItem(chat.chatInfo, command.chatItem)
                      }
                    }
                  }
                }
                expanded = false
              },
            ) {
              Text(text = getBurnerTimerText(itemValue))
            }
          }
        }
      }
    }
  }
}*/

fun getBurnerTimerText(burnerTimer: Long): String {
  if (burnerTimer == 30L) return "30 seconds"
  if (burnerTimer == 300L) return "5 minutes"
  if (burnerTimer == 3600L) return "1 hour"
  if (burnerTimer == 28800L) return "8 hours"
  if (burnerTimer == 86400L) return "1 day"
  if (burnerTimer == 432000L) return "5 days"
  return "None"
}

@Composable
fun ServerImage(networkStatus: NetworkStatus) {
  Box(Modifier.size(18.dp)) {
    when (networkStatus) {
      is NetworkStatus.Connected ->
        Icon(Icons.Filled.Circle, stringResource(R.string.icon_descr_server_status_connected), tint = MaterialTheme.colors.primaryVariant)
      is NetworkStatus.Disconnected ->
        Icon(Icons.Filled.Pending, stringResource(R.string.icon_descr_server_status_disconnected), tint = HighOrLowlight)
      is NetworkStatus.Error ->
        Icon(Icons.Filled.Error, stringResource(R.string.icon_descr_server_status_error), tint = HighOrLowlight)
      else -> Icon(Icons.Outlined.Circle, stringResource(R.string.icon_descr_server_status_pending), tint = HighOrLowlight)
    }
  }
}

@Composable
fun SimplexServers(text: String, servers: List<String>) {
  val info = servers.joinToString(separator = ", ") { it.substringAfter("@") }
  val clipboardManager: ClipboardManager = LocalClipboardManager.current
  InfoRowEllipsis(text, info) {
    clipboardManager.setText(AnnotatedString(servers.joinToString(separator = ",")))
    Toast.makeText(SimplexApp.context, generalGetString(R.string.copied), Toast.LENGTH_SHORT).show()
  }
}

@Composable
fun ClearChatButton(onClick: () -> Unit) {
  SettingsActionItem(
    Icons.Outlined.Restore,
    stringResource(R.string.clear_chat_button),
    click = onClick,
    textColor = WarningOrange,
    iconColor = WarningOrange,
  )
}

@Composable
fun SharePublicKeyButton(onClick: () -> Unit) {
  SettingsActionItem(
    Icons.Outlined.Key,
    stringResource(R.string.share_public_key),
    click = onClick,
    textColor = Blue,
    iconColor = Blue,
  )
}

@Composable
fun DeleteContactButton(onClick: () -> Unit) {
  SettingsActionItem(
    Icons.Outlined.Delete,
    stringResource(R.string.button_delete_contact),
    click = onClick,
    textColor = Color.Red,
    iconColor = Color.Red,
  )
}
/*@Preview
@Composable
fun PreviewChatInfoLayout() {
  SimpleXTheme {
    ChatInfoLayout(
      chat = Chat(
        chatInfo = ChatInfo.Direct.sampleData,
        chatItems = arrayListOf(),
        serverInfo = Chat.ServerInfo(Chat.NetworkStatus.Error("agent BROKER TIMEOUT"))
      ),
      Contact.sampleData,
      localAlias = "",
      developerTools = false,
      connStats = null,
      onLocalAliasChanged = {},
      customUserProfile = null,
      deleteContact = {}, clearChat = {}
    )
  }
}*/
