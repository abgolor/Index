package chat.echo.app.views.newchat

import android.app.Activity
import android.content.*
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
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
import androidx.core.content.ContextCompat.getSystemService
import chat.echo.app.R
import chat.echo.app.TAG
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chat.item.ItemAction
import chat.echo.app.views.helpers.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Composable
fun PasteToConnectView(chatModel: ChatModel, isPasteInstead: MutableState<Boolean>, close: () -> Unit) {
  val connectionLink = remember { mutableStateOf("") }
  val context = LocalContext.current
  val clipboard = getSystemService(context, ClipboardManager::class.java)
  PasteToConnectLayout(
    connectionLink = connectionLink,
    isPasteInstead = isPasteInstead,
    pasteFromClipboard = {
      connectionLink.value = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context) as? String ?: return@PasteToConnectLayout
    },
    connectViaLink = { connReqUri ->
      try {
        val uri = Uri.parse(connReqUri)
        withUriAction(uri) { action ->
          if (connectViaUri(chatModel, action, uri)){
            close()
          }
        }
      }
      catch (e: RuntimeException) {
        AlertManager.shared.showAlertMsg(
          title = generalGetString(R.string.invalid_connection_link),
          text = generalGetString(R.string.this_string_is_not_a_connection_link)
        )
      }
    },
  )
}

@Composable
fun PasteToConnectLayout(
  connectionLink: MutableState<String>,
  isPasteInstead: MutableState<Boolean>,
  pasteFromClipboard: () -> Unit,
  connectViaLink: (String) -> Unit,
) {
  Column(
    Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = DEFAULT_PADDING),
    horizontalAlignment = Alignment.Start,
  ) {
    Box(Modifier.padding(top = 16.dp, bottom = 6.dp)) {
      TextEditor(Modifier.height(180.dp), text = connectionLink)
    }
    Row(
      Modifier.fillMaxWidth().padding(vertical = 20.dp),
      horizontalArrangement = Arrangement.Start,
    ) {
      if (connectionLink.value == "") {
        Text(
          text = generalGetString(R.string.paste_button),
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          color = Color.Black,
          textDecoration = TextDecoration.Underline,
          modifier = Modifier.clickable {
            pasteFromClipboard()
          }
        )
      } else {
        Text(
          text = generalGetString(R.string.clear_verb),
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          color = Color.Black,
          textDecoration = TextDecoration.Underline,
          modifier = Modifier.clickable {
            connectionLink.value = ""
          }
        )
      }
      Spacer(Modifier.weight(1f).fillMaxWidth())
      Text(
        text = generalGetString(R.string.connect_button),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable {
          connectViaLink(connectionLink.value)
        }
      )
    }
    Text(
      text = generalGetString(R.string.paste_an_index_link_instruction),
      textAlign = TextAlign.Center,
      fontSize = 14.sp,
      fontWeight = FontWeight.Normal,
      color = HighOrLowlight
    )
    Text(
      text = generalGetString(R.string.scan_qr_code_instead),
      fontSize = 14.sp,
      fontWeight = FontWeight.Normal,
      color = Color.Black,
      textAlign = TextAlign.Center,
      textDecoration = TextDecoration.Underline,
      modifier = Modifier.clickable {
        isPasteInstead.value = false
      }
        .fillMaxWidth()
        .padding(top = 20.dp)
    )


    //Text(annotatedStringResource(R.string.you_can_also_connect_by_clicking_the_link))
  }
}


/*@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  name = "Dark Mode"
)
@Composable
fun PreviewPasteToConnectTextbox() {
  SimpleXTheme {
    PasteToConnectLayout(
      chatModelIncognito = false,
      connectionLink = remember { mutableStateOf("") },
      pasteFromClipboard = {},
      connectViaLink = { link ->
        try {
          println(link)
  //        withApi { chatModel.controller.apiConnect(link) }
        } catch (e: Exception) {
          e.printStackTrace()
        }
      },
    )
  }
}*/
