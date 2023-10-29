package chat.echo.app.views.bottomsheet

import SectionItemView
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*

@Composable
fun DirectChatBurnerKeyBottomSheetView(
  chatModel: ChatModel,
  chatInfo: ChatInfo,
  onBurnerTimeChanged: (String) -> Unit,
  hideBottomSheet: () -> Unit
){
  val contact :  State<Contact?> = remember { derivedStateOf { (chatModel.getContactChat(chatInfo.apiId)?.chatInfo as? ChatInfo.Direct)?.contact } }
  val ct = contact.value ?: return
  var featuresAllowed by rememberSaveable(ct, stateSaver = serializableSaver()) { mutableStateOf(contactUserPrefsToFeaturesAllowed(ct.mergedPreferences)) }

  fun saveBurnerTimer(chatModel: ChatModel, featuresAllowed: ContactFeaturesAllowed, afterSave: (String) -> Unit = {}){
    withApi {
      val prefs = contactFeaturesAllowedToPrefs(featuresAllowed)
      val toContact = chatModel.controller.apiSetContactPrefs(ct.contactId, prefs)
      if (toContact != null) {
        chatModel.updateContact(toContact)
      }
      afterSave("")
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(color = Color.White)
      .wrapContentHeight()
      .onFocusChanged { focusState ->
        if (!focusState.hasFocus) hideBottomSheet()
      }
  ) {
    Column(
      Modifier
        .fillMaxWidth()
        .padding(top = 15.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
      ) {
        IconButton(
          modifier = Modifier.align(Alignment.CenterStart),
          onClick = hideBottomSheet) {
          Icon(
            Icons.Outlined.Close, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
        }
        Text(
          text = generalGetString(R.string.burner_time_settings),
          fontSize = 16.sp,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.SemiBold,
          color = Color.Black
        )
      }
      Column(
        Modifier
          .fillMaxWidth()
          .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceEvenly
      ) {
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(30), disabled =  featuresAllowed.timedMessagesTTL != 30, click = {
          featuresAllowed = featuresAllowed.copy(timedMessagesTTL = 30)
          saveBurnerTimer(chatModel, featuresAllowed, onBurnerTimeChanged)
        })
        Divider(color = PreviewTextColor)
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(300), disabled = featuresAllowed.timedMessagesTTL != 300, click = {
          featuresAllowed = featuresAllowed.copy(timedMessagesTTL = 300)
          saveBurnerTimer(chatModel, featuresAllowed, onBurnerTimeChanged)
        })
        Divider(color = PreviewTextColor)
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(3600), disabled = featuresAllowed.timedMessagesTTL != 3600, click = {
          featuresAllowed = featuresAllowed.copy(timedMessagesTTL = 3600)
          saveBurnerTimer(chatModel, featuresAllowed, onBurnerTimeChanged)
        })
        Divider(color = PreviewTextColor)
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(8 * 3600), disabled = featuresAllowed.timedMessagesTTL != 8 * 3600, click = {
          featuresAllowed = featuresAllowed.copy(timedMessagesTTL = 8 * 3600)
          saveBurnerTimer(chatModel, featuresAllowed, onBurnerTimeChanged)
        })
        Divider(color = PreviewTextColor)
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(86400), disabled = featuresAllowed.timedMessagesTTL != 86400, click = {
          featuresAllowed = featuresAllowed.copy(timedMessagesTTL = 86400)
          saveBurnerTimer(chatModel, featuresAllowed, onBurnerTimeChanged)
        })
        Divider(color = PreviewTextColor)
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(5 * 86400), disabled = featuresAllowed.timedMessagesTTL != 5 * 86400, click = {
          featuresAllowed = featuresAllowed.copy(timedMessagesTTL = 5 * 86400)
          saveBurnerTimer(chatModel, featuresAllowed, onBurnerTimeChanged)
        })
      }
    }
  }
}

