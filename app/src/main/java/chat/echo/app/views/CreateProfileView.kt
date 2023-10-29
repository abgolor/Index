package chat.echo.app.views

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.TAG
import chat.echo.app.model.*
import chat.echo.app.ui.theme.buttonDisabled
import chat.echo.app.ui.theme.buttonEnabled
import chat.echo.app.views.helpers.*
import chat.echo.app.views.onboarding.OnboardingStage
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.launch

@Composable
fun CreateProfileView(chatModel: ChatModel) {
  val displayName = remember { mutableStateOf("") }
  val fullName = remember { mutableStateOf("") }
  val deletePhrase = remember {
    mutableStateOf("")
  }
  val EXTRA_PUBLIC_KEY = "public_key"
  val EXTRA_OPEN_KEY_CHAIN_ID = "open_key_chain_id"
  val focusRequester = remember { FocusRequester() }
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  //Create an empty chatBioInfo instance because this is a new contact.
  var contactBioInfo by remember {
    mutableStateOf(
      ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
    )
  }
  val bringIntoViewRequester = BringIntoViewRequester()
  val modifier = Modifier
    .fillMaxWidth()
    .background(
      MaterialTheme.colors.secondary,
      RoundedCornerShape(5.dp)
    )
    .onFocusEvent { event ->
      if (event.isFocused) {
        coroutineScope.launch {
          bringIntoViewRequester.bringIntoView()
        }
      }
    }
    .height(55.64.dp)
    .clip(RoundedCornerShape(8.dp))
    .padding(10.dp)
    .navigationBarsWithImePadding()

  resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
    Log.i(TAG, "CreateProfilePanel: result code is " + result.resultCode)
    if (result.resultCode == Activity.RESULT_OK) {
      if (result.data != null) {
        val publicKey = result.data!!.getStringExtra(EXTRA_PUBLIC_KEY)
        val openKeyChainID = result.data!!.getStringExtra(EXTRA_OPEN_KEY_CHAIN_ID)

        chatModel.controller.appPrefs.publicKey.set(publicKey)
        chatModel.controller.appPrefs.openKeyChainID.set(openKeyChainID)

        if (publicKey != null && openKeyChainID != null) {
          withApi {
            val  user: User? = chatModel.controller.apiCreateActiveUser(
              Profile(displayName.value.replace(" ", "").toLowerCase(Locale.current), fullName.value)
            )
            chatModel.currentUser.value = user
            chatModel.controller.appPrefs.deletePassPhrase.set(deletePhrase.value)
            chatModel.onboardingStage.value = OnboardingStage.RegistrationComplete
            chatModel.userAuthorized.value = true
          }
        }
      }
    }
  }
  Column(
    Modifier
      .background(MaterialTheme.colors.background)
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
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
      contentDescription = "Index Icon",
      modifier = Modifier
        .height(214.dp)
        .width(179.dp)
        .align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.fillMaxHeight().weight(1f))
    Column(
      Modifier
        .align(Alignment.CenterHorizontally)
        .bringIntoViewRequester(bringIntoViewRequester)
    ) {
      CustomTextInputFieldView(displayName, "Your display name", focusRequester, modifier)
      Spacer(Modifier.height(15.dp))
      CustomPasswordInputFieldView(deletePhrase, "Delete phrase (Optional)", customModifier = modifier)
      Spacer(Modifier.height(15.dp))
      CustomTextInputFieldView(fullName, "Full name (Optional)", customModifier = modifier)
      val errorText = if (!isValidDisplayName(displayName.value)) stringResource(R.string.display_name_cannot_contain_whitespace) else ""
      Text(
        errorText,
        fontSize = 15.sp,
        color = MaterialTheme.colors.error
      )
      Spacer(Modifier.height(25.dp))
      val enabled = displayName.value.isNotEmpty() && isValidDisplayName(displayName.value) && isValidDeletePhrase(deletePhrase.value)
      val textColor: Color = if (enabled) {
        Color.White
      } else {
        MaterialTheme.colors.secondary
      }
      Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
          onClick = {
            ModalManager.shared.showCustomModal {
              CreateTwoAppPasswordView(chatModel = chatModel, displayName = displayName.value, fullName = fullName.value)
            }
          },
          modifier = Modifier
            .height(55.64.dp)
            .padding(8.dp),
          enabled = enabled,
          colors = ButtonDefaults.buttonColors(
            backgroundColor = buttonEnabled,
            disabledBackgroundColor = buttonDisabled
          )
        ) {
          Text(text = generalGetString(R.string.create_account), color = textColor)
        }
      }
    }
  }
}