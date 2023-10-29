package chat.echo.app.views.helpers.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.views.helpers.generalGetString

@Composable
fun NoticeDialogLayout(modifier: Modifier = Modifier,
  title: String,
  message: String? = null,
  ok: String = generalGetString(R.string.ok),
  onOKAction: () -> Unit = {}){
  Card(
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .padding(10.dp,5.dp,10.dp,10.dp),
    elevation = 8.dp
  ) {
    Column(
      modifier
        .background(Color.White)) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = title,
          modifier = Modifier
            .padding(top = 5.dp)
            .fillMaxWidth(),
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Start,
          fontSize = 16.sp,
          color = Color.Black
        )
        Spacer(modifier = Modifier.padding(top = 10.dp))
        if (message != null) {
          Text(
            text = message,
            modifier = Modifier
              .padding(top = 5.dp)
              .fillMaxWidth(),
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start,
            fontSize = 14.sp,
            color = Color.Black
          )
        }
      }

      //.......................................................................
      Row(
        Modifier
          .fillMaxWidth()
          .padding(top = 10.dp, bottom = 10.dp, end = 10.dp, start = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround) {
        Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
        TextButton(onClick = onOKAction) {
          Text(
            ok,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
          )
        }
      }
    }
  }
}