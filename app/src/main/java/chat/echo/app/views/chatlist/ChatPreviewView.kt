package chat.echo.app.views.chatlist

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chat.item.MarkdownText
import chat.echo.app.views.helpers.*

@Composable
fun ChatPreviewView(chat: Chat, chatModelIncognito: Boolean, currentUserProfileDisplayName: String?, stopped: Boolean,
  linkMode: SimplexLinkMode) {
  val cInfo = chat.chatInfo

  @Composable
  fun groupInactiveIcon() {
    Icon(
      Icons.Filled.Cancel,
      stringResource(R.string.icon_descr_group_inactive),
      Modifier.size(18.dp).background(MaterialTheme.colors.background, CircleShape),
      tint = HighOrLowlight
    )
  }

  @Composable
  fun chatPreviewImageOverlayIcon() {
    if (cInfo is ChatInfo.Group) {
      when (cInfo.groupInfo.membership.memberStatus) {
        GroupMemberStatus.MemLeft -> groupInactiveIcon()
        GroupMemberStatus.MemRemoved -> groupInactiveIcon()
        GroupMemberStatus.MemGroupDeleted -> groupInactiveIcon()
        else -> {}
      }
    }
  }

  @Composable
  fun chatPreviewTitleText(color: Color = Color.Black) {
    if (chat.chatInfo is ChatInfo.Direct) {
      Text(
        chat.chatInfo.fullName.ifEmpty { chat.chatInfo.localDisplayName },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontSize = 18.sp),
        fontWeight = FontWeight.SemiBold,
        color = color
      )
    } else {
      Text(
        chat.chatInfo.fullName.removePrefix("*").ifEmpty { chat.chatInfo.displayName.removePrefix("*") },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontSize = 18.sp),
        fontWeight = FontWeight.SemiBold,
        color = color
      )
    }
  }

  @Composable
  fun chatPreviewTitle() {
    when (cInfo) {
      is ChatInfo.Direct ->
        chatPreviewTitleText(if (cInfo.ready) Color.Black else PreviewTextColor)
      is ChatInfo.Group ->
        when (cInfo.groupInfo.membership.memberStatus) {
          GroupMemberStatus.MemInvited -> chatPreviewTitleText(if (chat.chatInfo.incognito) Indigo else Color.Black)
          GroupMemberStatus.MemAccepted -> chatPreviewTitleText(HighOrLowlight)
          else -> chatPreviewTitleText()
        }
      else -> chatPreviewTitleText()
    }
  }

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
  fun chatPreviewText(chatModelIncognito: Boolean) {
    val ci = chat.chatItems.lastOrNull()
    var text: String? = null
    if (ci != null) {
      val sent = ci.chatDir.sent
      val received = ci.chatDir is CIDirection.DirectRcv
      if (ci.text != "") {
        if (cInfo is ChatInfo.Direct) {
          text = when (val mc = ci.content.msgContent) {
            is MsgContent.MCText -> generalGetString(R.string.message_is_encrypted)
            is MsgContent.MCLink -> generalGetString(R.string.link_is_encrypted)
            is MsgContent.MCImage -> "\uD83D\uDCF7 " + (if (ci.chatDir.sent) generalGetString(R.string.you_sent_a_photo) else generalGetString(R.string.photo))
            is MsgContent.MCVideo -> "\uD83C\uDFA5 " + (if (ci.chatDir.sent) generalGetString(R.string.you_sent_a_video) else generalGetString(R.string.video))
            is MsgContent.MCVoice -> "\uD83C\uDFA4 " + (if (ci.chatDir.sent) generalGetString(R.string.you_sent_a_voice_message) + " (" + durationText(mc.duration) + ")" else generalGetString(R.string.voice_message) + " (" + durationText(mc.duration) + ")")
            is MsgContent.MCFile -> "📄 " + (if (ci.chatDir.sent) generalGetString(R.string.you_sent_a_file) else generalGetString(R.string.file))
            is MsgContent.MCPublicKey -> (if (ci.chatDir.sent) generalGetString(R.string.you_shared_your_public_key) else generalGetString(R.string.shared_public_key))
            is MsgContent.MCPublicKeyRequest -> (if (ci.chatDir.sent) generalGetString(R.string.you_requested_public_key) else generalGetString(R.string.public_key_requested))
            is MsgContent.MCGroupPublicKey -> generalGetString(R.string.group_public_key)
            is MsgContent.MCUnknown -> ci.text
            null -> ""
          }
        }
        if (cInfo is ChatInfo.Group) {
          text = when (val mc = ci.content.msgContent) {
            is MsgContent.MCText -> if (ci.chatDir.sent) generalGetString(R.string.you) + ": " + generalGetString(R.string.message_is_encrypted) else getGroupMessageSender(chat.chatInfo, ci) + ": " +  generalGetString(R.string.message_is_encrypted)
            is MsgContent.MCLink -> if (ci.chatDir.sent) generalGetString(R.string.you) + ": " + generalGetString(R.string.link_is_encrypted) else getGroupMessageSender(chat.chatInfo, ci) + ": " +  generalGetString(R.string.link_is_encrypted)
            is MsgContent.MCImage -> "\uD83D\uDCF7 " + (if (ci.chatDir.sent) generalGetString(R.string.you_sent_a_photo) else getGroupMessageSender(chat.chatInfo, ci) + ": " + generalGetString(R.string.photo))
            is MsgContent.MCVideo -> "\uD83C\uDFA5 " + (if (ci.chatDir.sent) generalGetString(R.string.you_sent_a_video) else getGroupMessageSender(chat.chatInfo, ci) + ": " + generalGetString(R.string.video))
            is MsgContent.MCVoice -> "\uD83C\uDFA4 " + (if (ci.chatDir.sent) generalGetString(R.string.you_sent_a_voice_message) + " (" + durationText(mc.duration) + ")" else getGroupMessageSender(chat.chatInfo, ci) + ": " + generalGetString(R.string.voice_message) + " (" + durationText(mc.duration) + ")")
            is MsgContent.MCFile -> "📄 " + (if (ci.chatDir.sent) generalGetString(R.string.you_sent_a_file) else getGroupMessageSender(chat.chatInfo, ci) + ": " + generalGetString(R.string.file))
            is MsgContent.MCPublicKey -> (if (ci.chatDir.sent) generalGetString(R.string.you_shared_your_public_key) else getGroupMessageSender(chat.chatInfo, ci) + ": " + generalGetString(R.string.shared_public_key))
            is MsgContent.MCPublicKeyRequest -> (if (ci.chatDir.sent) generalGetString(R.string.you_requested_public_key) else getGroupMessageSender(chat.chatInfo, ci) + ": " + generalGetString(R.string.public_key_requested))
            is MsgContent.MCGroupPublicKey -> generalGetString(R.string.group_public_key)
            is MsgContent.MCUnknown -> ci.text
            null -> ""
          }
        }
        if (ci.text.contains("deleted")) {
          text = "This message was deleted or has been burnt"
        } else {
          //text = ci.text
        }
      } else if (ci.text == "") {
        text = generalGetString(R.string.tap_to_start_new_chat)
      }
    } else {
      text = generalGetString(R.string.tap_to_start_new_chat)
    }

    if (text == "") {
      text = generalGetString(R.string.tap_to_start_new_chat)
    }

    if (ci != null) {
      MarkdownText(
        text!!,
        ci.formattedText,
        sender = null,
        metaText = null,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.body1.copy(color = if (isInDarkTheme()) PreviewTextColor else MessagePreviewLight, lineHeight = 18.sp, fontSize = 14.sp),
        modifier = Modifier.fillMaxWidth(),
        linkMode = linkMode,
      )
    } else {
      when (cInfo) {
        is ChatInfo.Direct ->
          if (!cInfo.ready) {
            //cInfo.contact.publicKeyAdded = true
            Text(stringResource(R.string.contact_connection_pending), color = HighOrLowlight)
          }
        is ChatInfo.Group ->
          when (cInfo.groupInfo.membership.memberStatus) {
            GroupMemberStatus.MemInvited -> Text(groupInvitationPreviewText(chatModelIncognito, currentUserProfileDisplayName, cInfo.groupInfo))
            GroupMemberStatus.MemAccepted -> Text(stringResource(R.string.group_connection_pending), color = HighOrLowlight)
            else -> {}
          }
        else -> {}
      }
    }
  }

  Row(Modifier.background(Color.White)) {
    Box(contentAlignment = Alignment.BottomEnd) {
      ContactInfoImage(cInfo, size = 60.dp, Color.Black)
    }
    Column(
      modifier = Modifier
        .padding(horizontal = 8.dp)
        .weight(1F)
    ) {
      chatPreviewTitle()
      chatPreviewText(chatModelIncognito)
    }
    val ts = chat.chatItems.lastOrNull()?.timestampText ?: getTimestampText(chat.chatInfo.updatedAt)

    Box(
      contentAlignment = Alignment.TopEnd
    ) {
      Column() {
        Text(
          ts,
          color = PreviewTextColor,
          style = TextStyle(fontSize = 14.sp),
          fontWeight = FontWeight.Normal,
          modifier = Modifier.padding(bottom = 5.dp)
        )
        val n = chat.chatStats.unreadCount
        val showNtfsIcon = !chat.chatInfo.ntfsEnabled && (chat.chatInfo is ChatInfo.Direct || chat.chatInfo is ChatInfo.Group)
        Row(Modifier.align(Alignment.End)) {
          if (showNtfsIcon) {
            Icon(
              Icons.Filled.NotificationsOff,
              contentDescription = generalGetString(R.string.notifications),
              tint = HighOrLowlight,
              modifier = Modifier
                .padding(horizontal = 3.dp)
                .padding(vertical = 1.dp)
                .size(17.dp)
            )
          }
          if (n > 0) {
            Text(
              unreadCountStr(n),
              color = MaterialTheme.colors.onPrimary,
              fontSize = 11.sp,
              modifier = Modifier
                .background(if (stopped || showNtfsIcon) HighOrLowlight else MaterialTheme.colors.primary, shape = CircleShape)
                .badgeLayout()
                .padding(horizontal = 3.dp)
                .padding(vertical = 1.dp)
            )
          }
        }
      }
    }
    /*    val n = chat.chatStats.unreadCount
        val showNtfsIcon = !chat.chatInfo.ntfsEnabled && (chat.chatInfo is ChatInfo.Direct || chat.chatInfo is ChatInfo.Group)
          Box(
            Modifier.padding(top = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            Row() {
              if (showNtfsIcon) {
                Icon(
                  Icons.Filled.NotificationsOff,
                  contentDescription = generalGetString(R.string.notifications),
                  tint = HighOrLowlight,
                  modifier = Modifier
                   .padding(horizontal = 3.dp)
                    .padding(vertical = 1.dp)
                    .size(17.dp)
                )
              }
              if(n > 0) {
                Text(
                  unreadCountStr(n),
                  color = MaterialTheme.colors.onPrimary,
                  fontSize = 11.sp,
                  modifier = Modifier
                    .background(if (stopped || showNtfsIcon) HighOrLowlight else MaterialTheme.colors.primary, shape = CircleShape)
                    .badgeLayout()
                    .padding(horizontal = 3.dp)
                    .padding(vertical = 1.dp)
                )
              }
            }

          *//*      else if (showNtfsIcon) {
              Box(
                Modifier.padding(top = 24.dp),
                contentAlignment = Alignment.Center
              ) {
              }
            }
            if (cInfo is ChatInfo.Direct) {
              Box(
                Modifier.padding(top = 52.dp),
                contentAlignment = Alignment.Center
              ) {
                ChatStatusImage(chat)
              }
            }*//*
    }*/
  }
}

