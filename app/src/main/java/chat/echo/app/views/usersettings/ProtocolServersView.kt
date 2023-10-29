package chat.echo.app.views.usersettings

import SectionDivider
import SectionItemView
import SectionSpacer
import SectionTextFooter
import SectionView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.model.ServerAddress.Companion.parseServerAddress
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import kotlinx.coroutines.launch

@Composable
fun ProtocolServersView(m: ChatModel, serverProtocol: ServerProtocol, close: () -> Unit) {
  var presetServers by remember { mutableStateOf(emptyList<String>()) }
  var servers by remember {
    mutableStateOf(m.userSMPServersUnsaved.value ?: emptyList())
  }
  val currServers = remember { mutableStateOf(servers) }
  val testing = rememberSaveable { mutableStateOf(false) }
  val serversUnchanged = remember { derivedStateOf { servers == currServers.value || testing.value } }
  val allServersDisabled = remember { derivedStateOf { servers.all { !it.enabled } } }
  val saveDisabled = remember {
    derivedStateOf {
      servers.isEmpty() ||
          servers == currServers.value ||
          testing.value ||
          !servers.all { srv ->
            val address = parseServerAddress(srv.server)
            address != null && uniqueAddress(srv, address, servers)
          } ||
          allServersDisabled.value
    }
  }

  LaunchedEffect(Unit) {
    val res = m.controller.getUserProtoServers(serverProtocol)
    if (res != null) {
      currServers.value = res.protoServers
      presetServers = res.presetServers
      if (servers.isEmpty()) {
        servers = currServers.value
      }
    }
  }

  fun showServer(server: ServerCfg) {
    ModalManager.shared.showModalCloseable(true) { close ->
      var old by remember { mutableStateOf(server) }
      val index = servers.indexOf(old)
      ProtocolServerView(
        m,
        old,
        serverProtocol,
        onUpdate = { updated ->
          val newServers = ArrayList(servers)
          newServers.removeAt(index)
          newServers.add(index, updated)
          old = updated
          servers = newServers
          m.userSMPServersUnsaved.value = servers
        },
        onDelete = {
          val newServers = ArrayList(servers)
          newServers.removeAt(index)
          servers = newServers
          m.userSMPServersUnsaved.value = servers
          close()
        })
    }
  }
  val scope = rememberCoroutineScope()
  ModalView(
    close = {
      if (saveDisabled.value) close()
      else showUnsavedChangesAlert({ saveServers(serverProtocol, currServers, servers, m, close) }, close)
    },
    background = if (isInDarkTheme()) MaterialTheme.colors.background else SettingsBackgroundLight
  ) {
    ProtocolServersLayout(
      serverProtocol,
      testing = testing.value,
      servers = servers,
      serversUnchanged = serversUnchanged.value,
      saveDisabled = saveDisabled.value,
      allServersDisabled = allServersDisabled.value,
      m.currentUser.value,
      addServer = {
        AlertManager.shared.showAlertDialogButtonsColumn(
          title = generalGetString(R.string.smp_servers_add),
          buttons = {
            Column {
              SectionItemView({
                AlertManager.shared.hideAlert()
                servers = servers + ServerCfg.empty
                // No saving until something will be changed on the next screen to prevent blank servers on the list
                showServer(servers.last())
              }) {
                Text(stringResource(R.string.smp_servers_enter_manually), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colors.primary)
              }
              SectionItemView({
                AlertManager.shared.hideAlert()
                ModalManager.shared.showModalCloseable { close ->
                  ScanProtocolServer {
                    close()
                    servers = servers + it
                    m.userSMPServersUnsaved.value = servers
                  }
                }
              }
              ) {
                Text(stringResource(R.string.smp_servers_scan_qr), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colors.primary)
              }
              val hasAllPresets = hasAllPresets(presetServers, servers, m)
              if (!hasAllPresets) {
                SectionItemView({
                  AlertManager.shared.hideAlert()
                  servers = (servers + addAllPresets(presetServers, servers, m)).sortedByDescending { it.preset }
                }) {
                  Text(stringResource(R.string.smp_servers_preset_add), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colors.onBackground)
                }
              }
            }
          }
        )
      },
      testServers = {
        scope.launch {
          testServers(testing, servers, m) {
            servers = it
            m.userSMPServersUnsaved.value = servers
          }
        }
      },
      resetServers = {
        servers = currServers.value ?: emptyList()
        m.userSMPServersUnsaved.value = null
      },
      saveSMPServers = {
        saveServers(serverProtocol, currServers, servers, m)
      },
      showServer = ::showServer,
    )

    if (testing.value) {
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
}

@Composable
private fun ProtocolServersLayout(
  serverProtocol: ServerProtocol,
  testing: Boolean,
  servers: List<ServerCfg>,
  serversUnchanged: Boolean,
  saveDisabled: Boolean,
  allServersDisabled: Boolean,
  currentUser: User?,
  addServer: () -> Unit,
  testServers: () -> Unit,
  resetServers: () -> Unit,
  saveSMPServers: () -> Unit,
  showServer: (ServerCfg) -> Unit,
) {
  Column(
    Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(bottom = DEFAULT_PADDING),
  ) {
    AppBarTitle(stringResource(if (serverProtocol == ServerProtocol.SMP) R.string.your_SMP_servers else R.string.your_XFTP_servers))

    SectionView(stringResource(if (serverProtocol == ServerProtocol.SMP) R.string.smp_servers else R.string.xftp_servers).uppercase()) {
      for (srv in servers) {
        SectionItemView({ showServer(srv) }, disabled = testing) {
          ProtocolServerView(serverProtocol, srv, servers, testing)
        }
        SectionDivider()
      }
      SettingsActionItem(
        Icons.Outlined.Add,
        stringResource(R.string.smp_servers_add),
        addServer,
        disabled = testing,
        textColor = if (testing) HighOrLowlight else MaterialTheme.colors.primary,
        iconColor = if (testing) HighOrLowlight else MaterialTheme.colors.primary
      )
    }
    SectionTextFooter(
      remember(currentUser?.displayName) {
        buildAnnotatedString {
          append(generalGetString(R.string.smp_servers_per_user) + " ")
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(currentUser?.displayName ?: "")
          }
          append(".")
        }
      }
    )
    SectionSpacer()
    SectionView {
      SectionItemView(resetServers, disabled = serversUnchanged) {
        Text(stringResource(R.string.reset_verb), color = if (!serversUnchanged) MaterialTheme.colors.onBackground else HighOrLowlight)
      }
      SectionDivider()
      val testServersDisabled = testing || allServersDisabled
      SectionItemView(testServers, disabled = testServersDisabled) {
        Text(stringResource(R.string.smp_servers_test_servers), color = if (!testServersDisabled) MaterialTheme.colors.onBackground else HighOrLowlight)
      }
      SectionDivider()
      SectionItemView(saveSMPServers, disabled = saveDisabled) {
        Text(stringResource(R.string.smp_servers_save), color = if (!saveDisabled) MaterialTheme.colors.onBackground else HighOrLowlight)
      }
    }
    SectionSpacer()
    SectionView {
      HowToButton()
    }
  }
}

@Composable
private fun ProtocolServerView(serverProtocol: ServerProtocol, srv: ServerCfg, servers: List<ServerCfg>, disabled: Boolean) {
  val address = parseServerAddress(srv.server)
  when {
    address == null || !address.valid || address.serverProtocol != serverProtocol || !uniqueAddress(srv, address, servers) -> InvalidServer()
    !srv.enabled -> Icon(Icons.Outlined.DoNotDisturb, null, tint = HighOrLowlight)
    else -> ShowTestStatus(srv)
  }
  Spacer(Modifier.padding(horizontal = 4.dp))
  val text = address?.hostnames?.firstOrNull() ?: srv.server
  if (srv.enabled) {
    Text(text, color = if (disabled) HighOrLowlight else MaterialTheme.colors.onBackground, maxLines = 1)
  } else {
    Text(text, maxLines = 1, color = HighOrLowlight)
  }
}

@Composable
private fun HowToButton() {
  val uriHandler = LocalUriHandler.current
  SettingsActionItem(
    Icons.Outlined.OpenInNew,
    stringResource(R.string.how_to_use_your_servers),
    { uriHandler.openUriCatching("https://github.com/simplex-chat/simplex-chat/blob/stable/docs/SERVER.md") },
    textColor = MaterialTheme.colors.primary,
    iconColor = MaterialTheme.colors.primary
  )
}

@Composable
fun InvalidServer() {
  Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colors.error)
}

