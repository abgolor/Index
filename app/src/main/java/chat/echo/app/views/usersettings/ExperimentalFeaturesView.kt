package chat.echo.app.views.usersettings

import chat.echo.app.R
import SectionView
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.echo.app.model.ChatModel

@Composable
fun ExperimentalFeaturesView(chatModel: ChatModel, enableCalls: MutableState<Boolean>) {
  Column(
    Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.Start
  ) {
    Text(
      stringResource(R.string.settings_experimental_features),
      style = MaterialTheme.typography.h1,
      modifier = Modifier.padding(start = 16.dp, bottom = 24.dp)
    )
    SectionView("") {
      SettingsPreferenceItem(Icons.Outlined.Videocam, stringResource(R.string.settings_audio_video_calls), chatModel.controller.appPrefs.experimentalCalls, enableCalls)
    }
  }
}