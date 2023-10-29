package chat.echo.app.views.chat.group

import SectionDivider
import SectionItemView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ContentAlpha.disabled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.contacts.AddContactsItemView
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.helpers.*
import java.util.*

@Composable
fun ChangeRoleView(
  chatModel: ChatModel,
  roles: List<GroupMemberRole>,
  member: GroupMember,
  groupInfo: GroupInfo,
  selectedRole: MutableState<GroupMemberRole>,
  close: () -> Unit,
  closeAll: () -> Unit
) {
  val scaffoldState = rememberScaffoldState()
  val newRole = remember { mutableStateOf(member.memberRole) }

  Scaffold(
    topBar = {
      Column() {
        ChangeRoleToolBar(close = close, centered = true)
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
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
      ) {
        Spacer(modifier = Modifier.padding(vertical = 10.dp))
        MemberRoleView(roles = roles, selectedRole = selectedRole, onSelected = {
          if (it == newRole.value) return@MemberRoleView
          val prevValue = newRole.value
          newRole.value = it
          updateMemberRoleDialog(it, member, onDismiss = {
            newRole.value = prevValue
          }) {
            withApi {
              kotlin.runCatching {
                val mem = chatModel.controller.apiMemberRole(groupInfo.groupId, member.groupMemberId, it)
                chatModel.upsertGroupMember(groupInfo, mem)
                close.invoke()
                closeAll.invoke()
              }.onFailure {
                newRole.value = prevValue
                close.invoke()
                closeAll.invoke()
              }
            }
          }
        })
      }
    }
  }
}

private fun updateMemberRoleDialog(
  newRole: GroupMemberRole,
  member: GroupMember,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(R.string.change_member_role_question),
    text = if (member.memberCurrent)
      String.format(generalGetString(R.string.member_role_will_be_changed_with_notification), newRole.text)
    else
      String.format(generalGetString(R.string.member_role_will_be_changed_with_invitation), newRole.text),
    confirmText = generalGetString(R.string.change_verb),
    onDismiss = onDismiss,
    onConfirm = onConfirm,
    onDismissRequest = onDismiss
  )
}

@Composable
fun ChangeRoleToolBar(close: () -> Unit, centered: Boolean) {
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
          text = generalGetString(R.string.change_member_role),
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
fun MemberRoleView(  roles: List<GroupMemberRole>,
  selectedRole: MutableState<GroupMemberRole>,
  onSelected: (GroupMemberRole) -> Unit){
  roles.forEachIndexed { index, role ->
    MemberRoleItemView(memberRoleText = role.text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }, textColor = Color.Black, onSelected = {onSelected(role)}, checked = role == selectedRole.value)
    if (index < roles.lastIndex) {
      SectionDivider()
    }
  }
}

@Composable
fun MemberRoleItemView(
  memberRoleText: String,
  textColor: Color,
  onSelected: () -> Unit,
  checked: Boolean
) {
  val icon: ImageVector
  val iconColor: Color
  if (checked) {
    icon = Icons.Filled.CheckCircle
    iconColor = Color.Black
  }
  else {
    icon = Icons.Outlined.Circle
    iconColor = PreviewTextColor
  }

  Row(Modifier.background(Color.White)
    .clickable {
      onSelected()
    }) {
    SectionItemView() {
      Text(memberRoleText, color = textColor)
      Spacer(Modifier.fillMaxWidth().weight(1f))
      Icon(
        icon,
        contentDescription = if(checked) generalGetString(R.string.member_role_checked) else generalGetString(R.string.member_role_unchecked),
        modifier = Modifier.size(26.dp),
        tint = iconColor
      )
    }
  }
}