@Composable
fun GroupChatBurnerKeyBottomSheetView(m: ChatModel, chatId: String,
  hideBottomSheet: () -> Unit, close: () -> Unit) {
  val groupInfo = remember { derivedStateOf { (m.getChat(chatId)?.chatInfo as? ChatInfo.Group)?.groupInfo } }
  val gInfo = groupInfo.value ?: return
  var preferences by rememberSaveable(gInfo, stateSaver = serializableSaver()) { mutableStateOf(gInfo.fullGroupPreferences) }

  fun saveBurnerTimer(afterSave: () -> Unit = {}) {
    withApi {
      val gp = gInfo.groupProfile.copy(groupPreferences = preferences.toGroupPreferences())
      val gInfo = m.controller.apiUpdateGroup(gInfo.groupId, gp)
      if (gInfo != null) {
        m.updateGroup(gInfo)
      }
      afterSave()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(color = Color.White)
      .wrapContentHeight()
      .onFocusChanged { focusState ->
        if (!focusState.hasFocus) hideBottomSheet()
      }
  ) {
    Column(
      Modifier
        .fillMaxWidth()
        .padding(top = 15.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
      ) {
        IconButton(
          modifier = Modifier.align(Alignment.CenterStart),
          onClick = hideBottomSheet) {
          Icon(
            Icons.Outlined.Close, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
        }
        Text(
          text = generalGetString(R.string.burner_time_settings),
          fontSize = 16.sp,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.SemiBold,
          color = Color.Black
        )
      }
      Column(
        Modifier
          .fillMaxWidth()
          .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceEvenly
      ) {
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(30), disabled =  preferences.timedMessages.ttl != 30, click = {
          preferences = preferences.copy(timedMessages = TimedMessagesGroupPreference(enable =  GroupFeatureEnabled.ON, ttl = 30))
          saveBurnerTimer {
            hideBottomSheet()
            close()
          }
        })
        Divider(color = PreviewTextColor)
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(300), disabled =  preferences.timedMessages.ttl != 300, click = {
          preferences = preferences.copy(timedMessages = TimedMessagesGroupPreference(enable =  GroupFeatureEnabled.ON, ttl = 300))
          saveBurnerTimer {
            hideBottomSheet()
            close()
          }
        })
        Divider(color = PreviewTextColor)
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(3600), disabled =  preferences.timedMessages.ttl != 3600, click = {
          preferences = preferences.copy(timedMessages = TimedMessagesGroupPreference(enable =  GroupFeatureEnabled.ON, ttl = 3600))
          saveBurnerTimer {
            hideBottomSheet()
            close()
          }
        })
        Divider(color = PreviewTextColor)
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(8 * 3600), disabled =  preferences.timedMessages.ttl != 8 * 3600, click = {
          preferences = preferences.copy(timedMessages = TimedMessagesGroupPreference(enable =  GroupFeatureEnabled.ON, ttl = 8 * 3600))
          saveBurnerTimer {
            hideBottomSheet()
            close()
          }
        })
        Divider(color = PreviewTextColor)
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(86400), disabled =  preferences.timedMessages.ttl != 86400, click = {
          preferences = preferences.copy(timedMessages = TimedMessagesGroupPreference(enable =  GroupFeatureEnabled.ON, ttl = 86400))
          saveBurnerTimer {
            hideBottomSheet()
            close()
          }
        })
        Divider(color = PreviewTextColor)
        BurnerTimeItem(activeIcon = Icons.Outlined.Check,  text = TimedMessagesPreference.ttlText(5 * 86400), disabled =  preferences.timedMessages.ttl != 5 * 86400, click = {
          preferences = preferences.copy(timedMessages = TimedMessagesGroupPreference(enable =  GroupFeatureEnabled.ON, ttl = 5 * 86400))
          saveBurnerTimer {
            hideBottomSheet()
            close()
          }
        })
      }
    }
  }
}

fun contactFeaturesAllowedToPrefs(contactFeaturesAllowed: ContactFeaturesAllowed): ChatPreferences =
 ChatPreferences(
    timedMessages = TimedMessagesPreference(if (contactFeaturesAllowed.timedMessagesAllowed) FeatureAllowed.YES else FeatureAllowed.NO, contactFeaturesAllowed.timedMessagesTTL),
    fullDelete = contactFeatureAllowedToPref(contactFeaturesAllowed.fullDelete),
    voice = contactFeatureAllowedToPref(contactFeaturesAllowed.voice),
    calls = contactFeatureAllowedToPref(contactFeaturesAllowed.calls)
  )

@Composable
fun BurnerTimeItem(activeIcon: ImageVector, text: String, click: (() -> Unit)? = null, textColor: Color = Color.Black, iconColor: Color = HighOrLowlight, disabled: Boolean = false) {
  SectionItemView(click) {
    Text(text, 
      modifier = Modifier.fillMaxWidth().weight(1f), 
      color = if (disabled) HighOrLowlight else textColor)
    Spacer(Modifier.padding(horizontal = 4.dp))
    (if(!disabled) activeIcon else null)?.let { Icon(it, text, tint = if (disabled) HighOrLowlight else iconColor) }
  }
}