@Composable
private fun groupInvitationPreviewText(chatModelIncognito: Boolean, currentUserProfileDisplayName: String?, groupInfo: GroupInfo): String {
  return if (groupInfo.membership.memberIncognito)
    String.format(stringResource(R.string.group_preview_join_as), groupInfo.membership.memberProfile.displayName)
  else if (chatModelIncognito)
    String.format(stringResource(R.string.group_preview_join_as), currentUserProfileDisplayName ?: "")
  else
    stringResource(R.string.group_preview_you_are_invited)
}

@Composable
fun unreadCountStr(n: Int): String {
  return if (n < 1000) "$n" else "${n / 1000}" + stringResource(R.string.thousand_abbreviation)
}

fun getGroupMessageSender(chatInfo: ChatInfo, chatItem: ChatItem): String {
  if (chatInfo is ChatInfo.Group && chatItem.chatDir is CIDirection.GroupRcv) {
    return if (chatItem.chatDir.groupMember.fullName != "") chatItem.chatDir.groupMember.fullName else chatItem.chatDir.groupMember.localDisplayName
  }
  return ""
}

/*@Composable
fun ChatStatusImage(chatModel: ChatModel, chat: Chat) {
  if(chat.chatInfo is ChatInfo.Direct) {
    val contact = chatModel.getContactChat(chat.chatInfo.contact)
    val descr = s.statusString
    if (s is Chat.NetworkStatus.Error) {
      Icon(
        Icons.Outlined.ErrorOutline,
        contentDescription = descr,
        tint = HighOrLowlight,
        modifier = Modifier
          .size(19.dp)
      )
    } else if (s !is Chat.NetworkStatus.Connected) {
      CircularProgressIndicator(
        Modifier
          .padding(horizontal = 2.dp)
          .size(15.dp),
        color = HighOrLowlight,
        strokeWidth = 1.5.dp
      )
    }
  }
}*/

@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewChatPreviewView() {
  SimpleXTheme {
    ChatPreviewView(Chat.sampleData, false, "", stopped = false, linkMode = SimplexLinkMode.DESCRIPTION)
  }
}
