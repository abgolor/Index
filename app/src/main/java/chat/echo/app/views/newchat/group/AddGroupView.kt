package chat.echo.app.views.newchat

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.CustomTextInputFieldView
import chat.echo.app.views.chat.group.AddGroupMembersView
import chat.echo.app.views.chat.group.getContactsToAdd
import chat.echo.app.views.chatlist.setGroupMembers
import chat.echo.app.views.helpers.*
import chat.echo.app.views.isValidDisplayName
import chat.echo.app.views.usersettings.*
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddGroupView(chatModel: ChatModel, isZeroKnowledge: Boolean, close: () -> Unit) {
  val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val scope = rememberCoroutineScope()
  val groupImage = remember { mutableStateOf<String?>(null) }
  val chosenImage = rememberSaveable { mutableStateOf<Uri?>(null) }
  val groupDisplayName = remember { mutableStateOf("") }
  val groupFullName = remember { mutableStateOf("") }
  val createGroup: (GroupProfile) -> Unit = { groupProfile ->
    withApi {
      val groupInfo = chatModel.controller.apiNewGroup(groupProfile)
      if (groupInfo != null) {
        chatModel.addChat(Chat(chatInfo = ChatInfo.Group(groupInfo), chatItems = listOf()))
        chatModel.chatItems.clear()
        chatModel.chatId.value = groupInfo.id
        setGroupMembers(groupInfo, chatModel)
        chatModel.isGroupCreated.value = true
        close.invoke()
        if (getContactsToAdd(chatModel).isNotEmpty()) {
          ModalManager.shared.showCustomModal { close ->
            AddGroupMembersView(chatModel, groupInfo, isZeroKnowledge, close)
          }
        }
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
                  text = if (isZeroKnowledge) generalGetString(R.string.create_zero_knowledge_group) else generalGetString(R.string.create_secret_group_title),
                  fontSize = 16.sp,
                  modifier = Modifier.align(Alignment.Center),
                  fontWeight = FontWeight.Medium,
                  color = Color.Black
                )
                Text(
                  text = generalGetString(R.string.create),
                  fontSize = 15.sp,
                  modifier = Modifier.align(Alignment.CenterEnd)
                    .clickable {
                      if (groupDisplayName.value.isNotEmpty()) {
                        if (isZeroKnowledge) {
                          createGroup(GroupProfile("*" + groupDisplayName.value.replace(" ", ""), groupDisplayName.value, groupImage.value))
                        } else {
                          createGroup(GroupProfile(groupDisplayName.value, groupFullName.value, groupImage.value))
                        }
                      }
                    },
                  fontWeight = FontWeight.Medium,
                  color = if (groupDisplayName.value.isNotEmpty()) Color.Black else PreviewTextColor
                )
              }
            }
            AddGroupContent(
              bottomSheetModalState = bottomSheetModalState,
              groupImage = groupImage,
              groupDisplayName = groupDisplayName,
              groupFullName = groupFullName,
              close = close
            )
          }
        }
      }
    }
  }
}

@Composable
fun AddGroupContent(
  bottomSheetModalState: ModalBottomSheetState,
  groupImage: MutableState<String?>,
  groupDisplayName: MutableState<String>,
  groupFullName: MutableState<String>,
  close: () -> Unit
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
        if (groupDisplayName.value.isEmpty()) {
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
        if (groupFullName.value.isEmpty()) {
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
}

@Composable
fun AddGroupLayout(chatModelIncognito: Boolean, createGroup: (GroupProfile) -> Unit, close: () -> Unit) {
  val bottomSheetModalState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
  val scope = rememberCoroutineScope()
  val displayName = remember { mutableStateOf("") }
  val fullName = remember { mutableStateOf("") }
  val profileImage = remember { mutableStateOf<String?>(null) }
  val chosenImage = rememberSaveable { mutableStateOf<Uri?>(null) }
  val focusRequester = remember { FocusRequester() }
  val isZeroKnowledge = remember { mutableStateOf(false) }

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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DEFAULT_PADDING)
        ) {
          AppBarTitle(stringResource(R.string.create_secret_group_title), false)
          Text(stringResource(R.string.group_is_decentralized))
          InfoAboutIncognito(
            chatModelIncognito,
            false,
            generalGetString(R.string.group_unsupported_incognito_main_profile_sent),
            generalGetString(R.string.group_main_profile_sent)
          )
          Box(
            Modifier
              .fillMaxWidth()
              .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            Box(contentAlignment = Alignment.TopEnd) {
              Box(contentAlignment = Alignment.Center) {
                ProfileImage(size = 192.dp, image = profileImage.value)
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
          CustomTextInputFieldView(fullName, "Your name")
          Spacer(modifier = (Modifier.height(5.dp)))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 15.dp)
          ) {
            Text(generalGetString(R.string.make_zero_knowledge))
            Spacer(Modifier.fillMaxWidth())
            Switch(
              checked = isZeroKnowledge.value,
              onCheckedChange = {
                isZeroKnowledge.value = it
              }
            )
          }
          Spacer(Modifier.height(8.dp))
          val enabled = displayName.value.isNotEmpty() && isValidDisplayName(displayName.value)
          if (enabled) {
            CreateGroupButton(MaterialTheme.colors.primary, Modifier
              .clickable {
                if (isZeroKnowledge.value) {
                  createGroup(GroupProfile("*" + displayName.value, fullName.value, profileImage.value))
                } else {
                  createGroup(GroupProfile(displayName.value, fullName.value, profileImage.value))
                }
              }
              .padding(8.dp))
          } else {
            CreateGroupButton(HighOrLowlight, Modifier.padding(8.dp))
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

@Composable
fun TextHeader(headerTitle: String, color: Color = TextHeader) {
  Row(Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 5.dp)) {
    Text(
      text = headerTitle,
      fontSize = 16.sp,
      modifier = Modifier.align(Alignment.CenterVertically),
      fontWeight = FontWeight.Normal,
      color = color
    )
  }
}

@Composable
fun CreateGroupButton(color: Color, modifier: Modifier) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End
  ) {
    Surface(shape = RoundedCornerShape(20.dp)) {
      Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.create_profile_button), style = MaterialTheme.typography.caption, color = color)
        Icon(Icons.Outlined.ArrowForwardIos, stringResource(R.string.create_profile_button), tint = color)
      }
    }
  }
}
/*@Preview
@Composable
fun PreviewAddGroupLayout() {
  SimpleXTheme {
    AddGroupLayout(
      chatModelIncognito = false,
      createGroup = {}, ,
      close = {}
    )
  }
}*/
