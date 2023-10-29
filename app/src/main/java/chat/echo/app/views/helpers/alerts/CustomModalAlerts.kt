package chat.echo.app.views.helpers.alerts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.window.Dialog
import chat.echo.app.R
import chat.echo.app.model.ChatModel
import chat.echo.app.views.helpers.generalGetString
import chat.echo.app.views.helpers.withApi

@Composable
fun CustomDialog(openDialogCustom: MutableState<Boolean>) {
  if (openDialogCustom.value) {
    Dialog(onDismissRequest = { openDialogCustom.value = false }) {
      //LoadingDialogLayout(isLoading = openDialogCustom)
    }
  }
}

@Composable
fun ConnectionTimeoutDialog(isDialogShown: MutableState<Boolean>) {
  if (isDialogShown.value) {
    Dialog(onDismissRequest = { isDialogShown.value = false }) {
      NoticeDialogLayout(
        title = generalGetString(R.string.connection_timeout),
        message = generalGetString(R.string.network_error_desc)
      )
    }
  }
}

@Composable
fun DeleteIndexAddressWarning(chatModel: ChatModel, isDialogShown: MutableState<Boolean>) {
  if(isDialogShown.value){
    Dialog(onDismissRequest = { isDialogShown.value = false }) {
      WarningDialogLayout(
        title = generalGetString(R.string.delete_address__question),
        message = generalGetString(R.string.all_your_contacts_will_remain_connected),
        ok = generalGetString((R.string.delete_verb)),
        onOKAction = {
          withApi {
            chatModel.controller.apiDeleteUserAddress()
            chatModel.userAddress.value = null
          }
        },
        onCancelActon = {
          isDialogShown.value = false
        }
      )
    }
  }
}

@Composable
fun GeneratingIndexCode(isDialogShown: MutableState<Boolean>) {
  if(isDialogShown.value){
    Dialog(onDismissRequest = { isDialogShown.value = true }) {
      LoadingDialogLayout()
    }
  }
}