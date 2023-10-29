package chat.echo.app.views.chat.item

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.ChatItem
import chat.echo.app.ui.theme.SimpleXTheme
import chat.echo.app.views.helpers.AlertManager
import chat.echo.app.views.helpers.generalGetString

@Composable
fun IntegrityErrorItemView(ci: ChatItem, timedMessagesTTL: Int?, showMember: Boolean = false) {
  Surface(
    Modifier.clickable(onClick = {
      AlertManager.shared.showAlertMsg(
        title = generalGetString(R.string.alert_title_skipped_messages),
        text = generalGetString(R.string.alert_text_skipped_messages_it_can_happen_when)
      )
    }),
    shape = RoundedCornerShape(18.dp),
    color = ReceivedColorLight,
  ) {
    Row(
      Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      Text(
        buildAnnotatedString {
          appendSender(this, if (showMember) ci.memberDisplayName else null, true)
          withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Red)) { append(ci.content.text) }
        },
        style = MaterialTheme.typography.body1.copy(lineHeight = 22.sp),
        modifier = Modifier.padding(end = 8.dp)
      )
      CIMetaView(ci, timedMessagesTTL)
    }
  }
}

@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  name = "Dark Mode"
)
@Composable
fun IntegrityErrorItemViewPreview() {
  SimpleXTheme {
    IntegrityErrorItemView(
      ChatItem.getDeletedContentSampleData(),
      null
    )
  }
}
