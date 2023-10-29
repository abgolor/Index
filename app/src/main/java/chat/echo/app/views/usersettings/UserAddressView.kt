package chat.echo.app.views.usersettings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import chat.echo.app.R
import chat.echo.app.model.ChatModel
import chat.echo.app.model.UserContactLinkRec
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.helpers.alerts.WarningDialogLayout
import chat.echo.app.views.navbar.BottomBarView
import chat.echo.app.views.newchat.QRCode

@Composable
fun UserAddressView(chatModel: ChatModel) {
  val cxt = LocalContext.current
  val isShowDeleteAddressWarning = remember {
    mutableStateOf(false)
  }
  UserAddressLayout(
    userAddress = remember { chatModel.userAddress }.value,
    createAddress = {
      withApi {
        val connReqContact = chatModel.controller.apiCreateUserAddress()
        if (connReqContact != null) {
          chatModel.userAddress.value = UserContactLinkRec(connReqContact)
        }
      }
    },
    refreshUserAddress = {
      AlertManager.shared.showAlertDialog(
        title = generalGetString(R.string.refresh_index_code_question),
        text = generalGetString(R.string.refresh_index_code_descr),
        confirmText = generalGetString(R.string.refresh_verb),
        onConfirm = {
          withApi {
            val connReqContact = chatModel.controller.apiRefreshUserAddress()
            if (connReqContact != null) {
              chatModel.userAddress.value = UserContactLinkRec(connReqContact)
            }
          }
        },
        dismissText = generalGetString(R.string.cancel_verb)
      )
    },
    share = { userAddress: String -> shareText(cxt, userAddress) },
    deleteAddress = {
      AlertManager.shared.showAlertMsg(
        title = generalGetString(R.string.delete_address__question),
        text = generalGetString(R.string.all_your_contacts_will_remain_connected),
        confirmText = generalGetString(R.string.delete_verb),
        onConfirm = {
          withApi {
            chatModel.controller.apiDeleteUserAddress()
            chatModel.userAddress.value = null
          }
        }
      )
    }
  )
}

@Composable
fun UserAddressLayout(
  userAddress: UserContactLinkRec?,
  createAddress: () -> Unit,
  refreshUserAddress: () -> Unit,
  share: (String) -> Unit,
  deleteAddress: () -> Unit
) {
  Column(
    Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.Center
  ) {
    Column(
      Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      if (userAddress == null) {
        Box(
          Modifier.fillMaxSize()
            .padding(bottom = 100.dp)
        ) {
          Column(Modifier.align(Alignment.Center)) {
            Icon(
              Icons.Outlined.MoodBad,
              contentDescription = "icon",
              tint = Color.Black,
              modifier = Modifier
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                .size(100.dp)
                .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
              text = generalGetString(R.string.no_index_code_found),
              textAlign = TextAlign.Center,
              fontSize = 14.sp,
              fontWeight = FontWeight.Normal,
              color = Color.Black
            )
            Spacer(modifier = Modifier.size(5.dp))
            Text(
              text = generalGetString(R.string.generate_index_code),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              color = Color.Black,
              textDecoration = TextDecoration.Underline,
              modifier = Modifier.clickable {
                createAddress()
              }
            )
          }
          /*if (!stopped && !newChatSheetState.collectAsState().value.isVisible()) {
           OnboardingButtons(showNewChatSheet)
         }*/
          //Text(stringResource(R.string.you_have_no_contacts), Modifier.align(Alignment.Center), color = HighOrLowlight)
        }
      }
      else {
            QRCode(userAddress.connReqContact, Modifier
              .size(300.dp)
              .weight(1f, fill = false).aspectRatio(1f))
            Row(
              horizontalArrangement = Arrangement.spacedBy(15.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(vertical = 20.dp)
            ) {
              IconButton(onClick = refreshUserAddress) {
                Icon(
                  imageVector = Icons.Outlined.Refresh,
                  contentDescription = generalGetString(R.string.refresh),
                  modifier = Modifier.size(30.dp),
                  tint = Color.Black
                )
              }
              IconButton(onClick = {
                if (userAddress != null) {
                  share(userAddress.connReqContact)
                }
              }) {
                Icon(
                  imageVector = Icons.Outlined.IosShare,
                  contentDescription = generalGetString(R.string.refresh),
                  modifier = Modifier.size(30.dp),
                  tint = Color.Black
                )
              }
              IconButton(onClick = deleteAddress) {
                Icon(
                  imageVector = Icons.Outlined.Delete,
                  contentDescription = generalGetString(R.string.refresh),
                  modifier = Modifier.size(30.dp),
                  tint = Color.Black
                )
              }
            }
            Text(
              text = generalGetString(R.string.my_index_code_instruction),
              textAlign = TextAlign.Center,
              fontSize = 14.sp,
              modifier = Modifier
                .padding(top = 20.dp),
              fontWeight = FontWeight.Normal,
              color = HighOrLowlight
            )
      }
    }
  }
}