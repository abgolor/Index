package chat.echo.app.views.chat.group

import SectionCustomFooter
import SectionDivider
import SectionItemView
import SectionSpacer
import SectionView
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.echo.app.R
import chat.echo.app.contacts.AddContactsItemView
import chat.echo.app.model.*
import chat.echo.app.ui.theme.*
import chat.echo.app.views.chat.ChatInfoToolbarTitle
import chat.echo.app.views.helpers.*
import chat.echo.app.views.newchat.AddGroupView
import chat.echo.app.views.newchat.group.SelectGroupTypeView
import chat.echo.app.views.usersettings.SettingsActionItem

@Composable
fun AddGroupMembersView(
  chatModel: ChatModel,
  groupInfo: GroupInfo,
  isZeroKnowledge: Boolean,
  close: () -> Unit
) {
  val selectedContacts = remember { mutableStateListOf<Long>() }
  val selectedRole = remember { mutableStateOf(GroupMemberRole.Member) }

  BackHandler(onBack = close)
  AddGroupMembersLayout(
    contactsToAdd = getContactsToAdd(chatModel),
    selectedContacts = selectedContacts,
    inviteMembers = {
      if(groupInfo != null){
        AlertManager.shared.showLoadingAlert()
        withApi {
          for (contactId in selectedContacts) {
            val member = chatModel.controller.apiAddMember(groupInfo.groupId, contactId, selectedRole.value)
            if (member != null) {
              chatModel.upsertGroupMember(groupInfo, member)
            } else {
              break
            }
          }
          AlertManager.shared.hideLoading()
          close.invoke()
        }
      }
    },
    clearSelection = { selectedContacts.clear() },
    addContact = { contactId -> if (contactId !in selectedContacts) selectedContacts.add(contactId) },
    removeContact = { contactId -> selectedContacts.removeIf { it == contactId } },
    isZeroKnowledge = isZeroKnowledge,
    close = close
  )
}

fun getContactsToAdd(chatModel: ChatModel): List<Contact> {
  val memberContactIds = chatModel.groupMembers
    .filter { it.memberCurrent }
    .mapNotNull { it.memberContactId }
  return chatModel.chats
    .asSequence()
    .map { it.chatInfo }
    .filterIsInstance<ChatInfo.Direct>()
    .map { it.contact }
    .filter { it.contactId !in memberContactIds }
    .sortedBy { it.displayName.lowercase() }
    .toList()
}

@Composable
fun AddGroupMembersLayout(
  contactsToAdd: List<Contact>,
  selectedContacts: SnapshotStateList<Long>,
  inviteMembers: () -> Unit,
  clearSelection: () -> Unit,
  addContact: (Long) -> Unit,
  removeContact: (Long) -> Unit,
  isZeroKnowledge: Boolean,
  close: () -> Unit
) {
  Column(
    Modifier
      .fillMaxSize()
      .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .background(Color.White)
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.Start,
  ) {
    Column(Modifier.fillMaxWidth()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(end = 15.dp, top = 10.dp, bottom = 10.dp)
      ) {
        IconButton(close) {
          Icon(
            Icons.Outlined.ArrowBackIos, stringResource(R.string.back), tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
        }
        Text(
          text = generalGetString(R.string.invite_participants),
          fontSize = 16.sp,
          modifier = Modifier.align(Alignment.Center),
          fontWeight = FontWeight.Medium,
          color = Color.Black
        )
        Text(
          text =  generalGetString(R.string.invite),
          fontSize = 15.sp,
          modifier = Modifier.align(Alignment.CenterEnd)
            .clickable {
              if (selectedContacts.isNotEmpty()) {
                inviteMembers()
              }
            },
          fontWeight = FontWeight.Medium,
          color = if (selectedContacts.isNotEmpty()) Color.Black else PreviewTextColor
        )
      }
    }
    Spacer(modifier = Modifier.padding(top = 10.dp))
    Row(
      Modifier.fillMaxWidth()
        .padding(top = 10.dp, start = 10.dp, end = 10.dp)
    ) {
      Text(
        text = generalGetString(R.string.participants) + ": " + selectedContacts.size,
        modifier = Modifier
          .align(Alignment.CenterVertically),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black
      )
      Spacer(Modifier.fillMaxWidth().weight(1f))
      if (selectedContacts.isNotEmpty()) {
        Text(
          text = generalGetString(R.string.clear_verb),
          modifier = Modifier
            .align(Alignment.CenterVertically)
            .clickable {
              //Clear contacts from the list of contacts
              clearSelection()
            },
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = Color.Black
        )
      }
    }
    Spacer(modifier = Modifier.padding(top = 5.dp))
    Text(
      text = generalGetString(R.string.add_one_or_more_participants),
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      fontSize = 15.sp,
      fontWeight = FontWeight.Medium,
      color = PreviewTextColor,
      textAlign = TextAlign.Start
    )
    Divider(color = DividerColor)
    Spacer(Modifier.padding(top = 10.dp))
    ContactList(contacts = contactsToAdd, selectedContacts, addContact, removeContact)
    /*    AppBarTitle(stringResource(R.string.button_add_members))
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          if(ChatInfo.Group(groupInfo) is ChatInfo.Group){
            ChatInfoToolbarTitle(
              ChatInfo.Group(groupInfo),
              imageSize = 60.dp,
              iconColor = if (isInDarkTheme()) SettingsSecondaryLight else GroupDark
            )
          }
        }
        SectionSpacer()

        if (contactsToAdd.isEmpty()) {
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              stringResource(R.string.no_contacts_to_add),
              Modifier.padding(),
              color = HighOrLowlight
            )
          }
        }
        else {
          SectionView {
            SectionItemView {
              RoleSelectionRow(groupInfo, selectedRole)
            }
            SectionDivider()
            InviteMembersButton(inviteMembers, disabled = selectedContacts.isEmpty())
          }
          SectionCustomFooter {
            InviteSectionFooter(selectedContactsCount = selectedContacts.count(), clearSelection)
          }
          SectionSpacer()

          SectionView {
            ContactList(contacts = contactsToAdd, selectedContacts, groupInfo, addContact, removeContact)
          }
          SectionSpacer()
        }*/
  }
}

