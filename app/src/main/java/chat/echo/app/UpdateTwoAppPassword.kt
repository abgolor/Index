package chat.echo.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import chat.echo.app.model.AppPreferences
import chat.echo.app.model.ChatModel
import chat.echo.app.ui.theme.HighOrLowlight
import chat.echo.app.views.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.onboarding.ReadableText
import com.google.accompanist.insets.ProvideWindowInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UpdateTwoAppPasswordView() {
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  val keyboardState by getKeyboardState()
  var savedKeyboardState by remember { mutableStateOf(keyboardState) }

  ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colors.background)
        .padding(20.dp)
    ) {
      UpdateTwoAppPasswordPanel()
      if (savedKeyboardState != keyboardState) {
        LaunchedEffect(keyboardState) {
          scope.launch {
            savedKeyboardState = keyboardState
            scrollState.animateScrollTo(scrollState.maxValue)
          }
        }
      }
    }
  }
}

@Composable
fun UpdateTwoAppPasswordPanel() {
  val passwordOne = remember { mutableStateOf("") }
  val passwordTwo = remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }

  Surface(Modifier.background(MaterialTheme.colors.onBackground)) {
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
      AppBarTitle(stringResource(R.string.update_two_app_password), false)
      Spacer(Modifier.height(10.dp))
      Text(
        stringResource(R.string.first_app_password),
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(bottom = 5.dp)
      )
      PasswordField(passwordOne, stringResource(id = R.string.enter_first_app_password), focusRequester)
      Spacer(Modifier.height(3.dp))
      Text(
        stringResource(R.string.second_app_password),
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(bottom = 10.dp)
      )
      PasswordField(password = passwordTwo, hint = stringResource(id = R.string.enter_second_app_password))
      Spacer(Modifier.fillMaxHeight().weight(1f))
      Row {
        Spacer(Modifier.fillMaxWidth().weight(1f))
        val enabled = (isPasswordValid(passwordOne.value) && isPasswordValid(passwordTwo.value))
        val createModifier: Modifier
        val createColor: Color
        if (enabled) {
          createModifier = Modifier.clickable {
            updateAppPassword(passwordOne.value, passwordTwo.value)
          }.padding(8.dp)
          createColor = MaterialTheme.colors.primary
        } else {
          createModifier = Modifier.padding(8.dp)
          createColor = HighOrLowlight
        }
        Surface(shape = RoundedCornerShape(20.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = createModifier) {
            Text(stringResource(R.string.save), style = MaterialTheme.typography.caption, color = createColor)
            Icon(Icons.Outlined.ArrowForwardIos, stringResource(R.string.save), tint = createColor)
          }
        }
        Spacer(Modifier.fillMaxWidth().weight(1f))
      }

      LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
      }
    }
  }
}

fun updateAppPassword(password1: String, password2: String) {
  if(password1 != password2){
    AppPreferences(SimplexApp.context).passwordOne.set(password1)
    AppPreferences(SimplexApp.context).passwordTwo.set(password2)
    ModalManager.shared.closeModal()
  } else {
    AlertManager.shared.showAlertMsg("Invalid Password Pattern",
      "First app password must be different from second app password"){
      AlertManager.shared.hideAlert()
    }
  }
}
