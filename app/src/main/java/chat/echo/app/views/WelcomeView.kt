package chat.echo.app.views

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.onboarding.*
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun isValidDisplayName(name: String): Boolean {
  return (name.firstOrNull { it.isWhitespace() }) == null
}

fun isValidOKCUserEmail(userEmail: String): Boolean {
  return (userEmail.firstOrNull() {
    it.isWhitespace()
  }) == null
}

fun isValidDeletePhrase(deletePhrase: String): Boolean {
  if (deletePhrase.isEmpty() || deletePhrase.length <= 15) {
    return true
  }
  return false
}

var resultLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>? = null

@Composable
fun CreateProfilePanel(chatModel: ChatModel) {
  val displayName = remember { mutableStateOf("") }
  val fullName = remember { mutableStateOf("") }
  val deletePhrase = remember {
    mutableStateOf("")
  }
  val EXTRA_PUBLIC_KEY = "public_key"
  val EXTRA_OPEN_KEY_CHAIN_ID = "open_key_chain_id"
  val focusRequester = remember { FocusRequester() }

/*  resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
    Log.i(TAG, "CreateProfilePanel: result code is " + result.resultCode)
    if (result.resultCode == Activity.RESULT_OK) {
      if (result.data != null) {
        val publicKey = result.data!!.getStringExtra(EXTRA_PUBLIC_KEY)
        val openKeyChainID = result.data!!.getStringExtra(EXTRA_OPEN_KEY_CHAIN_ID)

        if (publicKey != null && openKeyChainID != null) {
          withApi {
           val  user: User = chatModel.controller.apiCreateActiveUser(
              Profile(displayName.value, fullName.value)
            )
            val localContact = LocalContact(user.userContactId.toString(), user.displayName, user.fullName, user.localAlias, openKeyChainID, ChatType.Direct, "", false)
            chatModel.localContactViewModel?.createLocalContact(localContact)
            chatModel.controller.startChat(user!!)
            chatModel.controller.appPrefs.publicKey.set(publicKey)
            chatModel.controller.appPrefs.deletePassPhrase.set(deletePhrase.value)
            chatModel.controller.showBackgroundServiceNoticeIfNeeded()
            SimplexService.start(chatModel.controller.appContext)
          }
        }
      }
    }
  }*/

  Surface(Modifier.background(MaterialTheme.colors.onBackground)) {
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
      Box(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
        SimpleXLogo()
      }
      Spacer(Modifier.height(10.dp))
      Text(
        "Create account",
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(bottom = 5.dp)
      )
      Divider(color = MaterialTheme.colors.onBackground, thickness = 3.dp)

      Spacer(Modifier.height(20.dp))
      CustomTextInputFieldView(displayName, "Your display name", focusRequester)
      Spacer(Modifier.height(15.dp))
      CustomTextInputFieldView(fullName, "Full name (Optional)")
      Spacer(Modifier.height(15.dp))
      CustomPasswordInputFieldView(deletePhrase, "Delete phrase (Optional)")
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
              CreateTwoAppPasswordView(chatModel = chatModel, displayName = displayName.value, fullName = fullName.value, deletePhrase = deletePhrase.value)
            }
          },
          modifier = Modifier
            .height(40.dp)
            .padding(8.dp),
          enabled = enabled,
          colors = ButtonDefaults.buttonColors(
            backgroundColor = buttonEnabled,
            disabledBackgroundColor = buttonDisabled
          )
        ) {
          Text(text = generalGetString(R.string.continue_reg), color = textColor)
        }
      }

      LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
      }
    }
  }
}

@Composable
fun CustomTextInputFieldView(name: MutableState<String>, hint: String, focusRequester: FocusRequester? = null,
customModifier: Modifier? = null) {
  var  modifier = customModifier
    ?: Modifier
      .fillMaxWidth()
      .background(
        MaterialTheme.colors.secondary,
        RoundedCornerShape(5.dp)
      )
      .height(55.64.dp)
      .clip(RoundedCornerShape(8.dp))
      .padding(10.dp)
      .navigationBarsWithImePadding()
  BasicTextField(
    value = name.value,
    onValueChange = { name.value = it },
    decorationBox = { innerTextField ->
      Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp)
      ) {
        if (name.value.isEmpty()) {
          Text(
            text = hint,
          )
        }
        innerTextField()
      }
    },
    modifier = if (focusRequester == null) modifier else modifier.focusRequester(focusRequester),
    textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground),
    keyboardOptions = KeyboardOptions(
      capitalization = KeyboardCapitalization.None,
      autoCorrect = false
    ),
    singleLine = true,
    cursorBrush = SolidColor(HighOrLowlight)
  )
}

@Composable
fun AuthenticationPINView(pin: MutableState<String>,  focusRequester: FocusRequester){

  PinView(
    modifier = Modifier.border(
      BorderStroke(2.dp, Color.Black),
      shape = RoundedCornerShape(3.dp),
    ),
    value = pin.value,
    length = 6,
    disableKeypad = false,
    obscureText = "*",
    focusRequester = focusRequester
  ){
    pin.value = it
  }
}

