package chat.echo.app.views.chat.item

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.outlined.Masks
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.TAG
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chatlist.setGroupMembers
import chat.echo.app.views.helpers.*
import org.openintents.openpgp.OpenPgpError
import org.openintents.openpgp.util.OpenPgpApi
import java.io.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val SentColorLight = Color(0x1E45B8FF)
val ReceivedColorLight = ChatReceivedColor
val SentQuoteColorLight = Color(0x2545B8FF)
val ReceivedQuoteColorLight = Color(0x25B1B0B5)

@Composable
fun FramedItemView(
  chatModel: ChatModel,
  chatInfo: ChatInfo,
  ci: ChatItem,
  uriHandler: UriHandler? = null,
  imageProvider: (() -> ImageGalleryProvider)? = null,
  showMember: Boolean = false,
  linkMode: SimplexLinkMode,
  showMenu: MutableState<Boolean>,
  receiveFile: (Long) -> Unit,
  onLinkLongClick: (link: String) -> Unit = {},
  scrollToItem: (Long) -> Unit = {},
  deleteMessage: (Long, CIDeleteMode) -> Unit,
  decrypted: MutableState<Boolean>
) {
  val sent = ci.chatDir.sent

  fun membership(): GroupMember? {
    return if (chatInfo is ChatInfo.Group) chatInfo.groupInfo.membership else null
  }

  fun isZeroKnowledge(): Boolean {
    return if (chatInfo is ChatInfo.Group) chatInfo.groupInfo.displayName.startsWith("*", ignoreCase = true) else false
  }

  @Composable
  fun ciQuotedMsgView(qi: CIQuote) {
    Box(
      Modifier
        .padding(vertical = 6.dp, horizontal = 12.dp),
      contentAlignment = Alignment.TopStart
    ) {
      MarkdownText(
        qi.text, qi.formattedText, sender = qi.sender(membership(), isZeroKnowledge = isZeroKnowledge()), senderBold = true, maxLines = 3,
        style = TextStyle(fontSize = 15.sp, color = MaterialTheme.colors.onSurface),
        linkMode = linkMode
      )
    }
  }

  @Composable
  fun SingleFileSent(customText: String, qi: CIQuote, chatInfo: ChatInfo) {
    Box(
      Modifier.fillMaxWidth()
    ) {
      Box(
        Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
        contentAlignment = Alignment.TopStart
      ) {
        fun membership(): GroupMember? {
          return if (chatInfo is ChatInfo.Group) chatInfo.groupInfo.membership else null
        }

        MarkdownText(
          customText, qi.formattedText, sender = qi.sender(membership()), senderBold = true, maxLines = 3,
          style = TextStyle(fontSize = 15.sp, color = MaterialTheme.colors.onSurface),
          linkMode = linkMode
        )
      }
    }
    Icon(
      Icons.Filled.InsertDriveFile,
      stringResource(R.string.icon_descr_file),
      Modifier
        .padding(top = 6.dp, end = 4.dp)
        .size(22.dp),
      tint = if (isInDarkTheme()) FileDark else FileLight
    )
  }

  @Composable
  fun ciQuoteView(qi: CIQuote) {
    Row(
      Modifier
        .background(if (sent) SentQuoteColorLight else ReceivedQuoteColorLight)
        .fillMaxWidth()
        .clickable { scrollToItem(qi.itemId ?: return@clickable) }
    ) {
      when (qi.content) {
        is MsgContent.MCImage -> {
          SingleFileSent(customText = "You have viewed this item", qi = qi, chatInfo = chatInfo)
        }
        is MsgContent.MCFile -> {
          Box(Modifier.fillMaxWidth().weight(1f)) {
            ciQuotedMsgView(qi)
          }
          Icon(
            Icons.Filled.InsertDriveFile,
            stringResource(R.string.icon_descr_file),
            Modifier
              .padding(top = 6.dp, end = 4.dp)
              .size(22.dp),
            tint = if (isInDarkTheme()) FileDark else FileLight
          )
        }
        else -> ciQuotedMsgView(qi)
      }
    }
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color.Black,
    modifier = Modifier.clickable {
      showMenu.value = true
    }
  ) {
    var metaColor = HighOrLowlight
    Box(contentAlignment = Alignment.BottomEnd) {
      Column(
        Modifier.width(IntrinsicSize.Max)
      ) {
        val qi = ci.quotedItem
        if (qi != null) {
          ciQuoteView(qi)
        }
        if (ci.file == null && ci.formattedText == null && isShortEmoji(ci.content.text)) {
          Box(Modifier.padding(vertical = 6.dp, horizontal = 12.dp)) {
            Column(
              Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              EmojiText(ci.content.text)
              Text("")
            }
          }
        } else {
          Column(Modifier.fillMaxWidth()) {
            when (val mc = ci.content.msgContent) {
              is MsgContent.MCImage -> {
                CIPhotoView(file = ci.file, imageProvider = imageProvider ?: return@Box, edited = ci.meta.itemEdited, receiveFile = receiveFile, chatItem = ci, deleteMessage = deleteMessage)
                if (mc.text == "") {
                  metaColor = Color.Gray
                } else {
                  CIMarkdownText(
                    chatModel, ci, showMember, linkMode = linkMode, uriHandler, decrypted = decrypted,
                    isZeroKnowledge = if (chatInfo is ChatInfo.Group) chatInfo.groupInfo.displayName.startsWith("*", ignoreCase = true) else false, showMenu = showMenu
                  )
                }
              }
              is MsgContent.MCVideo -> {
                CIVideoView(file = ci.file, edited = ci.meta.itemEdited, receiveFile = receiveFile, chatItem = ci, deleteMessage = deleteMessage)
                if (mc.text == "") {
                  metaColor = Color.Gray
                } else {
                  CIMarkdownText(
                    chatModel, ci, showMember, linkMode = linkMode, uriHandler, decrypted = decrypted,
                    isZeroKnowledge = if (chatInfo is ChatInfo.Group) chatInfo.groupInfo.displayName.startsWith("*", ignoreCase = true) else false, showMenu = showMenu
                  )
                }
              }
              is MsgContent.MCFile -> {
                CIFileView(file = ci.file, imageProvider = imageProvider ?: return@Box, edited = ci.meta.itemEdited, receiveFile = receiveFile, chatItem = ci, deleteMessage = deleteMessage)
                if (mc.text != "") {
                  CIMarkdownText(
                    chatModel, ci, showMember, linkMode = linkMode, uriHandler, decrypted = decrypted,
                    isZeroKnowledge = if (chatInfo is ChatInfo.Group) chatInfo.groupInfo.displayName.startsWith("*", ignoreCase = true) else false, showMenu = showMenu
                  )
                }
              }
              is MsgContent.MCLink -> {
                ChatItemLinkView(mc.preview)
                CIMarkdownText(
                  chatModel, ci, showMember, linkMode = linkMode, uriHandler, decrypted = decrypted,
                  isZeroKnowledge = if (chatInfo is ChatInfo.Group) chatInfo.groupInfo.displayName.startsWith("*", ignoreCase = true) else false, showMenu = showMenu
                )
              }
              else -> CIMarkdownText(
                chatModel, ci, showMember, linkMode = linkMode, uriHandler, decrypted = decrypted,
                isZeroKnowledge = if (chatInfo is ChatInfo.Group) chatInfo.groupInfo.displayName.startsWith("*", ignoreCase = true) else false, showMenu = showMenu
              )
            }
          }
        }
      }
    }
  }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun BurnerPublicKeyView(chatModel: ChatModel, ci: ChatItem, chatInfo: ChatInfo) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = BurnerTimeBackground
  ) {
    var text = ""

    if (chatInfo is ChatInfo.Direct) {
      text = if (ci.chatDir.sent) {
        "You sent your public key to ${
          if (chatInfo.contact.profile.fullName != "") chatInfo.contact.profile.fullName else chatInfo.contact.profile.displayName
        }, if they are connected it will be added to their ninja list automatically."
      } else {
        "${
          if (chatInfo.contact.profile.fullName != "") chatInfo.contact.profile.fullName else chatInfo.contact.profile.displayName
        }'s public key has been added to your ninja list, your chat with them are now end-to-end encrypted."
      }
    }

    Box(contentAlignment = Alignment.Center) {
      Column(Modifier.width(IntrinsicSize.Max)) {
        Box(
          Modifier
            .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
          Row() {
            Icon(Icons.Outlined.Masks, "Ninja", tint = WarningOrange)
            Text(
              text,
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
}

@Composable
fun BurnerGroupPublicKeyView(chatModel: ChatModel, ci: ChatItem, chatInfo: ChatInfo, groupInfo: GroupInfo, isZeroKnowledge: Boolean) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = BurnerTimeBackground
  ) {
    var text = ""

    if (chatInfo is ChatInfo.Group) {
      if (ci.chatDir is CIDirection.GroupSnd) {
        text = "You sent your public key to this group. It will automatically be added to the ninja list of all group members that requested for it."
      } else if (ci.chatDir is CIDirection.GroupRcv) {
        text = "${if (isZeroKnowledge) ci.chatDir.groupMember.memberId.substring(0, 6) else ci.chatDir.groupMember.displayName}'s public key has been added to your ninja list. Your chat to them are now end-to-end encrypted"
      }
    }

    Box(contentAlignment = Alignment.Center) {
      Column(Modifier.width(IntrinsicSize.Max)) {
        Box(
          Modifier
            .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
          Row() {
            Icon(Icons.Outlined.Masks, "Ninja", tint = WarningOrange)
            Text(
              text,
              Modifier.padding(3.dp),
              fontSize = 12.sp,
              textAlign = TextAlign.Center,
              color = AnnouncementText
            )
          }
        }
      }
    }
    /*   Box(contentAlignment = Alignment.Center) {
         Column(Modifier.width(IntrinsicSize.Max)) {
           Box(
             Modifier
               .padding(vertical = 6.dp, horizontal = 12.dp)
           ) {
             Row() {
               if (ci.chatDir is CIDirection.GroupSnd) {
                 Icon(Icons.Outlined.Lock, generalGetString(R.string.publicKeyAdded), tint = Color.Blue)
               } else if(ci.chatDir is CIDirection.GroupRcv){
                 Icon(Icons.Outlined.LockPerson, generalGetString(R.string.publicKeyAdded), tint = Color.Green)
               }
               Text(
                 text,
                 Modifier.padding(3.dp),
                 fontSize = 12.sp,
                 textAlign = TextAlign.Center
               )
             }
             *//*  MarkdownText(
              ci.content.text, ci.formattedText
            )*//*
        }
      }
    }*/
  }
}

fun getUnencryptedUsers(chatModel: ChatModel): List<GroupMember> {
  return chatModel.groupMembers
    .filter { it.memberStatus != GroupMemberStatus.MemLeft && it.memberStatus != GroupMemberStatus.MemRemoved }
    .filter {
      it.memberProfile.localAlias == "" ||
          (it.memberProfile.localAlias != "" && json.decodeFromString(ContactBioInfoSerializer, it.memberProfile.localAlias).openKeyChainID == "")
    }
    .sortedBy { it.displayName.lowercase() }
}

fun getUnencryptedUsers(chatModel: ChatModel, isZeroKnowledge: Boolean): String {
  val unencryptedMembersDisplayName = mutableListOf<String>()
  val groupMembers = chatModel.groupMembers
    .filter { it.memberStatus != GroupMemberStatus.MemLeft && it.memberStatus != GroupMemberStatus.MemRemoved }
    .filter {
      it.memberProfile.localAlias == "" ||
          (it.memberProfile.localAlias != "" && json.decodeFromString(ContactBioInfoSerializer, it.memberProfile.localAlias).openKeyChainID == "")
    }
    .sortedBy { it.displayName.lowercase() }

  for (member in groupMembers) {
    unencryptedMembersDisplayName.add(if (isZeroKnowledge) member.memberId.substring(0, 6) else member.localDisplayName)
  }

  return unencryptedMembersDisplayName.joinToString(separator = ", ")
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun PublicKeyRequestView(chatModel: ChatModel, ci: ChatItem, chatInfo: ChatInfo, messageContent: MsgContent) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = BurnerTimeBackground
  ) {
    var text = ""

    if (chatInfo is ChatInfo.Direct) {
      text = if (ci.chatDir.sent) {
        "You requested for ${
          if (chatInfo.contact.profile.fullName != "") chatInfo.contact.profile.fullName else chatInfo.contact.profile.displayName
        }'s public key."
      } else {
        "${
          if (chatInfo.contact.profile.fullName != "") chatInfo.contact.profile.fullName else chatInfo.contact.profile.displayName
        } is requesting for your public key. Click here to share your public key to them."
      }
    }
   if (chatInfo is ChatInfo.Group) {
      text =
        if (ci.chatDir.sent) {

          "You requested for the public keys of all unencrypted members. Members will be informed and their public keys will be added automatically once it is sent."
        } else if (ci.chatDir is CIDirection.GroupRcv && messageContent is MsgContent.MCPublicKeyRequest) {
          "Your public key was requested by ${messageContent.requestedBy}. Click here to send it to them."
        } else {
          ""
        }
    }


    Box(contentAlignment = Alignment.Center) {
      Column(Modifier.width(IntrinsicSize.Max)) {
        Box(
          Modifier
            .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
          val color = when (ci.chatDir) {
            is CIDirection.DirectSnd -> WarningOrange
            is CIDirection.GroupSnd -> WarningOrange
            is CIDirection.DirectRcv -> MaterialTheme.colors.primary
            is CIDirection.GroupRcv -> MaterialTheme.colors.primary
            else -> MaterialTheme.colors.onError
          }
          Row() {
            Icon(Icons.Outlined.VpnKey, "Ninja", tint = color)
            Text(
              text,
              Modifier.padding(3.dp),
              fontSize = 12.sp,
              textAlign = TextAlign.Center,
              color = AnnouncementText
            )
          }
          /*  MarkdownText(
              ci.content.text, ci.formattedText
            )*/
        }
      }
    }
  }
}

