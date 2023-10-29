package chat.echo.app.views.usersettings

import SectionDivider
import SectionItemView
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.BuildConfig
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.database.DatabaseView
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.*
import chat.echo.app.views.notification.NotificationPermissionView
import chat.echo.app.views.topbar.CustomTopBarCloseableView
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsNewView(
  chatModel: ChatModel,
  showModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  showSettingsModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  showCustomModal: (@Composable (ChatModel, () -> Unit) -> Unit) -> () -> Unit,
  setPerformLA: (Boolean) -> Unit,
  close: () -> Unit
) {
  BackHandler() {
    close()
  }
  val user = chatModel.currentUser.value
  if (user != null) {
    var profile by remember { mutableStateOf(user.profile.toProfile()) }
    val editProfile = remember { mutableStateOf(false) }
    SettingsNewLayout(chatModel = chatModel,
      showModal = showModal,
      showSettingsModal = showSettingsModal,
      setPerformLA = setPerformLA,
      close = close,
      editProfile = editProfile,
      profile = profile,
      showCustomModal = showCustomModal,
      saveProfile = { displayName, fullName, image ->
        withApi {
          val p = Profile(displayName, fullName, image)
          val newProfile = chatModel.controller.apiUpdateProfile(p)
          if (newProfile != null) {
            chatModel.currentUser.value?.profile?.profileId?.let {
              chatModel.updateCurrentUser(newProfile)
            }
            profile = newProfile
          }
          Log.i("TAG", "SettingsNewView: image is " + image.toString())
          editProfile.value = false
        }
      })
  }
}

@Composable
fun SettingsNewLayout(
  chatModel: ChatModel,
  showModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  showSettingsModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  showCustomModal: (@Composable (ChatModel, () -> Unit) -> Unit) -> () -> Unit,
  setPerformLA: (Boolean) -> Unit,
  close: () -> Unit,
  editProfile: MutableState<Boolean>,
  profile: Profile,
  saveProfile: (String, String, String?) -> Unit
) {
  val focusRequester = remember { FocusRequester() }
  val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val scope = rememberCoroutineScope()
  val image = remember {
    mutableStateOf(profile.image)
  }

  val chosenImage = rememberSaveable { mutableStateOf<Uri?>(null) }

  ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
    ModalBottomSheetLayout(
      scrimColor = Color.Black.copy(alpha = 0.12F),
      modifier = Modifier.navigationBarsWithImePadding(),
      sheetContent = {
        GetImageBottomSheet(
          chosenImage,
          image,
          onImageChange = { bitmap -> image.value = resizeImageToStrSize(cropToSquare(bitmap), maxDataSize = 12500) },
          hideBottomSheet = {
            scope.launch { bottomSheetModalState.hide() }
          })
      },
      sheetState = bottomSheetModalState,
      sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
      SettingsContentView(
        chatModel = chatModel,
        editProfile = editProfile,
        profile = profile,
        bottomSheetModalState = bottomSheetModalState,
        setPerformLA = setPerformLA,
        image = image,
        showModal = showModal,
        showSettingsModal = showSettingsModal,
        showCustomModal = showCustomModal,
        saveProfile = saveProfile
      )

      Column(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
          .background(Color.White)
      ) {
        Column(Modifier.fillMaxWidth()) {
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
                    Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Text(
                  text = generalGetString(R.string.settings),
                  fontSize = 16.sp,
                  modifier = Modifier.align(Alignment.Center),
                  fontWeight = FontWeight.Medium,
                  color = Color.Black
                )
              }
            }
            SettingsContentView(
              chatModel = chatModel,
              editProfile = editProfile,
              profile = profile,
              bottomSheetModalState = bottomSheetModalState,
              setPerformLA = setPerformLA,
              image = image,
              showModal = showModal,
              showSettingsModal = showSettingsModal,
              saveProfile = saveProfile,
              showCustomModal = showCustomModal
            )
          }
        }
      }
      /*      CustomTopBarCloseableView(close = close, title = generalGetString(R.string.settings)) {

            }*/
    }
  }
}

