package chat.echo.app.views.usersettings

import chat.echo.app.R
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import chat.echo.app.ui.theme.DEFAULT_PADDING
import chat.echo.app.ui.theme.SimpleXTheme
import chat.echo.app.views.chatlist.ChatHelpView
import chat.echo.app.views.helpers.AppBarTitle

@Composable
fun HelpView(userDisplayName: String) {
  HelpLayout(userDisplayName)
}

@Composable
fun HelpLayout(userDisplayName: String) {
  Column(
    Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = DEFAULT_PADDING),
    horizontalAlignment = Alignment.Start
  ){
    AppBarTitle(String.format(stringResource(R.string.personal_welcome), userDisplayName), false)
    ChatHelpView()
  }
}

@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewHelpView() {
  SimpleXTheme {
    HelpLayout("Alice")
  }
}
