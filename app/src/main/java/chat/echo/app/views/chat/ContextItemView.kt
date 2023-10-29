package chat.echo.app.views.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.HighOrLowlight
import chat.echo.app.ui.theme.SimpleXTheme
import chat.echo.app.views.chat.item.*
import kotlinx.datetime.Clock

@Composable
fun ContextItemView(
  contextItem: ChatItem,
  contextIcon: ImageVector,
  isZeroKnowledge: Boolean,
  cancelContextItem: () -> Unit
) {
  Log.i("TAG", "ContextItemView: is zero knowledge today? " + isZeroKnowledge)
  val sent = contextItem.chatDir.sent
  Row(
    Modifier
      .padding(top = 8.dp)
      .background(if (sent) Color.Black else Color.Black),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      Modifier
        .padding(vertical = 12.dp)
        .fillMaxWidth()
        .weight(1F),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        contextIcon,
        modifier = Modifier
          .padding(horizontal = 8.dp)
          .height(20.dp)
          .width(20.dp),
        contentDescription = stringResource(R.string.icon_descr_context),
        tint = Color.White,
      )
      MarkdownText(
        contextItem.text, contextItem.formattedText,
        sender =  if (isZeroKnowledge) contextItem.memberHiddenID  else contextItem.memberDisplayName, senderBold = true, maxLines = 3,
        modifier = Modifier.fillMaxWidth(),
        linkMode = SimplexLinkMode.DESCRIPTION,
      )
    }
    IconButton(onClick = cancelContextItem) {
      Icon(
        Icons.Outlined.Close,
        contentDescription = stringResource(R.string.cancel_verb),
        tint = Color.White,
        modifier = Modifier.padding(10.dp)
      )
    }
  }
}

/*@Preview
@Composable
fun PreviewContextItemView() {
  SimpleXTheme {
    ContextItemView(
      contextItem = ChatItem.getSampleData(1, CIDirection.DirectRcv(), Clock.System.now(), "hello"),
      contextIcon = Icons.Filled.Edit,
      cancelContextItem = {}
    )
  }
}*/
