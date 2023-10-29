package chat.echo.app.views.chat.item

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chat.ComposeState
import chat.echo.app.views.helpers.*

fun isAnnouncement(cItem: ChatItem): Boolean {
  return when (val c = cItem.content) {
    is CIContent.RcvGroupEventContent -> true
    is CIContent.SndGroupEventContent -> true
    is CIContent.SndChatFeature -> true
    is CIContent.RcvChatFeature -> true
    else -> false
  }
}

@Composable
fun ChatItemView(
  user: User,
  chatModel: ChatModel,
  cInfo: ChatInfo,
  cItem: ChatItem,
  composeState: MutableState<ComposeState>,
  cxt: Context,
  uriHandler: UriHandler? = null,
  imageProvider: (() -> ImageGalleryProvider)? = null,
  showMember: Boolean = false,
  chatModelIncognito: Boolean,
  useLinkPreviews: Boolean,
  linkMode: SimplexLinkMode,
  deleteMessage: (Long, CIDeleteMode) -> Unit,
  receiveFile: (Long) -> Unit,
  joinGroup: (Long) -> Unit,
  acceptCall: (Contact) -> Unit,
  scrollToItem: (Long) -> Unit,
) {
  val context = LocalContext.current
 val sent = cItem.chatDir is CIDirection.DirectSnd || cItem.chatDir is CIDirection.GroupSnd
  val alignment = if (isAnnouncement(cItem)) Alignment.Center else if (sent) Alignment.CenterEnd else Alignment.CenterStart
  val showMenu = remember { mutableStateOf(false) }
  val saveFileLauncher = rememberSaveFileLauncher(cxt = context, ciFile = cItem.file)
  val decrypted = remember {
    mutableStateOf(false)
  }
  var profile by remember { mutableStateOf(user.profile.toProfile()) }
  val mc = cItem.content.msgContent
  Box(
    modifier = Modifier
      .padding(top = 4.dp, bottom = 4.dp)
      .fillMaxWidth(),
    contentAlignment = alignment,
  ) {
    Column() {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        if (sent) {
          Box(
            Modifier.weight(1f),
            contentAlignment = alignment
          ) {
            Column() {
              Box(
                Modifier.fillMaxWidth(),
                contentAlignment = alignment
              ) {
                @Composable fun ContentItem() {
                  if (cItem.file == null && cItem.quotedItem == null && isShortEmoji(cItem.content.text)) {
                    EmojiItemView(cItem, cInfo.timedMessagesTTL)
                  } else if (mc is MsgContent.MCVoice && cItem.content.text.isEmpty()) {
                    CIVoiceView(mc.duration, cItem.file, cItem.meta.itemEdited, cItem.chatDir.sent, hasText = false, cItem, cInfo.timedMessagesTTL, longClick = {
                      //  onLinkLongClick("")
                    })
                    // MsgContentItemDropdownMenu()
                  } else if (cItem.file != null) {
                    val onLinkLongClick = { _: String -> showMenu.value = true }
                    FramedItemView(chatModel, cInfo, cItem, uriHandler, imageProvider, showMember = showMember, linkMode = linkMode, showMenu, receiveFile, onLinkLongClick, scrollToItem, deleteMessage, decrypted)
                  }  else {
                    val onLinkLongClick = { _: String -> showMenu.value = true }
                    FramedItemView(chatModel, cInfo, cItem, uriHandler, imageProvider, showMember = showMember, linkMode = linkMode, showMenu, receiveFile, onLinkLongClick, scrollToItem, deleteMessage, decrypted)
                  }
                  DropdownMenu(
                    expanded = showMenu.value,
                    onDismissRequest = { showMenu.value = false },
                    Modifier.width(220.dp)
                  ) {
                    /*ItemAction(stringResource(R.string.reply_verb), Icons.Outlined.Reply, onClick = {
                      if (composeState.value.editing) {
                        composeState.value = ComposeState(contextItem = ComposeContextItem.QuotedItem(cItem), useLinkPreviews = useLinkPreviews)
                      } else {
                        composeState.value = composeState.value.copy(contextItem = ComposeContextItem.QuotedItem(cItem))
                      }
                      showMenu.value = false
                    })*/
                    ItemAction(stringResource(R.string.copy_verb), Icons.Outlined.ContentCopy, onClick = {
                      copyText(cxt, cItem.content.text)
                      showMenu.value = false
                    })
                    if (cItem.content.msgContent is MsgContent.MCImage || cItem.content.msgContent is MsgContent.MCFile) {
                      val filePath = getLoadedFilePath(context, cItem.file)

                      if (filePath != null) {
                        ItemAction(stringResource(R.string.save_verb), Icons.Outlined.SaveAlt, onClick = {
                          when (cItem.content.msgContent) {
                            is MsgContent.MCImage -> saveImage(context, cItem.file)
                            is MsgContent.MCFile -> saveFileLauncher.launch(cItem.file?.fileName)
                            else -> {}
                          }
                          showMenu.value = false
                        })
                      }
                    }
                    if (cItem.meta.editable) {
                      ItemAction(stringResource(R.string.edit_verb), Icons.Filled.Edit, onClick = {
                        composeState.value = ComposeState(editingItem = cItem, useLinkPreviews = useLinkPreviews)
                        showMenu.value = false
                      })
                    }
                    ItemAction(
                      stringResource(R.string.delete_verb),
                      Icons.Outlined.Delete,
                      onClick = {
                        showMenu.value = false
                        deleteMessageAlertDialog(cItem, deleteMessage = deleteMessage)
                      },
                      color = Color.Red
                    )
                  }
                }

                @Composable fun DeletedItem() {
                  DeletedItemView(cItem, cInfo.timedMessagesTTL, showMember = showMember)
                  DropdownMenu(
                    expanded = showMenu.value,
                    onDismissRequest = { showMenu.value = false },
                    Modifier.width(220.dp)
                  ) {
                    ItemAction(
                      stringResource(R.string.delete_verb),
                      Icons.Outlined.Delete,
                      onClick = {
                        showMenu.value = false
                        deleteMessageAlertDialog(cItem, deleteMessage = deleteMessage)
                      },
                      color = Color.Red
                    )
                  }
                }

                @Composable fun CallItem(status: CICallStatus, duration: Int) {
                  CICallItemView(cInfo, cItem, status, duration, acceptCall)
                }

                when (val c = cItem.content) {
                  is CIContent.SndMsgContent -> ContentItem()
                  is CIContent.RcvMsgContent -> ContentItem()
                  is CIContent.SndDeleted -> DeletedItem()
                  is CIContent.RcvDeleted -> DeletedItem()
                  is CIContent.SndCall -> CallItem(c.status, c.duration)
                  is CIContent.RcvCall -> CallItem(c.status, c.duration)
                  is CIContent.RcvIntegrityError -> IntegrityErrorItemView(cItem, cInfo.timedMessagesTTL, showMember = showMember)
                  is CIContent.RcvGroupInvitation -> CIGroupInvitationView(cItem, c.groupInvitation, c.memberRole, joinGroup = joinGroup, chatIncognito = cInfo.incognito)
                  is CIContent.SndGroupInvitation -> CIGroupInvitationView(cItem, c.groupInvitation, c.memberRole, joinGroup = joinGroup, chatIncognito = cInfo.incognito)
                  is CIContent.RcvGroupEventContent -> AnnouncementView(cInfo, cItem.text, groupMember = if (cItem.chatDir is CIDirection.GroupRcv) cItem.chatDir.groupMember else null, false)
                  is CIContent.SndGroupEventContent -> AnnouncementView(cInfo, cItem.text, isSent = true)
                  is CIContent.RcvChatFeature -> BurnerTimerChatItemView(cInfo = cInfo, cItem = cItem, isSent = false)
                  is CIContent.SndChatFeature -> BurnerTimerChatItemView(cInfo = cInfo, cItem = cItem, isSent = true)
                  else -> {}
                }
              }
              val messageStatus = when (val status = cItem.meta.itemStatus) {
                is CIStatus.SndNew -> if (cItem.chatDir is CIDirection.GroupSnd) "Sent" else "Sending"
                is CIStatus.SndError -> "Failed"
                is CIStatus.SndErrorAuth -> "Error"
                is CIStatus.SndSent -> "Sent"
                else -> ""
              }

              if (!isAnnouncement(cItem)) {
                Box(
                  modifier = Modifier.fillMaxWidth(),
                  contentAlignment = Alignment.CenterEnd
                ) {
                  Row(
                  ) {
                    Text(
                      text = cItem.timestampText + " • " + messageStatus + if (cItem.meta.isLive && cItem.meta.itemLive == true) " • Live" else "",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Normal,
                      color = TimeStampColor,
                      modifier = Modifier.padding(5.dp)
                    )
                  }
                }
              }
            }
          }
          /* if (!isAnnouncement(cItem)) {
             Box() {
               Column() {
                   if (showMember) {
                     ChatViewImage(size = 42.dp, image = profile.image, color = MaterialTheme.colors.secondary)
                   } else {
                     Spacer(modifier = Modifier.size(42.dp))
                     //Spacer(modifier = Modifier.padding(horizontal = 3.5.dp))
                   }
                 Spacer(modifier = Modifier.padding(bottom = 25.dp))
               }
             }
           }*/
        } else {
          if (!isAnnouncement(cItem)) {
            Box() {
              Column() {
                if (cInfo is ChatInfo.Group && cItem.chatDir is CIDirection.GroupRcv) {
                  val member = cItem.chatDir.groupMember
                  if (showMember) {
                    if (cInfo.groupInfo.displayName.startsWith("*")) {
                      ChatViewImage(size = 42.dp, image = null, color = MaterialTheme.colors.secondary)
                    } else {
                      ChatViewImage(size = 42.dp, image = member.image, color = MaterialTheme.colors.secondary)
                    }
                  } else {
                    Spacer(modifier = Modifier.size(42.dp))
                    //Spacer(modifier = Modifier.padding(horizontal = 3.5.dp))
                  }
                }
                /*else {
                  ChatViewImage(size = 42.dp, image = cInfo.image, color = MaterialTheme.colors.secondary)
                }*/
                Spacer(modifier = Modifier.padding(bottom = 25.dp))
              }
            }
          }
          Box(
            Modifier
              .weight(1f),
            contentAlignment = alignment
          ) {
            Column() {
              @Composable fun ContentItem() {
                if (cItem.file == null && cItem.quotedItem == null && isShortEmoji(cItem.content.text)) {
                  EmojiItemView(cItem, cInfo.timedMessagesTTL)
                } else if (mc is MsgContent.MCVoice && cItem.content.text.isEmpty()) {
                  CIVoiceView(mc.duration, cItem.file, cItem.meta.itemEdited, cItem.chatDir.sent, hasText = false, cItem, cInfo.timedMessagesTTL, longClick = {
                    //  onLinkLongClick("")
                  })
                }
                else if (cItem.file != null) {
                  val onLinkLongClick = { _: String -> showMenu.value = true }
                  FramedItemView(chatModel, cInfo, cItem, uriHandler, imageProvider, showMember = showMember, linkMode = linkMode, showMenu, receiveFile, onLinkLongClick, scrollToItem, deleteMessage, decrypted)
                } else {
                  val onLinkLongClick = { _: String -> showMenu.value = true }
                  FramedItemView(chatModel, cInfo, cItem, uriHandler, imageProvider, showMember = showMember, linkMode = linkMode, showMenu, receiveFile, onLinkLongClick, scrollToItem, deleteMessage, decrypted)
                }
                DropdownMenu(
                  expanded = showMenu.value,
                  onDismissRequest = { showMenu.value = false },
                  Modifier.width(220.dp)
                ) {
                  /*ItemAction(stringResource(R.string.reply_verb), Icons.Outlined.Reply, onClick = {
                    if (composeState.value.editing) {
                      composeState.value = ComposeState(contextItem = ComposeContextItem.QuotedItem(cItem), useLinkPreviews = useLinkPreviews)
                    } else {
                      composeState.value = composeState.value.copy(contextItem = ComposeContextItem.QuotedItem(cItem))
                    }
                    showMenu.value = false
                  })*/
                  ItemAction(stringResource(R.string.copy_verb), Icons.Outlined.ContentCopy, onClick = {
                    copyText(cxt, cItem.content.text)
                    showMenu.value = false
                  })
                  if (cItem.content.msgContent is MsgContent.MCImage || cItem.content.msgContent is MsgContent.MCFile) {
                    val filePath = getLoadedFilePath(context, cItem.file)

                    if (filePath != null) {
                      ItemAction(stringResource(R.string.save_verb), Icons.Outlined.SaveAlt, onClick = {
                        when (cItem.content.msgContent) {
                          is MsgContent.MCImage -> saveImage(context, cItem.file)
                          is MsgContent.MCFile -> saveFileLauncher.launch(cItem.file?.fileName)
                          else -> {}
                        }
                        showMenu.value = false
                      })
                    }
                  }
                  if (cItem.meta.editable) {
                    ItemAction(stringResource(R.string.edit_verb), Icons.Filled.Edit, onClick = {
                      composeState.value = ComposeState(editingItem = cItem, useLinkPreviews = useLinkPreviews)
                      showMenu.value = false
                    })
                  }
                  ItemAction(
                    stringResource(R.string.delete_verb),
                    Icons.Outlined.Delete,
                    onClick = {
                      showMenu.value = false
                      deleteMessageAlertDialog(cItem, deleteMessage = deleteMessage)
                    },
                    color = Color.Red
                  )
                }
              }

              @Composable fun DeletedItem() {
                DeletedItemView(cItem, cInfo.timedMessagesTTL, showMember = showMember)
                DropdownMenu(
                  expanded = showMenu.value,
                  onDismissRequest = { showMenu.value = false },
                  Modifier.width(220.dp)
                ) {
                  ItemAction(
                    stringResource(R.string.delete_verb),
                    Icons.Outlined.Delete,
                    onClick = {
                      showMenu.value = false
                      deleteMessageAlertDialog(cItem, deleteMessage = deleteMessage)
                    },
                    color = Color.Red
                  )
                }
              }

              @Composable fun CallItem(status: CICallStatus, duration: Int) {
                CICallItemView(cInfo, cItem, status, duration, acceptCall)
              }

              when (val c = cItem.content) {
                is CIContent.SndMsgContent -> ContentItem()
                is CIContent.RcvMsgContent -> ContentItem()
                is CIContent.SndDeleted -> DeletedItem()
                is CIContent.RcvDeleted -> DeletedItem()
                is CIContent.SndCall -> CallItem(c.status, c.duration)
                is CIContent.RcvCall -> CallItem(c.status, c.duration)
                is CIContent.RcvIntegrityError -> IntegrityErrorItemView(cItem, cInfo.timedMessagesTTL, showMember = showMember)
                is CIContent.RcvGroupInvitation -> CIGroupInvitationView(cItem, c.groupInvitation, c.memberRole, joinGroup = joinGroup, chatIncognito = cInfo.incognito)
                is CIContent.SndGroupInvitation -> CIGroupInvitationView(cItem, c.groupInvitation, c.memberRole, joinGroup = joinGroup, chatIncognito = cInfo.incognito)
                is CIContent.RcvGroupEventContent -> AnnouncementView(cInfo, cItem.text, groupMember = if (cItem.chatDir is CIDirection.GroupRcv) cItem.chatDir.groupMember else null, false)
                is CIContent.SndGroupEventContent -> AnnouncementView(cInfo, cItem.text, isSent = true)
                is CIContent.RcvChatFeature -> BurnerTimerChatItemView(cInfo = cInfo, cItem = cItem, isSent = false)
                is CIContent.SndChatFeature -> BurnerTimerChatItemView(cInfo = cInfo, cItem = cItem, isSent = true)
                else -> {}
              }
              if (!isAnnouncement(cItem)) {
                if(cInfo is ChatInfo.Group){
                  var preferences by rememberSaveable(cInfo.groupInfo, stateSaver = serializableSaver()) { mutableStateOf(cInfo.groupInfo.fullGroupPreferences) }
                  var currentPreferences by rememberSaveable(cInfo.groupInfo, stateSaver = serializableSaver()) { mutableStateOf(preferences) }
                  Text(
                    text = getMemberName(cItem, cInfo, cInfo.groupInfo.displayName.startsWith("*")) + cItem.timestampText + if (cItem.meta.isLive && cItem.meta.itemLive == true) " • Live" else "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TimeStampColor,
                    modifier = Modifier.padding(5.dp)
                  )
                } else {
                  Text(
                    text = getMemberName(cItem, cInfo, false) + cItem.timestampText + if (cItem.meta.isLive && cItem.meta.itemLive == true) " • Live" else "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TimeStampColor,
                    modifier = Modifier.padding(5.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

fun getMemberName(cItem: ChatItem, cInfo: ChatInfo, isZeroKnowledge: Boolean): String {
  if (cInfo is ChatInfo.Group && cItem.chatDir is CIDirection.GroupRcv) {
    return (if (isZeroKnowledge) cItem.chatDir.groupMember.memberId.substring(0, 6)
    else if (cItem.chatDir.groupMember.fullName != "") cItem.chatDir.groupMember.fullName else cItem.chatDir.groupMember.displayName) + " • "
  }
  return ""
}

@Composable
fun BurnerTimerChatItemView(cInfo: ChatInfo, cItem: ChatItem, isSent: Boolean) {
  val alignment = Alignment.Center
  val text = remember {
    mutableStateOf("")
  }

  if (cInfo is ChatInfo.Direct) {
    if (cItem.chatDir.sent) {
      text.value = "${generalGetString(R.string.you)} ${cItem.content.text}"
    } else {
      text.value = "${cInfo.fullName.ifEmpty { cInfo.localDisplayName }} ${cItem.content.text}"
      // text.value = "${chatInfo.displayName} turned on burning messages. All new messages will be burnt from this chat after ${TimedMessagesPreference.ttlText(burnerTime)}."
    }
  }

  Box(
    modifier = Modifier
      .padding(top = 2.dp, bottom = 2.dp, start = 20.dp, end = 20.dp)
      .fillMaxWidth(),
    contentAlignment = alignment,
  ) {
    Column(
      Modifier
        .clip(RoundedCornerShape(18.dp))
        .clickable {
        }
    ) {
      if (isSent) {
        BurnerTimerSndChatItem(message = text.value)
      } else {
        BurnerTimerRcvChatItem(message = text.value)
      }
    }
  }
}

@Composable
fun BurnerTimerRcvChatItem(message: String) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = AnnouncementBackground
  ) {
    val coroutineScope = rememberCoroutineScope()

    Box(contentAlignment = Alignment.Center) {
      Column(Modifier.width(IntrinsicSize.Max)) {
        Box(
          Modifier
            .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
          Text(
            message,
            Modifier.padding(3.dp),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = AnnouncementText
          )
        }
      }
    }
  }
}

@Composable
fun BurnerTimerSndChatItem(message: String) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = AnnouncementBackground
  ) {
    Box(contentAlignment = Alignment.Center) {
      Column(Modifier.width(IntrinsicSize.Max)) {
        Box(
          Modifier
            .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
          Text(
            message,
            Modifier.padding(3.dp),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = AnnouncementText
          )
        }
      }
    }
  }
}

@Composable
fun AnnouncementView(cInfo: ChatInfo, message: String, groupMember: GroupMember? = null, isSent: Boolean) {
  if (message.isNotEmpty()) {
    val alignment = Alignment.Center

    Box(
      modifier = Modifier
        .padding(top = 2.dp, bottom = 2.dp)
        .fillMaxWidth(),
      contentAlignment = alignment,
    ) {
      Column(
        Modifier
          .clip(RoundedCornerShape(18.dp))
          .clickable {
          }
      ) {
        if (isSent) {
          AnnouncementSndItemView(message = message)
        } else {
          if (cInfo is ChatInfo.Group && groupMember != null) {
            var preferences by rememberSaveable(cInfo.groupInfo, stateSaver = serializableSaver()) { mutableStateOf(cInfo.groupInfo.fullGroupPreferences) }
            var currentPreferences by rememberSaveable(cInfo.groupInfo, stateSaver = serializableSaver()) { mutableStateOf(preferences) }

            AnnouncementRcvItemView(message, groupMember, cInfo.groupInfo.displayName.startsWith("*"))
          }
        }
      }
    }
  }
}

@Composable
fun AnnouncementRcvItemView(message: String, groupMember: GroupMember, isZeroKnowledge: Boolean) {
  val groupMemberName = if (isZeroKnowledge) {
    groupMember.memberId.substring(0, 6)
  } else {
    if (groupMember.fullName != "") groupMember.fullName else groupMember.displayName
  }
  val announcementMessage = when (message) {
    generalGetString(R.string.rcv_group_event_member_connected) ->
      "$groupMemberName " + generalGetString(R.string.is_now_connected)
    generalGetString(R.string.rcv_group_event_member_left) ->
      "$groupMemberName " + " " + generalGetString(R.string.rcv_group_event_member_left)
    generalGetString(R.string.rcv_group_event_member_deleted) ->
      "$groupMemberName " + " " + generalGetString(R.string.rcv_group_event_member_deleted)
    generalGetString(R.string.rcv_group_event_user_deleted) ->
      "$groupMemberName " + " " + generalGetString(R.string.rcv_group_event_user_deleted)
    generalGetString(R.string.rcv_group_event_group_deleted) ->
      "$groupMemberName " + " " + generalGetString(R.string.rcv_group_event_group_deleted)
    generalGetString(R.string.rcv_group_event_updated_group_profile) ->
      "$groupMemberName " + " " + generalGetString(R.string.rcv_group_event_updated_group_profile)
    generalGetString(R.string.rcv_group_event_updated_group_profile) ->
      "$groupMemberName " + " " + generalGetString(R.string.rcv_group_event_updated_group_profile)
    else -> {
      message
    }
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = AnnouncementBackground
  ) {
    val coroutineScope = rememberCoroutineScope()

    Box(contentAlignment = Alignment.Center) {
      Column(Modifier.width(IntrinsicSize.Max)) {
        Box(
          Modifier
            .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
          Text(
            announcementMessage,
            Modifier.padding(3.dp),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = AnnouncementText
          )
        }
      }
    }
  }
}

@Composable
fun AnnouncementSndItemView(message: String) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = AnnouncementBackground
  ) {
    Box(contentAlignment = Alignment.Center) {
      Column(Modifier.width(IntrinsicSize.Max)) {
        Box(
          Modifier
            .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
          Text(
            message,
            Modifier.padding(3.dp),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = AnnouncementText
          )
        }
      }
    }
  }
}

@Composable
fun BurnerPublicKeyChatItemView(
  cModel: ChatModel,
  cInfo: ChatInfo,
  cItem: ChatItem,
  onChatUpdated: (Chat) -> Unit
) {
  val alignment = Alignment.Center
  val ECHO_CHAT_OPEN_KEYCHAIN_CLONE = "org.sufficientlysecure.keychain"
  val USER_PUBLIC_KEYS = "user_public_keys"
  val EXTRA_OPEN_KEY_CHAIN_USER_ID = "open_key_chain_user_id"
  val resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      if (result.data != null) {
        val openKeyChainIDs = result.data!!.getStringArrayExtra(EXTRA_OPEN_KEY_CHAIN_USER_ID)
        if (cInfo is ChatInfo.Direct) {
          if (openKeyChainIDs != null && !openKeyChainIDs.isEmpty()) {
            withApi {
              val localContact = LocalContact(
                cInfo.contact.id, cInfo.contact.displayName, cInfo.contact.fullName,
                cInfo.contact.localAlias, openKeyChainIDs.get(0), cInfo.chatType, "", false
              )
              cModel.localContactViewModel?.updateLocalContact(localContact)
              val deletedItem = cModel.controller.apiDeleteChatItem(cInfo.chatType, cInfo.apiId, cItem.id, CIDeleteMode.cidmInternal)
              if (deletedItem != null) cModel.removeChatItem(cInfo, deletedItem.deletedChatItem.chatItem)
            }
          }
        }
      }
    }
  }

  Box(
    modifier = Modifier
      .padding(top = 10.dp, bottom = 10.dp)
      .fillMaxWidth(),
    contentAlignment = alignment,
  ) {
    Column(
      Modifier
        .clip(RoundedCornerShape(18.dp))
    ) {
      BurnerPublicKeyView(cModel, cItem, cInfo)
    }
  }
}

@Composable
fun GroupPublicKeyChatItemView(
  cModel: ChatModel,
  cInfo: ChatInfo,
  groupInfo: GroupInfo,
  cItem: ChatItem
) {
  val alignment = Alignment.Center

  Box(
    modifier = Modifier
      .padding(top = 10.dp, bottom = 10.dp)
      .fillMaxWidth(),
    contentAlignment = alignment,
  ) {
    Column(
      Modifier
        .clip(RoundedCornerShape(18.dp))
        .clickable {
          /* if (cItem.chatDir is CIDirection.DirectRcv) {
             val intent = Intent(Intent.ACTION_MAIN)
             intent.component = ComponentName.unflattenFromString(ECHO_CHAT_OPEN_KEYCHAIN_CLONE + ".debug" + "/" + ECHO_CHAT_OPEN_KEYCHAIN_CLONE + ".ui.AutomaticallyImportKey")
             intent.addCategory(Intent.CATEGORY_LAUNCHER)
             intent.putExtra(USER_PUBLIC_KEYS, arrayOf(cItem.content.text))
             resultLauncher.launch(intent)
           }*/
        }
    ) {
      BurnerGroupPublicKeyView(cModel, cItem, cInfo, groupInfo, groupInfo.displayName.startsWith("*", true))
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PublicKeyRequestChatItemView(
  cModel: ChatModel,
  cInfo: ChatInfo,
  cItem: ChatItem,
  chat: Chat,
  messageContent: MsgContent
) {
  val alignment = Alignment.Center
  val keyboardController = LocalSoftwareKeyboardController.current

  Box(
    modifier = Modifier
      .padding(top = 10.dp, bottom = 10.dp)
      .fillMaxWidth(),
    contentAlignment = alignment,
  ) {
    Column(
      Modifier
        .clip(RoundedCornerShape(18.dp))
        .clickable {
          if (chat.chatInfo is ChatInfo.Direct && cItem.chatDir is CIDirection.DirectRcv) {
            withApi {
              val content = MsgContent.MCPublicKey(cModel.controller.appPrefs.publicKey.get()!!)
              if (cModel.contactNetworkStatus(chat.chatInfo.contact) is NetworkStatus.Connected) {
                val command = cModel.controller.apiSendMessage(chat.chatInfo.chatType, chat.chatInfo.apiId, null, null, content)
                keyboardController?.hide()
                if (command != null) {
                  cModel.addChatItem(chat.chatInfo, command.chatItem)
                }
              }
            }
          }
          if (cItem.chatDir is CIDirection.GroupRcv) {
            withApi {
              val content = MsgContent.MCGroupPublicKey(chat.chatInfo.displayName, cModel.controller.appPrefs.publicKey.get()!!, cItem.chatDir.groupMember.memberId)
              val command = cModel.controller.apiSendMessage(chat.chatInfo.chatType, chat.chatInfo.apiId, null, null, content)
              keyboardController?.hide()
              if (command != null) {
                cModel.addChatItem(chat.chatInfo, command.chatItem)
              }
            }
          }
        }
    ) {
      PublicKeyRequestView(cModel, cItem, cInfo, messageContent)
    }
  }
}

@Composable
fun ItemAction(text: String, icon: ImageVector, onClick: () -> Unit, color: Color = MaterialTheme.colors.onBackground) {
  DropdownMenuItem(onClick) {
    Row {
      Text(
        text,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1F)
          .padding(end = 15.dp),
        color = color
      )
      Icon(icon, text, tint = color)
    }
  }
}

fun deleteMessageAlertDialog(chatItem: ChatItem, deleteMessage: (Long, CIDeleteMode) -> Unit) {
  AlertManager.shared.showAlertDialogButtons(
    title = generalGetString(R.string.delete_message__question),
    text = generalGetString(R.string.delete_message_cannot_be_undone_warning),
    buttons = {
      Row(
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(onClick = {
          deleteMessage(chatItem.id, CIDeleteMode.cidmInternal)
          AlertManager.shared.hideAlert()
        }) { Text(stringResource(R.string.for_me_only)) }
        if (chatItem.meta.editable) {
          Spacer(Modifier.padding(horizontal = 4.dp))
          TextButton(onClick = {
            deleteMessage(chatItem.id, CIDeleteMode.cidmBroadcast)
            AlertManager.shared.hideAlert()
          }) { Text(stringResource(R.string.for_everybody)) }
        }
      }
    }
  )
}
/*@Preview
@Composable
fun PreviewChatItemView() {
  SimpleXTheme {
    ChatItemView(
      User.sampleData,
      ChatModel,
      ChatInfo.Direct.sampleData,
      ChatItem.getSampleData(
        1, CIDirection.DirectSnd(), Clock.System.now(), "hello"
      ),
      useLinkPreviews = true,
      composeState = remember { mutableStateOf(ComposeState(useLinkPreviews = true)) },
      cxt = LocalContext.current,
      chatModelIncognito = false,
      deleteMessage = { _, _ -> },
      receiveFile = {},
      joinGroup = {},
      acceptCall = { _ -> },
    ) {}
  }
}

@Preview
@Composable
fun PreviewChatItemViewDeletedContent() {
  SimpleXTheme {
    ChatItemView(
      User.sampleData,
      Chat.sampleData,
      ChatInfo.Direct.sampleData,
      ChatItem.getDeletedContentSampleData(),
      useLinkPreviews = true,
      composeState = remember { mutableStateOf(ComposeState(useLinkPreviews = true)) },
      cxt = LocalContext.current,
      chatModelIncognito = false,
      deleteMessage = { _, _ -> },
      receiveFile = {},
      joinGroup = {},
      acceptCall = { _ -> },
    ) {}
  }
}*/
