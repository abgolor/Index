package chat.echo.app.views.helpers

import SectionDivider
import SectionItemView
import SectionItemViewSpaceBetween
import SectionSpacer
import SectionView
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.TAG
import chat.echo.app.model.*
import chat.echo.app.model.ServerAddress.Companion.parseServerAddress
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.QRCode
import chat.echo.app.views.usersettings.PreferenceToggle
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ProtocolServerView(m: ChatModel, server: ServerCfg, serverProtocol: ServerProtocol, onUpdate: (ServerCfg) -> Unit, onDelete: () -> Unit) {
  var testing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  ProtocolServerLayout(
    testing,
    server,
    serverProtocol,
    testServer = {
      testing = true
      scope.launch {
        val res = testServerConnection(server, m)
        if (isActive) {
          onUpdate(res.first)
          testing = false
        }
      }
    },
    onUpdate,
    onDelete
  )
  if (testing) {
    Box(
      Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(
        Modifier
          .padding(horizontal = 2.dp)
          .size(30.dp),
        color = HighOrLowlight,
        strokeWidth = 2.5.dp
      )
    }
  }
}

@Composable
private fun ProtocolServerLayout(
  testing: Boolean,
  server: ServerCfg,
  serverProtocol: ServerProtocol,
  testServer: () -> Unit,
  onUpdate: (ServerCfg) -> Unit,
  onDelete: () -> Unit,
) {
  Column(
    Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(bottom = DEFAULT_PADDING)
  ) {
    AppBarTitle(stringResource(if (server.preset) R.string.smp_servers_preset_server else R.string.smp_servers_your_server))

    if (server.preset) {
      PresetServer(testing, server, testServer, onUpdate, onDelete)
    } else {
      CustomServer(testing, server, serverProtocol, testServer, onUpdate, onDelete)
    }
  }
}

@Composable
private fun PresetServer(
  testing: Boolean,
  server: ServerCfg,
  testServer: () -> Unit,
  onUpdate: (ServerCfg) -> Unit,
  onDelete: () -> Unit,
) {
  SectionView(stringResource(R.string.smp_servers_preset_address).uppercase()) {
    SelectionContainer {
      Text(
        server.server,
        Modifier.padding(start = DEFAULT_PADDING, top = 5.dp, end = DEFAULT_PADDING, bottom = 10.dp),
        style = TextStyle(
          fontFamily = FontFamily.Monospace, fontSize = 16.sp,
          color = HighOrLowlight
        )
      )
    }
  }
  SectionSpacer()
  UseServerSection(true, testing, server, testServer, onUpdate, onDelete)
}

@Composable
private fun CustomServer(
  testing: Boolean,
  server: ServerCfg,
  serverProtocol: ServerProtocol,
  testServer: () -> Unit,
  onUpdate: (ServerCfg) -> Unit,
  onDelete: () -> Unit,
) {
  val serverAddress = remember { mutableStateOf(server.server) }
  val valid = remember {
    derivedStateOf {
      with(parseServerAddress(serverAddress.value)) {
        this?.valid == true && this.serverProtocol == serverProtocol
      }
    }
  }
  SectionView(
    stringResource(R.string.smp_servers_your_server_address).uppercase(),
    icon = Icons.Outlined.ErrorOutline,
    iconTint = if (!valid.value) MaterialTheme.colors.error else Color.Transparent,
  ) {
    val testedPreviously = remember { mutableMapOf<String, Boolean?>() }
    TextEditor(
      Modifier.height(144.dp),
      text = serverAddress,
      border = false,
      fontSize = 16.sp,
      background = if (isInDarkTheme()) GroupDark else MaterialTheme.colors.background
    ) {
      testedPreviously[server.server] = server.tested
      onUpdate(server.copy(server = it, tested = testedPreviously[serverAddress.value]))
    }
  }
  SectionSpacer()
  UseServerSection(valid.value, testing, server, testServer, onUpdate, onDelete)
  SectionSpacer()

  if (valid.value) {
    SectionView(stringResource(R.string.smp_servers_add_to_another_device).uppercase()) {
      QRCode(serverAddress.value, Modifier.aspectRatio(1f))
    }
  }
}

@Composable
private fun UseServerSection(
  valid: Boolean,
  testing: Boolean,
  server: ServerCfg,
  testServer: () -> Unit,
  onUpdate: (ServerCfg) -> Unit,
  onDelete: () -> Unit,
) {
  SectionView(stringResource(R.string.smp_servers_use_server).uppercase()) {
    SectionItemViewSpaceBetween(testServer, disabled = !valid || testing) {
      Text(stringResource(R.string.smp_servers_test_server), color = if (valid && !testing) MaterialTheme.colors.onBackground else HighOrLowlight)
      ShowTestStatus(server)
    }
    SectionDivider()
    SectionItemView {
      val enabled = rememberUpdatedState(server.enabled)
      PreferenceToggle(stringResource(R.string.smp_servers_use_server_for_new_conn), enabled.value) { onUpdate(server.copy(enabled = it)) }
    }
    SectionDivider()
    SectionItemView(onDelete, disabled = testing) {
      Text(stringResource(R.string.smp_servers_delete_server), color = if (testing) HighOrLowlight else MaterialTheme.colors.error)
    }
  }
}

@Composable
fun ShowTestStatus(server: ServerCfg, modifier: Modifier = Modifier) =
  when (server.tested) {
    true -> Icon(Icons.Outlined.Check, null, modifier, tint = SimplexGreen)
    false -> Icon(Icons.Outlined.Clear, null, modifier, tint = MaterialTheme.colors.error)
    else -> Icon(Icons.Outlined.Check, null, modifier, tint = Color.Transparent)
  }

suspend fun testServerConnection(server: ServerCfg, m: ChatModel): Pair<ServerCfg, ProtocolTestFailure?> =
  try {
    val r = m.controller.testProtoServer(server.server)
    server.copy(tested = r == null) to r
  } catch (e: Exception) {
    Log.e(TAG, "testServerConnection ${e.stackTraceToString()}")
    server.copy(tested = false) to null
  }

fun serverHostname(srv: String): String =
  parseServerAddress(srv)?.hostnames?.firstOrNull() ?: srv
