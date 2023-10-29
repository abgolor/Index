package chat.echo.app.views.helpers

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import chat.echo.app.R
import chat.echo.app.model.ChatModel
import chat.echo.app.ui.theme.PreviewTextColor
import chat.echo.app.views.helpers.alerts.*
import chat.echo.app.views.newchat.*
import chat.echo.app.views.newchat.group.SelectGroupTypeView
import chat.echo.app.views.usersettings.SettingsActionItem
import kotlinx.coroutines.launch

@Composable
fun AddContactsBottomSheetView(
  hideBottomSheet: () -> Unit,
  chatModel: ChatModel
) {
  val showModal:  (@Composable (ChatModel) -> Unit) -> () -> Unit =
    { modalView -> { ModalManager.shared.showModal { modalView(chatModel) } } }

  val showCustomModal:  (@Composable (ChatModel, () -> Unit) -> Unit) -> () -> Unit =
    { modalView -> {
      hideBottomSheet()
      ModalManager.shared.showCustomModal { close ->  modalView(chatModel, close) }
    } }

  val showModalCloseable:  (@Composable (ChatModel, () -> Unit) -> Unit) -> () -> Unit =
    { modalView -> { ModalManager.shared.showModalCloseable { close ->  modalView(chatModel, close) } } }

  val showCustomDialog = remember{mutableStateOf(false)}

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
          text = generalGetString(R.string.add_new_contacts),
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
          Icons.Outlined.QrCode,
          stringResource(R.string.create_link_qr_code),
          showCustomModal { chatModel, close ->
                          CreateUserLinkView(chatModel, 1, false, close)
                    },
          Color.Black,
          Color.Black
        )
        Divider(color = PreviewTextColor)
        SettingsActionItem(
          Icons.Outlined.Share,
          stringResource(R.string.connect_via_link),
          showCustomModal {chatModel, close ->  CreateUserLinkView(chatModel, 0, true, close) },
          Color.Black,
          Color.Black
        )
        Divider(color = PreviewTextColor)
        SettingsActionItem(
          Icons.Outlined.QrCodeScanner,
          stringResource(R.string.scan_QR_code),
          showCustomModal { chatModel, close ->
            CreateUserLinkView(model = chatModel, initialSelection = 0, pasteInstead = false, close = close)
          },
          Color.Black,
          Color.Black
        )
        Divider(color = PreviewTextColor)
        SettingsActionItem(
          Icons.Outlined.Groups,
          stringResource(R.string.create_secret_group_title),
          {
            chatModel.isGroupCreated.value = false
            chatModel.selectedGroupContacts.value = mutableStateListOf<Long>()
            ModalManager.shared.showCustomModal { close -> SelectGroupTypeView(chatModel, close) }
          },
          Color.Black,
          Color.Black
        )
      }
    }
  }
}