const val CHAT_IMAGE_LAYOUT_ID = "chatImage"

/**
 * Equal to [androidx.compose.ui.unit.Constraints.MaxFocusMask], which is 0x3FFFF - 1
 * Other values make a crash `java.lang.IllegalArgumentException: Can't represent a width of 123456 and height of 9909 in Constraints`
 * See [androidx.compose.ui.unit.Constraints.createConstraints]
 * */
const val MAX_SAFE_WIDTH = 0x3FFFF - 1

@Composable
fun CIMarkdownText(
  chatModel: ChatModel,
  ci: ChatItem,
  showMember: Boolean,
  linkMode: SimplexLinkMode,
  uriHandler: UriHandler?,
  onLinkLongClick: (link: String) -> Unit = {},
  isZeroKnowledge: Boolean,
  decrypted: MutableState<Boolean>,
  showMenu: MutableState<Boolean>
) {
  val decryptedMessage = remember {
    mutableStateOf(ci.content.text)
  }
  var resultLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>? = null

  Box(
    Modifier
      .padding(vertical = 6.dp, horizontal = 12.dp)
  ) {
    if (!decrypted.value) {
      ExpandableText(text = ci.content.text, isDecrypted = decrypted, showMenu = showMenu)
    } else {
      Log.i(TAG, "CIMarkdownText: is zero knowledge? " + isZeroKnowledge + " member hidden: " + ci.memberHiddenID)

      MarkdownText(
        decryptedMessage.value, ci.formattedText,
        metaText = ci.timestampText, edited = ci.meta.itemEdited,
        uriHandler = uriHandler, senderBold = true,
        onLinkLongClick = onLinkLongClick,
        linkMode = linkMode
      )
    }
    /* MarkdownText(
       if(decrypted.value)  decryptedMessage.value else ci.content.text, ci.formattedText, if (showMember) ci.memberDisplayName else null,
       metaText = ci.timestampText, edited = ci.meta.itemEdited,
       uriHandler = uriHandler, senderBold = true,
       onLinkLongClick = onLinkLongClick
     )*/
  }


  fun decryptMessage(encryptedMessage: String, resultLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) {
    val data = Intent()
    data.action = OpenPgpApi.ACTION_DECRYPT_VERIFY
    val inputStream: InputStream = ByteArrayInputStream(encryptedMessage.toByteArray(charset("UTF-8")))
    val outputStream = ByteArrayOutputStream()
    val api = OpenPgpApi(chatModel.activity!!.applicationContext, chatModel.mServiceConnection!!.service)
    val callBack = OpenPgpApi.IOpenPgpCallback { result ->
      if (result != null) {
        when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
          OpenPgpApi.RESULT_CODE_SUCCESS -> {
            if (outputStream != null) {
              try {
                decryptedMessage.value = outputStream.toString()
              } catch (e: UnsupportedEncodingException) {
                Log.e("TAG", "decryptMessage: exception is " + e.message, e)
              }
            }
          }
          OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
            val pi: PendingIntent? = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT)
            try {
              resultLauncher.launch(IntentSenderRequest.Builder(pi!!).build())
            } catch (e: IntentSender.SendIntentException) {
              Log.e(chat.echo.app.TAG, "SendIntentException", e)
            }
          }
          OpenPgpApi.RESULT_CODE_ERROR -> {
            val error: OpenPgpError? = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR)
            if (error != null) {
              Log.d(chat.echo.app.TAG, "onReturn: error is " + error.message)
            }
          }
        }
      }
    }
    api.executeApiAsync(data, inputStream, outputStream, callBack)
  }

  resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
    Log.i(TAG, "CreateProfilePanel: result code is " + result.resultCode)
    if (result.resultCode == Activity.RESULT_OK) {
      decryptMessage(ci.content.text, resultLauncher!!)
    } else {
      AlertManager.shared.showAlertMsg(
        title = generalGetString(R.string.unable_to_decrypt),
        text = generalGetString(R.string.open_key_chain_not_allowed_for_decryption),
        confirmText = generalGetString(R.string.ok),
        onConfirm = {
          decrypted.value = false
          AlertManager.shared.hideAlert()
        }
      )
    }
  }

  if (decrypted.value) {
    decryptMessage(ci.content.text, resultLauncher!!)
    Executors.newSingleThreadScheduledExecutor().schedule({
      decrypted.value = false
    }, 10, TimeUnit.SECONDS)
  }
}

