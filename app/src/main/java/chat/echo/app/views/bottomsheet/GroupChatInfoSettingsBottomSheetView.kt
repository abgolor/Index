package chat.echo.app.views.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.ChatInfo
import chat.echo.app.model.GroupInfo
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.generalGetString
import chat.echo.app.views.usersettings.SettingsActionItem


@Composable
fun GroupChatInfoSettingsBottomSheetView(
  hideBottomSheet: () -> Unit,
  groupInfo: GroupInfo,
  chatInfo: ChatInfo,
  toggleGroupChatNotification: () -> Unit,
  deleteGroup: () -> Unit,
  clearChat: () -> Unit,
  leaveGroup: () -> Unit,
) {
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
          text = generalGetString(R.string.group_chat_settings),
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
        SettingsActionItem(
          if (chatInfo.ntfsEnabled) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
          stringResource(if (chatInfo.ntfsEnabled) R.string.mute_group else R.string.unmute_group),
          toggleGroupChatNotification,
          LightBlue,
          LightBlue
        )
        Divider(color = PreviewTextColor)
        SettingsActionItem(
          Icons.Outlined.Restore,
          stringResource(R.string.clear_chat_button),
          clearChat,
          WarningOrange,
          WarningOrange
        )
        if(groupInfo.membership.memberCurrent){
          Divider(color = PreviewTextColor)
          SettingsActionItem(
            Icons.Outlined.Logout,
            stringResource(R.string.leave_group),
            leaveGroup,
            Color.Red,
            Color.Red
          )
        }
        if(groupInfo.canDelete){
          Divider(color = PreviewTextColor)
          SettingsActionItem(
            Icons.Outlined.Delete,
            stringResource(R.string.delete_group),
            deleteGroup,
            Color.Red,
            Color.Red
          )
        }
      }
    }
  }
}
