package chat.echo.app.views.chatlist

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.ui.theme.SimpleXTheme
import chat.echo.app.views.helpers.annotatedStringResource
import chat.echo.app.views.usersettings.simplexTeamUri

val bold = SpanStyle(fontWeight = FontWeight.Bold)

@Composable
fun ChatHelpView(addContact: (() -> Unit)? = null) {
  Column(
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    val uriHandler = LocalUriHandler.current

    Text(stringResource(R.string.thank_you_for_installing_simplex), lineHeight = 22.sp)
    Text(
      annotatedStringResource(R.string.you_can_connect_to_simplex_chat_founder),
      modifier = Modifier.clickable(onClick = {
        uriHandler.openUri(simplexTeamUri)
      }),
      lineHeight = 22.sp
    )

    Column(
      Modifier.padding(top = 24.dp),
      horizontalAlignment = Alignment.Start,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(
        stringResource(R.string.to_start_a_new_chat_help_header),
        style = MaterialTheme.typography.h2,
        lineHeight = 22.sp
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(stringResource(R.string.chat_help_tap_button))
        Icon(
          Icons.Outlined.PersonAdd,
          stringResource(R.string.add_contact),
          modifier = if (addContact != null) Modifier.clickable(onClick = addContact) else Modifier,
        )
        Text(stringResource(R.string.above_then_preposition_continuation))
      }
      Text(annotatedStringResource(R.string.add_new_contact_to_create_one_time_QR_code), lineHeight = 22.sp)
      Text(annotatedStringResource(R.string.scan_QR_code_to_connect_to_contact_who_shows_QR_code), lineHeight = 22.sp)
    }

    Column(
      Modifier.padding(top = 24.dp),
      horizontalAlignment = Alignment.Start,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(stringResource(R.string.to_connect_via_link_title), style = MaterialTheme.typography.h2)
      Text(stringResource(R.string.if_you_received_simplex_invitation_link_you_can_open_in_browser), lineHeight = 22.sp)
      Text(annotatedStringResource(R.string.desktop_scan_QR_code_from_app_via_scan_QR_code), lineHeight = 22.sp)
      Text(annotatedStringResource(R.string.mobile_tap_open_in_mobile_app_then_tap_connect_in_app), lineHeight = 22.sp)
    }
  }
}

@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewChatHelpLayout() {
  SimpleXTheme {
    ChatHelpView {}
  }
}
