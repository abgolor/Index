package chat.echo.app.views.usersettings

import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.isValidDisplayName
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.launch

@Composable
fun UserProfileView(chatModel: ChatModel, close: () -> Unit) {
  val user = chatModel.currentUser.value
  if (user != null) {
    val editProfile = remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(user.profile.toProfile()) }
    UserProfileLayout(
      editProfile = editProfile,
      profile = profile,
      close,
      saveProfile = { displayName, fullName, deletePhrase,  image ->
        withApi {
          val p = Profile(displayName, fullName, image)
          val newProfile = chatModel.controller.apiUpdateProfile(p)
          if (newProfile != null) {
            chatModel.currentUser.value?.profile?.profileId?.let {
              chatModel.updateCurrentUser(newProfile)
            }
            profile = newProfile
          }
          AppPreferences(chatModel.controller.appContext).deletePassPhrase.set(deletePhrase)
          editProfile.value = false
        }
      }
    )
  }
}

@Composable
fun UserProfileLayout(
  editProfile: MutableState<Boolean>,
  profile: Profile,
  close: () -> Unit,
  saveProfile: (String, String, String?, String?) -> Unit,
) {
  val context = LocalContext.current
  val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val displayName = remember { mutableStateOf(profile.displayName) }
  val deletePhrase = remember { mutableStateOf(AppPreferences(context).deletePassPhrase.get().toString()) }
  val fullName = remember { mutableStateOf(profile.fullName) }
  val chosenImage = rememberSaveable { mutableStateOf<Uri?>(null) }
  val profileImage = remember { mutableStateOf(profile.image) }
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  val keyboardState by getKeyboardState()
  var savedKeyboardState by remember { mutableStateOf(keyboardState) }
  ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
    ModalBottomSheetLayout(
      scrimColor = Color.Black.copy(alpha = 0.12F),
      modifier = Modifier.navigationBarsWithImePadding(),
      sheetContent = {
        GetImageBottomSheet(
          chosenImage,
          onImageChange = { bitmap -> profileImage.value = resizeImageToStrSize(cropToSquare(bitmap), maxDataSize = 12500) },
          hideBottomSheet = {
            scope.launch { bottomSheetModalState.hide() }
          })
      },
      sheetState = bottomSheetModalState,
      sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
      ModalView(close = close) {
        Column(
          Modifier
            .verticalScroll(scrollState)
            .padding(horizontal = DEFAULT_PADDING),
          horizontalAlignment = Alignment.Start
        ) {
          AppBarTitle(stringResource(R.string.your_chat_profile), false)
          Text(
            stringResource(R.string.your_profile_is_stored_on_device_and_shared_only_with_contacts_simplex_cannot_see_it),
            Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colors.onBackground,
            lineHeight = 22.sp
          )
          if (editProfile.value) {
            Column(
              Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.Start
            ) {
              Box(
                Modifier
                  .fillMaxWidth()
                  .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
              ) {
                Box(contentAlignment = Alignment.TopEnd) {
                  Box(contentAlignment = Alignment.Center) {
                    ProfileImage(192.dp, profileImage.value)
                    EditImageButton { scope.launch { bottomSheetModalState.show() } }
                  }
                  if (profileImage.value != null) {
                    DeleteImageButton { profileImage.value = null }
                  }
                }
              }
              Box {
                if (!isValidDisplayName(displayName.value)) {
                  Icon(Icons.Outlined.Info, tint = Color.Red, contentDescription = stringResource(R.string.display_name_cannot_contain_whitespace))
                }
                ProfileNameTextField(displayName, "Display Name", "Enter display name")
              }
              ProfileNameTextField(fullName, "Profile Name", "Enter profile name")
              Box {
                if (!isValidDisplayName(displayName.value)) {
                  Icon(Icons.Outlined.Info, tint = Color.Red, contentDescription = stringResource(R.string.delete_pass_phrase_length_issues))
                }
                DeletePassPhraseTextField(deletePhrase)
              }
              Row {
                TextButton(stringResource(R.string.cancel_verb), Color.Red) {
                  displayName.value = profile.displayName
                  fullName.value = profile.fullName
                  profileImage.value = profile.image
                  editProfile.value = false
                }
                Spacer(Modifier.padding(horizontal = 8.dp))
                val enabled = displayName.value.isNotEmpty() && isValidDisplayName(displayName.value)
                Button(
                  onClick = {
                    if (enabled) {
                      saveProfile(displayName.value, fullName.value, deletePhrase.value, profileImage.value)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(backgroundColor = if (enabled) MaterialTheme.colors.primary else HighOrLowlight)
                )
                {
                  Text(text = stringResource(id = R.string.save_and_notify_contacts), color = Color.White)
                }
              }
            }
          } else {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.Start
            ) {
              Box(
                Modifier
                  .fillMaxWidth()
                  .padding(bottom = 24.dp), contentAlignment = Alignment.Center
              ) {
                ProfileImage(192.dp, profile.image)
                if (profile.image == null) {
                  EditImageButton {
                    scope.launch { bottomSheetModalState.show() }
                  }
                }
              }

              ProfileNameRow(stringResource(R.string.display_name__field), profile.displayName)
              ProfileNameRow(stringResource(R.string.full_name__field), profile.fullName)
              ProfileNameRow("OKC User ID:", profile.displayName + "@echo.chat")
              ProfileNameRow("Delete Pass Phrase:", deletePhrase.value)
              TextButton(stringResource(R.string.edit_verb)) { editProfile.value = true }
            }
          }
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
  }
}

@Composable
fun ProfileNameTextField(name: MutableState<String>, label: String, placeholder: String) {
  OutlinedTextField(
    value = name.value,
    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = "delete") },
    onValueChange = {
      name.value = it
    },
    label = {
      Text(
        text = label,
        fontSize = 15.sp
      )
    },
    placeholder = { Text(text = placeholder) },
    modifier = Modifier
      .padding(bottom = 15.dp)
      .padding(start = 5.dp)
      .padding(end = 5.dp)
      .fillMaxWidth()
  )
}

@Composable
fun DeletePassPhraseTextField(passPhrase: MutableState<String>) {
  OutlinedTextField(
    value = passPhrase.value,
    leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = "delete") },
    onValueChange = {
      passPhrase.value = it
    },
    label = {
      Text(
        text = "Delete Phrase",
        fontSize = 15.sp
      )
    },
    placeholder = { Text(text = "Enter delete phrase") },
    modifier = Modifier
      .padding(bottom = 15.dp)
      .padding(start = 5.dp)
      .padding(end = 5.dp)
      .fillMaxWidth()
  )
}

