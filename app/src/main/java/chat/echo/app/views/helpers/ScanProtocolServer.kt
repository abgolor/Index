package chat.echo.app.views.helpers

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.echo.app.R
import chat.echo.app.model.ServerAddress.Companion.parseServerAddress
import chat.echo.app.model.ServerCfg
import chat.echo.app.ui.theme.DEFAULT_PADDING
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.QRCodeScanner
import com.google.accompanist.permissions.rememberPermissionState

@Composable
fun ScanProtocolServer(onNext: (ServerCfg) -> Unit) {
  val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
  LaunchedEffect(Unit) {
    cameraPermissionState.launchPermissionRequest()
  }
  ScanProtocolServerLayout(onNext)
}

@Composable
private fun ScanProtocolServerLayout(onNext: (ServerCfg) -> Unit) {
  Column(
    Modifier
      .fillMaxSize()
      .padding(horizontal = DEFAULT_PADDING)
  ) {
    AppBarTitle(stringResource(R.string.smp_servers_scan_qr), false)
    Box(
      Modifier
        .fillMaxWidth()
        .aspectRatio(ratio = 1F)
        .padding(bottom = 12.dp)
    ) {
      QRCodeScanner { text ->
        val res = parseServerAddress(text)
        if (res != null) {
          onNext(ServerCfg(text, false, null, true))
        } else {
          AlertManager.shared.showAlertMsg(
            title = generalGetString(R.string.smp_servers_invalid_address),
            text = generalGetString(R.string.smp_servers_check_address)
          )
        }
      }
    }
  }
}
