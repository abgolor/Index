package chat.echo.app.views.bottomsheet

import SectionItemView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.Contact
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chatlist.SortChat
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.CreateUserLinkView
import chat.echo.app.views.newchat.group.SelectGroupTypeView
import chat.echo.app.views.usersettings.SettingsActionItem

@Composable
fun SortChatBottomSheetView(
  sortChatMode: MutableState<SortChat>,
  hideBottomSheet: () -> Unit
){

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(color = Color.White)
      .wrapContentHeight()
      .onFocusChanged { focusState ->
        if (!focusState.hasFocus) hideBottomSheet()
      }
  ) {
    Column(
      Modifier
        .fillMaxWidth()
        .padding(top = 15.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
      ) {
        IconButton(
          modifier = Modifier.align(Alignment.CenterStart),
          onClick = hideBottomSheet) {
          Icon(
            Icons.Outlined.Close, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
        }
        Text(
          text = generalGetString(R.string.sort_chats_by),
          fontSize = 16.sp,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.SemiBold,
          color = Color.Black
        )
      }
      Column(
        Modifier
          .fillMaxWidth()
          .padding(vertical = 20.dp, ),
        verticalArrangement = Arrangement.SpaceEvenly
      ) {
        SortChatItemView(sortChatMode = generalGetString(R.string.time_received), actionIcon = Icons.Outlined.Timer, checked =
        sortChatMode.value == SortChat.SortByTime) {
          sortChatMode.value = SortChat.SortByTime
          hideBottomSheet()
        }
        Divider(color = PreviewTextColor)
        SortChatItemView(sortChatMode = generalGetString(R.string.unread_messages), actionIcon = Icons.Outlined.MarkUnreadChatAlt, checked =
        sortChatMode.value == SortChat.SortByUnread) {
          sortChatMode.value = SortChat.SortByUnread
          hideBottomSheet()
        }
      }
    }
  }
}

@Composable
fun SortChatItemView(
  sortChatMode: String,
  actionIcon: ImageVector,
  checked: Boolean,
  onClickAction: () -> Unit
) {
  val icon: ImageVector
  val iconColor: Color
  if (checked) {
    icon = Icons.Filled.CheckCircle
    iconColor = Color.Black
  }
  else {
    icon = Icons.Outlined.Circle
    iconColor = PreviewTextColor
  }

  SectionItemView(onClickAction) {
    Icon(actionIcon, sortChatMode, tint = iconColor)
    Spacer(Modifier.padding(horizontal = 10.dp))
    Text(sortChatMode, color = iconColor)
    Spacer(Modifier.fillMaxWidth().weight(1f))
    Icon(
      icon,
      contentDescription = if(checked) generalGetString(R.string.icon_descr_sort_mode_checked) else generalGetString(R.string.icon_descr_sort_mode_unchecked),
      modifier = Modifier.size(26.dp),
      tint = iconColor
    )
  }
}