@Composable
fun ProfileNameRow(label: String, text: String) {
  Row(Modifier.padding(bottom = 24.dp)) {
    Text(
      label,
      color = MaterialTheme.colors.onBackground
    )
    Spacer(Modifier.padding(horizontal = 4.dp))
    Text(
      text,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colors.onBackground
    )
  }
}

@Composable
fun TextButton(text: String, background: Color = MaterialTheme.colors.primary, click: () -> Unit) {
  Button(
    onClick = click,
    colors = ButtonDefaults.buttonColors(backgroundColor = background)
  )
  {
    Text(text = text, color = Color.White)
  }
}

@Composable
fun EditImageButton(click: () -> Unit) {
  IconButton(
    onClick = click,
    modifier = Modifier.background(Color(1f, 1f, 1f, 0.2f), shape = CircleShape)
  ) {
    Icon(
      Icons.Outlined.PhotoCamera,
      contentDescription = stringResource(R.string.edit_image),
      tint = MaterialTheme.colors.primary,
      modifier = Modifier.size(36.dp)
    )
  }
}

@Composable
fun DeleteImageButton(click: () -> Unit) {
  IconButton(onClick = click) {
    Icon(
      Icons.Outlined.Close,
      contentDescription = stringResource(R.string.delete_image),
      tint = MaterialTheme.colors.primary,
      modifier = Modifier.padding(5.dp)
    )
  }
}

@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewUserProfileLayoutEditOff() {
  SimpleXTheme {
    UserProfileLayout(
      profile = Profile.sampleData,
      close = {},
      editProfile = remember { mutableStateOf(false) },
      saveProfile = { _, _, _, _-> }
    )
  }
}

@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewUserProfileLayoutEditOn() {
  SimpleXTheme {
    UserProfileLayout(
      profile = Profile.sampleData,
      close = {},
      editProfile = remember { mutableStateOf(true) },
      saveProfile = { _, _, _, _-> }
    )
  }
}
