package chat.echo.app.views.chat.group

import SectionDivider
import SectionItemView
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.bottomsheet.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.*
import chat.echo.app.views.usersettings.EditImageButton
import chat.echo.app.views.usersettings.UserAddressView
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChangeBurnerTimeView(chatModel: ChatModel, chatId: String,  onBurnerTimeChanged: (GroupProfile) -> Unit, close: () -> Unit, checked: Boolean) {

  val scaffoldState = rememberScaffoldState()
  val burnerTimes = listOf(30, 300, 3600, 8 * 3600, 86400)
  val groupInfo = remember { derivedStateOf { (chatModel.getChat(chatId)?.chatInfo as? ChatInfo.Group)?.groupInfo } }
  val gInfo = groupInfo.value ?: return
  var preferences by rememberSaveable(gInfo, stateSaver = serializableSaver()) { mutableStateOf(gInfo.fullGroupPreferences) }
  var currentPreferences by rememberSaveable(gInfo, stateSaver = serializableSaver()) { mutableStateOf(preferences) }

  fun saveBurnerTimer(chatModel: ChatModel, afterSave: (GroupProfile) -> Unit = {}){
    withApi {
      val gp = gInfo.groupProfile.copy(groupPreferences = preferences.toGroupPreferences())
      val gInfo = chatModel.controller.apiUpdateGroup(gInfo.groupId, gp)
      if (gInfo != null) {
        chatModel.updateGroup(gInfo)
        currentPreferences = preferences
      }
      //TODO: afterSave()
    }
  }

  BackHandler(onBack = close)
  Scaffold(
    topBar = {
      Column() {
        BurnerTimeSettingsToolbar(close = close, centered = true)
        Divider(color = PreviewTextColor)
      }
    },
    scaffoldState = scaffoldState,
    drawerGesturesEnabled = false,
    backgroundColor = Color.White
  ) {
    Box(modifier = Modifier.padding(it)) {
      Column(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
      ) {
        Spacer(modifier = Modifier.padding(vertical = 10.dp))
        burnerTimes.forEachIndexed{index, burnerTime ->
          BurnerTimeItemView(burnerTimerText = TimedMessagesPreference.ttlText(86400), textColor = Color.Black, onSelected = {
            saveBurnerTimer(chatModel = chatModel, {})}, checked = burnerTime == currentPreferences.timedMessages.ttl)
          if (index < burnerTimes.lastIndex) {
            SectionDivider()
          }
        }
      }
    }
  }
}

@Composable
fun BurnerTimeItemView(
  burnerTimerText: String,
  textColor: Color,
  onSelected: () -> Unit,
  checked: Boolean
) {
  val icon: ImageVector
  val iconColor: Color
  if (checked) {
    icon = Icons.Filled.CheckCircle
    iconColor = Color.Black
  }
  else {
    icon = Icons.Outlined.Circle
    iconColor = PreviewTextColor
  }

  Row(Modifier.background(Color.White)
    .clickable {
      onSelected()
    }) {
    SectionItemView() {
      Text(burnerTimerText, color = textColor)
      Spacer(Modifier.fillMaxWidth().weight(1f))
      Icon(
        icon,
        contentDescription = if(checked) generalGetString(R.string.burner_time_checked) else generalGetString(R.string.burner_time_unchecked),
        modifier = Modifier.size(26.dp),
        tint = iconColor
      )
    }
  }
}

@Composable fun BurnerTimeSettingsToolbar(close: () -> Unit, centered: Boolean) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(AppBarHeight)
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color.White)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(
        Modifier
          .fillMaxHeight()
          .width(TitleInsetWithIcon - AppBarHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(close) {
          Icon(
            Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(22.dp)
          )
        }
      }
      val startPadding = TitleInsetWithIcon
      val endPadding = (0 * 50f).dp
      Box(
        Modifier
          .fillMaxWidth()
          .padding(
            start = if (centered) kotlin.math.max(startPadding.value, endPadding.value).dp else startPadding,
            end = if (centered) kotlin.math.max(startPadding.value, endPadding.value).dp else endPadding
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = generalGetString(R.string.burner_time_settings),
          fontSize = 18.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.Medium,
          color = Color.Black
        )
      }
    }
  }
}

/*
fun saveBurnerTimer(chatModel: ChatModel, groupInfo: GroupInfo, burnerTime: Long,  onBurnerTimeChanged: (GroupProfile) -> Unit){
  withApi {
    val content = MsgContent.MCBurnerTimer("cmd burner ${burnerTime}", burnerTime)
    val command = chatModel.controller.apiSendMessage(ChatType.Group, groupInfo.apiId, null, null, content)
    if(command != null){
      val initialContactBioExtra = if(groupInfo.localAlias != "") {
        json.decodeFromString(ContactBioInfoSerializer, groupInfo.localAlias)
      } else {
        ContactBioInfo.ContactBioExtra("", "", publicKey = "", openKeyChainID = "")
      }
      val currentContactBioExtra = ContactBioInfo.ContactBioExtra(initialContactBioExtra.tag, initialContactBioExtra.notes, burnerTime, initialContactBioExtra.publicKey, initialContactBioExtra.openKeyChainID)
      onBurnerTimeChanged(GroupProfile(displayName = groupInfo.displayName, fullName = groupInfo.fullName, image = groupInfo.image, localAlias = json.encodeToString(ContactBioInfoSerializer, currentContactBioExtra)))
    }
  }
}
*/

fun getBurnerTimerText(burnerTimer: Long): String {
  if (burnerTimer == 30L) return "30 seconds"
  if (burnerTimer == 300L) return "5 minutes"
  if (burnerTimer == 3600L) return "1 hour"
  if (burnerTimer == 28800L) return "8 hours"
  if (burnerTimer == 86400L) return "1 day"
  if (burnerTimer == 432000L) return "5 days"
  return "None"
}
