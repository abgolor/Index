package chat.echo.app.views

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import chat.echo.app.*
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.onboarding.OnboardingStage
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openintents.openpgp.OpenPgpError
import org.openintents.openpgp.util.OpenPgpApi
import java.util.*

fun isPasswordValid(password: String): Boolean{
  return password.trim().isNotEmpty() && password.length >= 6
}

@Composable
fun CreateTwoAppPasswordView(chatModel: ChatModel, displayName: String = "", fullName: String = "", deletePhrase: String = "",  isFirstTime: Boolean = true) {
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  val keyboardState by getKeyboardState()
  var savedKeyboardState by remember { mutableStateOf(keyboardState) }
  val EXTRA_PUBLIC_KEY = "public_key"
  val EXTRA_OPEN_KEY_CHAIN_ID = "open_key_chain_id"
  val focusRequester = remember { FocusRequester() }
  val resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
    Log.i(TAG, "CreateProfilePanel: result code is " + result.resultCode)
    if (result.resultCode == Activity.RESULT_OK) {
      if (result.data != null) {
        val publicKey = result.data!!.getStringExtra(EXTRA_PUBLIC_KEY)
        val openKeyChainID = result.data!!.getStringExtra(EXTRA_OPEN_KEY_CHAIN_ID)

        if (publicKey != null && openKeyChainID != null) {
          withApi {
            val  user: User? = chatModel.controller.apiCreateActiveUser(
              Profile(displayName, fullName)
            )
            chatModel.controller.startChat(user!!)
            chatModel.controller.appPrefs.publicKey.set(publicKey)
            chatModel.controller.appPrefs.deletePassPhrase.set(deletePhrase)
            SimplexService.start(chatModel.controller.appContext)
          }
        }
      }
    }
  }

  ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colors.background)
        .padding(20.dp)
    ) {
      CreateTwoAppPasswordPanel(chatModel, displayName, fullName,  isFirstTime)
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
fun CreateTwoAppPasswordPanel(chatModel: ChatModel, displayName: String,fullName: String,  isFirstTime: Boolean) {
  val passwordOne = remember { mutableStateOf("") }
  val passwordTwo = remember { mutableStateOf("") }
  val EXTRA_PUBLIC_KEY = "public_key"
  val EXTRA_OPEN_KEY_CHAIN_ID = "open_key_chain_id"
  val focusRequester = remember { FocusRequester() }
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val bringIntoViewRequester = BringIntoViewRequester()
  val modifier = Modifier
    .fillMaxWidth()
    .background(
      MaterialTheme.colors.secondary,
      RoundedCornerShape(5.dp)
    )
    .onFocusEvent { event ->
      if(event.isFocused){
        coroutineScope.launch {
          bringIntoViewRequester.bringIntoView()
        }
      }
    }
    .height(55.64.dp)
    .clip(RoundedCornerShape(8.dp))
    .padding(10.dp)
    .navigationBarsWithImePadding()


  Surface(Modifier.background(MaterialTheme.colors.onBackground)) {
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
        // .padding(10.dp)
      )
      Spacer(Modifier.fillMaxHeight().weight(1f))
      Column(
        Modifier
          .align(Alignment.CenterHorizontally)
          .bringIntoViewRequester(bringIntoViewRequester)
      ) {
        CustomPasswordInputFieldView(passwordOne, stringResource(R.string.first_app_password), true,  ImeAction.Next, focusRequester = focusRequester, customModifier = modifier)
        Spacer(Modifier.height(15.dp))
        CustomPasswordInputFieldView(passwordTwo, stringResource(R.string.second_app_password),true, ImeAction.Done, customModifier = modifier)
        Spacer(Modifier.height(25.dp))
        val enabled = isPasswordValid(passwordOne.value) && isPasswordValid(passwordTwo.value)
        CustomButtonView(text = stringResource(id = R.string.create_account), isEnabled = enabled) {
          saveAppPassword(chatModel, passwordOne.value, passwordTwo.value, isFirstTime)
        }
        LaunchedEffect(Unit) {
          delay(300)
          focusManager.clearFocus()
        }
      }
    }
  }
}

