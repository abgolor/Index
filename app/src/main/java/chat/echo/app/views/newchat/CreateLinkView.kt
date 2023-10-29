package chat.echo.app.views.newchat

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.ChatModel
import chat.echo.app.ui.theme.DEFAULT_PADDING
import chat.echo.app.ui.theme.HighOrLowlight
import chat.echo.app.views.helpers.withApi
import chat.echo.app.views.usersettings.UserAddressView

enum class CreateLinkTab {
  ONE_TIME,
  LONG_TERM
}

@Composable
fun CreateLinkView(m: ChatModel, initialSelection: CreateLinkTab) {
  val selection = remember { mutableStateOf(initialSelection) }
  val connReqInvitation = remember { mutableStateOf("") }
  val creatingConnReq = remember { mutableStateOf(false) }
  LaunchedEffect(selection.value) {
    if (selection.value == CreateLinkTab.ONE_TIME && connReqInvitation.value.isEmpty() && !creatingConnReq.value) {
      createInvitation(m, creatingConnReq, connReqInvitation)
    }
  }
  val tabTitles = CreateLinkTab.values().map {
    when {
      it == CreateLinkTab.ONE_TIME && connReqInvitation.value.isEmpty() -> stringResource(R.string.create_one_time_link)
      it == CreateLinkTab.ONE_TIME -> stringResource(R.string.one_time_link)
      it == CreateLinkTab.LONG_TERM -> stringResource(R.string.your_contact_address)
      else -> ""
    }
  }
  Column(
    Modifier
      .fillMaxHeight()
      .padding(horizontal = DEFAULT_PADDING),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column(Modifier.weight(1f)) {
      when (selection.value) {
        CreateLinkTab.ONE_TIME -> {
          AddContactView(m, connReqInvitation.value, m.incognito.value)
        }
        CreateLinkTab.LONG_TERM -> {
          UserAddressView(m)
        }
      }
    }
    TabRow(
      selectedTabIndex = selection.value.ordinal,
      backgroundColor = MaterialTheme.colors.background,
      contentColor = MaterialTheme.colors.primary,
    ) {
      tabTitles.forEachIndexed { index, it ->
        Tab(
          selected = selection.value.ordinal == index,
          onClick = {
            selection.value = CreateLinkTab.values()[index]
          },
          text = { Text(it, fontSize = 13.sp) },
          icon = {
            Icon(
              if (CreateLinkTab.ONE_TIME.ordinal == index) Icons.Outlined.RepeatOne else Icons.Outlined.AllInclusive,
              it
            )
          },
          selectedContentColor = MaterialTheme.colors.primary,
          unselectedContentColor = HighOrLowlight,
        )
      }
    }
  }
}



private fun createInvitation(m: ChatModel, creatingConnReq: MutableState<Boolean>, connReqInvitation: MutableState<String>) {
  withApi {
    val connReq = m.controller.apiAddContact()
    if (connReq != null) {
      connReqInvitation.value = connReq
  } else {
      creatingConnReq.value = false
    }
    }

}
