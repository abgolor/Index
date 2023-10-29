package chat.echo.app.views.chat.group

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIos
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chatlist.setGroupMembers
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.TextHeader
import chat.echo.app.views.usersettings.EditImageButton
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditGroupView(chatModel: ChatModel, groupInfo: GroupInfo,  groupProfile: GroupProfile, isZeroKnowledge: Boolean, close: () -> Unit) {
  val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val scope = rememberCoroutineScope()
  val groupImage = remember { mutableStateOf<String?>(groupProfile.image) }
  val chosenImage = rememberSaveable { mutableStateOf<Uri?>(null) }
  val groupDisplayName = remember {
    mutableStateOf(
      TextFieldValue(
        text = if (isZeroKnowledge) groupProfile.displayName.removePrefix("*") else groupProfile.displayName,
        selection = TextRange(if (isZeroKnowledge) groupProfile.displayName.length - 1 else groupProfile.displayName.length)
      )
    )
  }
  val groupFullName = remember {
    mutableStateOf(
      TextFieldValue(
        text = groupProfile.fullName,
        selection = TextRange(groupProfile.fullName.length)
      )
    )
  }
  val updateGroupProfile: (GroupProfile) -> Unit = { groupProfile ->
    withApi {
      val gInfo = chatModel.controller.apiUpdateGroup(groupInfo.groupId, groupProfile)
      if (gInfo != null) {
        chatModel.updateGroup(gInfo)
        close.invoke()
      }
    }
  }

  BackHandler(onBack = close)
  ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
    ModalBottomSheetLayout(
      scrimColor = Color.Black.copy(alpha = 0.12F),
      modifier = Modifier.navigationBarsWithImePadding(),
      sheetContent = {
        GetImageBottomSheet(
          chosenImage,
          groupImage,
          onImageChange = { bitmap -> groupImage.value = resizeImageToStrSize(cropToSquare(bitmap), maxDataSize = 12500) },
          hideBottomSheet = {
            scope.launch { bottomSheetModalState.hide() }
          })
      },
      sheetState = bottomSheetModalState,
      sheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
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
                  .padding(end = 15.dp, top = 10.dp, bottom = 10.dp)
              ) {
                IconButton(onClick = close) {
                  Icon(
                    Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Text(
                  text = generalGetString(R.string.edit_group),
                  fontSize = 16.sp,
                  modifier = Modifier.align(Alignment.Center),
                  fontWeight = FontWeight.Medium,
                  color = Color.Black
                )
                Text(
                  text = generalGetString(R.string.update),
                  fontSize = 15.sp,
                  modifier = Modifier.align(Alignment.CenterEnd)
                    .clickable {
                      if (groupDisplayName.value.text.isNotEmpty()) {
                        if (isZeroKnowledge) {
                          updateGroupProfile(GroupProfile("*" + groupDisplayName.value.text.replace(" ", ""), groupFullName.value.text, groupImage.value))
                        } else {
                          updateGroupProfile(GroupProfile(groupDisplayName.value.text, groupFullName.value.text, groupImage.value))
                        }
                      }
                    },
                  fontWeight = FontWeight.Medium,
                  color = if (groupDisplayName.value.text.isNotEmpty()) Color.Black else PreviewTextColor
                )
              }
            }
            EditGroupContent(
              bottomSheetModalState = bottomSheetModalState,
              groupImage = groupImage,
              groupDisplayName = groupDisplayName,
              groupFullName = groupFullName
            )
          }
        }
      }
    }

  }
}

@Composable
fun EditGroupContent(
  bottomSheetModalState: ModalBottomSheetState,
  groupImage: MutableState<String?>,
  groupDisplayName:  MutableState<TextFieldValue>,
  groupFullName:  MutableState<TextFieldValue>
) {
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }

  Box(
    Modifier
      .fillMaxWidth()
      .padding(5.dp),
    contentAlignment = Alignment.Center
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      Box(contentAlignment = Alignment.Center) {
        ProfileImage(size = 200.dp, image = groupImage.value, color = MaterialTheme.colors.secondary)
        EditImageButton {
          coroutineScope.launch {
            focusManager.clearFocus()
            bottomSheetModalState.show()
          }
        }
      }
    }
  }
  TextHeader(headerTitle = generalGetString(R.string.group_display_name), color = Color.Black)
  BasicTextField(
    value = groupDisplayName.value,
    onValueChange = {
      groupDisplayName.value = it
    },
    modifier = Modifier
      .padding(start = 10.dp, end = 10.dp)
      .fillMaxWidth()
      .focusRequester(focusRequester),
    decorationBox = { innerTextField ->
      Box(
        contentAlignment = Alignment.CenterStart
      ) {
        if (groupDisplayName.value.text.isEmpty()) {
          Text(
            text = generalGetString(R.string.enter_group_display_name),
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
  Spacer(modifier = Modifier.padding(top = 10.dp))
  TextHeader(headerTitle = generalGetString(R.string.group_full_name), color = Color.Black)
  BasicTextField(
    value = groupFullName.value,
    onValueChange = {
      groupFullName.value = it
    },
    modifier = Modifier
      .padding(start = 10.dp, end = 10.dp)
      .fillMaxWidth(),
    decorationBox = { innerTextField ->
      Box(
        contentAlignment = Alignment.CenterStart
      ) {
        if (groupFullName.value.text.isEmpty()) {
          Text(
            text = generalGetString(R.string.enter_group_full_name),
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
      capitalization = KeyboardCapitalization.Words,
      autoCorrect = false
    ),
    singleLine = true,
    cursorBrush = SolidColor(HighOrLowlight)
  )

  LaunchedEffect(Unit){
    delay(300)
    focusRequester.requestFocus()
  }
}