fun saveAppPassword(chatModel: ChatModel, password1: String, password2: String, isFirstTime: Boolean) {
  if(password1 != password2){
    if(isFirstTime){
      chatModel.controller.appPrefs.laNoticeShown.set(true)
      chatModel.performLA.value = true
      AppPreferences(SimplexApp.context).performLA.set(true)
      AppPreferences(SimplexApp.context).passwordOne.set(password1)
      AppPreferences(SimplexApp.context).passwordTwo.set(password2)
      if(isFirstTime) {
        createProfile(chatModel)
      }
      ModalManager.shared.closeModal()
    } else {
      authenticate(
        generalGetString(R.string.auth_enable_simplex_lock),
        generalGetString(R.string.auth_confirm_credential),
        chatModel.activity!! as FragmentActivity,
        completed = { laResult ->
          when (laResult) {
            LAResult.Success -> {
              chatModel.performLA.value = true
              AppPreferences(SimplexApp.context).passwordOne.set(password1)
              AppPreferences(SimplexApp.context).passwordTwo.set(password2)
              ModalManager.shared.closeModal()
            }
            is LAResult.Error -> {
              chatModel.performLA.value = true
              laErrorToast(SimplexApp.context, laResult.errString)
            }
            LAResult.Failed -> {
              chatModel.performLA.value = true
              laFailedToast(SimplexApp.context)
            }
            LAResult.Unavailable -> {
              chatModel.performLA.value = true
              ModalManager.shared.closeModal()
            }
          }
        }
      )
    }
  } else {
    AlertManager.shared.showAlertMsg("Invalid Password Pattern",
      "First app password must be different from second app password"){
      AlertManager.shared.hideAlert()
    }
  }
}

fun createProfile(chatModel: ChatModel) {
  withApi {
    try {
      createEncryptionKey(chatModel)
    } catch (e: Exception){
      createEncryptionKey(chatModel)
    }
  }
}

fun createEncryptionKey(chatModel: ChatModel) {
  val data = Intent()
  val uuid = UUID.randomUUID().toString()
  val currentTimeInMillis = System.currentTimeMillis()
  val openKeyChainID = uuid.substring(0, uuid.indexOf("-")) + currentTimeInMillis
  data.action = OpenPgpApi.ACTION_GET_SIGN_KEY_ID
  data.putExtra(OpenPgpApi.EXTRA_USER_ID, openKeyChainID)
  val api = OpenPgpApi(chatModel.activity!!.applicationContext, chatModel.mServiceConnection!!.service)
  val callBack = OpenPgpApi.IOpenPgpCallback { result ->
    if (result != null) {
      when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
        OpenPgpApi.RESULT_CODE_SUCCESS -> {
          val keyId = result.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, 0)
        }
        OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
          val pi: PendingIntent? = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT)
          try {
            resultLauncher!!.launch(IntentSenderRequest.Builder(pi!!).build())
          } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "SendIntentException", e)
          }
        }
        OpenPgpApi.RESULT_CODE_ERROR -> {
          val error: OpenPgpError? = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR)
          if (error != null) {
            Log.d(TAG, "onReturn: error is " + error.message)
          }
        }
      }
    }
  }
  api.executeApiAsync(data, null, null, callBack)
}

@Composable
fun PasswordField(password: MutableState<String>, hint: String,  focusRequester: FocusRequester? = null){
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
    textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground),  keyboardOptions = KeyboardOptions(
      capitalization = KeyboardCapitalization.None,
      autoCorrect = false
    ),
    singleLine = true,
    modifier = if (focusRequester == null) modifier else modifier.focusRequester(focusRequester)
  )
}