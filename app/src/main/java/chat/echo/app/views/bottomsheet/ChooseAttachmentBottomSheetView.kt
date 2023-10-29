package chat.echo.app.views.bottomsheet

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import chat.echo.app.R
import chat.echo.app.views.helpers.AttachmentOption
import chat.echo.app.views.helpers.generalGetString
import chat.echo.app.views.newchat.ActionButton

@Composable
fun ChooseAttachmentBottomSheetView(
  attachmentOption: MutableState<AttachmentOption?>,
  hide: () -> Unit){
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(color = Color.White)
      .wrapContentHeight()
      .onFocusChanged { focusState ->
        if (!focusState.hasFocus) hide()
      }
  ) {
    Column(
      Modifier
        .fillMaxWidth()
        .padding(15.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
      ) {
        IconButton(hide) {
          Icon(
            Icons.Outlined.Close, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
        }
        Text(
          text = generalGetString(R.string.choose_attachment),
          fontSize = 16.sp,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.SemiBold,
          color = Color.Black
        )
      }
      Row(
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 30.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        ActionButton(null, stringResource(R.string.use_camera_button), icon = Icons.Outlined.PhotoCamera) {
          attachmentOption.value = AttachmentOption.CameraPhoto
          hide()
        }
        ActionButton(null, stringResource(R.string.image), icon = Icons.Outlined.Collections) {
          attachmentOption.value = AttachmentOption.GalleryImage
          hide()
        }
        ActionButton(null, stringResource(R.string.video), icon = Icons.Outlined.Collections) {
          attachmentOption.value = AttachmentOption.GalleryVideo
          hide()
        }
        ActionButton(null, stringResource(R.string.file), icon = Icons.Outlined.InsertDriveFile) {
          attachmentOption.value = AttachmentOption.File
          hide()
        }
      }
    }
  }
}