package chat.echo.app.views.notification

import android.os.Build
import android.util.Log
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import chat.echo.app.R
import chat.echo.app.SimplexService
import chat.echo.app.model.*
import chat.echo.app.ui.theme.PreviewTextColor
import chat.echo.app.views.chat.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.navbar.BottomBarView
import chat.echo.app.views.newchat.CreateUserLinkView
import chat.echo.app.views.onboarding.OnboardingStage
import chat.echo.app.views.search.SearchViewLayout
import kotlinx.coroutines.launch
import java.util.jar.Manifest

@Composable
fun NotificationPermissionView(
  chatModel: ChatModel
) {
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
        IconButton({
          AlertManager.shared.showAlertDialog(
            title = generalGetString(R.string.notification_not_enabled),
            text = generalGetString(R.string.notification_not_enabled_desc),
            confirmText = generalGetString(R.string.continue_reg),
            onConfirm = {
              chatModel.controller.appPrefs.isNotificationEnabled.set(true)
              chatModel.onboardingStage.value = OnboardingStage.SigningIn
            }
          )
        }) {
          Icon(
            Icons.Outlined.Close, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(22.dp)
          )
        }
        Text(
          text = generalGetString(R.string.notification_permission),
          fontSize = 18.sp,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.Medium,
          color = Color.Black
        )
      }
      Divider(color = PreviewTextColor)
      NotificationPermissionLayout(chatModel)
    }
  }
}

@Composable
fun NotificationPermissionLayout(
  chatModel: ChatModel
) {
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){isGranted ->
    Log.i("TAG", "NotificationPermissionLayout: is granted is " + isGranted)
    if(isGranted){
      chatModel.controller.appPrefs.isNotificationEnabled.set(true)
    } else {
      chatModel.controller.appPrefs.isNotificationEnabled.set(false)
    }
    chatModel.onboardingStage.value = OnboardingStage.SigningIn
  }

  Box(
    Modifier
      .background(Color.White)
      .fillMaxSize()
      .padding(10.dp)
  ) {
    Column(
      Modifier
        .fillMaxWidth()
        .padding(20.dp)
        .align(Alignment.Center)
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_notification_icon),
        contentDescription = generalGetString(R.string.notification_icon),
        tint = Color.Black,
        modifier = Modifier
          .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
          .size(100.dp)
          .align(Alignment.CenterHorizontally)
      )
      Spacer(modifier = Modifier.size(10.dp))
      Text(
        text = generalGetString(R.string.enable_notification),
        textAlign = TextAlign.Center,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth(),
        fontWeight = FontWeight.SemiBold,
        color = Color.Black,
      )
      Spacer(modifier = Modifier.size(5.dp))
      Text(
        text = generalGetString(R.string.enable_notification_desc),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Black
      )
      Spacer(modifier = Modifier.size(30.dp))
      Button(
        onClick = {
          if (Build.VERSION.SDK_INT >= 33) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
          }
        },
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .height(55.64.dp)
          .padding(8.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = MaterialTheme.colors.background
        ),
        shape = RoundedCornerShape(5.dp)
      ) {
        Text(
          text = generalGetString(R.string.allow_notifications),
          textAlign = TextAlign.Center,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White,
          modifier = Modifier.padding(start = 15.dp, end = 15.dp)
        )
      }
    }
  }
}