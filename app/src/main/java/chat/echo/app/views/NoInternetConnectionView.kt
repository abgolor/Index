package chat.echo.app.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import chat.echo.app.R
import chat.echo.app.views.helpers.generalGetString

@Composable
fun NoInternetConnectionView(
) {
    Column(
      Modifier
        .background(MaterialTheme.colors.background)
        .fillMaxSize()
      //.padding(40.dp)
    ) {
      Image(
        painter = painterResource(R.drawable.index_logo),
        contentDescription = generalGetString(R.string.index_icon),
        modifier = Modifier
          .height(214.dp)
          .width(179.dp)
          .align(Alignment.CenterHorizontally)
      )
      Spacer(Modifier.height(10.dp))
      Image(
        painter = painterResource(R.drawable.ic_open_key_chain),
        contentDescription = generalGetString(R.string.open_key_chain_icon),
        modifier = Modifier
          .height(214.dp)
          .width(179.dp)
          .align(Alignment.CenterHorizontally)
        // .padding(10.dp)
      )
      Spacer(Modifier.fillMaxHeight().weight(1f))
      Column(Modifier.align(Alignment.CenterHorizontally)) {
        Row(Modifier.align(Alignment.CenterHorizontally)) {
          Text(
            text = generalGetString(R.string.no_internet_connection)
          )
          Spacer(modifier = Modifier.width(10.dp))
          CircularProgressIndicator(
            Modifier.size(14.dp)
              .align(Alignment.CenterVertically),
            color = MaterialTheme.colors.onSurface
          )
        }
        Spacer(modifier = Modifier.height(30.dp))
      }
    }
  }