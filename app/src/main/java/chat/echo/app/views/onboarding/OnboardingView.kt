package chat.echo.app.views.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.echo.app.model.ChatModel
import chat.echo.app.views.CreateProfilePanel
import chat.echo.app.views.CreateProfileView
import chat.echo.app.views.helpers.getKeyboardState
import com.google.accompanist.insets.ProvideWindowInsets
import kotlinx.coroutines.launch

enum class OnboardingStage {
  NewUser,
  About,
  NotificationPermission,
  RegistrationComplete,
  SigningIn,
  NoInternetConnection
}

@Composable
fun CreateProfile(chatModel: ChatModel) {
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
     CreateProfileView(chatModel = chatModel)
      //CreateProfilePanel(chatModel)
      if (savedKeyboardState != keyboardState) {
        LaunchedEffect(keyboardState) {
          scope.launch {
            savedKeyboardState = keyboardState
            scrollState.animateScrollTo(scrollState.value)
          }
        }
      }
    }
  }
}
