package chat.echo.app.views.helpers.alerts

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import chat.echo.app.R
import chat.echo.app.views.helpers.generalGetString

@Composable
fun WarningDialogLayout(modifier: Modifier = Modifier,
  title: String,
  message: String,
  cancelText: String = generalGetString(R.string.cancel_verb),
  ok: String = generalGetString(R.string.ok),
  onCancelActon: () -> Unit = {},
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
          fontSize = 16.sp,
          color = Color.Black
        )
        Spacer(modifier = Modifier.padding(top = 10.dp))
        Text(
          text = message,
          modifier = Modifier
            .padding(top = 5.dp)
            .fillMaxWidth(),
          fontWeight = FontWeight.Normal,
          fontSize = 14.sp,
          color = Color.Black
        )
      }

      Spacer(modifier = Modifier.padding(top = 5.dp))
      //.......................................................................
      Row(
        Modifier
          .fillMaxWidth()
          .padding(top = 10.dp, bottom = 15.dp),
        horizontalArrangement = Arrangement.SpaceAround) {

        TextButton(onClick = onCancelActon) {
          Text(
            cancelText,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
          )
        }
        TextButton(onClick = onOKAction) {
          Text(
            ok,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
          )
        }
      }
    }
  }
}

@SuppressLint("UnrememberedMutableState")
@Preview (name="Custom Dialog")
@Composable
fun MyDialogUIPreview(){
  //CustomDialog(openDialogCustom = mutableStateOf(false))
}
