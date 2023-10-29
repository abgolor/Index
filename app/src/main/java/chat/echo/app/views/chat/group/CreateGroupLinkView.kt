package chat.echo.app.views.chat.group

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.HighOrLowlight
import chat.echo.app.ui.theme.PreviewTextColor
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.QRCode

@Composable
fun GroupLinkViewNew(
  chatModel: ChatModel, groupInfo: GroupInfo, connReqContact: String?, memberRole: GroupMemberRole?, onGroupLinkUpdated: (Pair<String?, GroupMemberRole?>) -> Unit,
  close: () -> Unit
) {
  var groupLink by rememberSaveable { mutableStateOf(connReqContact) }
  val groupLinkMemberRole = rememberSaveable { mutableStateOf(memberRole) }
  var creatingLink by rememberSaveable { mutableStateOf(false) }
  val cxt = LocalContext.current
  val scaffoldState = rememberScaffoldState()
  fun createLink() {
    creatingLink = true
    withApi {
      val link = chatModel.controller.apiCreateGroupLink(groupInfo.groupId)
      if (link != null) {
        groupLink = link.first
        groupLinkMemberRole.value = link.second
        onGroupLinkUpdated(groupLink to groupLinkMemberRole.value)
      }
      creatingLink = false
    }
  }

  LaunchedEffect(Unit) {
    if (groupLink == null && !creatingLink) {
      createLink()
    }
  }

  Scaffold(
    topBar = {
      Column() {
        CreateGroupLinkToolBar(close = close, centered = true)
        Divider(color = PreviewTextColor)
      }
    },
    scaffoldState = scaffoldState,
    drawerGesturesEnabled = false,
    backgroundColor = Color.White
  ) {
    Box(modifier = Modifier.padding(it)) {
      Column(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .padding(start = 10.dp, end = 10.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
      ) {
        GroupLinkViewLayout(
          groupLink = groupLink,
          createLink = ::createLink,
          share = { shareText(cxt, groupLink ?: return@GroupLinkViewLayout) },
          updateLink = {
            val role = groupLinkMemberRole.value
            if (role != null) {
              withBGApi {
                val link = chatModel.controller.apiGroupLinkMemberRole(groupInfo.groupId, role)
                if (link != null) {
                  groupLink = link.first
                  groupLinkMemberRole.value = link.second
                  onGroupLinkUpdated(groupLink to groupLinkMemberRole.value)
                }
              }
            }
          },
          deleteLink = {
            AlertManager.shared.showAlertMsg(
              title = generalGetString(R.string.delete_link_question),
              text = generalGetString(R.string.all_group_members_will_remain_connected),
              confirmText = generalGetString(R.string.delete_verb),
              onConfirm = {
                withApi {
                  val r = chatModel.controller.apiDeleteGroupLink(groupInfo.groupId)
                  if (r) {
                    groupLink = null
                    onGroupLinkUpdated(null to null)
                  }
                }
              }
            )
          }
        )
      }
    }
  }
  /* GroupLinkViewLayout(
     groupLink = groupLink,
     createAddress = ::createLink,
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
     share = { shareText(cxt, groupLink ?: return@GroupLinkViewLayout) },
     deleteAddress = {
       AlertManager.shared.showAlertMsg(
         title = generalGetString(R.string.delete_link_question),
         text = generalGetString(R.string.all_group_members_will_remain_connected),
         confirmText = generalGetString(R.string.delete_verb),
         onConfirm = {
           withApi {
             val r = chatModel.controller.apiDeleteGroupLink(groupInfo.groupId)
             if (r) {
               groupLink = null
               onGroupLinkUpdated(null to null)
             }
           }
         }
       )
     }
   )*/
}

@Composable
fun CreateGroupLinkToolBar(close: () -> Unit, centered: Boolean) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(AppBarHeight)
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color.White)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(
        Modifier
          .fillMaxHeight()
          .width(TitleInsetWithIcon - AppBarHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(close) {
          Icon(
            Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(22.dp)
          )
        }
      }
      val startPadding = TitleInsetWithIcon
      val endPadding = (0 * 50f).dp
      Box(
        Modifier
          .fillMaxWidth()
          .padding(
            start = if (centered) kotlin.math.max(startPadding.value, endPadding.value).dp else startPadding,
            end = if (centered) kotlin.math.max(startPadding.value, endPadding.value).dp else endPadding
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = generalGetString(R.string.create_group_link),
          fontSize = 18.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.Medium,
          color = Color.Black
        )
      }
    }
  }
}

@Composable
fun GroupLinkViewLayout(
  groupLink: String?,
  createLink: () -> Unit,
  share: () -> Unit,
  updateLink: () -> Unit,
  deleteLink: () -> Unit
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
      if (groupLink == null) {
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
              text = generalGetString(R.string.no_group_link_found),
              textAlign = TextAlign.Center,
              fontSize = 14.sp,
              fontWeight = FontWeight.Normal,
              color = Color.Black
            )
            Spacer(modifier = Modifier.size(5.dp))
            Text(
              text = generalGetString(R.string.generate_group_link),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              color = Color.Black,
              textDecoration = TextDecoration.Underline,
              modifier = Modifier.clickable {
                createLink()
              }
            )
          }
          /*if (!stopped && !newChatSheetState.collectAsState().value.isVisible()) {
           OnboardingButtons(showNewChatSheet)
         }*/
          //Text(stringResource(R.string.you_have_no_contacts), Modifier.align(Alignment.Center), color = HighOrLowlight)
        }
      } else {

            QRCode(
              groupLink, Modifier
                .size(300.dp)
              //  .weight(1f, fill = false).aspectRatio(1f)
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(15.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(vertical = 20.dp)
            ) {
              IconButton(onClick = updateLink) {
                Icon(
                  imageVector = Icons.Outlined.Refresh,
                  contentDescription = generalGetString(R.string.refresh),
                  modifier = Modifier.size(30.dp),
                  tint = Color.Black
                )
              }
              IconButton(onClick = {
                share()
              }) {
                Icon(
                  imageVector = Icons.Outlined.IosShare,
                  contentDescription = generalGetString(R.string.refresh),
                  modifier = Modifier.size(30.dp),
                  tint = Color.Black
                )
              }
              IconButton(onClick = deleteLink) {
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