private fun uniqueAddress(s: ServerCfg, address: ServerAddress, servers: List<ServerCfg>): Boolean = servers.all { srv ->
  address.hostnames.all { host ->
    srv.id == s.id || !srv.server.contains(host)
  }
}

private fun hasAllPresets(presetServers: List<String>, servers: List<ServerCfg>, m: ChatModel): Boolean =
  presetServers.all { hasPreset(it, servers) } ?: true

private fun addAllPresets(presetServers: List<String>, servers: List<ServerCfg>, m: ChatModel): List<ServerCfg> {
  val toAdd = ArrayList<ServerCfg>()
  for (srv in presetServers) {
    if (!hasPreset(srv, servers)) {
      toAdd.add(ServerCfg(srv, preset = true, tested = null, enabled = true))
    }
  }
  return toAdd
}

private fun hasPreset(srv: String, servers: List<ServerCfg>): Boolean =
  servers.any { it.server == srv }

private suspend fun testServers(testing: MutableState<Boolean>, servers: List<ServerCfg>, m: ChatModel, onUpdated: (List<ServerCfg>) -> Unit) {
  val resetStatus = resetTestStatus(servers)
  onUpdated(resetStatus)
  testing.value = true
  val fs = runServersTest(resetStatus, m) { onUpdated(it) }
  testing.value = false
  if (fs.isNotEmpty()) {
    val msg = fs.map { it.key + ": " + it.value.localizedDescription }.joinToString("\n")
    AlertManager.shared.showAlertMsg(
      title = generalGetString(R.string.smp_servers_test_failed),
      text = generalGetString(R.string.smp_servers_test_some_failed) + "\n" + msg
    )
  }
}

