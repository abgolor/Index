package chat.echo.app.views

import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import chat.echo.app.*
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.HighOrLowlight
import chat.echo.app.views.helpers.*
import chat.echo.app.views.onboarding.ReadableText
import com.google.accompanist.insets.ProvideWindowInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun isPasswordOneCorrect(passwordOne: String): Boolean {
  return passwordOne.isNotEmpty() && passwordOne == AppPreferences(SimplexApp.context).passwordOne.get().toString()
}

fun isPasswordTwoCorrect(passwordTwo: String): Boolean {
  return passwordTwo.isNotEmpty() && passwordTwo == AppPreferences(SimplexApp.context).passwordTwo.get().toString()
}

sealed class AuthenticationResult {
  object Success: AuthenticationResult()
  object Whatsapp: AuthenticationResult()
  object Failed: AuthenticationResult()
}

@Composable
fun TwoAppPasswordView(
  action: (AuthenticationResult) -> Unit
) {
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  val keyboardState by getKeyboardState()
  var savedKeyboardState by remember { mutableStateOf(keyboardState) }
  val password = remember { mutableStateOf("") }

  ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colors.background)
        .padding(20.dp)
    ) {
      TwoAppPasswordPanel(
        password = password,
        action
      )
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
fun TwoAppPasswordPanel(
  password: MutableState<String>,
  action: (AuthenticationResult) -> Unit
) {
  val focusRequester = remember { FocusRequester() }
  Surface(Modifier.background(MaterialTheme.colors.onBackground)) {
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
      AppBarTitle(stringResource(R.string.two_app_password), false)
      ReadableText(R.string.two_fa_authentication_explanation)
      Spacer(Modifier.height(10.dp))
      CustomPasswordInputFieldView(password, stringResource(R.string.enter_app_password), true, ImeAction.Done, keyboardAction = { authenticateAppPassword(password.value, action) },  focusRequester)
      Spacer(Modifier.fillMaxHeight().weight(1f))
      Row {
        Spacer(Modifier.fillMaxWidth().weight(1f))
        val enabled = (password.value.isNotEmpty() && password.value.trim() != "")
        val createModifier: Modifier
        val createColor: Color
        if (enabled) {
         createModifier = Modifier.clickable {
           authenticateAppPassword(password.value, action)
         }.padding(8.dp)
          createColor = MaterialTheme.colors.primary
        } else {
          createModifier = Modifier.padding(8.dp)
          createColor = HighOrLowlight
        }
        Surface(shape = RoundedCornerShape(20.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = createModifier) {
            Text(stringResource(R.string.authenticate), style = MaterialTheme.typography.caption, color = createColor)
            Icon(Icons.Outlined.ArrowForwardIos, stringResource(R.string.authenticate), tint = createColor)
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

fun authenticateAppPassword(
  password: String,
  action: (AuthenticationResult) -> Unit
) {
  if (isPasswordOneCorrect(password)) {
    action(AuthenticationResult.Success)
  }  else if (isPasswordTwoCorrect(password)){
    action(AuthenticationResult.Whatsapp)
  } else {
    action(AuthenticationResult.Failed)
  }
}

fun authenticateAppPassword(userAuthorized: MutableState<Boolean?>, password: String) {
  if (isPasswordOneCorrect(password)) {
    userAuthorized.value = true
    ModalManager.shared.closeModals()
  } else if (isPasswordTwoCorrect(password)) {
    val whatsappIntent = SimplexApp.context.packageManager.getLaunchIntentForPackage("com.whatsapp")
    val whatsappBusinessIntent = SimplexApp.context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
    if (whatsappIntent != null) {
      whatsappIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
      SimplexApp.context.startActivity(whatsappIntent)
    } else if (whatsappBusinessIntent != null) {
      whatsappBusinessIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
      SimplexApp.context.startActivity(whatsappBusinessIntent)
    } else {
      AlertManager.shared.showAlertMsg("Whatsapp Not Installed", "There is no whatsapp or whatsapp business installed on your device.", onConfirm = {
        AlertManager.shared.hideAlert()
      })
    }
  } else {
    AlertManager.shared.showAlertMsg("Operation Failed", "Incorrect password, please try again!", onConfirm = {
      AlertManager.shared.hideAlert()
    })
  }
}

@Composable
fun TwoAppPasswordField(password: MutableState<String>, hint: String, focusRequester: FocusRequester? = null) {
  var passwordVisible by remember { mutableStateOf(false) }
  val modifier = Modifier
    .padding(bottom = 15.dp)
    .fillMaxWidth()
  OutlinedTextField(
    value = password.value,
    trailingIcon = {
      val image = if (passwordVisible) Icons.Filled.Visibility
      else Icons.Filled.VisibilityOff
      val description = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
      IconButton(onClick = {
        passwordVisible = !passwordVisible
      }) {
        Icon(imageVector = image, contentDescription = description)
      }
    },
    visualTransformation =
    if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    onValueChange = {
      password.value = it
    },
    placeholder = { Text(text = hint) },
    textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground), keyboardOptions = KeyboardOptions(
      capitalization = KeyboardCapitalization.None,
      autoCorrect = false
    ),
    singleLine = true,
    modifier = if (focusRequester == null) modifier else modifier.focusRequester(focusRequester)
  )
}