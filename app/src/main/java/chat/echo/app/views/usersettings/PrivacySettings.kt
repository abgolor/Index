package chat.echo.app.views.usersettings

import SectionDivider
import SectionSpacer
import SectionView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import chat.echo.app.R
import chat.echo.app.UpdateTwoAppPasswordView
import chat.echo.app.model.ChatModel
import chat.echo.app.views.CreateTwoAppPasswordView
import chat.echo.app.views.helpers.*

@Composable
fun PrivacySettingsView(chatModel: ChatModel, setPerformLA: (Boolean) -> Unit) {
  Column(
    Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.Start
  ) {
    AppBarTitle(stringResource(R.string.your_privacy))
    SectionView(stringResource(R.string.settings_section_title_device)) {
      ChatLockItem(chatModel.performLA, setPerformLA)
      SectionDivider()
      ChangeAppPasswordItem(chatModel.performLA){
        ModalManager.shared.showModal {
          UpdateTwoAppPasswordView()
        }
      }
    }
    SectionSpacer()

    SectionView(stringResource(R.string.settings_section_title_chats)) {
      SettingsPreferenceItem(Icons.Outlined.Image, stringResource(R.string.auto_accept_images), chatModel.controller.appPrefs.privacyAcceptImages)
      SectionDivider()
      SettingsPreferenceItem(Icons.Outlined.TravelExplore, stringResource(R.string.send_link_previews), chatModel.controller.appPrefs.privacyLinkPreviews)
    }
  }
}
