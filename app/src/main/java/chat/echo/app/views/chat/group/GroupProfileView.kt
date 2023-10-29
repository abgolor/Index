package chat.echo.app.views.chat.group

import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.CustomTextInputFieldView
import chat.echo.app.views.helpers.*
import chat.echo.app.views.isValidDisplayName
import chat.echo.app.views.usersettings.*
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GroupProfileView(groupInfo: GroupInfo, chatModel: ChatModel, close: () -> Unit) {
  GroupProfileLayout(
    close = close,
    groupProfile = groupInfo.groupProfile,
    saveProfile = { p ->
      withApi {
        val gInfo = chatModel.controller.apiUpdateGroup(groupInfo.groupId, p)
        if (gInfo != null) {
          chatModel.updateGroup(gInfo)
          close.invoke()
        }
      }
    }
  )
}

@Composable
fun GroupProfileLayout(
  close: () -> Unit,
  groupProfile: GroupProfile,
  saveProfile: (GroupProfile) -> Unit,
) {
  val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val displayName = remember { mutableStateOf(groupProfile.displayName.removePrefix("*")) }
  val fullName = remember { mutableStateOf(groupProfile.fullName) }
  val chosenImage = rememberSaveable { mutableStateOf<Uri?>(null) }
  val profileImage = remember { mutableStateOf(groupProfile.image) }
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  val focusRequester = remember { FocusRequester() }

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
          Text(
            stringResource(R.string.group_profile_is_stored_on_members_devices),
            Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colors.onBackground,
            lineHeight = 22.sp
          )
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
            Text(
              stringResource(R.string.group_display_name_field),
              Modifier.padding(bottom = 3.dp)
            )
            CustomTextInputFieldView(displayName, "Your display name", focusRequester)
            val errorText = if (!isValidDisplayName(displayName.value)) stringResource(R.string.display_name_cannot_contain_whitespace) else ""
            Text(
              errorText,
              fontSize = 15.sp,
              color = MaterialTheme.colors.error
            )
            Spacer(Modifier.height(3.dp))
            Text(
              stringResource(R.string.group_full_name_field),
              Modifier.padding(bottom = 5.dp)
            )
            CustomTextInputFieldView(fullName, " Your name")
            Spacer(Modifier.height(16.dp))
            Row {
              TextButton(stringResource(R.string.cancel_verb)) {
                close.invoke()
              }
              Spacer(Modifier.padding(horizontal = 8.dp))
              val enabled = displayName.value.isNotEmpty() && isValidDisplayName(displayName.value)
              if (enabled) {
                Text(
                  stringResource(R.string.save_group_profile),
                  modifier = Modifier.clickable {
                    if (groupProfile.displayName.get(0).equals('*', true)&&
                      !displayName.value.contains("*")
                    ) {
                      displayName.value = "*${displayName.value}"
                    }
                    saveProfile(GroupProfile(displayName.value, fullName.value, profileImage.value, ""))
                  },
                  color = MaterialTheme.colors.primary
                )
              } else {
                Text(
                  stringResource(R.string.save_group_profile),
                  color = HighOrLowlight
                )
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
  }
}

@Preview(showBackground = true)
@Preview(
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  showBackground = true,
  name = "Dark Mode"
)
@Composable
fun PreviewGroupProfileLayout() {
  SimpleXTheme {
    GroupProfileLayout(
      close = {},
      groupProfile = GroupProfile.sampleData,
      saveProfile = { _ -> }
    )
  }
}
