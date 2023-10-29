package chat.echo.app.views.navbar

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import chat.echo.app.R
import chat.echo.app.model.ChatModel
import chat.echo.app.ui.theme.*
import chat.echo.app.views.CustomTextInputFieldView
import chat.echo.app.views.helpers.ModalManager
import chat.echo.app.views.helpers.generalGetString
import chat.echo.app.views.newchat.ConnectByIndexCodeTab
import chat.echo.app.views.newchat.CreateUserLinkView
import chat.echo.app.views.search.SearchView
import chat.echo.app.views.usersettings.CurrentPage
import kotlinx.coroutines.delay

@Composable
fun BottomBar(
  navController: NavHostController,
  chatModel: ChatModel
) {
  val screens = listOf(
    BottomBarView.Contacts,
    BottomBarView.Chats
  )
  val navStackbackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navStackbackEntry?.destination
  val search = remember {
    mutableStateOf("")
  }
  val focusRequester = FocusRequester()
  val focusManager = LocalFocusManager.current

  Column(
    Modifier.fillMaxWidth().background(Color.White)
  ) {
    Card(
      shape = RoundedCornerShape(10.dp),
      backgroundColor = MaterialTheme.colors.background,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(start = 30.dp, end = 30.dp)
        .fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(40.dp)
          .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_search),
          contentDescription = generalGetString(R.string.search),
          tint = Color.White,
          modifier = Modifier
            .size(15.dp)
        )
        Spacer(
          modifier = Modifier
            .padding(end = 5.dp)
        )
        BasicTextField(
          value = search.value,
          onValueChange = { search.value = it },
          decorationBox = { innerTextField ->
            Box(
              contentAlignment = Alignment.CenterStart
            ) {
              if (search.value.isEmpty()) {
                Text(
                  text = generalGetString(R.string.search),
                  color = PreviewTextColor,
                  fontSize = 12.sp
                )
              }
              innerTextField()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .focusRequester(focusRequester)
            .onFocusChanged {
              if (it.hasFocus) {
                focusManager.clearFocus()
                chatModel.showSearchView.value = true
              }
            },
          textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onSurface),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false
          ),
          singleLine = true,
          cursorBrush = SolidColor(HighOrLowlight)
        )
        Spacer(
          modifier = Modifier
            .padding(end = 5.dp)
        )
        IconButton(onClick = {
          ModalManager.shared.showCustomModal {close ->
            CreateUserLinkView(model = chatModel, initialSelection = 0, pasteInstead = false, close = close)
          }
        }) {
          Icon(
            painter = painterResource(id = R.drawable.ic_scan_code),
            contentDescription = "icon",
            tint = Color.White,
            modifier = Modifier
              .size(15.dp)
          )
        }
      }
    }
    Spacer(modifier = Modifier.padding(top = 15.dp))
    Card(
      shape = RoundedCornerShape(100.dp),
      backgroundColor = MaterialTheme.colors.background,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(start = 70.dp, end = 70.dp)
        .fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        screens.forEach { screen ->
          AddItem(
            screen = screen,
            currentDestination = currentDestination,
            navController = navController
          )
        }
      }
    }
    Spacer(modifier = Modifier.padding(top = 10.dp))
  }
}

@Composable
fun BottomSearchBar(search: MutableState<TextFieldValue>, focusRequester: FocusRequester) {
  Column(
    Modifier.fillMaxWidth()
  ) {
    Card(
      shape = RoundedCornerShape(10.dp),
      backgroundColor = MaterialTheme.colors.background,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(start = 20.dp, end = 20.dp)
        .fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(45.dp)
          .padding(start = 10.dp, end = 5.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_search),
          contentDescription = "icon",
          tint = Color.White,
          modifier = Modifier
            .size(20.dp)
        )
        Spacer(
          modifier = Modifier
            .padding(end = 5.dp)
        )
        BasicTextField(
          value = search.value,
          onValueChange = {
            search.value = it
          },
          decorationBox = { innerTextField ->
            Box(
              contentAlignment = Alignment.CenterStart
            ) {
              if (search.value.text.isEmpty()) {
                Text(
                  text = generalGetString(R.string.search),
                )
              }
              innerTextField()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .focusRequester(focusRequester),
          textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onSurface),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false
          ),
          singleLine = true,
          cursorBrush = SolidColor(HighOrLowlight)
        )
      }
    }
    Spacer(modifier = Modifier.padding(top = 15.dp))
  }
}

@Composable
fun AddItem(
  screen: BottomBarView,
  currentDestination: NavDestination?,
  navController: NavHostController
) {
  val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
  val alpha = if (selected) 1f else ContentAlpha.disabled
  val contentColor = Color.White

  Icon(
    painter = painterResource(id = screen.icon),
    contentDescription = "icon",
    tint = contentColor,
    modifier = Modifier
      .padding(start = 10.dp, end= 10.dp,  top = 8.dp, bottom = 8.dp)
      .size(40.dp)
      .alpha(alpha)
      .clickable(onClick = {
        navController.navigate(screen.route) {
          popUpTo(navController.graph.findStartDestination().id)
          launchSingleTop = true
        }
      })
  )
}