class EditedProvider: PreviewParameterProvider<Boolean> {
  override val values = listOf(false, true).asSequence()
}
/*@Preview
@Composable
fun PreviewTextItemViewSnd(@PreviewParameter(EditedProvider::class) edited: Boolean) {
  val showMenu = remember { mutableStateOf(false) }
  val decrypted = remember { mutableStateOf(false) }
  SimpleXTheme {
    FramedItemView(
      ChatInfo.Direct.sampleData,
      ChatItem.getSampleData(
        1, CIDirection.DirectSnd(), Clock.System.now(), "hello", itemEdited = edited,
      ),
      showMenu = showMenu,
      receiveFile = {},
      deleteMessage = { _, _ -> }, 
      decrypted = decrypted
    )
  }
}

@Preview
@Composable
fun PreviewTextItemViewRcv(@PreviewParameter(EditedProvider::class) edited: Boolean) {
  val showMenu = remember { mutableStateOf(false) }
  val decrypted = remember { mutableStateOf(false) }
  SimpleXTheme {
    FramedItemView(
      ChatInfo.Direct.sampleData,
      ChatItem.getSampleData(
        1, CIDirection.DirectRcv(), Clock.System.now(), "hello", itemEdited = edited
      ),
      showMenu = showMenu,
      receiveFile = {},
      deleteMessage = { _, _ -> },
      decrypted = decrypted
    )
  }
}

@Preview
@Composable
fun PreviewTextItemViewLong(@PreviewParameter(EditedProvider::class) edited: Boolean) {
  val showMenu = remember { mutableStateOf(false) }
  val decrypted = remember { mutableStateOf(false) }
  SimpleXTheme {
    FramedItemView(
      ChatInfo.Direct.sampleData,
      ChatItem.getSampleData(
        1,
        CIDirection.DirectSnd(),
        Clock.System.now(),
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        itemEdited = edited
      ),
      showMenu = showMenu,
      receiveFile = {},
      deleteMessage = { _, _ -> },
      decrypted = decrypted
    )
  }
}

@Preview
@Composable
fun PreviewTextItemViewQuote(@PreviewParameter(EditedProvider::class) edited: Boolean) {
  val showMenu = remember { mutableStateOf(false) }
  val decrypted = remember { mutableStateOf(false) }
  SimpleXTheme {
    FramedItemView(
      ChatInfo.Direct.sampleData,
      ChatItem.getSampleData(
        1, CIDirection.DirectSnd(),
        Clock.System.now(),
        "https://simplex.chat",
        CIStatus.SndSent(),
        quotedItem = CIQuote.getSample(1, Clock.System.now(), "hi", chatDir = CIDirection.DirectRcv()),
        itemEdited = edited
      ),
      showMenu = showMenu,
      receiveFile = {},
      deleteMessage = { _, _ -> },
      decrypted = decrypted
    )
  }
}

@Preview
@Composable
fun PreviewTextItemViewEmoji(@PreviewParameter(EditedProvider::class) edited: Boolean) {
  val showMenu = remember { mutableStateOf(false) }
  val decrypted = remember { mutableStateOf(false) }
  SimpleXTheme {
    FramedItemView(
      ChatInfo.Direct.sampleData,
      ChatItem.getSampleData(
        1, CIDirection.DirectSnd(),
        Clock.System.now(),
        "👍",
        CIStatus.SndSent(),
        quotedItem = CIQuote.getSample(1, Clock.System.now(), "Lorem ipsum dolor sit amet", chatDir = CIDirection.DirectRcv()),
        itemEdited = edited
      ),
      showMenu = showMenu,
      receiveFile = {},
      deleteMessage = { _, _ -> },
      decrypted = decrypted
    )
  }
}

@Preview
@Composable
fun PreviewQuoteWithTextAndImage(@PreviewParameter(EditedProvider::class) edited: Boolean) {
  val ciQuote = CIQuote(
    chatDir = CIDirection.DirectRcv(), itemId = 1, sentAt = Clock.System.now(),
    content = MsgContent.MCImage(
      text = "Hello there",
      image = "data:image/jpg;base64,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"
    )
  )
  val showMenu = remember { mutableStateOf(false) }
  val decrypted = remember { mutableStateOf(false) }
  SimpleXTheme {
    FramedItemView(
      ChatInfo.Direct.sampleData,
      ChatItem.getSampleData(
        1, CIDirection.DirectSnd(),
        Clock.System.now(),
        "Hello there",
        CIStatus.SndSent(),
        quotedItem = ciQuote,
        itemEdited = edited
      ),
      showMenu = showMenu,
      receiveFile = {},
      deleteMessage = { _, _ -> },
      decrypted = decrypted
    )
  }
}

@Preview
@Composable
fun PreviewQuoteWithLongTextAndImage(@PreviewParameter(EditedProvider::class) edited: Boolean) {
  val ciQuote = CIQuote(
    chatDir = CIDirection.DirectRcv(), itemId = 1, sentAt = Clock.System.now(),
    content = MsgContent.MCImage(
      text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
      image = "data:image/jpg;base64,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"
    )
  )
  val showMenu = remember { mutableStateOf(false) }
  val decrypted = remember { mutableStateOf(false) }
  SimpleXTheme {
    FramedItemView(
      ChatInfo.Direct.sampleData,
      ChatItem.getSampleData(
        1, CIDirection.DirectSnd(),
        Clock.System.now(),
        "Hello there",
        CIStatus.SndSent(),
        quotedItem = ciQuote,
        itemEdited = edited
      ),
      showMenu = showMenu,
      receiveFile = {},
      deleteMessage = { _, _ -> },
      decrypted = decrypted
    )
  }
}

@Preview
@Composable
fun PreviewQuoteWithLongTextAndFile(@PreviewParameter(EditedProvider::class) edited: Boolean) {
  val ciQuote = CIQuote(
    chatDir = CIDirection.DirectRcv(), itemId = 1, sentAt = Clock.System.now(),
    content = MsgContent.MCFile(
      text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    )
  )
  val showMenu = remember { mutableStateOf(false) }
  val decrypted = remember { mutableStateOf(false) }
  SimpleXTheme {
    FramedItemView(
      ChatInfo.Direct.sampleData,
      ChatItem.getSampleData(
        1, CIDirection.DirectSnd(),
        Clock.System.now(),
        "Hello there",
        CIStatus.SndSent(),
        quotedItem = ciQuote,
        itemEdited = edited
      ),
      showMenu = showMenu,
      receiveFile = {},
      deleteMessage = { _, _ -> },
      decrypted = decrypted
    )
  }
}*/

