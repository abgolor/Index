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
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.*
import chat.echo.app.views.usersettings.SettingsActionItem

@Composable
fun DirectChatInfoSettingsBottomSheetView(
  hideBottomSheet: () -> Unit,
  chatInfo: ChatInfo,
  toggleChatNotification: () -> Unit,
  sharePublicKey: () -> Unit,
  clearChat: () -> Unit,
  deleteContact: () -> Unit
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
          text = generalGetString(R.string.chat_settings),
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
          Icons.Outlined.VpnKey,
          stringResource(R.string.share_public_key),
          sharePublicKey,
          Color.Blue,
          Color.Blue
        )
        Divider(color = PreviewTextColor)
        SettingsActionItem(
          if (chatInfo.ntfsEnabled) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
          stringResource(if (chatInfo.ntfsEnabled) R.string.mute_chat_button else R.string.unmute_chat_button),
          toggleChatNotification,
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
          Divider(color = PreviewTextColor)
          SettingsActionItem(
            Icons.Outlined.Delete,
            stringResource(R.string.delete_contact),
            deleteContact,
            Color.Red,
            Color.Red
          )
      }
    }
  }
}