@Composable
fun SettingsContentView(
  chatModel: ChatModel,
  editProfile: MutableState<Boolean>, profile: Profile,
  bottomSheetModalState: ModalBottomSheetState,
  setPerformLA: (Boolean) -> Unit,
  image: MutableState<String?>,
  showModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  showSettingsModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  showCustomModal: (@Composable (ChatModel, () -> Unit) -> Unit) -> () -> Unit,
  saveProfile: (String, String, String?) -> Unit
) {
  val stopped = chatModel.chatRunning.value == false
  val focusRequester = remember { FocusRequester() }
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val displayName = remember {
    mutableStateOf(
      TextFieldValue(
        text = profile.displayName,
        selection = TextRange(profile.displayName.length)
      )
    )
  }
  val fullName = remember {
    mutableStateOf(
      TextFieldValue(
        text = profile.fullName,
        selection = TextRange(profile.fullName.length)
      )
    )
  }

  Column(
    Modifier.fillMaxWidth()
      .verticalScroll(rememberScrollState())
  ) {
    SubHeaderSettings(title = generalGetString(R.string.profile))
    Box(
      Modifier
        .fillMaxWidth()
        .padding(5.dp),
      contentAlignment = Alignment.Center
    ) {
      if (editProfile.value) {
        Box(contentAlignment = Alignment.TopEnd) {
          Box(contentAlignment = Alignment.Center) {
            ProfileImage(size = 200.dp, image = image.value, color = MaterialTheme.colors.secondary)
            EditImageButton {
              scope.launch {
                focusManager.clearFocus()
                bottomSheetModalState.show()
              }
            }
          }
        }
      } else {
        ProfileImage(size = 200.dp, image = profile.image, color = MaterialTheme.colors.secondary)
      }
    }
    ProfileHeader(headerTitle = generalGetString(R.string.display_name), editProfile = editProfile, isEdit = true) {
      if (editProfile.value) {
        if (displayName.value.text != "") {
          saveProfile(displayName.value.text.trim(), fullName.value.text, image.value)
        } else {
          saveProfile(profile.displayName.trim(), fullName.value.text, image.value)
        }
        focusManager.clearFocus()
      } else {
        editProfile.value = true
      }
    }
    if (editProfile.value) {
      BasicTextField(
        value = displayName.value,
        onValueChange = {
          displayName.value = it
        },
        modifier = if (editProfile.value) Modifier
          .padding(start = 10.dp, end = 10.dp)
          .fillMaxWidth()
          .focusRequester(focusRequester) else Modifier.padding(start = 10.dp, end = 10.dp),
        decorationBox = { innerTextField ->
          Box(
            contentAlignment = Alignment.CenterStart
          ) {
            if (displayName.value.text.isEmpty()) {
              Text(
                text = generalGetString(R.string.enter_display_name),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextHeader,
              )
            }
            innerTextField()
          }
        },
        textStyle = MaterialTheme.typography.body1.copy(
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = Color.Black
        ),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.None,
          autoCorrect = false
        ),
        singleLine = true,
        cursorBrush = SolidColor(HighOrLowlight)
      )
    } else {
      Text(
        profile.displayName,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp)
      )
    }
    ProfileHeader(headerTitle = generalGetString(R.string.full_name), editProfile = editProfile) {
    }
    if (editProfile.value) {
      BasicTextField(
        value = fullName.value,
        onValueChange = {
          fullName.value = it
        },
        modifier = Modifier.padding(start = 10.dp, end = 10.dp)
          .fillMaxWidth(),
        decorationBox = { innerTextField ->
          Box(
            contentAlignment = Alignment.CenterStart
          ) {
            if (fullName.value.text.isEmpty()) {
              Text(
                text = generalGetString(R.string.enter_full_name),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextHeader,
              )
            }
            innerTextField()
          }
        },
        textStyle = MaterialTheme.typography.body1.copy(
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          color = Color.Black
        ),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.None,
          autoCorrect = false
        ),
        singleLine = true,
        cursorBrush = SolidColor(HighOrLowlight)
      )
    } else {
      Text(
        if (profile.fullName != "") profile.fullName else generalGetString(R.string.not_available),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp)
      )
    }
    Spacer(modifier = Modifier.padding(top = 20.dp))
    SubHeaderSettings(title = generalGetString(R.string.settings))
    Spacer(modifier = Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 5.dp))

    SettingsActionItem(
      Icons.Outlined.QrCode,
      stringResource(R.string.your_simplex_contact_address),
      showCustomModal { chatModel, close ->
        CreateUserLinkView(chatModel, 1, false, close)
      },
      Color.Black,
      Color.Black,
      disabled = stopped
    )
    SectionDivider()
    SettingsActionItem(
      Icons.Outlined.ImportExport,
      stringResource(R.string.import_and_export_contacts),
      showSettingsModal { DatabaseView(it, showSettingsModal) },
      Color.Black,
      Color.Black,
      disabled = stopped
    )
    SectionDivider()
    SettingsActionItem(
      Icons.Outlined.Bolt,
      stringResource(R.string.private_notifications),
      showSettingsModal { NotificationsSettingsView(it) },
      Color.Black,
      Color.Black,
      disabled = stopped
    )
    SectionDivider()
    SettingsActionItem(
      Icons.Outlined.Videocam,
      stringResource(R.string.settings_audio_video_calls),
      showSettingsModal { CallSettingsView(it, showModal) },
      Color.Black,
      Color.Black,
      disabled = stopped
    )
    SectionDivider()
    SettingsActionItem(
      Icons.Outlined.Lock,
      stringResource(R.string.privacy_and_security),
      showSettingsModal { PrivacySettingsView(it, setPerformLA) },
      Color.Black,
      Color.Black,
      disabled = stopped
    )
    SectionDivider()
    SettingsActionItem(
      Icons.Outlined.LightMode,
      stringResource(R.string.appearance_settings),
      showSettingsModal { AppearanceView() },
      Color.Black,
      Color.Black,
      disabled = stopped
    )
    SectionDivider()
    SettingsActionItem(
      Icons.Outlined.WifiTethering,
      stringResource(R.string.network_and_servers),
      showSettingsModal { NetworkAndServersView(it, showModal, showSettingsModal, showCustomModal) },
      Color.Black,
      Color.Black,
      disabled = stopped
    )
  }

  LaunchedEffect(editProfile.value) {
    if (editProfile.value) {
      delay(200)
      focusRequester.requestFocus()
    }
  }
}

