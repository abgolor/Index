package chat.echo.app.views.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIos
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.views.helpers.generalGetString

@Composable
fun CustomTopBarCloseableView(
  close: () -> Unit,
  title: String,
  content: @Composable()
  (ColumnScope.() -> Unit)
){

  Column(Modifier.fillMaxWidth()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
    ) {
      IconButton(close) {
        Icon(
          Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
          modifier = Modifier.size(20.dp)
        )
      }
      Text(
        text = title,
        fontSize = 16.sp,
        modifier = Modifier.align(Alignment.Center),
        fontWeight = FontWeight.Medium,
        color = Color.Black
      )
    }
    content
  }
}
