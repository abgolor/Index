package chat.echo.app.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chat.ChatInfoView
import chat.echo.app.views.chat.group.GroupChatInfoView
import chat.echo.app.views.chat.item.MarkdownText
import chat.echo.app.views.helpers.*

@Composable
fun ContactPreviewView(chatModel: ChatModel, chat: Chat, chatModelIncognito: Boolean, currentUserProfileDisplayName: String?, stopped: Boolean) {
  val cInfo = chat.chatInfo

  @Composable
  fun contactNameText(color: Color = Color.Unspecified) {

    if (chat.chatInfo is ChatInfo.Direct) {
      Text(
        chat.chatInfo.fullName.ifEmpty { chat.chatInfo.localDisplayName },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontSize = 16.sp),
        fontWeight = FontWeight.SemiBold,
        color = color
      )
    }
  }

  @Composable
  fun chatPreviewTitle() {
    when (cInfo) {
      is ChatInfo.Direct ->
        contactNameText(if (cInfo.ready) Color.Black else HighOrLowlight)
      is ChatInfo.Group ->
        when (cInfo.groupInfo.membership.memberStatus) {
          GroupMemberStatus.MemInvited -> contactNameText(if (chat.chatInfo.incognito) Indigo else MaterialTheme.colors.primary)
          GroupMemberStatus.MemAccepted -> contactNameText(HighOrLowlight)
          else -> contactNameText()
        }
      else -> contactNameText()
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

  Row(Modifier.background(Color.White)) {
    Box(contentAlignment = Alignment.BottomEnd,
    modifier = Modifier.clickable {
      withApi {
        chatModel.chatId.value = chat.id
        chatModel.chatInfo.value = chat.chatInfo
        chatModel.contactInfo.value = chatModel.controller.apiContactInfo(cInfo.apiId)
      }
    }) {
      ContactInfoImage(cInfo, size = 60.dp, Color.Black)
    }
    Column(
      modifier = Modifier
        .padding(horizontal = 5.dp)
        .weight(1F)
        .align(Alignment.CenterVertically)
    ) {
      chatPreviewTitle()
    }
    val ts = chat.chatItems.lastOrNull()?.timestampText ?: getTimestampText(chat.chatInfo.updatedAt)

    Box(
      Modifier.align(Alignment.CenterVertically),
      contentAlignment = Alignment.TopEnd
    ) {
      Row() {
        IconButton(onClick = { directChatAction(chat.chatInfo, chatModel)}) {
          Icon(painter = painterResource(id = R.drawable.ic_chat), 
            contentDescription = generalGetString(R.string.chat),
            tint = Color.Black,
            modifier = Modifier
              .padding(horizontal = 1.dp)
              .padding(vertical = 1.dp)
              .size(24.dp)
          )
        }
        Spacer(modifier = Modifier.padding(end = 1.dp))
        IconButton(onClick = {
          directCallAction(chatModel, chat)
        }) {
          Icon(painter = painterResource(id = R.drawable.ic_phone),
            contentDescription = generalGetString(R.string.voice_call),
            tint = Color.Black,
            modifier = Modifier
              .padding(horizontal = 3.dp)
              .padding(vertical = 1.dp)
              .size(24.dp)
          )
        }
      }
      /*    Text(
        ts,
        color = HighOrLowlight,
        style = MaterialTheme.typography.body2,
        modifier = Modifier.padding(bottom = 5.dp)
      )
      val n = chat.chatStats.unreadCount
      val showNtfsIcon = !chat.chatInfo.ntfsEnabled && (chat.chatInfo is ChatInfo.Direct || chat.chatInfo is ChatInfo.Group)
      if (n > 0) {
        Box(
          Modifier.padding(top = 24.dp),
          contentAlignment = Alignment.Center
        ) {
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
      } else if (showNtfsIcon) {
        Box(
          Modifier.padding(top = 24.dp),
          contentAlignment = Alignment.Center
        ) {
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
      }
      if (cInfo is ChatInfo.Direct) {
        Box(
          Modifier.padding(top = 52.dp),
          contentAlignment = Alignment.Center
        ) {
          ChatStatusImage(chat)
        }
      }
    */
    }
  }
}

/*
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

@Composable
fun ChatStatusImage(chat: Chat) {
  val s = chat.serverInfo.networkStatus
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
}*/
