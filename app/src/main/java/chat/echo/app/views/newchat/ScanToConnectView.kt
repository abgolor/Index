package chat.echo.app.views.newchat

import android.Manifest
import android.content.res.Configuration
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import chat.echo.app.R
import chat.echo.app.model.ChatModel
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.navbar.BottomBarView
import com.google.accompanist.permissions.rememberPermissionState



@Composable
fun ScanToConnectView(chatModel: ChatModel,  isPasteInstead: MutableState<Boolean>,  close: () -> Unit) {
  val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
  LaunchedEffect(Unit) {
    cameraPermissionState.launchPermissionRequest()
  }
  ConnectContactLayout(
    qrCodeScanner = {
      QRCodeScanner { connReqUri ->
        try {
          val uri = Uri.parse(connReqUri)
          withUriAction(uri) { action ->
            if (connectViaUri(chatModel, action, uri)) {
              close()
            }
          }
        } catch (e: RuntimeException) {
          AlertManager.shared.showAlertMsg(
            title = generalGetString(R.string.invalid_QR_code),
            text = generalGetString(R.string.this_QR_code_is_not_a_link)
          )
        }
      }
    },
    isPasteInstead = isPasteInstead
  )
}

fun withUriAction(uri: Uri, run: suspend (String) -> Unit) {
  val action = uri.path?.drop(1)?.replace("/", "")
  if (action == "contact" || action == "invitation") {
    withApi { run(action) }
  } else {
    AlertManager.shared.showAlertMsg(
      title = generalGetString(R.string.invalid_contact_link),
      text = generalGetString(R.string.this_link_is_not_a_valid_connection_link)
    )
  }
}

suspend fun connectViaUri(chatModel: ChatModel, action: String, uri: Uri): Boolean {
  val r = chatModel.controller.apiConnect(uri.toString())
  if (r) {
    AlertManager.shared.showAlertMsg(
      title = generalGetString(R.string.connection_request_sent),
      text =
        if (action == "contact") generalGetString(R.string.you_will_be_connected_when_your_connection_request_is_accepted)
        else generalGetString(R.string.you_will_be_connected_when_your_contacts_device_is_online)
    )
  }
  return r
}

@Composable
fun ConnectContactLayout(
  qrCodeScanner: @Composable () -> Unit,
  isPasteInstead: MutableState<Boolean>
) {
  Column(
    Modifier
      .fillMaxSize(),
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.Center
  ) {

    Box(modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center){
      Column(Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center){
        Box(
          Modifier
            .size(300.dp)
            .aspectRatio(1f)
        ) { qrCodeScanner() }
        Text(
          text = generalGetString(R.string.scan_an_index_code_instruction),
          textAlign = TextAlign.Center,
          fontSize = 14.sp,
          modifier = Modifier
            .padding(top = 20.dp),
          fontWeight = FontWeight.Normal,
          color = HighOrLowlight
        )
        Text(
          text = generalGetString(R.string.paste_link_instead),
          fontSize = 14.sp,
          fontWeight = FontWeight.Normal,
          color = Color.Black,
          textDecoration = TextDecoration.Underline,
          modifier = Modifier.clickable {
            isPasteInstead.value = true
          }
            .padding(vertical = 20.dp)
        )
      }
    }
 /*   Column(
      Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
    }*/
  }
}

/*
@Composable
fun ConnectContactLayout(chatModelIncognito: Boolean, qrCodeScanner: @Composable () -> Unit) {
  Column(
    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = DEFAULT_PADDING),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    AppBarTitle(stringResource(R.string.scan_QR_code), false)
    InfoAboutIncognito(
      chatModelIncognito,
      true,
      generalGetString(R.string.incognito_random_profile_description),
      generalGetString(R.string.your_profile_will_be_sent)
    )
    Box(
      Modifier
        .fillMaxWidth()
        .aspectRatio(ratio = 1F)
    ) { qrCodeScanner() }
    Text(
      annotatedStringResource(R.string.if_you_cannot_meet_in_person_scan_QR_in_video_call_or_ask_for_invitation_link),
      lineHeight = 22.sp
    )
  }
}
*/