@Composable private fun AppVersionItem(showVersion: () -> Unit) {
  SectionItemView(showVersion) { AppVersionText() }
}

@Composable fun AppVersionText() {
  Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
}

@Composable
fun PreferenceToggle(
  text: String,
  checked: Boolean,
  onChange: (Boolean) -> Unit = {},
) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(text)
    Spacer(Modifier.fillMaxWidth().weight(1f))
    Switch(
      checked = checked,
      onCheckedChange = onChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colors.primary,
        uncheckedThumbColor = HighOrLowlight
      )
    )
  }
}

@Composable
fun PreferenceToggleWithIcon(
  text: String,
  icon: ImageVector? = null,
  iconColor: Color? = HighOrLowlight,
  checked: Boolean,
  onChange: (Boolean) -> Unit = {},
) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    if (icon != null) {
      Icon(
        icon,
        null,
        tint = iconColor ?: HighOrLowlight
      )
      Spacer(Modifier.padding(horizontal = 4.dp))
    }
    Text(text)
    Spacer(Modifier.fillMaxWidth().weight(1f))
    Switch(
      checked = checked,
      onCheckedChange = {
        onChange(it)
      },
      colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colors.primary,
        uncheckedThumbColor = HighOrLowlight
      )
    )
  }
}

@Composable
fun ProfileHeader(headerTitle: String, color: Color = TextHeader, isEdit: Boolean = false, editProfile: MutableState<Boolean>, onClick: () -> Unit) {
  Row(Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 5.dp)) {
    Text(
      text = headerTitle,
      fontSize = 14.sp,
      modifier = Modifier.align(Alignment.CenterVertically),
      fontWeight = FontWeight.Normal,
      color = color
    )
    Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
    if (isEdit) {
      Text(
        text = if (editProfile.value) generalGetString(R.string.save) else generalGetString(R.string.edit),
        fontSize = 14.sp,
        modifier = Modifier
          .clickable {
            onClick()
          }
          .align(Alignment.CenterVertically),
        fontWeight = FontWeight.Medium,
        color = TextHeader
      )
    }
  }
}