@Composable
private fun RoleSelectionRow(groupInfo: GroupInfo, selectedRole: MutableState<GroupMemberRole>) {
  Row(
    Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    val values = GroupMemberRole.values().filter { it <= groupInfo.membership.memberRole }.map { it to it.text }
    ExposedDropDownSettingRow(
      generalGetString(R.string.new_member_role),
      values,
      selectedRole,
      icon = null,
      enabled = remember { mutableStateOf(true) },
      onSelected = { selectedRole.value = it }
    )
  }
}

@Composable
fun InviteMembersButton(onClick: () -> Unit, disabled: Boolean) {
  SettingsActionItem(
    Icons.Outlined.Check,
    stringResource(R.string.invite_to_group_button),
    click = onClick,
    textColor = MaterialTheme.colors.primary,
    iconColor = MaterialTheme.colors.primary,
    disabled = disabled,
  )
}

@Composable
fun InviteSectionFooter(selectedContactsCount: Int, clearSelection: () -> Unit) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (selectedContactsCount >= 1) {
      Text(
        String.format(generalGetString(R.string.num_contacts_selected), selectedContactsCount),
        color = HighOrLowlight,
        fontSize = 12.sp
      )
      Box(
        Modifier.clickable { clearSelection() }
      ) {
        Text(
          stringResource(R.string.clear_contacts_selection_button),
          color = MaterialTheme.colors.primary,
          fontSize = 12.sp
        )
      }
    } else {
      Text(
        stringResource(R.string.no_contacts_selected),
        color = HighOrLowlight,
        fontSize = 12.sp
      )
    }
  }
}

@Composable
fun ContactList(
  contacts: List<Contact>,
  selectedContacts: SnapshotStateList<Long>,
  addContact: (Long) -> Unit,
  removeContact: (Long) -> Unit
) {
  Column {
    contacts.forEachIndexed { index, contact ->
      AddContactsItemView(
        contact, addContact, removeContact,
        checked = selectedContacts.contains(contact.apiId)
      )
      if (index < contacts.lastIndex) {
        SectionDivider()
      }
    }
  }
}

@Composable
fun ContactCheckRow(
  contact: Contact,
  groupInfo: GroupInfo,
  addContact: (Long) -> Unit,
  removeContact: (Long) -> Unit,
  checked: Boolean
) {
  val prohibitedToInviteIncognito = !groupInfo.membership.memberIncognito && contact.contactConnIncognito
  val icon: ImageVector
  val iconColor: Color
  if (prohibitedToInviteIncognito) {
    icon = Icons.Filled.TheaterComedy
    iconColor = HighOrLowlight
  } else if (checked) {
    icon = Icons.Filled.CheckCircle
    iconColor = MaterialTheme.colors.primary
  } else {
    icon = Icons.Outlined.Circle
    iconColor = HighOrLowlight
  }
  SectionItemView(click = {
    if (prohibitedToInviteIncognito) {
      showProhibitedToInviteIncognitoAlertDialog()
    } else if (!checked)
      addContact(contact.apiId)
    else
      removeContact(contact.apiId)
  }) {
    ProfileImage(size = 36.dp, contact.image)
    Spacer(Modifier.width(DEFAULT_SPACE_AFTER_ICON))
    Text(
      contact.fullName, maxLines = 1, overflow = TextOverflow.Ellipsis,
      color = if (prohibitedToInviteIncognito) HighOrLowlight else Color.Unspecified
    )
    Spacer(Modifier.fillMaxWidth().weight(1f))
    Icon(
      icon,
      contentDescription = stringResource(R.string.icon_descr_contact_checked),
      tint = iconColor
    )
  }
}

fun showProhibitedToInviteIncognitoAlertDialog() {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(R.string.invite_prohibited),
    text = generalGetString(R.string.invite_prohibited_description),
    confirmText = generalGetString(R.string.ok),
  )
}
/*@Preview
@Composable
fun PreviewAddGroupMembersLayout() {
  SimpleXTheme {
    AddGroupMembersLayout(
      contactsToAdd = listOf(Contact.sampleData, Contact.sampleData, Contact.sampleData),
      selectedContacts = remember { mutableStateListOf() },
      selectedRole = remember { mutableStateOf(GroupMemberRole.Admin) },
      inviteMembers = {},
      clearSelection = {},
      addContact = {},
      removeContact = {},
      isZeroKnowledge = false,
      isNewGroup = true,
      close = {}
    )
  }
}*/