@Composable
fun CustomPasswordInputFieldView(
  name: MutableState<String>, hint: String, isNumbersOnly: Boolean = false,
  imeAction: ImeAction = ImeAction.Next,
  keyboardAction: () -> Unit = {},
  focusRequester: FocusRequester? = null, customModifier: Modifier? = null) {
  var passwordVisible by remember { mutableStateOf(false) }
  var  modifier = customModifier
    ?: Modifier
      .fillMaxWidth()
      .background(
        MaterialTheme.colors.secondary,
        RoundedCornerShape(5.dp)
      )
      .height(55.64.dp)
      .clip(RoundedCornerShape(8.dp))
      .padding(10.dp)
      .navigationBarsWithImePadding()

  val image = if (passwordVisible) Icons.Filled.Visibility
  else Icons.Filled.VisibilityOff
  val description = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)

  BasicTextField(
    value = name.value,
    onValueChange = { name.value = it },
    decorationBox = { innerTextField ->
      Row(
        Modifier
          .padding(start = 8.dp, end = 8.dp)
          .fillMaxWidth()
      ) {
        Box(
          modifier = Modifier.align(Alignment.CenterVertically),
          contentAlignment = Alignment.CenterStart
        ) {
          if (name.value.isEmpty()) {
            Text(
              text = hint,
            )
          }
          innerTextField()
        }
        Spacer(Modifier.fillMaxWidth().weight(1f))
        IconButton(onClick = {
          passwordVisible = !passwordVisible
        }) {
          Icon(
            imageVector = image, contentDescription = description,
            modifier = Modifier.size(25.dp)
          )
        }
      }
    },
    visualTransformation =
    if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    modifier = if (focusRequester == null) modifier else modifier.focusRequester(focusRequester),
    textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground),
    keyboardOptions = KeyboardOptions(
      keyboardType = if(isNumbersOnly == true) KeyboardType.NumberPassword else KeyboardType.Text,
      capitalization = KeyboardCapitalization.None,
      autoCorrect = false,
      imeAction = imeAction
    ),
    keyboardActions = KeyboardActions(onDone = {
      keyboardAction()
    }),
    singleLine = true,
    cursorBrush = SolidColor(HighOrLowlight)
  )
}

@Composable
fun CustomButtonView(text: String, isEnabled: Boolean, action: () -> Unit) {
  val textColor: Color = if (isEnabled) {
    Color.White
  } else {
    MaterialTheme.colors.secondary
  }
  Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Button(
      onClick = action,
      modifier = Modifier
        .height(55.64.dp)
        .padding(8.dp),
      enabled = isEnabled,
      colors = ButtonDefaults.buttonColors(
        backgroundColor = buttonEnabled,
        disabledBackgroundColor = buttonDisabled
      )
    ) {
      Text(text = text, color = textColor)
    }
  }
}


@Composable
fun OtpCell(
  modifier: Modifier,
  value: Char?,
  isCursorVisible: Boolean = false,
  obscureText: String?
) {
  val scope = rememberCoroutineScope()
  val (cursorSymbol, setCursorSymbol) = remember { mutableStateOf("") }

  LaunchedEffect(key1 = cursorSymbol, isCursorVisible) {
    if (isCursorVisible) {
      scope.launch {
        delay(350)
        setCursorSymbol(if (cursorSymbol.isEmpty()) "|" else "")
      }
    }
  }

  Box(
    modifier = modifier
  ) {
    Text(
      text = if (isCursorVisible) cursorSymbol else if (!obscureText.isNullOrBlank() && value?.toString()
          .isNullOrBlank().not()
      ) obscureText else value?.toString() ?: "",
      style = MaterialTheme.typography.body1,
      modifier = Modifier.align(Alignment.Center)
    )
  }
}

@OptIn(ExperimentalComposeUiApi::class)

@Composable
fun PinView(
  modifier: Modifier = Modifier,
  length: Int = 5,
  value: String = "",
  disableKeypad: Boolean = false,
  obscureText: String? = "*",
  focusRequester: FocusRequester,
  onValueChanged: (String) -> Unit
) {
  val isFocused = remember { mutableStateOf(false) }
  modifier.size(0.dp)
  val keyboard = LocalSoftwareKeyboardController.current
  TextField(
    readOnly = disableKeypad,
    value = value,
    onValueChange = {
      if (it.length <= length) {
        if (it.all { c -> c in '0'..'9' }) {
          onValueChanged(it)
        }
        if (it.length >= length) {
         // keyboard?.hide()
        }
      }
    },
    // Hide the text field
    modifier = if (focusRequester == null) modifier.size(0.dp)
      .onFocusChanged { isFocused.value = it.isFocused }
    else modifier
      .size(0.dp)
      .focusRequester(focusRequester),
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Number
    )
  )

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
  ) {
    repeat(length) {
      OtpCell(
        modifier = modifier
          .size(width = 45.dp, height = 45.dp)
          .clip(MaterialTheme.shapes.large)
          .background(
            MaterialTheme.colors.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(3.dp)
          )
          .clickable {
            focusRequester?.requestFocus()
            keyboard?.show()
          },
        value = value.getOrNull(it),
        isCursorVisible = value.length == it && isFocused.value,
        obscureText
      )
      Spacer(modifier = Modifier.size(8.dp))
    }
  }
}