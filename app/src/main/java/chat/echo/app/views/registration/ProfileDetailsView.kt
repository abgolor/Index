package chat.echo.app.views.registration

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.echo.app.SimplexService
import chat.echo.app.TAG
import chat.echo.app.model.*
import chat.echo.app.ui.theme.HighOrLowlight
import chat.echo.app.ui.theme.SimpleButton
import chat.echo.app.views.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.onboarding.OnboardingStage
import kotlinx.coroutines.delay

@Composable
fun ProfileDetailsPanel(chatModel: ChatModel) {
  val displayName = remember { mutableStateOf("") }
  val ockUserEmail = remember { mutableStateOf("") }
  val fullName = remember { mutableStateOf("") }
  val deletePhrase = remember {
    mutableStateOf("")
  }
  val EXTRA_PUBLIC_KEY = "public_key"
  val EXTRA_OPEN_KEY_CHAIN_ID = "open_key_chain_id"
  val focusRequester = remember { FocusRequester() }


  resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
    Log.i(TAG, "CreateProfilePanel: result code is " + result.resultCode)
    if (result.resultCode == Activity.RESULT_OK) {
      if (result.data != null) {
        val publicKey = result.data!!.getStringExtra(EXTRA_PUBLIC_KEY)
        val openKeyChainID = result.data!!.getStringExtra(EXTRA_OPEN_KEY_CHAIN_ID)

        Log.i(TAG, "ProfileDetailsPanel: public key is  $publicKey $openKeyChainID")

        if (publicKey != null && openKeyChainID != null) {
          withApi {
            val user: User? = chatModel.controller.apiCreateActiveUser(
              Profile(displayName.value, fullName.value)
            )
            if (user != null) {
              user.openKeyChainID = openKeyChainID
              chatModel.controller.startChat(user!!)
              chatModel.controller.appPrefs.publicKey.set(publicKey)
              chatModel.controller.appPrefs.deletePassPhrase.set(deletePhrase.value)
              chatModel.controller.showBackgroundServiceNoticeIfNeeded()
              SimplexService.start(chatModel.controller.appContext)
            }
          }
        }
      }
    }
  }

  Surface(Modifier.background(MaterialTheme.colors.onBackground)) {
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
      AppBarTitle(stringResource(chat.echo.app.R.string.create_profile), false)/*
      ReadableText(R.string.your_profile_is_stored_on_your_device)
      ReadableText(R.string.profile_is_only_shared_with_your_contacts)*/
      Spacer(Modifier.height(10.dp))
      /*Text(
        stringResource(R.string.display_name),
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(bottom = 3.dp)
      )
      ProfileNameField(displayName, focusRequester)
      val errorText = if (!isValidDisplayName(displayName.value)) stringResource(R.string.display_name_cannot_contain_whitespace) else ""
      Text(
        errorText,
        fontSize = 15.sp,
        color = MaterialTheme.colors.error
      )
      Spacer(Modifier.height(3.dp))*/
      Text(
        stringResource(chat.echo.app.R.string.full_name_optional__prompt),
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(bottom = 5.dp)
      )
      CustomTextInputFieldView(fullName, "Your name", focusRequester)
      Spacer(Modifier.height(15.dp))
      Text(
        stringResource(chat.echo.app.R.string.delete_pass_phrase_optional_prompt),
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(bottom = 5.dp)
      )
      CustomTextInputFieldView(deletePhrase, "Your delete phrase")
      Spacer(Modifier.fillMaxHeight().weight(1f))
      Row {
        SimpleButton(
          text = stringResource(chat.echo.app.R.string.about_simplex),
          icon = Icons.Outlined.ArrowBackIosNew
        ) { chatModel.onboardingStage.value = OnboardingStage.About }

        Spacer(Modifier.fillMaxWidth().weight(1f))
        val enabled = displayName.value.isNotEmpty() && isValidDisplayName(displayName.value) && isValidDeletePhrase(deletePhrase.value)
        val createModifier: Modifier
        val createColor: Color
        if (enabled) {
          createModifier = Modifier.clickable {
            ModalManager.shared.showCustomModal {
              CreateTwoAppPasswordView(chatModel = chatModel, displayName = displayName.value, fullName = fullName.value)
            }
          }.padding(8.dp)
          createColor = MaterialTheme.colors.primary
        } else {
          createModifier = Modifier.padding(8.dp)
          createColor = HighOrLowlight
        }
        Surface(shape = RoundedCornerShape(20.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = createModifier) {
            Text(stringResource(chat.echo.app.R.string.next), style = MaterialTheme.typography.caption, color = createColor)
            Icon(Icons.Outlined.ArrowForwardIos, stringResource(chat.echo.app.R.string.next), tint = createColor)
          }
        }
      }

      LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
      }
    }
  }
}