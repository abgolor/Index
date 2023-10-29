package chat.echo.app.views.usersettings

import SectionItemViewSpaceBetween
import androidx.compose.runtime.Composable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.ChatModel
import chat.echo.app.ui.theme.*
import chat.echo.app.views.call.parseRTCIceServers
import chat.echo.app.views.helpers.*

@Composable
fun RTCServersView(
  chatModel: ChatModel
) {
  var userRTCServers by remember {
    mutableStateOf(chatModel.controller.appPrefs.webrtcIceServers.get()?.split("\n") ?: listOf())
  }
  var isUserRTCServers by remember { mutableStateOf(userRTCServers.isNotEmpty()) }
  var editRTCServers by remember { mutableStateOf(!isUserRTCServers) }
  val userRTCServersStr = remember { mutableStateOf(if (isUserRTCServers) userRTCServers.joinToString(separator = "\n") else "") }
  fun saveUserRTCServers() {
    val srvs = userRTCServersStr.value.split("\n")
    if (srvs.isNotEmpty() && srvs.toSet().size == srvs.size && parseRTCIceServers(srvs) != null) {
      userRTCServers = srvs
      chatModel.controller.appPrefs.webrtcIceServers.set(srvs.joinToString(separator = "\n"))
      editRTCServers = false
    } else {
      AlertManager.shared.showAlertMsg(
        generalGetString(R.string.error_saving_ICE_servers),
        generalGetString(R.string.ensure_ICE_server_address_are_correct_format_and_unique)
      )
    }
  }

  fun resetRTCServers() {
    isUserRTCServers = false
    userRTCServers = listOf()
    chatModel.controller.appPrefs.webrtcIceServers.set(null)
  }

  RTCServersLayout(
    isUserRTCServers = isUserRTCServers,
    editRTCServers = editRTCServers,
    userRTCServersStr = userRTCServersStr,
    isUserRTCServersOnOff = { switch ->
      if (switch) {
        isUserRTCServers = true
      } else if (userRTCServers.isNotEmpty()) {
          AlertManager.shared.showAlertDialog(
            title = generalGetString(R.string.use_simplex_chat_servers__question),
            text = generalGetString(R.string.saved_ICE_servers_will_be_removed),
            confirmText = generalGetString(R.string.confirm_verb),
            onConfirm = {
              resetRTCServers()
              isUserRTCServers = false
              userRTCServersStr.value = ""
            }
          )
        } else {
        isUserRTCServers = false
        userRTCServersStr.value = ""
      }
    },
    cancelEdit = {
      isUserRTCServers = userRTCServers.isNotEmpty()
      editRTCServers = !isUserRTCServers
      userRTCServersStr.value = if (isUserRTCServers) userRTCServers.joinToString(separator = "\n") else ""
    },
    saveRTCServers = ::saveUserRTCServers,
    editOn = { editRTCServers = true },
  )
}

@Composable
fun RTCServersLayout(
  isUserRTCServers: Boolean,
  editRTCServers: Boolean,
  userRTCServersStr: MutableState<String>,
  isUserRTCServersOnOff: (Boolean) -> Unit,
  cancelEdit: () -> Unit,
  saveRTCServers: () -> Unit,
  editOn: () -> Unit,
) {
  Column {
    AppBarTitle(stringResource(R.string.your_ICE_servers))
    Column(
      Modifier
        .fillMaxWidth()
        .padding(horizontal = DEFAULT_PADDING),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      SectionItemViewSpaceBetween(padding = PaddingValues()) {
        Text(stringResource(R.string.configure_ICE_servers), Modifier.padding(end = 24.dp))
        Switch(
          checked = isUserRTCServers,
          onCheckedChange = isUserRTCServersOnOff,
          colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colors.primary,
            uncheckedThumbColor = HighOrLowlight
          ),
        )
      }

      if (!isUserRTCServers) {
        Text(stringResource(R.string.using_simplex_chat_servers), lineHeight = 22.sp)
      } else {
        Text(stringResource(R.string.enter_one_ICE_server_per_line))
        if (editRTCServers) {
          TextEditor(Modifier.height(160.dp), text = userRTCServersStr)

          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(horizontalAlignment = Alignment.Start) {
              Row {
                Text(
                  stringResource(R.string.cancel_verb),
                  color = MaterialTheme.colors.primary,
                  modifier = Modifier
                    .clickable(onClick = cancelEdit)
                )
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(
                  stringResource(R.string.save_servers_button),
                  color = MaterialTheme.colors.primary,
                  modifier = Modifier.clickable(onClick = {
                    saveRTCServers()
                  })
                )
              }
            }
            Column(horizontalAlignment = Alignment.End) {
              howToButton()
            }
          }
        } else {
          Surface(
            modifier = Modifier
              .height(160.dp)
              .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colors.secondary)
          ) {
            SelectionContainer(
              Modifier.verticalScroll(rememberScrollState())
            ) {
              Text(
                userRTCServersStr.value,
                Modifier
                  .padding(vertical = 5.dp, horizontal = 7.dp),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
              )
            }
          }
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(horizontalAlignment = Alignment.Start) {
              Text(
                stringResource(R.string.edit_verb),
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                  .clickable(onClick = editOn)
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              howToButton()
            }
          }
        }
      }
    }
  }
}

@Composable
private fun howToButton() {
  val uriHandler = LocalUriHandler.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.clickable { uriHandler.openUri("https://github.com/simplex-chat/simplex-chat/blob/stable/docs/WEBRTC.md#configure-mobile-apps") }
  ) {
    Text(stringResource(R.string.how_to), color = MaterialTheme.colors.primary)
    Icon(
      Icons.Outlined.OpenInNew, stringResource(R.string.how_to), tint = MaterialTheme.colors.primary,
      modifier = Modifier.padding(horizontal = 5.dp)
    )
  }
}
