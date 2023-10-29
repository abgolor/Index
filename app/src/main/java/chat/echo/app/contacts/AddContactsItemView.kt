package chat.echo.app.contacts

import SectionItemView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chat.group.showProhibitedToInviteIncognitoAlertDialog
import chat.echo.app.views.chatlist.directCallAction
import chat.echo.app.views.chatlist.directChatAction
import chat.echo.app.views.helpers.*

@Composable
fun AddContactsItemView(
  contact: Contact,
  addContact: (Long) -> Unit,
  removeContact: (Long) -> Unit,
  checked: Boolean
) {
  //val prohibitedToInviteIncognito = !groupInfo.membership.memberIncognito && contact.contactConnIncognito
  val icon: ImageVector
  val iconColor: Color
/*  if (prohibitedToInviteIncognito) {
    icon = Icons.Filled.TheaterComedy
    iconColor = HighOrLowlight
  }*/
  if (checked) {
    icon = Icons.Filled.CheckCircle
    iconColor = Color.Black
  }
  else {
    icon = Icons.Outlined.Circle
    iconColor = PreviewTextColor
  }

  @Composable
  fun contactNameText(color: Color = Color.Unspecified) {
      Text(
        if(contact.fullName != "") contact.fullName else contact.localDisplayName,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontSize = 16.sp),
        fontWeight = FontWeight.SemiBold,
        color = color
      )
  }

  Row(Modifier.background(Color.White)
    .clickable {
 /*     if (prohibitedToInviteIncognito) {
        showProhibitedToInviteIncognitoAlertDialog()
      } else*/ if (!checked)
        addContact(contact.apiId)
      else
        removeContact(contact.apiId)
    }) {

    SectionItemView(
      click = {
/*      if (prohibitedToInviteIncognito) {
        showProhibitedToInviteIncognitoAlertDialog()
      } else*/
        if (!checked)
        addContact(contact.apiId)
      else
        removeContact(contact.apiId)
    },
      minHeight = 55.dp
    ) {
      ProfileImage(image = contact.image, size = 48.dp, color = Color.Black)
      Spacer(Modifier.width(DEFAULT_SPACE_AFTER_ICON))
      contactNameText(color = Color.Black)
      Spacer(Modifier.fillMaxWidth().weight(1f))
      Icon(
        icon,
        contentDescription = if(checked) generalGetString(R.string.icon_descr_contact_checked) else generalGetString(R.string.icon_descr_contact_unchecked),
        modifier = Modifier.size(26.dp),
        tint = iconColor
      )
    }
/*    Box(contentAlignment = Alignment.CenterStart) {
      ProfileImage(image = contact.image, size = 48.dp, color = Color.Black)
    }
    Column(
      modifier = Modifier
        .padding(horizontal = 8.dp)
        .weight(1F)
        .align(Alignment.CenterVertically)
    ) {
      contactNameText(Color.Black)
    }
    Box(
      contentAlignment = Alignment.CenterEnd
    ) {
      Icon(
        icon,
        contentDescription = if(checked) generalGetString(R.string.icon_descr_contact_checked) else generalGetString(R.string.icon_descr_contact_unchecked),
        tint = Color.Black,
        modifier = Modifier
          .padding(horizontal = 3.dp)
          .padding(vertical = 1.dp)
          .size(26.dp)
      )
    }*/
  }
}