private fun resetTestStatus(servers: List<ServerCfg>): List<ServerCfg> {
  val copy = ArrayList(servers)
  for ((index, server) in servers.withIndex()) {
    if (server.enabled) {
      copy.removeAt(index)
      copy.add(index, server.copy(tested = null))
    }
  }
  return copy
}

private suspend fun runServersTest(servers: List<ServerCfg>, m: ChatModel, onUpdated: (List<ServerCfg>) -> Unit): Map<String, ProtocolTestFailure> {
  val fs: MutableMap<String, ProtocolTestFailure> = mutableMapOf()
  val updatedServers = ArrayList<ServerCfg>(servers)
  for ((index, server) in servers.withIndex()) {
    if (server.enabled) {
      val (updatedServer, f) = testServerConnection(server, m)
      updatedServers.removeAt(index)
      updatedServers.add(index, updatedServer)
      // toList() is important. Otherwise, Compose will not redraw the screen after first update
      onUpdated(updatedServers.toList())
      if (f != null) {
        fs[serverHostname(updatedServer.server)] = f
      }
    }
  }
  return fs
}

private fun saveServers(protocol: ServerProtocol, currServers: MutableState<List<ServerCfg>>, servers: List<ServerCfg>, m: ChatModel, afterSave: () -> Unit = {}) {
  withApi {
    if (m.controller.setUserProtoServers(protocol, servers)) {
      currServers.value = servers
      m.userSMPServersUnsaved.value = null
    }
    afterSave()
  }
}

private fun showUnsavedChangesAlert(save: () -> Unit, revert: () -> Unit) {
  AlertManager.shared.showAlertDialogStacked(
    title = generalGetString(R.string.smp_save_servers_question),
    confirmText = generalGetString(R.string.save_verb),
    dismissText = generalGetString(R.string.exit_without_saving),
    onConfirm = save,
    onDismiss = revert,
  )
}
