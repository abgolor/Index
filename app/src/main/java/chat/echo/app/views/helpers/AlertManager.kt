package chat.echo.app.views.helpers

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import chat.echo.app.R
import chat.echo.app.TAG
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.alerts.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class AlertManager {
  var alertView = mutableStateOf<(@Composable () -> Unit)?>(null)
  var loadingView = mutableStateOf<(@Composable () -> Unit)?>(null)
  var presentAlert = mutableStateOf<Boolean>(false)
  var showLoading = mutableStateOf<Boolean>(false)
  val mainScope = MainScope()

  fun showAlert(alert: @Composable () -> Unit) {
    Log.d(TAG, "AlertManager.showAlert")
    alertView.value = alert
    showLoading.value = false
    presentAlert.value = true
  }

  fun showLoading(alert: @Composable () -> Unit) {
    Log.d(TAG, "showLoading: AlertManager.showLoading")
    loadingView.value = alert
    showLoading.value = true
    alertView.value = null
    presentAlert.value = false
  }

  fun hideLoading() {
    loadingView.value = null
    showLoading.value = false
  }

  fun hideAlert() {
    presentAlert.value = false
    showLoading.value = false
    loadingView.value = null
    alertView.value = null
  }

  fun showLoadingAlert() {
    mainScope.launch {
      showLoading {
        Dialog(onDismissRequest = { null }) {
          LoadingDialogLayout()
        }
      }
    }
  }

  fun showAlertDialogButtons(
    title: String,
    text: String? = null,
    buttons: @Composable () -> Unit,
  ) {
    val alertText: (@Composable () -> Unit)? = if (text == null) null else { -> Text(text) }
    showAlert {
      AlertDialog(
        onDismissRequest = this::hideAlert,
        title = { Text(title) },
        text = alertText,
        buttons = buttons
      )
    }
  }

  fun showAlertDialog(
    title: String,
    text: String = "",
    confirmText: String = generalGetString(R.string.ok),
    onConfirm: (() -> Unit) = {},
    dismissText: String = generalGetString(R.string.cancel_verb),
    onDismiss: (() -> Unit) = { },
    onDismissRequest: (() -> Unit) = {},
    destructive: Boolean = true
  ) {
    mainScope.launch {
      showAlert {
        Dialog(onDismissRequest = { onDismissRequest?.invoke(); if (destructive) hideAlert() }) {
          WarningDialogLayout(title = title, message = text, ok = confirmText, onOKAction = { onConfirm(); hideAlert() },
            onCancelActon = { onDismiss(); hideAlert() }, cancelText = dismissText
          )
        }
    }
    }
  }

  fun showAlertDialogButtonsColumn(
    title: String,
    text: AnnotatedString? = null,
    buttons: @Composable () -> Unit,
  ) {
   showAlert {
      Dialog(onDismissRequest = this::hideAlert) {
        Column(
          Modifier
            .background(if (isInDarkTheme()) Color(0xff222222) else MaterialTheme.colors.background, RoundedCornerShape(corner = CornerSize(25.dp)))
            .padding(bottom = DEFAULT_PADDING)
        ) {
          Text(
            title,
            Modifier.fillMaxWidth().padding(vertical = DEFAULT_PADDING),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
          )
          CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
            if (text != null) {
              Text(text, Modifier.fillMaxWidth().padding(start = DEFAULT_PADDING, end = DEFAULT_PADDING, bottom = DEFAULT_PADDING * 1.5f), fontSize = 16.sp, textAlign = TextAlign.Center, color = HighOrLowlight)
            }
            buttons()
          }
        }
      }
    }
  }

  fun showAlertDialogStacked(
    title: String,
    text: String? = null,
    confirmText: String = generalGetString(R.string.ok),
    onConfirm: (() -> Unit)? = null,
    dismissText: String = generalGetString(R.string.cancel_verb),
    onDismiss: (() -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null,
    destructive: Boolean = false
  ) {
    mainScope.launch {
      showAlert {
        AlertDialog(
          onDismissRequest = { onDismissRequest?.invoke(); hideAlert() },
          title = alertTitle(title),
          text = alertText(text),
          buttons = {
            Column(
              Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 16.dp, bottom = 2.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              TextButton(onClick = {
                onDismiss?.invoke()
                hideAlert()
              }) { Text(dismissText) }
              TextButton(onClick = {
                onConfirm?.invoke()
                hideAlert()
              }) { Text(confirmText, color = if (destructive) Color.Red else Color.Unspecified, textAlign = TextAlign.End) }
            }
          },
          backgroundColor = if (isInDarkTheme()) Color(0xff222222) else MaterialTheme.colors.background,
          shape = RoundedCornerShape(corner = CornerSize(25.dp))
        )
      }
    }
  }

  fun showAlertMsg(
    title: String, text: String? = null,
    confirmText: String = generalGetString(R.string.ok), onConfirm: (() -> Unit) = {}
  ) {
    val alertText: (@Composable () -> Unit)? = if (text == null) null else { -> Text(text) }
    showAlert {
      Dialog(onDismissRequest = this::hideAlert) {
        NoticeDialogLayout(title = title, message = text, ok = confirmText, onOKAction = {
          onConfirm.invoke()
          hideAlert()
        })
      }
    }
  }

  fun showAlertMsg(
    title: Int,
    text: Int? = null,
    confirmText: Int = R.string.ok,
    onConfirm: (() -> Unit) = {}
  ) = showAlertMsg(generalGetString(title), if (text != null) generalGetString(text) else null, generalGetString(confirmText), onConfirm)

  private fun alertTitle(title: String): (@Composable () -> Unit)? {
    return {
      Text(
        title,
        Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 20.sp
      )
    }
  }

  private fun alertText(text: String?): (@Composable () -> Unit)? {
    return if (text == null) {
      null
    } else {
      ({
        Text(
          text,
          Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
          fontSize = 16.sp,
          color = HighOrLowlight
        )
      })
    }
  }

  @Composable
  fun showInView() {
    if (presentAlert.value) alertView.value?.invoke()
    if (showLoading.value) loadingView.value?.invoke()
  }

  companion object {
    val shared = AlertManager()
  }
}