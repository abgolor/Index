package chat.echo.app.views.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import chat.echo.app.R
import chat.echo.app.model.Chat
import chat.echo.app.model.ChatModel
import chat.echo.app.ui.theme.DeleteRed
import chat.echo.app.ui.theme.PreviewTextColor
import chat.echo.app.views.chat.deleteContactDialog
import chat.echo.app.views.chatlist.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.navbar.*
import com.google.accompanist.insets.ProvideWindowInsets
import de.charlex.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchView(chatModel: ChatModel, close: () -> Unit){

  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

  BackHandler() {
    close()
    focusManager.clearFocus()
  }

  Scaffold(
    backgroundColor = Color.White,
    drawerGesturesEnabled = true,
    bottomBar = { BottomSearchBar(chatModel.search, focusRequester) }
  ) {
    Box(
      Modifier.padding(it)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
          .background(Color.White)
      ) {
        Column(Modifier.fillMaxWidth()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
          ) {
            IconButton(close) {
              Icon(
                Icons.Outlined.Close, stringResource(R.string.back), tint = Color.Black,
                modifier = Modifier.size(22.dp)
              )
            }
            Text(
              text = generalGetString(R.string.search),
              fontSize = 18.sp,
              modifier = Modifier.align(Alignment.Center),
              fontWeight = FontWeight.Medium,
              color = Color.Black
            )
          }
          Divider(color = PreviewTextColor)
          SearchViewLayout(chatModel = chatModel, search = chatModel.search)
        }
      }
    }
  }

  LaunchedEffect(Unit){
    delay(300)
    focusRequester.requestFocus()
  }
}

@Composable
fun SearchViewLayout(chatModel: ChatModel, search: MutableState<TextFieldValue>){
Column() {
  Row(
    Modifier.fillMaxWidth()
      .padding(top = 10.dp, start = 10.dp, end = 10.dp)
  ) {
    Text(
      text = generalGetString(R.string.contacts),
      modifier = Modifier
        .align(Alignment.CenterVertically),
      fontSize = 18.sp,
      fontWeight = FontWeight.Medium,
      color = Color.Black
    )
  }
  Box(
    Modifier.fillMaxSize()
      .padding(bottom = 15.dp)
  ) {
    if(search.value.text == "" || chatModel.chats.size == 0){
      Column(Modifier.align(Alignment.Center)) {
        Text(
          text = generalGetString(R.string.search_instructions),
          fontSize = 16.sp,
          fontWeight = FontWeight.Normal,
          color = Color.Black,
          textAlign = TextAlign.Center
        )
      }
    } else {
      ContactList(chatModel = chatModel, search = search.value.text)
    }
  }
}
}