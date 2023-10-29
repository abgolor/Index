package chat.echo.app.views.helpers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.ui.theme.DividerColor

@Composable
fun SubHeaderSettings(title: String) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 10.dp, end = 10.dp),
    backgroundColor = DividerColor,
    shape = RoundedCornerShape(5.dp)
  ) {
    Row(Modifier.padding(5.dp)) {
      Text(
        text = title,
        fontSize = 16.sp,
        color = Color.Black,
        modifier = Modifier
          .fillMaxWidth()
          .alpha(0.5f),
        fontWeight = FontWeight.Medium
      )
    }
  }
}

@Composable
fun ServerInformationHeader(title: String) {
  Card(
    modifier = Modifier
      .fillMaxWidth(),
    backgroundColor = DividerColor,
    shape = RoundedCornerShape(5.dp)
  ) {
    Row(Modifier.padding(start= 10.dp, top = 5.dp, end = 10.dp, bottom = 5.dp)) {
      Text(
        text = title,
        fontSize = 14.sp,
        color = Color.Black,
        modifier = Modifier
          .fillMaxWidth()
          .alpha(0.5f),
        fontWeight = FontWeight.Medium
      )
    }
  }
}

@Composable
fun SubHeaderContacts(title: String) {
  Card(
    modifier = Modifier
      .fillMaxWidth(),
    backgroundColor = DividerColor,
    shape = RoundedCornerShape(0.dp)
  ) {
    Row(
      Modifier.padding(start = 10.dp, end = 10.dp)
    ) {
      Text(
        text = title,
        fontSize = 10.sp,
        color = Color.Black,
        modifier = Modifier
          .fillMaxWidth()
          .alpha(0.5f),
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}